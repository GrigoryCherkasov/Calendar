package ws.grigory.calendar

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.icu.text.DateTimePatternGenerator
import android.os.Build
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ws.grigory.calendar.settings.CalendarStorage
import ws.grigory.calendar.settings.SettingsActivity
import ws.grigory.calendar.settings.toDateKey
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar

const val UPDATE = "UPDATE"

open class CalendarWidget : AppWidgetProvider() {

    companion object {
        private val ID_CLASS = R.id::class.java
        private const val BACKGROUND_METHOD = "setBackgroundColor"
        private const val TEXT_COLOR_METHOD = "setTextColor"
        private const val DATE_PATTERN = "LLLLyyyy"
        private const val WORK_TAG = "WORK_TAG"
        private const val MONTH_SHIFT = "MONTH_SHIFT"
        private const val F = "f"
        private const val I = "i"
        private const val TIME = "time"
        private val CALENDAR_URI = CalendarContract.CONTENT_URI
        val SETTINGS_INTENT: Intent = Intent("SETTINGS")
        init {
            SETTINGS_INTENT.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    class WidgetUpdater(context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.putExtra(UPDATE, true)
            applicationContext.sendBroadcast(intent)
            return Result.success()
        }
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (isWorkerEnqueue(context, intent)) {
            val monthShift =
                if (intent.extras != null) intent.extras!!.getLong(MONTH_SHIFT, 0) else 0
            fillDates(context, monthShift)
        }
    }

    override fun onEnabled(context: Context) {
        isWorkerEnqueue(context, null)
        fillDates(context, 0)
    }

    private fun fillDates(context: Context, monthShift: Long) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, this::class.java)
        val widget = RemoteViews(componentName.packageName, R.layout.calendar_widget)
        val currentDate = LocalDate.now()
        val requstedDate = currentDate.plusMonths(monthShift)
        val nonWorkDays = CalendarStorage(context).nonWorkDaysSet(requstedDate)
        val eventDays = CalendarStorage(context).getCalendarEventsSet(requstedDate)

        val dateFormatter = DateTimeFormatter.ofPattern(
            DateTimePatternGenerator.getInstance().getBestPattern(DATE_PATTERN)
        )
        widget.setTextViewText(R.id.date, capitalizeFirstLetter(dateFormatter.format(requstedDate)))

        val lastDayRequstedMonth = requstedDate.month.length(requstedDate.isLeapYear)
        val firstCellRequstedMonth = YearMonth.from(requstedDate).atDay(1).dayOfWeek.value - 1

        for (day in 1..lastDayRequstedMonth) {
            val fieldId = getFieldId(day + firstCellRequstedMonth)
            val imageId = getImageId(day + firstCellRequstedMonth)

            markWorkDaysAndEvents(
                context, widget, fieldId, imageId, currentDate, requstedDate, requstedDate.year,
                requstedDate.monthValue, day, eventDays
            )
            markNonWorkDays(
                context, widget, fieldId, requstedDate.year, requstedDate.monthValue, day,
                nonWorkDays, R.color.red
            )
        }

        if (firstCellRequstedMonth > 0) {
            val previousDate = requstedDate.minusMonths(1)
            val lastDayPreviousMonth = previousDate.month.length(requstedDate.isLeapYear) -
                    firstCellRequstedMonth

            for (dayCount in 1..firstCellRequstedMonth) {
                val day = lastDayPreviousMonth + dayCount
                val fieldId = getFieldId(dayCount)
                val imageId = getImageId(dayCount)

                markWorkDaysAndEvents(
                    context, widget, fieldId, imageId, currentDate, requstedDate, previousDate.year,
                    previousDate.monthValue, day, eventDays
                )
                markNonWorkDays(
                    context, widget, fieldId, previousDate.year, previousDate.monthValue, day,
                    nonWorkDays, R.color.red_pale
                )
            }
        }

        val nextDate = requstedDate.plusMonths(1)
        val firstCellNextMonth = lastDayRequstedMonth + firstCellRequstedMonth
        val nextMonthDays = 42 - firstCellNextMonth
        for (day in 1..nextMonthDays) {
            val fieldId = getFieldId(firstCellNextMonth + day)
            val imageId = getImageId(firstCellNextMonth + day)

            markWorkDaysAndEvents(
                context, widget, fieldId, imageId, currentDate, requstedDate, nextDate.year,
                nextDate.monthValue, day, eventDays
            )
            markNonWorkDays(
                context, widget, fieldId, nextDate.year, nextDate.monthValue, day, nonWorkDays,
                R.color.red_pale
            )
        }

        widget.setOnClickPendingIntent(R.id.f1, getUpdateIntent(context, R.id.f1, monthShift - 1))
        widget.setOnClickPendingIntent(R.id.f42, getUpdateIntent(context, R.id.f42, monthShift + 1))
        setBacktoCurrent(context, widget, currentDate, requstedDate)
        setOpenSettings(context, widget)

        appWidgetManager.updateAppWidget(appWidgetManager.getAppWidgetIds(componentName), widget)
    }

    private fun setOpenSettings(
        context: Context, widget: RemoteViews
    ) {
        SETTINGS_INTENT.setClass(context, SettingsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            SETTINGS_INTENT,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        widget.setOnClickPendingIntent(R.id.settings, pendingIntent)
    }

    private fun setBacktoCurrent(
        context: Context, widget: RemoteViews,
        now: LocalDate, showed: LocalDate
    ) {
        if (showed.year != now.year || showed.month != now.month) {
            widget.setImageViewResource(R.id.back, R.mipmap.back)
            widget.setOnClickPendingIntent(R.id.back, getUpdateIntent(context, R.id.back, 0))
        } else {
            widget.setImageViewResource(R.id.back, -1)
        }
    }

    private fun markWorkDaysAndEvents(
        context: Context, widget: RemoteViews, fieldId: Int, imageId: Int,
        now: LocalDate, showed: LocalDate, year: Int, month: Int, day: Int,
        eventDays: Set<Int>?
    ) {

        if (day == now.dayOfMonth && month == now.monthValue && year == now.year) {
            val dateString = SpannableString(day.toString())
            dateString.setSpan(StyleSpan(Typeface.BOLD), 0, dateString.length, 0)
            widget.setTextViewText(fieldId, dateString)
            widget.setInt(fieldId, BACKGROUND_METHOD, context.getColor(R.color.cell_line))
        } else {
            widget.setTextViewText(fieldId, day.toString())
            widget.setInt(fieldId, BACKGROUND_METHOD, context.getColor(R.color.transparent))
        }

        widget.setInt(
            fieldId,
            TEXT_COLOR_METHOD,
            context.getColor(
                if (month != showed.monthValue || year != showed.year)
                    R.color.cell_line else R.color.white
            )
        )
        widget.setImageViewResource(
            imageId, if (eventDays != null && eventDays.contains(toDateKey(year, month, day)))
                if (month != showed.monthValue || year != showed.year)
                    R.drawable.mark_left_top_disabled else R.drawable.mark_left_top else -1
        )
        widget.setOnClickPendingIntent(
            fieldId,
            getCalendarIntent(context, fieldId, year, month, day)
        )
    }


    private fun markNonWorkDays(
        context: Context, widget: RemoteViews, fieldId: Int,
        year: Int, month: Int, day: Int,
        nonWorkDays: Set<Int>?, color: Int
    ) {

        if (nonWorkDays != null && nonWorkDays.contains(toDateKey(year, month, day))) {
            widget.setInt(fieldId, TEXT_COLOR_METHOD, context.getColor(color))
            widget.setOnClickPendingIntent(
                fieldId,
                getCalendarIntent(context, fieldId, year, month, day)
            )
        }
    }

    private fun isWorkerEnqueue(context: Context, intent: Intent?): Boolean {
        var reset = true
        var update = false

        if (intent != null) {
            val action = intent.action

            reset = Intent.ACTION_TIMEZONE_CHANGED == action ||
                    Intent.ACTION_DATE_CHANGED == action ||
                    Intent.ACTION_TIME_CHANGED == action ||
                    Intent.ACTION_LOCALE_CHANGED == action ||
                    Intent.ACTION_PROVIDER_CHANGED == action ||
                    intent.extras != null && intent.extras!!.getBoolean(UPDATE, false)

            update = !reset && AppWidgetManager.ACTION_APPWIDGET_UPDATE == action
        }

        if (reset || update) {
            val workManagerInstance = WorkManager.getInstance(context)
            if (update) {
                reset = workManagerInstance.getWorkInfosByTag(WORK_TAG).get().none { workInfo ->
                    (workInfo.state == WorkInfo.State.RUNNING) or
                            (workInfo.state == WorkInfo.State.ENQUEUED)
                }
            }
            if (reset) {
                val now = LocalDateTime.now()
                val nextDay = now.toLocalDate().plusDays(1).atStartOfDay()
                val locationListenableWorker =
                    OneTimeWorkRequest.Builder(WidgetUpdater::class.java).addTag(WORK_TAG)
                        .setInitialDelay(Duration.between(now, nextDay))
                        .build()//Duration.ofMillis(10000)).build()
                workManagerInstance.cancelAllWork()
                workManagerInstance.enqueue(locationListenableWorker)
            }
            return true
        }
        return false
    }

    private fun getImageId(fieldId: Int): Int {
        return getId(I, fieldId)
    }

    private fun getFieldId(fieldId: Int): Int {
        return getId(F, fieldId)
    }

    private fun getId(prefix: String, fieldId: Int): Int {
        return try {
            val idField = ID_CLASS.getDeclaredField(prefix + fieldId)
            idField.getInt(idField)
        } catch (e: Exception) {
            -1
        }
    }

    private fun getCalendarIntent(
        context: Context,
        code: Int,
        year: Int,
        month: Int,
        day: Int
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            code,
            Intent(
                Intent.ACTION_VIEW
            ).setData(
                CALENDAR_URI.buildUpon().appendPath(TIME)
                    .appendPath(GregorianCalendar(year, month - 1, day).time.time.toString())
                    .build()
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getUpdateIntent(context: Context, code: Int, monthShift: Long): PendingIntent {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(MONTH_SHIFT, monthShift)
        intent.putExtra(UPDATE, true)

        val flag: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        return PendingIntent.getBroadcast(context, code, intent, flag)
    }

    private fun capitalizeFirstLetter(word: String): String {
        val chars = word.toCharArray()
        chars[0] = chars[0].uppercaseChar()
        return String(chars)
    }
}