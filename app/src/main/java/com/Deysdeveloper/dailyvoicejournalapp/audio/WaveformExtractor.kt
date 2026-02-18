package com.Deysdeveloper.dailyvoicejournalapp.audio

import android.media.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Extracts amplitude data from audio files for waveform visualization.
 * Decodes audio to PCM and extracts evenly-spaced amplitude samples.
 */
object WaveformExtractor {

    private const val TAG = "WaveformExtractor"
    private const val SAMPLE_COUNT = 100 // Number of bars in waveform
    private const val DECODE_TIMEOUT_US = 5000L

    /**
     * Extract waveform amplitudes from an audio file.
     * Returns a list of normalized amplitude values (0.0 to 1.0).
     */
    suspend fun extractWaveform(filePath: String): List<Float> = withContext(Dispatchers.Default) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return@withContext emptyList()
            }

            // Get audio duration
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            retriever.release()

            if (durationMs <= 0) {
                Log.e(TAG, "Invalid duration: $durationMs")
                return@withContext emptyList()
            }

            Log.d(TAG, "Extracting waveform for: $filePath")
            Log.d(TAG, "Duration: ${durationMs}ms, Target samples: $SAMPLE_COUNT")

            // Decode and extract amplitudes
            val amplitudes = decodeAndExtract(filePath, durationMs)

            Log.d(TAG, "Extracted ${amplitudes.size} amplitudes")
            amplitudes

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting waveform", e)
            emptyList()
        }
    }

    private fun decodeAndExtract(filePath: String, durationMs: Long): List<Float> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(filePath)

            // Find audio track
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                Log.e(TAG, "No audio track found")
                return emptyList()
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)

            Log.d(TAG, "Audio format: mime=$mime, sampleRate=$sampleRate, channels=$channels")

            // Create decoder
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            // Collect all PCM data first, then sample
            val allAmplitudes = mutableListOf<Int>()

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false

            while (!isOutputEOS) {
                // Feed input
                if (!isInputEOS) {
                    val inputBufferId = codec.dequeueInputBuffer(DECODE_TIMEOUT_US)
                    if (inputBufferId >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferId) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputEOS = true
                            Log.d(TAG, "Input EOS reached")
                        } else {
                            codec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Get output
                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT_US)
                when {
                    outputBufferId >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferId) ?: continue

                        // Process PCM samples
                        val remaining = outputBuffer.remaining()
                        val sampleCount = remaining / 2 // 16-bit samples

                        // Read all samples from this buffer
                        outputBuffer.asShortBuffer().let { shortBuffer ->
                            while (shortBuffer.hasRemaining()) {
                                val sample = shortBuffer.get().toInt()
                                allAmplitudes.add(abs(sample))
                            }
                        }

                        outputBuffer.clear()
                        codec.releaseOutputBuffer(outputBufferId, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isOutputEOS = true
                            Log.d(TAG, "Output EOS reached, total samples: ${allAmplitudes.size}")
                        }
                    }
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed")
                    }
                    outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output available yet, check if we're done
                        if (isInputEOS) {
                            // Small delay to let decoder finish
                            Thread.sleep(5)
                        }
                    }
                }
            }

            // Now sample evenly from allAmplitudes to get SAMPLE_COUNT points
            return if (allAmplitudes.isNotEmpty()) {
                downsampleToFixedCount(allAmplitudes, SAMPLE_COUNT)
            } else {
                Log.w(TAG, "No amplitudes collected")
                emptyList()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Decoding error", e)
            return emptyList()
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing codec", e)
            }
            extractor.release()
        }
    }

    private fun downsampleToFixedCount(amplitudes: List<Int>, targetCount: Int): List<Float> {
        if (amplitudes.isEmpty() || targetCount <= 0) return emptyList()
        if (amplitudes.size <= targetCount) {
            // Pad with minimum values if not enough samples
            return amplitudes.map { normalizeAmplitude(it) } + List(targetCount - amplitudes.size) { 0.1f }
        }

        // Calculate how many samples per segment
        val samplesPerSegment = amplitudes.size / targetCount
        val result = mutableListOf<Float>()

        for (i in 0 until targetCount) {
            val start = i * samplesPerSegment
            val end = if (i == targetCount - 1) amplitudes.size else (i + 1) * samplesPerSegment

            // Get max amplitude in this segment
            var maxAmp = 0
            for (j in start until end) {
                if (amplitudes[j] > maxAmp) maxAmp = amplitudes[j]
            }
            result.add(normalizeAmplitude(maxAmp))
        }

        return result
    }

    private fun normalizeAmplitude(amplitude: Int): Float {
        // Convert 16-bit sample (0-32768) to 0.1-1.0 range
        val normalized = amplitude / 32768f
        return max(0.1f, min(1f, normalized))
    }

    fun serializeWaveform(amplitudes: List<Float>): String {
        return amplitudes.joinToString(",") { "%.3f".format(it) }
    }

    fun deserializeWaveform(data: String): List<Float> {
        if (data.isBlank()) return emptyList()
        return data.split(",").mapNotNull { it.toFloatOrNull() }
    }
}
