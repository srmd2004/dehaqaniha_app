package ir.dehaqaniha.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var swipe: SwipeRefreshLayout
    private val postAdapter = PostAdapter { p ->
        startActivity(ArticleActivity.intent(this, p))
    }
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "دهاقانی‌ها"
        val rv = RecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = postAdapter
        swipe = SwipeRefreshLayout(this)
        swipe.addView(rv)
        swipe.setOnRefreshListener { load() }
        setContentView(swipe)

        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, req)

        load()
    }

    private fun load() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Repo.fetch() } }
            r.onSuccess { list ->
                postAdapter.submit(list)
                if (list.isNotEmpty()) {
                    getSharedPreferences("state", MODE_PRIVATE).edit()
                        .putString("last", list[0].id).apply()
                }
            }.onFailure {
                Toast.makeText(this@MainActivity, "خطا در دریافت اخبار؛ اینترنت را بررسی کنید", Toast.LENGTH_LONG).show()
            }
            swipe.isRefreshing = false
        }
    }
}
