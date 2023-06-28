package ws.grigory.calendar.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import ws.grigory.calendar.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val CALENDAR_URL = "content://com.android.calendar/events"
private const val DTSTART = "dtstart"
private const val DTSTART_INTERVAL = "dtstart >= ? AND dtstart < ?"
private const val DATABASE_NAME = "CALENDAR"    // Database Name
private const val TABLE_NAME = "NON_WORK_DAY"   // Table Name
private const val DATABASE_VERSION = 1    // Database Version
private const val DATE = "DATE"     // Column 1
private const val DESCRIPTION = "DESCRIPTION"    //Column 2
private const val YEAR = "cast((DATE / 10000) as int) AS YEAR"
private const val DATE_INTERVAL = "DATE >= ? AND DATE < ?"

private const val CREATE_TABLE =
    "CREATE TABLE $TABLE_NAME ($DATE INTEGER, $DESCRIPTION VARCHAR(255));"
private const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"


class CalendarStorage(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val context: Context
    private val zoneId: ZoneId = ZoneId.systemDefault()

    init {
        this.context = context
    }

    @SuppressLint("Range")
    fun nonWorkDaysSet(now: LocalDate): Set<Int>? {
        var result: Set<Int>? = null
        val database = readableDatabase
        val cursor = database.query(TABLE_NAME, arrayOf(DATE), DATE_INTERVAL,
            arrayOf(
                toMonthKey(now.minusMonths(1)),
                toMonthKey(now.plusMonths(2))
            ), null, null, null)

        if (cursor.count > 0) {
            result = HashSet()
            while (cursor.moveToNext()) {
                result.add(cursor.getInt(0))
            }
        }
        cursor.close()
        database.close()
        return result
    }

    fun getCalendarEventsSet(now: LocalDate): Set<Int>? {

        var result: Set<Int>? = null
        try {
            val cursor = context.contentResolver
                .query(Uri.parse(CALENDAR_URL), arrayOf(DTSTART), DTSTART_INTERVAL,
                    arrayOf(
                        toCalendarMonth(now.minusMonths(1)),
                        toCalendarMonth(now.plusMonths(2))
                    ), null)

            if (cursor != null) {
                if (cursor.count > 0) {
                    result = HashSet()
                    while (cursor.moveToNext()) {
                        result.add(toDateKey(cursor.getLong(0)))
                    }
                }
                cursor.close()
            }
        } catch (ignore: Exception) {}
        return result
    }

    @SuppressLint("Range")
    fun getYears(): List<Int>? {
        val database = readableDatabase
        val cursor = database.query(true, TABLE_NAME, arrayOf(YEAR), null,
            null, null, null, "1", null)
        var result: ArrayList<Int>? = null
        if (cursor.count > 0) {
            result = ArrayList()

            while (cursor.moveToNext()) {
                result.add(cursor.getInt(0))
            }
        }
        cursor.close()
        database.close()
        return result
    }

    fun reload(activity: Activity, year: Int){
        read(activity, year, null, null)
    }

    fun load(activity: Activity, year: Int, years: ArrayList<Int>, yearAdapter: YearAdapter){
        read(activity, year, years, yearAdapter)
    }

    private fun read(activity: Activity, year: Int, years: ArrayList<Int>?, yearAdapter: YearAdapter?) {
        val loadingProgressLayout = activity.findViewById<LinearLayout>(R.id.loadingProgressLayout)
        val loadingProgressBar = activity.findViewById<ProgressBar>(R.id.loadingProgressBar)
        val handler = Handler(Looper.myLooper()!!)

        Thread {
            var isFail = true
            handler.post {
                loadingProgressLayout.visibility = View.VISIBLE
                loadingProgressBar.isEnabled = false
            }

            val nonWorkingDays = getNonWorkingDays(year)
            if (nonWorkingDays != null) {
                delete(year)
                insert(year, nonWorkingDays)
                years?.add(year)
                isFail = false
            }

            handler.post {
                loadingProgressLayout.visibility = View.INVISIBLE
                loadingProgressBar.isEnabled = true
                if(isFail) {
                    showWarning(activity)
                } else {
                    if (years != null && yearAdapter != null) {
                        yearAdapter.notifyItemInserted(years.size - 1)
                    }
                }
            }
        }.start()

    }

    fun delete(position: Int, years: ArrayList<Int>, yearAdapter: YearAdapter) {
        delete(years[position])
        years.removeAt(position)
        yearAdapter.notifyItemRemoved(position)
    }

    private fun delete(year: Int) {
        val database = writableDatabase
        //db.delete(TABLE_NAME, null, null)
        database.delete(TABLE_NAME, DATE_INTERVAL, arrayOf(
            (year * 10000).toString(),
            ((year + 1) * 10000).toString()))
        database.close()
    }

    private fun insert(year: Int, dates: List<Pair<IntArray, String>>?) {
        if (dates != null) {
            delete(year)
            val database = writableDatabase
            for (date in dates) {
                val contentValues = ContentValues()
                contentValues.put(DATE,
                    toDateKey(date.first[2], date.first[1], date.first[0])
                )
                contentValues.put(DESCRIPTION, date.second)
                database.insert(TABLE_NAME, null, contentValues)
            }
            database.close()
        }
    }

    override fun onCreate(database: SQLiteDatabase?) {
        try {
            database!!.execSQL(CREATE_TABLE)
        } catch (ignore: Exception) {
            if(database != null && database.isOpen){
                database.close()
            }
        }
    }

    override fun onUpgrade(database: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        try {
            database!!.execSQL(DROP_TABLE)
            onCreate(database)
        } catch (ignore: Exception) {
            if(database != null && database.isOpen){
                database.close()
            }
        }
    }

    private fun toMonthKey(date: LocalDate): String {
        return (date.year * 10000 + date.monthValue * 100).toString()
    }

    private fun toCalendarMonth(date: LocalDate): String {
        return date.withDayOfMonth(1).atStartOfDay().
            atZone(zoneId).toInstant().toEpochMilli().toString()
    }

    private fun toDateKey(date: Long): Int {
        val localDate = Instant.ofEpochMilli(date).atZone(zoneId).toLocalDate()
        return toDateKey(localDate.year, localDate.monthValue, localDate.dayOfMonth)
    }
}

fun toDateKey(year: Int, month: Int, day: Int): Int {
    return year * 10000 + month * 100 + day
}

fun showWarning(activity: Activity){
    Toast.makeText(
        activity,
        activity.getString(R.string.incorrect_year),
        LENGTH_SHORT
    ).show()
}