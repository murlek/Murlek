package com.murlek.minimallinea

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(CarBridge(), "Car")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun hideSystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    inner class CarBridge {
        @JavascriptInterface
        fun openApps() {
            runOnUiThread {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
                val apps = pm.queryIntentActivities(intent, 0)
                    .filter { it.activityInfo.packageName != packageName }
                    .sortedBy { it.loadLabel(pm).toString().lowercase() }

                val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
                if (apps.isEmpty()) {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Cihazda açılabilir uygulama bulunamadı.")
                        .setPositiveButton("Tamam", null).show()
                    return@runOnUiThread
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Uygulamalar")
                    .setItems(labels) { _, which ->
                        val item = apps[which]
                        val launch = pm.getLaunchIntentForPackage(item.activityInfo.packageName)
                        if (launch != null) startActivity(launch)
                    }
                    .setNegativeButton("Kapat", null)
                    .show()
            }
        }

        @JavascriptInterface
        fun openCategory(category: String) {
            runOnUiThread {
                val intent = when (category) {
                    "maps" -> Intent("android.intent.action.VIEW").apply {
                        data = android.net.Uri.parse("google.navigation:q=")
                    }
                    "music" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
                    "radio" -> null // Radio apps are manufacturer-specific; use installed-app list.
                    "phone" -> Intent(Intent.ACTION_DIAL)
                    "car" -> null // CANBUS data needs the head unit's vendor interface.
                    else -> null
                }
                if (intent != null) {
                    try { startActivity(intent) }
                    catch (_: Exception) { openApps() }
                } else {
                    openApps()
                }
            }
        }

        @JavascriptInterface
        fun openBluetooth() {
            runOnUiThread {
                try { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                catch (_: Exception) { openApps() }
            }
        }

        @JavascriptInterface
        fun openSettings() {
            runOnUiThread {
                try { startActivity(Intent(Settings.ACTION_SETTINGS)) }
                catch (_: Exception) { }
            }
        }

        @JavascriptInterface
        fun showVehicleInfo() {
            runOnUiThread {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Araç bilgileri")
                    .setMessage("Bu bölüm henüz CANBUS verisine bağlı değil. Yakıt, hız ve sıcaklık değerleri bağlantı doğrulanmadan gösterilmez.")
                    .setPositiveButton("Tamam", null)
                    .show()
            }
        }
    }
}
