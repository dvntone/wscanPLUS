package com.wscanplus.app.capabilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object CameraIrSelfTest {
    const val IR_BRIGHTNESS_THRESHOLD = 245
    const val IR_MIN_BRIGHT_FRAMES = 3
    const val MAX_FRAMES = 150

    suspend fun runTest(
        context: Context,
        lifecycleOwner: LifecycleOwner,
    ): CameraIrStatus =
        suspendCancellableCoroutine { continuation ->
            if (!hasCameraPermission(context)) {
                continuation.resume(CameraIrStatus.UNTESTED)
                return@suspendCancellableCoroutine
            }
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                continuation.resume(CameraIrStatus.NOT_CAPABLE)
                return@suspendCancellableCoroutine
            }

            val classes = loadCameraXClasses()
            if (classes == null) {
                continuation.resume(CameraIrStatus.UNTESTED)
                return@suspendCancellableCoroutine
            }

            val analyzerExecutor = Executors.newSingleThreadExecutor()
            val completed = AtomicBoolean(false)
            var providerRef: Any? = null

            fun finish(status: CameraIrStatus) {
                if (!completed.compareAndSet(false, true)) {
                    return
                }
                analyzerExecutor.shutdownNow()
                ContextCompat.getMainExecutor(context).execute {
                    runCatching {
                        providerRef
                            ?.javaClass
                            ?.getMethod("unbindAll")
                            ?.invoke(providerRef)
                    }
                    if (continuation.isActive) {
                        continuation.resume(status)
                    }
                }
            }

            continuation.invokeOnCancellation {
                finish(CameraIrStatus.UNTESTED)
            }

            try {
                val providerFuture =
                    classes.processCameraProvider
                        .getMethod("getInstance", Context::class.java)
                        .invoke(null, context)
                providerFuture
                    .javaClass
                    .getMethod("addListener", Runnable::class.java, Executor::class.java)
                    .invoke(
                        providerFuture,
                        Runnable {
                            try {
                                providerRef =
                                    providerFuture
                                        .javaClass
                                        .getMethod("get")
                                        .invoke(providerFuture)

                                val imageAnalysis = buildImageAnalysis(classes)
                                val analyzer =
                                    createAnalyzer(
                                        classes = classes,
                                        executor = analyzerExecutor,
                                        finish = ::finish,
                                    )
                                imageAnalysis
                                    .javaClass
                                    .getMethod(
                                        "setAnalyzer",
                                        Executor::class.java,
                                        classes.analyzer,
                                    ).invoke(imageAnalysis, analyzerExecutor, analyzer)

                                val selector =
                                    createCameraSelector(
                                        selectorBuilderClass = classes.cameraSelectorBuilder,
                                        context = context,
                                    )
                                val useCaseArray =
                                    java.lang.reflect.Array.newInstance(classes.useCase, 1).also { array ->
                                        java.lang.reflect.Array
                                            .set(array, 0, imageAnalysis)
                                    }

                                providerRef
                                    ?.javaClass
                                    ?.getMethod(
                                        "bindToLifecycle",
                                        LifecycleOwner::class.java,
                                        classes.cameraSelector,
                                        useCaseArray.javaClass,
                                    )?.invoke(providerRef, lifecycleOwner, selector, useCaseArray)
                            } catch (_: Throwable) {
                                finish(CameraIrStatus.UNTESTED)
                            }
                        },
                        ContextCompat.getMainExecutor(context),
                    )
            } catch (_: Throwable) {
                finish(CameraIrStatus.UNTESTED)
            }
        }

    private fun buildImageAnalysis(classes: CameraXClasses): Any {
        val builder =
            classes.imageAnalysisBuilder
                .getDeclaredConstructor()
                .newInstance()
        runCatching {
            val keepLatest =
                classes.imageAnalysis
                    .getField("STRATEGY_KEEP_ONLY_LATEST")
                    .getInt(null)
            builder
                .javaClass
                .getMethod("setBackpressureStrategy", Int::class.javaPrimitiveType)
                .invoke(builder, keepLatest)
        }
        return builder
            .javaClass
            .getMethod("build")
            .invoke(builder) as Any
    }

    private fun createAnalyzer(
        classes: CameraXClasses,
        executor: ExecutorService,
        finish: (CameraIrStatus) -> Unit,
    ): Any {
        val frameState = FrameState()
        return Proxy.newProxyInstance(
            classes.analyzer.classLoader,
            arrayOf(classes.analyzer),
        ) { _, method, args ->
            if (method.name != "analyze") {
                return@newProxyInstance null
            }
            val imageProxy = args?.firstOrNull() ?: return@newProxyInstance null
            try {
                frameState.totalFrames += 1
                if (isBrightFrame(imageProxy)) {
                    frameState.brightFrames += 1
                }
                if (frameState.brightFrames >= IR_MIN_BRIGHT_FRAMES) {
                    finish(CameraIrStatus.CAPABLE)
                } else if (frameState.totalFrames >= MAX_FRAMES) {
                    finish(CameraIrStatus.NOT_CAPABLE)
                }
            } finally {
                imageProxy
                    .javaClass
                    .getMethod("close")
                    .invoke(imageProxy)
                if (frameState.totalFrames >= MAX_FRAMES) {
                    executor.shutdown()
                }
            }
            null
        }
    }

    private fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun isBrightFrame(imageProxy: Any): Boolean {
        val planes =
            imageProxy
                .javaClass
                .getMethod("getPlanes")
                .invoke(imageProxy) as Array<*>
        val yPlane = planes.firstOrNull() ?: return false
        val buffer =
            yPlane
                .javaClass
                .getMethod("getBuffer")
                .invoke(yPlane) as ByteBuffer
        val duplicate = buffer.duplicate()

        var brightest = 0
        while (duplicate.hasRemaining()) {
            val luminance = duplicate.get().toInt() and 0xFF
            if (luminance > brightest) {
                brightest = luminance
            }
            if (brightest >= IR_BRIGHTNESS_THRESHOLD) {
                return true
            }
        }
        return false
    }

    private fun createCameraSelector(
        selectorBuilderClass: Class<*>,
        context: Context,
    ): Any {
        val builder =
            selectorBuilderClass
                .getDeclaredConstructor()
                .newInstance()
        builder
            .javaClass
            .getMethod("requireLensFacing", Int::class.javaPrimitiveType)
            .invoke(builder, preferredLensFacing(context))
        return builder
            .javaClass
            .getMethod("build")
            .invoke(builder) as Any
    }

    private fun preferredLensFacing(context: Context): Int {
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val backFacing = CameraCharacteristics.LENS_FACING_BACK
        val frontFacing = CameraCharacteristics.LENS_FACING_FRONT

        cameraManager
            ?.cameraIdList
            ?.forEach { cameraId: String ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == backFacing) {
                    return backFacing
                }
            }

        cameraManager
            ?.cameraIdList
            ?.forEach { cameraId: String ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == frontFacing) {
                    return frontFacing
                }
            }

        return backFacing
    }

    private fun loadCameraXClasses(): CameraXClasses? =
        try {
            CameraXClasses(
                processCameraProvider = Class.forName("androidx.camera.lifecycle.ProcessCameraProvider"),
                imageAnalysis = Class.forName("androidx.camera.core.ImageAnalysis"),
                imageAnalysisBuilder = Class.forName("androidx.camera.core.ImageAnalysis\$Builder"),
                analyzer = Class.forName("androidx.camera.core.ImageAnalysis\$Analyzer"),
                cameraSelector = Class.forName("androidx.camera.core.CameraSelector"),
                cameraSelectorBuilder = Class.forName("androidx.camera.core.CameraSelector\$Builder"),
                useCase = Class.forName("androidx.camera.core.UseCase"),
            )
        } catch (_: Throwable) {
            null
        }

    private data class CameraXClasses(
        val processCameraProvider: Class<*>,
        val imageAnalysis: Class<*>,
        val imageAnalysisBuilder: Class<*>,
        val analyzer: Class<*>,
        val cameraSelector: Class<*>,
        val cameraSelectorBuilder: Class<*>,
        val useCase: Class<*>,
    )

    private data class FrameState(
        var totalFrames: Int = 0,
        var brightFrames: Int = 0,
    )
}
