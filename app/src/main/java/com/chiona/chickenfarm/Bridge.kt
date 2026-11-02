package com.chiona.chickenfarm

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import java.io.File
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

    /** Saves a file into Downloads/Chiona Chicken Farm and returns its address, "perm" or "". */
    @JavascriptInterface
    fun saveFile(name: String, mime: String, b64: String): String {
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues()
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime)
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Chiona Chicken Farm")
                val uri = a.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return ""
                val out = a.contentResolver.openOutputStream(uri) ?: return ""
                out.use { it.write(bytes) }
                uri.toString()
            } else {
                if (a.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    a.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 21)
                    return "perm"
                }
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Chiona Chicken Farm")
                dir.mkdirs()
                val f = File(dir, name)
                f.writeBytes(bytes)
                val v = ContentValues()
                v.put(MediaStore.Files.FileColumns.DATA, f.absolutePath)
                v.put(MediaStore.Files.FileColumns.DISPLAY_NAME, name)
                v.put(MediaStore.Files.FileColumns.MIME_TYPE, mime)
                val uri = a.contentResolver.insert(MediaStore.Files.getContentUri("external"), v)
                uri?.toString() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun openFile(uri: String, mime: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            a.startActivity(i)
        } catch (e: Exception) {
            a.js("toast('No app on this phone can open this file. Find it in your Downloads folder.')")
        }
    }

    @JavascriptInterface
    fun shareFile(uri: String, mime: String, title: String) {
        try {
            val i = Intent(Intent.ACTION_SEND).setType(mime)
                .putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            a.startActivity(Intent.createChooser(i, title))
        } catch (e: Exception) {
            a.js("toast('Could not share the file')")
        }
    }

    @JavascriptInterface
    fun pickBackup() {
        a.runOnUiThread {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
            a.startActivityForResult(i, 31)
        }
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
