package com.wscanplus.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wscanplus.app.capabilities.CapabilityProbe
import com.wscanplus.app.capabilities.DeviceCapabilityManifest
import com.wscanplus.app.cti.BuildConfigCrowdSecCtiKeyProvider
import com.wscanplus.app.cti.CrowdSecCtiClient
import com.wscanplus.app.cti.CtiCacheRepository
import com.wscanplus.app.cti.CtiLookupResult
import com.wscanplus.app.cti.SharedPrefsQuotaTracker
import com.wscanplus.app.db.DbPassphraseProvider
import com.wscanplus.app.gemini.GeminiAnalysisResult
import com.wscanplus.app.gemini.GeminiThreatAnalyzer
import com.wscanplus.app.kismet.KismetConfigStore
import com.wscanplus.app.kismet.KismetGpsClient
import com.wscanplus.app.location.FusedLocationSampler
import com.wscanplus.app.location.LocationSample
import com.wscanplus.app.privacy.ConsentStore
import com.wscanplus.app.sensor.BarometerSampler
import com.wscanplus.app.sensor.FloorEstimate
import com.wscanplus.core.db.RetentionManager
import com.wscanplus.core.db.WscanDatabase
import com.wscanplus.core.db.entity.GeminiNarrativeEntity
import com.wscanplus.core.db.entity.ScanResultEntity
import com.wscanplus.core.db.entity.ScanSessionEntity
import com.wscanplus.core.db.entity.ThreatSignalEntity
import com.wscanplus.core.scanner.ScannerChain
import com.wscanplus.core.scanner.WifiScanResult
import com.wscanplus.core.scanner.toScanInput
import com.wscanplus.core.threat.BssidFingerprintHeuristic
import com.wscanplus.core.threat.EncryptionDowngradeHeuristic
import com.wscanplus.core.threat.EnvironmentType
import com.wscanplus.core.threat.EvilTwinHeuristic
import com.wscanplus.core.threat.HeuristicEngine
import com.wscanplus.core.threat.KarmaHeuristic
import com.wscanplus.core.threat.OuiAssetLoader
import com.wscanplus.core.threat.OuiLookup
import com.wscanplus.core.threat.PolicyGate
import com.wscanplus.core.threat.RssiAnomalyHeuristic
import com.wscanplus.core.threat.ScanContext
import com.wscanplus.core.threat.ScanInput
import com.wscanplus.core.threat.SsidFloodingHeuristic
import com.wscanplus.core.threat.WepOpenHeuristic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * WatchdogService manages the scanner chain and the ADB communication socket.
 *
 * Foreground service — foregroundServiceType includes location|dataSync.
 * Must call startForeground() within a few seconds of startForegroundService() or the
 * system throws ForegroundServiceDidNotStartInTimeException (API 31+).
 *
 * Serial number is the primary device key in all data structures.
 *
 * Android 15 constraint: the retained dataSync role still has a 6-hour runtime budget in a
 * rolling 24-hour window. On API 35+, the service must handle onTimeout() and stop cleanly.
 * A pre-timeout restart does not reset that budget unless the user brings the app foreground,
 * so the correct hardening behavior is graceful shutdown rather than a restart loop.
 *
 * Threading rule: scanner chain and ServerSocket operations MUST run on background
 * threads. Never call scanners or socket operations on the main thread.
 *
 * TODO (Phase 3): open ServerSocket(9000) on a background thread for ADB comms.
 */
class WatchdogService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ouiLookupRef = AtomicReference<OuiLookup?>(null)
    private val watchdogJson =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }
    private var scannerChain: ScannerChain? = null
    private var scannerExecutor: ExecutorService? = null
    private var startFuture: Future<*>? = null
    private var locationSampler: FusedLocationSampler? = null
    private var startTimeMillis: Long = 0L
    private var latestLocationSample: LocationSample? = null
    private var currentSessionId: Long? = null
    private var lastKismetSendAtMillis: Long = 0L
    private var degradedMode = false
    private lateinit var database: WscanDatabase
    private lateinit var kismetGpsClient: KismetGpsClient
    private lateinit var ctiCacheRepository: CtiCacheRepository
    private lateinit var retentionManager: RetentionManager
    private lateinit var geminiThreatAnalyzer: GeminiThreatAnalyzer
    private var capabilityManifest: DeviceCapabilityManifest? = null
    private var barometerSampler: BarometerSampler? = null

    @Volatile
    var currentFloorEstimate: FloorEstimate? = null
        private set
    private var helloServerSocket: ServerSocket? = null
    private var helloClientSocket: Socket? = null
    private var helloServerJob: Job? = null
    private var lastGeminiAnalysisAtMs: Long = 0L
    private val geminiAnalysisInFlight = AtomicBoolean(false)
    private val engine =
        HeuristicEngine(
            listOf(
                WepOpenHeuristic(),
                EvilTwinHeuristic(),
                EncryptionDowngradeHeuristic(),
                KarmaHeuristic(),
                SsidFloodingHeuristic(),
                RssiAnomalyHeuristic(),
                BssidFingerprintHeuristic(ouiLookup = { bssid -> ouiLookupRef.get()?.lookup(bssid) }),
            ),
        )
    private val policyGate = PolicyGate()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val passphrase = DbPassphraseProvider(applicationContext).getOrCreate()
        if (passphrase == null) {
            Log.e(TAG, "DB passphrase unavailable — cannot open encrypted database; stopping service")
            stopSelf()
            return
        }
        SQLiteDatabase.loadLibs(applicationContext)
        database = WscanDatabase.getInstance(applicationContext, SupportFactory(passphrase))
        retentionManager = RetentionManager(database.scanSessionDao())
        kismetGpsClient =
            KismetGpsClient(
                KismetConfigStore(applicationContext),
                ConsentStore(applicationContext),
            )
        val consentStore = ConsentStore(applicationContext)
        val ctiKeyProvider = BuildConfigCrowdSecCtiKeyProvider()
        val ctiClient = CrowdSecCtiClient(ctiKeyProvider, consentStore)
        val quotaTracker = SharedPrefsQuotaTracker(applicationContext)
        ctiCacheRepository = CtiCacheRepository(database.ctiCacheDao(), ctiClient, quotaTracker)
        geminiThreatAnalyzer = GeminiThreatAnalyzer(consentStore)
        Log.i(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // FOREGROUND_SERVICE_TYPE_* constants are API 29 — safe to inline: ServiceCompat.startForeground()
    // guards the type parameter internally and falls back to startForeground(id, notification)
    // on API < 29. The constant value is copied at compile time and causes no runtime issue.
    @SuppressLint("InlinedApi")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val degraded = intent?.getBooleanExtra(EXTRA_DEGRADED, false) ?: false
        if (!hasStartPermission(degraded)) {
            Log.w(TAG, "Required permission missing on (re)start — stopping and notifying user")
            showPermissionRevokedNotification()
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        degradedMode = degraded
        Log.i(TAG, "Service started (degraded=$degradedMode)")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (scannerChain == null) {
            startTimeMillis = SystemClock.elapsedRealtime()
            startHelloServerIfNeeded()
            val executor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "wscanplus-watchdog")
                }
            scannerExecutor = executor
            if (!degradedMode) {
                locationSampler =
                    FusedLocationSampler(applicationContext, executor) { sample ->
                        latestLocationSample = sample
                        maybeSendKismetUpdate(sample)
                    }
                // Gate on capability manifest when already probed; fall back to
                // runtime sensor check (isAvailable) which is equivalent to manifest.barometer.
                val barometerCapable = capabilityManifest?.barometer ?: true
                val sensorManager = getSystemService(android.hardware.SensorManager::class.java)
                if (barometerCapable && sensorManager != null) {
                    val sampler =
                        BarometerSampler(
                            sensorManager = sensorManager,
                            onEstimate = { estimate -> currentFloorEstimate = estimate },
                            autoCalibrate = true,
                        )
                    if (sampler.isAvailable) {
                        sampler.start()
                        barometerSampler = sampler
                        Log.i(TAG, "BarometerSampler started (autoCalibrate=true)")
                    } else {
                        Log.i(TAG, "No barometer — floor tracking unavailable")
                    }
                }
            }
            scannerChain =
                ScannerChain(applicationContext) { results: List<WifiScanResult> ->
                    // StandardScanner already filters stale results at its own threshold before
                    // delivering here; no duplicate filter needed at this layer (#165).
                    Log.i(TAG, "Scan batch: ${results.size} result(s), session=$currentSessionId")
                    val scanInputs: List<ScanInput> =
                        results.map { result: WifiScanResult ->
                            result.toScanInput()
                        }
                    val context =
                        ScanContext(
                            currentResults = scanInputs,
                            knownProfiles = emptyMap(),
                            baselineNetworkCount = null,
                            baselineStdDev = null,
                            environmentType = EnvironmentType.RESIDENTIAL,
                        )
                    val rawSignals = engine.analyze(context)
                    val filtered = policyGate.filter(rawSignals)
                    Log.i(
                        TAG,
                        "PolicyGate: ${filtered.size} of ${rawSignals.size} signals passed " +
                            "(min confidence ${filtered.minOfOrNull { signal -> signal.confidence } ?: 0f})",
                    )
                    persistResults(
                        results = results,
                        filteredSignals = filtered,
                    )
                    if (filtered.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        if (now - lastGeminiAnalysisAtMs >= GEMINI_COOLDOWN_MS &&
                            geminiAnalysisInFlight.compareAndSet(false, true)
                        ) {
                            val snapSessionId = currentSessionId
                            serviceScope.launch {
                                try {
                                    when (val result = geminiThreatAnalyzer.analyze(filtered)) {
                                        is GeminiAnalysisResult.Success -> {
                                            Log.i(TAG, "Gemini narrative: ${result.narrative}")
                                            if (snapSessionId != null) {
                                                database.geminiNarrativeDao().insert(
                                                    GeminiNarrativeEntity(
                                                        sessionId = snapSessionId,
                                                        narrative = result.narrative,
                                                        generatedAt = System.currentTimeMillis(),
                                                        signalCount = filtered.size,
                                                        modelName = GeminiThreatAnalyzer.MODEL_NAME,
                                                    ),
                                                )
                                                lastGeminiAnalysisAtMs = System.currentTimeMillis()
                                            }
                                        }
                                        GeminiAnalysisResult.NoThreats ->
                                            Log.d(TAG, "Gemini: no threats")
                                        GeminiAnalysisResult.ConsentRequired ->
                                            Log.d(TAG, "Gemini: consent not granted")
                                        GeminiAnalysisResult.Unavailable ->
                                            Log.w(TAG, "Gemini: analysis unavailable")
                                    }
                                } finally {
                                    geminiAnalysisInFlight.set(false)
                                }
                            }
                        }
                    }
                }
            startFuture =
                executor.submit {
                    try {
                        ouiLookupRef.set(OuiAssetLoader.load(applicationContext))
                    } catch (e: Exception) {
                        Log.e(TAG, "OUI asset load failed; vendor signals will be skipped", e)
                    }
                    currentSessionId = createSession()
                    Log.i(TAG, "Scan session created: id=$currentSessionId")
                    serviceScope.launch {
                        try {
                            ctiCacheRepository.pruneExpired()
                            Log.i(TAG, "CTI cache pruned")
                        } catch (e: Exception) {
                            Log.w(TAG, "CTI cache prune failed", e)
                        }
                        try {
                            retentionManager.purge()
                        } catch (e: Exception) {
                            Log.w(TAG, "Retention purge failed", e)
                        }
                    }
                    if (!degradedMode) {
                        locationSampler?.start()
                    }
                    startChain(scannerChain!!)
                }
        }
        return START_STICKY
    }

    /**
     * Returns true if the service has the permissions needed to start.
     *
     * Both modes require ACCESS_FINE_LOCATION — the scanner stack cannot function with
     * coarse-only location. Degraded mode (degraded=true) skips ACCESS_BACKGROUND_LOCATION
     * and the GPS sampler, allowing the service to run without "Allow all the time" access.
     * Full mode (degraded=false) additionally requires ACCESS_BACKGROUND_LOCATION on API 29+.
     */
    private fun hasStartPermission(degraded: Boolean): Boolean {
        val hasFine =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasFine) return false
        if (degraded) return true
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Fires a non-ongoing notification prompting the user to re-grant location permission.
     * On API 33+, silently skips if POST_NOTIFICATIONS was not granted.
     */
    private fun showPermissionRevokedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot show revoked-permission notification — POST_NOTIFICATIONS not granted")
            return
        }
        // Explicit intent to MainActivity — avoids implicit-intent PendingIntent warning.
        // MainActivity.onResume() calls refreshScannerState() which surfaces the re-grant prompt.
        val relaunchIntent =
            Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                relaunchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
        notificationBuilder.setContentTitle("wscan+ scanning paused")
        notificationBuilder.setContentText("Location permission was revoked. Tap to re-grant.")
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
        notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setAutoCancel(true)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_REVOKED, notificationBuilder.build())
    }

    /**
     * Perform a CTI reputation lookup for [ip].
     *
     * Results are logged; callers (e.g. ADB/Kismet integration in Phase 4) can use the
     * returned [CtiLookupResult] to attach reputation data to threat signals.
     *
     * Must be called from a coroutine — [CtiCacheRepository.lookup] is a suspend function.
     */
    suspend fun lookupIp(ip: String): CtiLookupResult {
        val result = ctiCacheRepository.lookup(ip)
        Log.i(TAG, "CTI lookup $ip → ${result::class.simpleName}")
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "Service destroyed")
        helloServerJob?.cancel()
        helloServerJob = null
        closeHelloSockets()
        // Cancel any queued start task before submitting stop, so a pending start cannot
        // race with or follow the stop on the executor queue.
        startFuture?.cancel(true)
        startFuture = null
        val executor = scannerExecutor
        val chain = scannerChain
        scannerChain = null
        scannerExecutor = null
        locationSampler?.stop()
        locationSampler = null
        barometerSampler?.stop()
        barometerSampler = null
        currentFloorEstimate = null
        val sessionId = currentSessionId
        currentSessionId = null
        // stop() must run on a background thread per ScannerChain/StandardScanner threading rule.
        executor?.execute {
            chain?.stop()
            sessionId?.let {
                Log.i(TAG, "Scan session closed: id=$it")
                database.scanSessionDao().markCompleted(it, System.currentTimeMillis())
            }
        }
        executor?.shutdown()
    }

    private fun startHelloServerIfNeeded() {
        if (helloServerJob != null) {
            return
        }

        helloServerJob =
            serviceScope.launch(Dispatchers.IO) {
                if (capabilityManifest == null) {
                    capabilityManifest =
                        try {
                            CapabilityProbe
                                .probe(applicationContext)
                        } catch (e: Exception) {
                            Log.w(TAG, "Capability probe failed; hello payload will omit capabilities", e)
                            null
                        }
                }

                runHelloServerLoop()
            }
    }

    private fun runHelloServerLoop() {
        try {
            val loopbackAddress =
                InetAddress
                    .getByName(HELLO_BIND_HOST)
            val serverSocket =
                ServerSocket(
                    HELLO_PORT,
                    1,
                    loopbackAddress,
                )
            helloServerSocket = serverSocket
            Log.i(TAG, "Watchdog hello transport listening on $HELLO_BIND_HOST:$HELLO_PORT")

            while (!serverSocket.isClosed) {
                val clientSocket =
                    try {
                        serverSocket.accept()
                    } catch (e: SocketException) {
                        if (serverSocket.isClosed) {
                            break
                        }
                        throw e
                    }

                handleHelloConnection(clientSocket)
            }
        } catch (e: Exception) {
            if (!isExpectedHelloShutdown(e)) {
                Log.e(TAG, "Watchdog hello transport failed", e)
            }
        } finally {
            closeHelloSockets()
        }
    }

    private fun handleHelloConnection(clientSocket: Socket) {
        helloClientSocket = clientSocket

        try {
            val helloMessage =
                buildWatchdogHelloJson(
                    deviceId = buildHelloDeviceId(),
                    capabilityManifest = capabilityManifest,
                    json = watchdogJson,
                )

            clientSocket
                .getOutputStream()
                .writer(Charsets.UTF_8)
                .apply {
                    write(helloMessage)
                    write("\n")
                    flush()
                }

            val inputStream = clientSocket.getInputStream()
            while (inputStream.read() != -1) {
                // Keep the connection open until the desktop side disconnects.
            }
        } catch (e: Exception) {
            if (!isExpectedHelloShutdown(e)) {
                Log.w(TAG, "Watchdog hello client connection failed", e)
            }
        } finally {
            try {
                clientSocket.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to close hello client socket", e)
            }
            if (helloClientSocket === clientSocket) {
                helloClientSocket = null
            }
        }
    }

    private fun closeHelloSockets() {
        val clientSocket = helloClientSocket
        helloClientSocket = null
        try {
            clientSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close hello client socket", e)
        }

        val serverSocket = helloServerSocket
        helloServerSocket = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close hello server socket", e)
        }
    }

    private fun isExpectedHelloShutdown(error: Exception): Boolean =
        error is SocketException &&
            (
                error.message?.contains("Socket closed", ignoreCase = true) == true ||
                    error.message?.contains("closed", ignoreCase = true) == true
            )

    private fun createSession(): Long =
        database.scanSessionDao().insert(
            ScanSessionEntity(
                startedAt = System.currentTimeMillis(),
                endedAt = null,
                environmentType = EnvironmentType.RESIDENTIAL,
                deviceSerial = buildDeviceIdentifier(),
            ),
        )

    private fun buildDeviceIdentifier(): String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"

    @Suppress("DEPRECATION")
    private fun buildHelloDeviceId(): String {
        val buildSerial = Build.SERIAL
        return if (buildSerial.isNullOrBlank() || buildSerial == Build.UNKNOWN) {
            buildDeviceIdentifier()
        } else {
            buildSerial
        }
    }

    private fun persistResults(
        results: List<WifiScanResult>,
        filteredSignals: List<com.wscanplus.core.threat.ThreatSignal>,
    ) {
        val sessionId = currentSessionId ?: return
        val locationSample = latestLocationSample
        database.scanResultDao().insertAll(
            results.map { result ->
                ScanResultEntity(
                    sessionId = sessionId,
                    bssid = result.bssid,
                    ssid = result.ssid,
                    capabilities = result.capabilities,
                    rssiDbm = result.signalLevel,
                    frequencyMhz = result.frequencyMhz,
                    channelWidth = result.channelWidth,
                    timestamp = result.timestamp / 1000,
                    isHidden = result.ssid.isBlank(),
                    latitude = locationSample?.latitude,
                    longitude = locationSample?.longitude,
                    accuracyMeters = locationSample?.accuracyMeters,
                    altitudeMeters = locationSample?.altitudeMeters,
                    speedKph = locationSample?.speedKph,
                    locationTimestamp = locationSample?.capturedAt,
                    locationProvider = locationSample?.provider,
                    isMockLocation = locationSample?.isMock,
                )
            },
        )
        if (filteredSignals.isNotEmpty()) {
            database.threatSignalDao().insertAll(
                filteredSignals.map { signal ->
                    ThreatSignalEntity(
                        sessionId = sessionId,
                        bssid = signal.bssid,
                        confidence = signal.confidence,
                        source = signal.source,
                        heuristicType = signal.heuristicType,
                        reasons = signal.reasons,
                        detectedAt = signal.detectedAt,
                        schemaVersion = signal.schemaVersion,
                    )
                },
            )
        }
    }

    private fun maybeSendKismetUpdate(sample: LocationSample) {
        val now = System.currentTimeMillis()
        if (!sample.isFresh(now)) return
        if (now - lastKismetSendAtMillis < KISMET_SEND_INTERVAL_MS) return
        if (kismetGpsClient.send(sample)) {
            lastKismetSendAtMillis = now
        }
    }

    // onStartCommand() re-validates runtime location permission before invoking the chain.
    // This suppresses lint for the background-thread call path after that guard succeeds.
    @SuppressLint("MissingPermission")
    private fun startChain(chain: ScannerChain) = chain.start()

    /**
     * Android 15+ (API 35): dataSync foreground services receive onTimeout() once their
     * rolling 24-hour budget is exhausted. Stop within the grace window so the system
     * does not terminate the process with a timeout failure.
     */
    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        Log.w(
            TAG,
            "Foreground service timeout after ${uptimeMinutes()} minutes " +
                "(startId=$startId, fgsType=0x${fgsType.toString(16)}), stopping service",
        )
        stopSelf()
    }

    private fun uptimeMinutes(): Long {
        val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
        return elapsed / 1000 / 60
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val scannerChannel =
                NotificationChannel(
                    CHANNEL_ID,
                    "wscan+ Scanner",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Active while the WiFi scanner chain is running"
                }
            val alertChannel =
                NotificationChannel(
                    CHANNEL_ID_ALERT,
                    "wscan+ Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Permission and service alerts requiring user action"
                }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(scannerChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder.setContentTitle(if (degradedMode) "wscan+ scanning (degraded)" else "wscan+ scanning")
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setOngoing(true)
        return builder.build()
    }

    companion object {
        private const val TAG = "WatchdogService"
        private const val CHANNEL_ID = "wscanplus_watchdog"
        private const val CHANNEL_ID_ALERT = "wscanplus_alerts"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_ID_REVOKED = 2
        private const val HELLO_BIND_HOST = "127.0.0.1"
        private const val HELLO_PORT = 9000
        private const val KISMET_SEND_INTERVAL_MS = 10_000L
        private const val GEMINI_COOLDOWN_MS = 5 * 60 * 1000L

        /**
         * Pass true to start in degraded mode: ACCESS_FINE_LOCATION is required but
         * ACCESS_BACKGROUND_LOCATION is not checked, and the GPS location sampler is skipped.
         */
        const val EXTRA_DEGRADED = "extra_degraded"
    }
}

@Serializable
internal data class WatchdogHelloPayload(
    val type: String,
    val deviceId: String,
    val capabilities: DeviceCapabilityManifest? = null,
)

internal fun buildWatchdogHelloJson(
    deviceId: String,
    capabilityManifest: DeviceCapabilityManifest?,
    json: Json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        },
): String =
    json.encodeToString(
        WatchdogHelloPayload(
            type = "hello",
            deviceId = deviceId,
            capabilities = capabilityManifest,
        ),
    )
