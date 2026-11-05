package ir.dehaqaniha.app

import android.util.Xml
import androidx.core.text.HtmlCompat
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

const val SITE = "https://dehaqaniha.ir"

/** صفحه پیش‌نمایش عمومی کانال‌ها (آدرس خواندن، آدرس اصلی پست‌ها) */
val CHANNELS = listOf(
    "https://t.me/s/dehaghaniha" to "https://t.me/",
    "https://eitaa.com/dehaghanihA" to "https://eitaa.com/"
)

data class Post(
    val id: String, val title: String, val link: String,
    val image: String?, val date: String, val excerpt: String,
    val body: String? = null, val ts: Long = 0L
)

object Repo {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) DehaqanihaApp/1.0")
        try {
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun strip(s: String): String =
        HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

    private fun parseTs(s: String): Long {
        for (f in listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss")) {
            try {
                return SimpleDateFormat(f, Locale.US).parse(s)!!.time
            } catch (e: Exception) {
            }
        }
        return 0L
    }

    /** اول کانال‌ها (تلگرام و ایتا)، اگر چیزی نیامد وردپرس، بعد RSS */
    fun fetch(): List<Post> {
        val ch = mutableListOf<Post>()
        for ((url, origin) in CHANNELS) {
            try {
                ch.addAll(fromChannel(url, origin))
            } catch (e: Exception) {
            }
        }
        if (ch.isNotEmpty()) {
            return ch.sortedByDescending { it.ts }
                .distinctBy { p -> p.title.filter { c -> c.isLetterOrDigit() }.take(40) }
        }
        return try {
            val r = fromWp()
            if (r.isEmpty()) fromRss() else r
        } catch (e: Exception) {
            fromRss()
        }
    }

    private fun fromChannel(url: String, origin: String): List<Post> {
        val html = get(url)
        val blocks = html.split(Regex("<div class=\"(?:tgme|etme)_widget_message_wrap")).drop(1)
        val out = mutableListOf<Post>()
        for (b in blocks) {
            val post = Regex("data-post=\"([^\"]+)\"").find(b)?.groupValues?.get(1) ?: continue
            val raw = Regex("_widget_message_text[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
                .find(b)?.groupValues?.get(1) ?: continue
            val body = strip(raw)
            if (body.isEmpty()) continue
            val img = Regex("background-image:url\\('([^']+)'\\)").find(b)?.groupValues?.get(1)
            val time = Regex("<time[^>]*datetime=\"([^\"]+)\"").find(b)?.groupValues?.get(1) ?: ""
            val title = body.lines().firstOrNull { it.isNotBlank() }?.trim()?.take(100) ?: body.take(100)
            out.add(Post(post, title, origin + post, img, time.take(10), body.take(160), body, parseTs(time)))
        }
        return out
    }

    private fun fromWp(): List<Post> {
        val arr = JSONArray(get("$SITE/wp-json/wp/v2/posts?_embed&per_page=30"))
        val out = mutableListOf<Post>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val img = o.optJSONObject("_embedded")
                ?.optJSONArray("wp:featuredmedia")
                ?.optJSONObject(0)
                ?.optString("source_url")
            out.add(
                Post(
                    o.getInt("id").toString(),
                    strip(o.getJSONObject("title").getString("rendered")),
                    o.getString("link"),
                    if (img.isNullOrEmpty()) null else img,
                    o.getString("date").take(10),
                    strip(o.getJSONObject("excerpt").getString("rendered")),
                    null,
                    parseTs(o.getString("date"))
                )
            )
        }
        return out
    }

    private fun fromRss(): List<Post> {
        val p = Xml.newPullParser()
        p.setInput(StringReader(get("$SITE/feed")))
        val out = mutableListOf<Post>()
        var t = ""; var l = ""; var d = ""; var desc = ""
        var img: String? = null
        var inItem = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                val n = p.name
                if (n == "item") { inItem = true; t = ""; l = ""; d = ""; desc = ""; img = null }
                else if (inItem && n == "title") t = p.nextText()
                else if (inItem && n == "link") l = p.nextText()
                else if (inItem && n == "pubDate") d = p.nextText()
                else if (inItem && n == "description") desc = p.nextText()
                else if (inItem && n == "content:encoded") {
                    val h = p.nextText()
                    if (img == null) img = Regex("<img[^>]+src=[\"']([^\"']+)").find(h)?.groupValues?.get(1)
                } else if (inItem && (n == "enclosure" || n == "media:content")) {
                    if (img == null) img = p.getAttributeValue(null, "url")
                }
            } else if (ev == XmlPullParser.END_TAG && p.name == "item") {
                out.add(Post(l, strip(t), l, img, d.take(16), strip(desc)))
                inItem = false
            }
            ev = p.next()
        }
        return out
    }
}
