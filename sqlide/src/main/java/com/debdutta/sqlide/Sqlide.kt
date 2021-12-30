package com.debdutta.sqlide

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

typealias Block = Sqlide.() -> Unit
class Sqlide {
    private constructor()
    private var blocks: ArrayList<Block> = ArrayList()
    private var context: Context? = null
    private var db_name = "sqlide_db"
    private var running: AtomicBoolean = AtomicBoolean(false)
    private var d: SQLiteDatabase? = null

    companion object{
        fun initialize(context: Application,db_name: String = ""){
            if(db_name.isNotEmpty()){
                instance.db_name = db_name
            }
            instance.context = context
        }

        private var _instance: Sqlide? = null
        private val instance: Sqlide
        get(){
            if(_instance==null){
                _instance = Sqlide()
            }
            return _instance!!
        }

        fun execute(block: Sqlide.() -> Unit){
            instance.blocks.add(block)
            if(instance.running.get()){
                return
            }
            instance.running.set(true)
            newSingleThreadContext("ctx").use{
                instance.d = instance.db
                while (instance.blocks.size>0){
                    val b = instance.blocks.removeAt(0)
                    instance.d?.beginTransaction()
                    try {
                        b(instance)
                        instance.d?.setTransactionSuccessful()
                    } finally {
                        instance.d?.endTransaction()
                        instance.running.set(false)
                    }
                }
                instance.d?.close()
                instance.d = null
            }
        }
    }



    private val db: SQLiteDatabase
    get(){
        return (context as Context).openOrCreateDatabase(db_name,MODE_PRIVATE,null)
    }

    fun exec(sql: String){
        d?.execSQL(sql)
    }
}