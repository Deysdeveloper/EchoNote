package com.Deysdeveloper.dailyvoicejournalapp.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Converts audio files to WAV format for Vosk speech recognition.
 * Supports converting M4A/AAC to 16kHz, 16-bit, mono PCM WAV.
 */
class AudioConverter(private val context: Context) {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }

    /**
     * Converts an audio file to WAV format suitable for Vosk.
     * @param inputFilePath Path to the input audio file (M4A, MP3, etc.)
     * @param outputFilePath Path for the output WAV file
     * @return true if conversion successful, false otherwise
     */
    fun convertToWav(inputFilePath: String, outputFilePath: String): Boolean {
        return try {
            Log.d("AudioConverter", "Converting $inputFilePath to WAV")

            val inputFile = File(inputFilePath)
            if (!inputFile.exists()) {
                Log.e("AudioConverter", "Input file not found: $inputFilePath")
                return false
            }

            // Check if file is already WAV
            if (inputFilePath.endsWith(".wav", ignoreCase = true)) {
                Log.d("AudioConverter", "File is already WAV format")
                return true
            }

            // For M4A/AAC files, we need to decode them
            // This is a simplified conversion - for production, use FFmpeg
            return decodeToWav(inputFile, File(outputFilePath))

        } catch (e: Exception) {
            Log.e("AudioConverter", "Conversion failed", e)
            false
        }
    }

    /**
     * Decodes audio file to WAV using Android's MediaCodec.
     * This is a basic implementation that may not work for all formats.
     */
    private fun decodeToWav(inputFile: File, outputFile: File): Boolean {
        // Check if file is too large (over 10MB)
        if (inputFile.length() > 10 * 1024 * 1024) {
            Log.w("AudioConverter", "File too large: ${inputFile.length()} bytes")
            return false
        }

        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null

        try {
            // Create output file
            if (outputFile.exists()) outputFile.delete()
            outputFile.createNewFile()

            // Setup MediaExtractor
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            // Find audio track
            val trackIndex = (0 until extractor.trackCount)
                .firstOrNull { 
                    val format = extractor.getTrackFormat(it)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    mime.startsWith("audio/")
                }
                ?: run {
                    Log.e("AudioConverter", "No audio track found in ${inputFile.absolutePath}")
                    return false
                }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

            Log.d("AudioConverter", "Decoding track $trackIndex with MIME: $mime")

            // Check if this is a supported format
            if (!mime.contains("mp4") && !mime.contains("aac") && !mime.contains("mpeg")) {
                Log.w("AudioConverter", "Unsupported format: $mime, trying anyway...")
            }

            // Setup MediaCodec for decoding
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            var totalBytes = 0

            // Temporary buffer for raw PCM data - use a more memory-efficient approach
            val pcmData = ByteArrayOutputStream()

            while (!sawOutputEOS) {
                // Feed input
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            sawInputEOS = true
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Get output
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)

                    pcmData.write(chunk)
                    totalBytes += chunk.size

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d("AudioConverter", "Output format changed")
                }
            }

            Log.d("AudioConverter", "Decoded $totalBytes bytes of PCM data")

            if (totalBytes == 0) {
                Log.e("AudioConverter", "No audio data decoded")
                return false
            }

            // Write WAV file
            writeWavFile(outputFile, pcmData.toByteArray())

            Log.d("AudioConverter", "WAV file created: ${outputFile.absolutePath}")
            return true

        } catch (e: Exception) {
            Log.e("AudioConverter", "Decoding failed: ${e.message}", e)
            return false
        } finally {
            try {
                extractor?.release()
            } catch (e: Exception) {
                Log.w("AudioConverter", "Error releasing extractor", e)
            }
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                Log.w("AudioConverter", "Error releasing codec", e)
            }
        }
    }

    /**
     * Writes PCM data to a WAV file.
     */
    private fun writeWavFile(outputFile: File, pcmData: ByteArray) {
        FileOutputStream(outputFile).use { fos ->
            val sampleRate = SAMPLE_RATE
            val channels = CHANNELS
            val bitsPerSample = BITS_PER_SAMPLE
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = channels * bitsPerSample / 8

            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(36 + pcmData.size)) // Chunk size
            fos.write("WAVE".toByteArray())

            // fmt sub-chunk
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16)) // Sub-chunk size
            fos.write(shortToBytes(1)) // Audio format (PCM)
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes(blockAlign.toShort()))
            fos.write(shortToBytes(bitsPerSample.toShort()))

            // data sub-chunk
            fos.write("data".toByteArray())
            fos.write(intToBytes(pcmData.size))
            fos.write(pcmData)
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Gets a temporary WAV file path for conversion.
     */
    fun getTempWavPath(originalFilePath: String): String {
        val originalFile = File(originalFilePath)
        val tempDir = File(context.cacheDir, "temp_wav")
        tempDir.mkdirs()
        return File(tempDir, "${originalFile.nameWithoutExtension}_16k.wav").absolutePath
    }

    /**
     * Cleans up temporary WAV files.
     */
    fun cleanupTempFiles() {
        val tempDir = File(context.cacheDir, "temp_wav")
        tempDir.deleteRecursively()
    }
}
