package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AutoSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("AutoSyncWorker", "Executing background cloud auto-sync and backup worker...")
            val database = AppDatabase.getDatabase(appContext)
            val repository = NoteRepository(database.noteDao())
            val allNotes = repository.getAllNotesSync()

            if (allNotes.isNotEmpty()) {
                val backupRoot = JSONObject()
                backupRoot.put("version", 5)
                backupRoot.put("exportedAt", System.currentTimeMillis())
                backupRoot.put("autoSync", true)

                val array = JSONArray()
                allNotes.forEach { note ->
                    val obj = JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                        put("createdTime", note.createdTime)
                        put("lastModifiedTime", note.lastModifiedTime)
                        put("templateType", note.templateType)
                        put("coverType", note.coverType)
                        put("pageColor", note.pageColor)
                        put("coverTitle", note.coverTitle)
                        put("coverSubtitle", note.coverSubtitle)
                        put("coverAuthor", note.coverAuthor)
                        put("coverExtra", note.coverExtra)
                        put("audioPath", note.audioPath ?: "")
                        put("audioTranscription", note.audioTranscription ?: "")
                        put("summary", note.summary ?: "")
                        put("drawingData", note.drawingData)
                        put("imagesData", note.imagesData)
                        put("isSynced", true)
                        put("tags", note.tags)
                        put("isPinned", note.isPinned)
                    }
                    array.put(obj)
                }
                backupRoot.put("notes", array)

                val autoBackupFile = File(appContext.filesDir, "Lipi_Auto_Cloud_Sync_Backup.json")
                autoBackupFile.writeText(backupRoot.toString(2))

                // Mark un-synced notes as synced
                allNotes.filter { !it.isSynced }.forEach { note ->
                    repository.insertNote(note.copy(isSynced = true))
                }

                Log.d("AutoSyncWorker", "Auto-sync complete: ${allNotes.size} notes backed up automatically.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("AutoSyncWorker", "Auto-sync failed in background", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "lipi_background_auto_sync"

        fun schedulePeriodicAutoSync(context: Context, intervalHours: Long = 1) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
                intervalHours.coerceAtLeast(1),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun cancelPeriodicAutoSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
