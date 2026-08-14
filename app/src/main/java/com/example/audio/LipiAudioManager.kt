package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Production-ready Audio Manager for Lipi Notes
 * Handles:
 * - High fidelity audio recording (M4A / AAC)
 * - Multi-format audio importing (MP3, M4A, WAV, AAC, OGG, FLAC)
 * - Media playback with scrubber, speed control, time display
 * - Waveform visualization data
 */
class LipiAudioManager(private val context: Context) {

    private val TAG = "LipiAudioManager"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Recording State
    var isRecording by mutableStateOf(false)
        private set
    var isRecordingPaused by mutableStateOf(false)
        private set
    var recordingDurationMs by mutableLongStateOf(0L)
        private set
    var currentRecordingFilePath by mutableStateOf<String?>(null)
        private set
    val liveAmplitudes = mutableStateListOf<Float>()

    private var mediaRecorder: MediaRecorder? = null
    private var recordStartTime = 0L
    private var recordingJob: Job? = null

    // Playback State
    var isPlaying by mutableStateOf(false)
        private set
    var currentPlayingBlockId by mutableStateOf<String?>(null)
        private set
    var playbackPositionMs by mutableLongStateOf(0L)
        private set
    var playbackDurationMs by mutableLongStateOf(0L)
        private set
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    init {
        // Ensure private audio attachment storage directory exists
        getAudioStorageDir()
    }

    fun getAudioStorageDir(): File {
        val dir = File(context.filesDir, "attachments/audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // ==========================================
    // RECORDING LOGIC
    // ==========================================

    fun startRecording(): String? {
        if (isRecording) return currentRecordingFilePath

        try {
            stopPlayback()

            val fileName = "rec_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a"
            val outputFile = File(getAudioStorageDir(), fileName)
            currentRecordingFilePath = outputFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            isRecordingPaused = false
            recordStartTime = System.currentTimeMillis()
            recordingDurationMs = 0L
            liveAmplitudes.clear()

            recordingJob = scope.launch(Dispatchers.Default) {
                while (isActive && isRecording) {
                    if (!isRecordingPaused) {
                        recordingDurationMs += 100L
                        val maxAmp = try {
                            mediaRecorder?.maxAmplitude ?: 0
                        } catch (e: Exception) {
                            0
                        }
                        val normalized = (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
                        withContext(Dispatchers.Main) {
                            if (liveAmplitudes.size > 50) {
                                liveAmplitudes.removeAt(0)
                            }
                            liveAmplitudes.add(normalized)
                        }
                    }
                    delay(100L)
                }
            }

            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            stopRecording(discard = true)
            return null
        }
    }

    fun pauseRecording() {
        if (!isRecording || isRecordingPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                isRecordingPaused = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (!isRecording || !isRecordingPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                isRecordingPaused = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recording", e)
            }
        }
    }

    data class RecordingResult(
        val filePath: String,
        val durationMs: Long,
        val fileName: String
    )

    fun stopRecording(discard: Boolean = false): RecordingResult? {
        if (!isRecording) return null

        val path = currentRecordingFilePath
        val duration = recordingDurationMs

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaRecorder stop failed, might have been too short: ${e.message}")
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
            isRecordingPaused = false
            recordingJob?.cancel()
            recordingJob = null
        }

        if (discard && path != null) {
            try {
                File(path).delete()
            } catch (e: Exception) {}
            return null
        }

        if (path != null) {
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                val finalDuration = if (duration > 0) duration else getAudioDuration(path)
                return RecordingResult(path, finalDuration, file.name)
            }
        }

        return null
    }

    // ==========================================
    // AUDIO FILE IMPORT (MP3, M4A, WAV, AAC, etc.)
    // ==========================================

    data class ImportedAudio(
        val localFilePath: String,
        val originalFileName: String,
        val durationMs: Long,
        val title: String
    )

    suspend fun importAudioFile(uri: Uri): ImportedAudio? = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_audio_${System.currentTimeMillis()}.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        fileName = displayName
                    }
                }
            }

            val ext = fileName.substringAfterLast('.', "mp3")
            val targetFile = File(getAudioStorageDir(), "audio_${UUID.randomUUID().toString().take(8)}_$fileName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val durationMs = getAudioDuration(targetFile.absolutePath)
            val extractedTitle = getAudioTitle(targetFile.absolutePath) ?: fileName.substringBeforeLast('.')

            ImportedAudio(
                localFilePath = targetFile.absolutePath,
                originalFileName = fileName,
                durationMs = durationMs,
                title = extractedTitle
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import audio file: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // PLAYBACK LOGIC
    // ==========================================

    fun playAudio(blockId: String, filePath: String, onCompletion: (() -> Unit)? = null) {
        if (currentPlayingBlockId == blockId && mediaPlayer != null) {
            if (!isPlaying) {
                mediaPlayer?.start()
                isPlaying = true
                startPlaybackTracker(onCompletion)
            }
            return
        }

        stopPlayback()

        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file not found: $filePath")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(playbackSpeed)
                }
                start()
                setOnCompletionListener {
                    this@LipiAudioManager.isPlaying = false
                    this@LipiAudioManager.playbackPositionMs = 0L
                    onCompletion?.invoke()
                }
            }

            currentPlayingBlockId = blockId
            isPlaying = true
            playbackDurationMs = mediaPlayer?.duration?.toLong() ?: 0L
            playbackPositionMs = 0L

            startPlaybackTracker(onCompletion)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback: ${e.message}", e)
            stopPlayback()
        }
    }

    fun pausePlayback() {
        if (!isPlaying || mediaPlayer == null) return
        try {
            mediaPlayer?.pause()
            isPlaying = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause playback", e)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                playbackPositionMs = positionMs
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seek audio", e)
            }
        }
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
            try {
                mediaPlayer?.let { player ->
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update playback speed", e)
            }
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        } finally {
            mediaPlayer = null
            isPlaying = false
            currentPlayingBlockId = null
            playbackPositionMs = 0L
            playbackJob?.cancel()
            playbackJob = null
        }
    }

    private fun startPlaybackTracker(onCompletion: (() -> Unit)?) {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Main) {
            while (isActive && isPlaying && mediaPlayer != null) {
                try {
                    playbackPositionMs = mediaPlayer?.currentPosition?.toLong() ?: 0L
                } catch (e: Exception) {
                    break
                }
                delay(100L)
            }
        }
    }

    // ==========================================
    // METADATA HELPERS
    // ==========================================

    fun getAudioDuration(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getAudioTitle(filePath: String): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            retriever.release()
            if (!title.isNullOrBlank()) title else null
        } catch (e: Exception) {
            null
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSecs = (durationMs / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    fun release() {
        stopRecording(discard = true)
        stopPlayback()
    }
}
