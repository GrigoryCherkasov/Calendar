package ws.grigory.calendar.settings

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ws.grigory.calendar.R
import ws.grigory.calendar.UPDATE

class SettingsActivity : AppCompatActivity() {
    private lateinit var yearAdapter: YearAdapter
    private lateinit var years: ArrayList<Int>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        setContentView(R.layout.activity_year_list)

        val yearListView: RecyclerView = findViewById(R.id.yearList)
        val addButton: FloatingActionButton = findViewById(R.id.fab)

        val data = CalendarStorage(this).getYears()

        if (data == null){
            years = ArrayList()
        }  else {
            years = ArrayList()
            years.addAll(data)
        }

        yearListView.layoutManager = LinearLayoutManager(this)
        yearAdapter = YearAdapter(this, years)
        yearListView.adapter = yearAdapter
        addButton.setOnClickListener {
            YearDialog(this, years, yearAdapter).show(this.supportFragmentManager, null)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressed()
                }
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        backPressed()
        return true
    }

    private fun backPressed() {
        intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(UPDATE, true)
        this.sendBroadcast(intent)
        finish()
    }
}