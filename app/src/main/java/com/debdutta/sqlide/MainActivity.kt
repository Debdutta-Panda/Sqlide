package com.debdutta.sqlide

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


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
                /*val result = Sqlide{
                    Thread.sleep(5000)
                    table("contacts").exists
                }
                Log.d("sqlide_debug","result 1 = ${result.toString()}")*/
                Toast.makeText(this@MainActivity, "1="+Sqlide{
                    Thread.sleep(5000)
                    table("contacts").exists
                }.toString(), Toast.LENGTH_SHORT).show()
            }

            lifecycleScope.launch() {
                val result = Sqlide{
                    query("select * from contacts").columnCount
                }
                Log.d("sqlide_debug","result 2 = ${result.toString()}")
                Toast.makeText(this@MainActivity, "2="+result.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}