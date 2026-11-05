package ir.dehaqaniha.app

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class PostAdapter(private val onClick: (Post) -> Unit) : RecyclerView.Adapter<PostAdapter.VH>() {
    private var items: List<Post> = emptyList()

    fun submit(l: List<Post>) {
        items = l
        notifyDataSetChanged()
    }

    class VH(val root: LinearLayout, val img: ImageView, val title: TextView, val meta: TextView) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val c = parent.context
        val d = c.resources.displayMetrics.density
        fun dp(x: Int) = (x * d).toInt()
        val img = ImageView(c).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(80))
        }
        val title = TextView(c).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
        }
        val meta = TextView(c).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = dp(12)
        val col = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = lp
            addView(title)
            addView(meta)
        }
        val root = LinearLayout(c).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundResource(android.R.drawable.list_selector_background)
            addView(img)
            addView(col)
        }
        return VH(root, img, title, meta)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = items[position]
        h.title.text = p.title
        h.meta.text = p.date
        if (p.image != null) {
            h.img.visibility = View.VISIBLE
            h.img.load(p.image) { crossfade(true) }
        } else {
            h.img.visibility = View.GONE
        }
        h.root.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size
}
