package ws.grigory.calendar.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import ws.grigory.calendar.R

private const val DIGITS = "0123456789"

class YearDialog(
    private val settingsActivity: SettingsActivity,
    private var years: ArrayList<Int>,
    private var yearAdapter: YearAdapter
) : DialogFragment() {

    private lateinit var yearValue: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle(resources.getString(R.string.year))
        val inflater: LayoutInflater = requireActivity().layoutInflater
        val inflaterView: View = inflater.inflate(R.layout.year_dialog, null)

        yearValue = inflaterView.findViewById(R.id.yearValue)
        yearValue.keyListener = DigitsKeyListener.getInstance(DIGITS)

        builder.setView(inflaterView)
            .setPositiveButton(R.string.save) { _, _ -> onDialogData(this) }
            .setNegativeButton(R.string.cancel) { _, _ -> }
        return builder.create()
    }

    private fun onDialogData(dialog: YearDialog) {
        val yearValue: Int = charSequenceToInt(dialog.yearValue.text)
        if (yearValue == 0) {
            showWarning(settingsActivity)
            return
        }
        val calendarStorage = CalendarStorage(settingsActivity)
        calendarStorage.load(settingsActivity, yearValue, years, yearAdapter)
    }

    private fun charSequenceToInt(text: CharSequence): Int {
        return try {
            text.toString().toInt()
        } catch (ignored: Exception) {
            0
        }
    }
}