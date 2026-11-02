package com.chiona.chickenfarm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** The small set of things the web screens may ask the phone to do. */
class Bridge(private val a: MainActivity) {

    @JavascriptInterface
    fun setReminder(hour: Int, minute: Int, on: Boolean) {
        Reminder.set(a, hour, minute, on)
    }

    @JavascriptInterface
    fun share(title: String, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, text)
        a.startActivity(Intent.createChooser(i, title))
    }

    @JavascriptInterface
    fun copyText(text: String) {
        val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("CHIONA CHICKEN FARM", text))
    }

    /** Sync calls go through the phone, so the web screens need no cross-site access. */
    @JavascriptInterface
    fun http(id: String, method: String, url: String, body: String) {
        Thread {
            var status = -1
            var text = ""
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = method
                c.connectTimeout = 15000
                c.readTimeout = 20000
                if (body.isNotEmpty()) {
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json")
                    c.outputStream.use { it.write(body.toByteArray()) }
                }
                status = c.responseCode
                val s = if (status in 200..299) c.inputStream else c.errorStream
                text = s?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (e: Exception) {
                status = -1
                text = e.message ?: "No connection"
            }
            a.js("window.__http(" + JSONObject.quote(id) + "," + status + "," + JSONObject.quote(text) + ")")
        }.start()
    }
}
