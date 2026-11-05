package ir.dehaqaniha.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val posts: List<Post>
        try {
            posts = Repo.fetch()
        } catch (e: Exception) {
            return Result.retry()
        }
        if (posts.isEmpty()) return Result.success()
        val sp = applicationContext.getSharedPreferences("state", Context.MODE_PRIVATE)
        val last = sp.getString("last", null)
        val newest = posts[0]
        if (last != null && last != newest.id) Notifier.show(applicationContext, newest)
        sp.edit().putString("last", newest.id).apply()
        return Result.success()
    }
}
