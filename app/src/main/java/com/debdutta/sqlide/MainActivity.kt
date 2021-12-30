package com.debdutta.sqlide

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Sqlide.initialize(this.application)

        Sqlide.execute {
            exec("""
                CREATE TABLE contacts (
                	contact_id INTEGER PRIMARY KEY,
                	first_name TEXT NOT NULL,
                	last_name TEXT NOT NULL,
                	email TEXT NOT NULL UNIQUE,
                	phone TEXT NOT NULL UNIQUE
                );
            """.trimIndent())
        }
    }
}