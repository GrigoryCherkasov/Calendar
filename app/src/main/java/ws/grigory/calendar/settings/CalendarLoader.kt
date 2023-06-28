package ws.grigory.calendar.settings

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private const val CALENDAR_URL = "http://calendar.by/?year=%s"
private const val EMPTY = ""
private const val TOOLTIP = "tooltip"
private const val HREF = "href"
private const val EQUAL = "="
private const val SPAN = "span"
private const val BRACKET = "("

@SuppressLint("SimpleDateFormat")

fun getNonWorkingDays(year: Int): List<Pair<IntArray, String>>? {
    var result: List<Pair<IntArray, String>>? = null
    try {
        OkHttpClient().newCall(Request.Builder().url(String.format(CALENDAR_URL, year)).build())
            .execute().use { response ->
                val responseBody = response.body
                if (responseBody != null) {
                    val nonWorkingDaysElements =
                        Jsoup.parse(responseBody.string()).getElementsByClass(TOOLTIP)
                    if (nonWorkingDaysElements.size > 0) {
                        result = nonWorkingDaysElements.map { it ->
                            it.attr(HREF).substringAfter(EQUAL).split("-").map { it.toInt() }
                                .toIntArray() to trimDescription(it)
                        }
                    }
                    if (result == null || result!!.isEmpty() || result!![0].first[2] != year) {
                        result = null
                    }
                }
            }
    } catch (ignore: Exception) {
    }
    return result
}

private fun trimDescription(element: Element): String {
    val string = element.getElementsByTag(SPAN).first()?.ownText()
    return if (string != null) {
        val index = string.lastIndexOf(BRACKET)
        if (index == -1) EMPTY else string.replaceRange(index, string.length, EMPTY).trim()
    } else {
        EMPTY
    }
}
