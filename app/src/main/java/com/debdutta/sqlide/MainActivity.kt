package com.debdutta.sqlide

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.time.LocalDate


class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    private var dateStart = LocalDate.now()
    @RequiresApi(Build.VERSION_CODES.O)
    private var dateEnd = LocalDate.from(dateStart)
    private var tv_text: TextView? = null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_text = findViewById(R.id.tv_text)
        tv_text?.setOnClickListener {
            lifecycleScope.launch() {
                process()
            }
        }
    }

    suspend fun process(){
        val name = Sqlide{
            tables[30].name
        }
        Log.d("sqlide_debug","$name")
    }
}