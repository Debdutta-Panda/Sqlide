package com.debdutta.sqlide

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.*
import android.util.Log
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Sqlide.initialize(this.application)
        Sqlide.errorCallback = {
            Log.d("sqlide_debug","error = $it")
        }
        Sqlide.transact {
            /*execute("""
                CREATE TABLE IF NOT EXISTS contacts (
                	contact_id INTEGER PRIMARY KEY AUTOINCREMENT,
                	first_name TEXT NOT NULL,
                	last_name TEXT NOT NULL,
                	email TEXT,
                	phone TEXT
                );
            """.trimIndent())
            val count = columnCount("contacts")

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@MainActivity, count.toString(), Toast.LENGTH_SHORT).show()
            }
            execute("""
                INSERT INTO contacts (first_name,last_name)
                VALUES( 'deb','pan');
            """.trimIndent())
            Log.d("sqlide_debug","${table("contacts").exists}")
            Log.d("sqlide_debug","${query("select * from contacts").sheet[10][1]}")*/
            var table = createTable("""
                CREATE TABLE if not exists contacts_news (
                	contact_id INTEGER PRIMARY KEY AUTOINCREMENT,
                	first_name TEXT NOT NULL,
                	last_name TEXT NOT NULL,
                	email TEXT,
                	phone TEXT
                );
            """.trimIndent())
            execute("""
                INSERT INTO contacts_news (first_name,last_name)
                VALUES( '${System.currentTimeMillis()}','pan');
            """.trimIndent())
            Log.d("sqlide_debug","${table.sheet()[20][1]}")

        }
    }
}