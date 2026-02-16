package com.Deysdeveloper.dailyvoicejournalapp.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Downloads and extracts the Vosk speech recognition model.
 * The model is downloaded from the official Vosk repository and extracted
 * to the app's files directory.
 */
class ModelDownloader(private val context: Context) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        object Extracting : DownloadState()
        object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    companion object {
        // Vosk small English model - ~40MB, good accuracy, fast
        const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        const val BUFFER_SIZE = 32768  // Increased from 8KB to 32KB for faster downloads
    }

    /**
     * Downloads and extracts the Vosk model.
     * @return true if successful, false otherwise
     */
    suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(0)

        val modelDir = File(context.filesDir, "vosk-model")
        val tempZipFile = File(context.cacheDir, "$MODEL_NAME.zip")

        try {
            // Download the model
            Log.d("ModelDownloader", "Starting download from: $MODEL_URL")
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 30000
                readTimeout = 60000  // Increased for slower connections
                instanceFollowRedirects = true
                setRequestProperty("Accept-Encoding", "gzip, deflate")
                setRequestProperty("User-Agent", "DailyVoiceJournal/1.0")
            }
            connection.connect()

            val totalSize = connection.contentLength
            val totalSizeMB = totalSize / (1024.0 * 1024.0)
            Log.d("ModelDownloader", "File size: %.2f MB".format(totalSizeMB))

            BufferedInputStream(connection.inputStream, BUFFER_SIZE).use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloadedSize = 0
                    var bytesRead: Int
                    var lastUpdateTime = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead

                        // Update progress every 500ms to reduce UI updates
                        val currentTime = System.currentTimeMillis()
                        if (totalSize > 0 && (currentTime - lastUpdateTime > 500 || downloadedSize == totalSize)) {
                            val progress = (downloadedSize * 100 / totalSize).toInt()
                            _downloadState.value = DownloadState.Downloading(progress)
                            lastUpdateTime = currentTime
                        }
                    }
                    output.flush()
                }
            }
            connection.disconnect()

            Log.d("ModelDownloader", "Download complete: ${tempZipFile.absolutePath}")

            // Extract the model
            _downloadState.value = DownloadState.Extracting
            extractZip(tempZipFile, modelDir)

            // Clean up temp file
            tempZipFile.delete()

            _downloadState.value = DownloadState.Success
            Log.d("ModelDownloader", "Model ready at: ${modelDir.absolutePath}")
            true

        } catch (e: Exception) {
            Log.e("ModelDownloader", "Failed to download model", e)
            _downloadState.value = DownloadState.Error("Download failed: ${e.message}")
            // Clean up temp file if exists
            tempZipFile.delete()
            false
        }
    }

    /**
     * Extracts a zip file to the specified directory.
     */
    private fun extractZip(zipFile: File, destDir: File) {
        Log.d("ModelDownloader", "Extracting: ${zipFile.absolutePath} to ${destDir.absolutePath}")

        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    // Ensure parent directories exist
                    newFile.parentFile?.mkdirs()

                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos, BUFFER_SIZE)
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        Log.d("ModelDownloader", "Extraction complete")
    }

    /**
     * Check if the model is already downloaded and extracted.
     * The model might be in a nested folder (e.g., vosk-model-small-en-us-0.15/)
     */
    fun isModelAvailable(): Boolean {
        val modelDir = File(context.filesDir, "vosk-model")
        if (!modelDir.exists() || !modelDir.isDirectory) return false
        
        // Check if model files exist directly in the folder
        if (hasModelFiles(modelDir)) return true
        
        // Check if there's a nested folder with model files
        val nestedDirs = modelDir.listFiles { file -> file.isDirectory }
        nestedDirs?.forEach { nestedDir ->
            if (hasModelFiles(nestedDir)) return true
        }
        
        return false
    }
    
    /**
     * Check if a directory contains Vosk model files.
     */
    private fun hasModelFiles(dir: File): Boolean {
        // Look for key model files
        val requiredFiles = listOf("am/final.mdl", "graph/Gr.fst", "ivector/final.dubm")
        return requiredFiles.all { File(dir, it).exists() }
    }
    
    /**
     * Get the actual path where the model is located (handles nested folders).
     */
    fun getModelPath(): String {
        val modelDir = File(context.filesDir, "vosk-model")
        
        // Check if model files exist directly in the folder
        if (hasModelFiles(modelDir)) return modelDir.absolutePath
        
        // Check nested folders
        val nestedDirs = modelDir.listFiles { file -> file.isDirectory }
        nestedDirs?.forEach { nestedDir ->
            if (hasModelFiles(nestedDir)) return nestedDir.absolutePath
        }
        
        // Default to the base path if not found
        return modelDir.absolutePath
    }

    /**
     * Reset the download state.
     */
    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Cancel any ongoing download.
     */
    fun cancelDownload() {
        _downloadState.value = DownloadState.Idle
    }
}
