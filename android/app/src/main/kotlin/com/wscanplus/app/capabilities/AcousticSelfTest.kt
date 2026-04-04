package com.wscanplus.app.capabilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object AcousticSelfTest {
    const val TEST_FREQUENCY_HZ = 19_000
    const val SAMPLE_RATE = 44_100
    const val DETECTION_THRESHOLD = 0.3f

    private const val TONE_DURATION_MS = 200
    private const val CAPTURE_DURATION_MS = 500
    private const val LEAD_IN_MS = 50L

    suspend fun runTest(context: Context): AcousticStatus =
        withContext(Dispatchers.IO) {
            if (!hasRecordAudioPermission(context)) {
                return@withContext AcousticStatus.UNTESTED
            }

            val recordBufferSize =
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            if (recordBufferSize <= 0) {
                return@withContext AcousticStatus.UNTESTED
            }

            val playbackSamples = generateTone()
            val recordedSamples = ShortArray((SAMPLE_RATE * CAPTURE_DURATION_MS) / 1000)
            val playbackBufferSize = playbackSamples.size * Short.SIZE_BYTES
            val captureBufferSize = max(recordBufferSize, recordedSamples.size * Short.SIZE_BYTES)

            val audioRecord =
                AudioRecord
                    .Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat
                            .Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build(),
                    ).setBufferSizeInBytes(captureBufferSize)
                    .build()
            val audioTrack =
                AudioTrack
                    .Builder()
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build(),
                    ).setAudioFormat(
                        AudioFormat
                            .Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    ).setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(playbackBufferSize)
                    .build()

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED ||
                audioTrack.state != AudioTrack.STATE_INITIALIZED
            ) {
                audioRecord.release()
                audioTrack.release()
                return@withContext AcousticStatus.UNTESTED
            }

            try {
                val written =
                    audioTrack.write(
                        playbackSamples,
                        0,
                        playbackSamples.size,
                        AudioTrack.WRITE_BLOCKING,
                    )
                if (written <= 0) {
                    return@withContext AcousticStatus.UNTESTED
                }

                audioRecord.startRecording()
                SystemClock.sleep(LEAD_IN_MS)
                audioTrack.play()
                val read =
                    audioRecord.read(
                        recordedSamples,
                        0,
                        recordedSamples.size,
                        AudioRecord.READ_BLOCKING,
                    )
                audioTrack.stop()
                audioRecord.stop()

                if (read <= 0) {
                    return@withContext AcousticStatus.UNTESTED
                }

                val ratio = computeEnergyRatio(recordedSamples, read)
                if (ratio >= DETECTION_THRESHOLD) {
                    AcousticStatus.CAPABLE
                } else {
                    AcousticStatus.NOT_CAPABLE
                }
            } finally {
                audioTrack.release()
                audioRecord.release()
            }
        }

    private fun hasRecordAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun generateTone(): ShortArray {
        val sampleCount = (SAMPLE_RATE * TONE_DURATION_MS) / 1000
        val samples = ShortArray(sampleCount)
        for (index in 0 until sampleCount) {
            val phase = (2.0 * PI * TEST_FREQUENCY_HZ * index) / SAMPLE_RATE
            samples[index] = (sin(phase) * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        return samples
    }

    private fun computeEnergyRatio(
        samples: ShortArray,
        count: Int,
    ): Float {
        if (count <= 0) {
            return 0f
        }

        var totalEnergy = 0.0
        for (index in 0 until count) {
            val sample = samples[index].toDouble() / Short.MAX_VALUE
            totalEnergy += sample * sample
        }
        if (totalEnergy <= 0.0) {
            return 0f
        }

        val targetEnergy = goertzel(samples, count, TEST_FREQUENCY_HZ.toDouble())
        return (targetEnergy / totalEnergy).toFloat()
    }

    private fun goertzel(
        samples: ShortArray,
        count: Int,
        frequencyHz: Double,
    ): Double {
        val normalizedFrequency = frequencyHz / SAMPLE_RATE
        val coefficient = 2.0 * cos(2.0 * PI * normalizedFrequency)

        var q0: Double
        var q1 = 0.0
        var q2 = 0.0

        for (index in 0 until count) {
            val sample = samples[index].toDouble() / Short.MAX_VALUE
            q0 = coefficient * q1 - q2 + sample
            q2 = q1
            q1 = q0
        }

        return q1 * q1 + q2 * q2 - coefficient * q1 * q2
    }
}
