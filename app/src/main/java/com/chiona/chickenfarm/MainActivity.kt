package com.chiona.chickenfarm

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reminder.ensureChannel(this)

        web = WebView(this)
        setContentView(web)

        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowContentAccess = false

        web.addJavascriptInterface(Bridge(this), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "file") return false
                try {
                    val i = if (uri.scheme == "tel") Intent(Intent.ACTION_DIAL, uri) else Intent(Intent.ACTION_VIEW, uri)
                    startActivity(i)
                } catch (e: Exception) {
                    // No app can open this link.
                }
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 12)
        }
    }

    fun js(code: String) {
        runOnUiThread { web.evaluateJavascript(code, null) }
    }

    override fun onResume() {
        super.onResume()
        js("window.__resume && window.__resume()")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("window.__back ? window.__back() : false") { r ->
            if (r != "true") finish()
        }
    }
}
