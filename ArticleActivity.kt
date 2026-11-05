package ir.dehaqaniha.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import coil.load

class ArticleActivity : AppCompatActivity() {
    companion object {
        fun intent(ctx: Context, p: Post): Intent =
            Intent(ctx, ArticleActivity::class.java)
                .putExtra("url", p.link)
                .putExtra("title", p.title)
                .putExtra("body", p.body)
                .putExtra("image", p.image)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: SITE
        val body = intent.getStringExtra("body")
        if (body != null) showNative(url, body) else showWeb(url)
    }

    private fun showNative(url: String, body: String) {
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = View.LAYOUT_DIRECTION_RTL
        col.setPadding(pad, pad, pad, pad)
        val image = intent.getStringExtra("image")
        if (image != null) {
            val iv = ImageView(this)
            iv.adjustViewBounds = true
            iv.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            iv.load(image)
            col.addView(iv)
        }
        val tv = TextView(this)
        tv.text = body
        tv.textSize = 17f
        tv.setLineSpacing(0f, 1.3f)
        tv.setTextIsSelectable(true)
        tv.setPadding(0, pad, 0, pad)
        Linkify.addLinks(tv, Linkify.WEB_URLS)
        col.addView(tv)
        val btn = Button(this)
        btn.text = "مشاهده در کانال"
        btn.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        col.addView(btn)
        val sv = ScrollView(this)
        sv.addView(col)
        setContentView(sv)
    }

    private fun showWeb(url: String) {
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        setContentView(web)
        web.loadUrl(url)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })
    }
}
