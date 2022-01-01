package com.debdutta.sqlide

import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import android.database.Cursor
import android.util.Log
import java.io.Closeable
import java.lang.Exception
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


typealias Block = Sqlide.() -> Any?



inline fun SQLiteDatabase.transact(block: SQLiteDatabase.() -> Any?): Any? {
    beginTransaction()
    return try {
        try {
            return block()
        } finally {
            setTransactionSuccessful()
        }
    }
    catch(e: Exception){
        e
    }
    finally {
        endTransaction()
    }
}

fun <T> ArrayList<T>.eat(block:(T)->Unit){
    while (this.size>0){
        block(this.removeAt(0))
    }
}
class Sqlide {
    private var blocks: ArrayList<Block> = ArrayList()
    private var context: Context? = null
    private var db_name = "sqlide_db"
    private var running: AtomicBoolean = AtomicBoolean(false)
    private var d: SQLiteDatabase? = null


    companion object {
        fun initialize(context: Application, db_name: String = "") {
            if (db_name.isNotEmpty()) {
                instance.db_name = db_name
            }
            instance.context = context
        }

        private var _instance: Sqlide? = null
        private val instance: Sqlide
            get() {
                if (_instance == null) {
                    _instance = Sqlide()
                }
                apply { }
                return _instance!!
            }

        data class BlockData(
            val block: Block,
            val callback: ((Any?) -> Unit)?
        )

        val blocks = ArrayList<BlockData>()
        fun transaction(block: Sqlide.() -> Any?,callback: ((Any?)->Unit)? = null){
            blocks.add(
                BlockData(
                    block, callback
                )
            )
            if(instance.running.get()){
                return
            }
            process()
        }

        private fun process() {
            Thread{
                if(blocks.size>0 && !instance.running.get()){
                    val block = blocks.removeAt(0)
                    instance.running.set(true)
                    instance.d = instance.db
                    instance.d?.use { db ->
                        var result = db.transact {
                            block.block(instance)
                        }
                        block.callback?.invoke(result)
                    }
                    instance.d = null
                    instance.running.set(false)
                    process()
                }

            }.start()

        }

        suspend operator fun invoke(block: Block?): Any? =
            suspendCoroutine { cont ->
                transaction({
                    block?.invoke(this)
                },{
                    cont.resume(it)
                })
            }

        suspend fun sqlide(block: Block?): Any? =
            suspendCoroutine { cont ->
                transaction({
                    block?.invoke(this)
                },{
                    cont.resume(it)
                })
            }
    }

    fun columnCount(table_name: String): Int {
        val dbCursor = d?.query(table_name, null, null, null, null, null, null)
        dbCursor.use { dbCursor ->
            return dbCursor?.columnCount ?: 0
        }
    }

    private val db: SQLiteDatabase
        get() {
            return (context as Context).openOrCreateDatabase(db_name, MODE_PRIVATE, null)
        }

    fun execute(sql: String) {
        d?.execSQL(sql)
    }

    fun query(sql: String): Kursor {
        return Kursor(d?.rawQuery(sql, null))
    }

    fun createTable(query: String): Table{
        return Table.create(query, d)
    }

    class Sheet(private val kursor: Kursor){
        private val header = ArrayList<String>()
        private val body = ArrayList<ArrayList<Any?>>()

        operator fun get(pos: Int): Row{
            return Row(body.getOrNull(pos))
        }

        class Row(var array: ArrayList<Any?>?){
            val size: Int
            get(){
                return array?.size?:0
            }
            operator fun get(pos: Int): Any?{
                return array?.getOrNull(pos)
            }
        }

        init {
            val rowCount = kursor.count
            val colCount = kursor.columnCount
            for(i in 0 until colCount){
                header.add(kursor.columnName(i))
            }
            for(i in 0 until rowCount){
                body.add(ArrayList())
                for(j in 0 until colCount){
                    body[i].add(kursor.cell(i,j))
                }
            }
        }
    }

    class Kursor(private val cursor: Cursor?) : Closeable {
        fun cell(row: Int, col: Int, def: Any? = null): Any?{
            to(row)
            return when(type(col)){
                TYPE.FIELD_TYPE_UNKNOWN,
                TYPE.FIELD_TYPE_NULL -> null
                TYPE.FIELD_TYPE_INTEGER -> int(col,(def as? Int)?:0)
                TYPE.FIELD_TYPE_FLOAT -> float(col,(def as? Float)?:0f)
                TYPE.FIELD_TYPE_STRING -> string(col,(def as? String)?:"")
                TYPE.FIELD_TYPE_BLOB -> blob(col)
            }
        }

        fun cellInt(row: Int, col: Int, def: Int = 0): Int{return (cell(row,col,def) as? Int)?:def}
        fun cellFloat(row: Int, col: Int, def: Float = 0f): Float{return (cell(row,col,def) as? Float)?:def}
        fun cellString(row: Int, col: Int, def: String = ""): String{return (cell(row,col,def) as? String)?:def}
        fun cellBlob(row: Int, col: Int): ByteArray{return (cell(row,col) as? ByteArray)?: ByteArray(0)
        }

        val count: Int
            get() {
                return cursor?.count ?: 0
            }
        val columnCount: Int
            get() {
                return cursor?.columnCount ?: 0
            }

        val position: Int
            get() {
                return cursor?.position ?: 0
            }

        fun to(pos: Int) = cursor?.moveToPosition(pos)
        fun toFirst() = cursor?.moveToFirst()
        fun toLast() = cursor?.moveToLast()
        fun toNext() = cursor?.moveToNext()
        fun toPrev() = cursor?.moveToPrevious()
        val isFirst: Boolean
            get() {
                return cursor?.isFirst == true
            }
        val isLast: Boolean
            get() {
                return cursor?.isLast == true
            }
        val isBeforeFirst: Boolean
            get() {
                return cursor?.isBeforeFirst == true
            }
        val isAfterLast: Boolean
            get() {
                return cursor?.isAfterLast == true
            }

        fun columnIndex(columnName: String): Int {
            return cursor?.getColumnIndex(columnName) ?: -1
        }

        fun columnName(pos: Int): String {
            return cursor?.getColumnName(pos) ?: ""
        }

        val columnNames: Array<String>
            get() {
                return cursor?.columnNames ?: Array(0) { "" }
            }

        fun blob(pos: Int): ByteArray {
            return cursor?.getBlob(pos) ?: ByteArray(0)
        }

        fun string(pos: Int, def: String = ""): String {return cursor?.getString(pos) ?: def}
        fun short(pos: Int, def: Short = 0): Short {return cursor?.getShort(pos) ?: def}
        fun int(pos: Int, def: Int = 0): Int {return cursor?.getInt(pos) ?: def}
        fun long(pos: Int, def: Long = 0): Long {return cursor?.getLong(pos) ?: def}
        fun float(pos: Int, def: Float = 0f): Float {return cursor?.getFloat(pos) ?: def}
        fun double(pos: Int, def: Double = 0.0): Double {return cursor?.getDouble(pos) ?: def}
        enum class TYPE{
            FIELD_TYPE_UNKNOWN,
            FIELD_TYPE_NULL,
            FIELD_TYPE_INTEGER,
            FIELD_TYPE_FLOAT,
            FIELD_TYPE_STRING,
            FIELD_TYPE_BLOB
        }

        fun type(pos: Int): TYPE {
            return when(cursor?.getType(pos)){
                Cursor.FIELD_TYPE_NULL->TYPE.FIELD_TYPE_NULL
                Cursor.FIELD_TYPE_INTEGER->TYPE.FIELD_TYPE_INTEGER
                Cursor.FIELD_TYPE_FLOAT->TYPE.FIELD_TYPE_FLOAT
                Cursor.FIELD_TYPE_STRING->TYPE.FIELD_TYPE_STRING
                Cursor.FIELD_TYPE_BLOB->TYPE.FIELD_TYPE_BLOB
                else-> TYPE.FIELD_TYPE_UNKNOWN
            }
        }

        fun isNull(pos: Int): Boolean{
            return cursor?.isNull(pos)==true
        }

        val isClosed: Boolean
        get(){
            return cursor?.isClosed==true
        }

        override fun close() {
            cursor?.close()
        }

        val sheet: Sheet
            get(){
                return Sheet(this)
            }
    }

    class Table(val name: String, private val db: SQLiteDatabase?){
        val exists: Boolean
        get(){
            val query =
                "select DISTINCT tbl_name from sqlite_master where tbl_name = '$name'"
            db?.rawQuery(query, null).use { cursor ->
                if (cursor != null) {
                    if (cursor.count > 0) {
                        return true
                    }
                }
                return false
            }
        }

        fun sheet(query: String = "select * from $name"): Sheet{
            return Kursor(db?.rawQuery(query,null)).sheet
        }

        companion object{
            fun create(query: String, db: SQLiteDatabase?): Table{
                var name = query.tableName
                val table = Table(name,db)
                if(name.isNotEmpty()){
                    db?.execSQL(query)
                }
                return table
            }

            val String.tableName: String
            get(){
                val regex = "create\\s+table\\s+(if\\s+not\\s+exists\\s+)?(\\w+).*".toRegex()
                val match = regex.find(this.lowercase())
                var name = (match?.destructured?.component2())?:""
                val index = this.lowercase().indexOf(name)
                if(index==-1){
                    return ""
                }
                val length = name.length
                return this.substring(index until index+length)
            }
        }
    }

    fun table(name: String): Table{
        if(name.contains("\\s+".toRegex()))
        {
            return Table.create(name,d)
        }
        return Table(name,d)
    }
}

fun Sqlide.Kursor.TYPE.name(): String{
    return when(this){
        Sqlide.Kursor.TYPE.FIELD_TYPE_NULL->"null"
        Sqlide.Kursor.TYPE.FIELD_TYPE_INTEGER->"int"
        Sqlide.Kursor.TYPE.FIELD_TYPE_FLOAT->"float"
        Sqlide.Kursor.TYPE.FIELD_TYPE_STRING->"string"
        Sqlide.Kursor.TYPE.FIELD_TYPE_BLOB->"blob"
        Sqlide.Kursor.TYPE.FIELD_TYPE_UNKNOWN->"unknown"
    }
}