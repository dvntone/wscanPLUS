package com.wscanplus.app.nmea

import android.util.Log
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

class UsbNmeaServer(
    private val host: String = DEFAULT_HOST,
    private val port: Int = DEFAULT_PORT,
) {
    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    val clientCount: Int
        get() = clients.size

    val boundPort: Int
        get() = serverSocket?.localPort ?: -1

    private val clients = CopyOnWriteArrayList<Socket>()
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    fun start() {
        if (isRunning) return
        try {
            val socket =
                ServerSocket(
                    port,
                    4,
                    InetAddress.getByName(host),
                )
            serverSocket = socket
            isRunning = true
            lastError = null
            acceptThread =
                Thread(
                    { acceptLoop(socket) },
                    "wscanplus-usb-nmea",
                ).apply {
                    isDaemon = true
                    start()
                }
            Log.i(TAG, "USB NMEA server listening on $host:$port")
        } catch (error: Exception) {
            lastError = error.message ?: error.javaClass.simpleName
            Log.e(TAG, "Failed to start USB NMEA server", error)
            stop()
        }
    }

    fun stop() {
        isRunning = false
        acceptThread?.interrupt()
        acceptThread = null

        try {
            serverSocket?.close()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to close USB NMEA server socket", error)
        }
        serverSocket = null

        clients.forEach { socket ->
            try {
                socket.close()
            } catch (error: Exception) {
                Log.w(TAG, "Failed to close USB NMEA client socket", error)
            }
        }
        clients.clear()
    }

    fun broadcast(line: String) {
        if (!isRunning || clients.isEmpty()) return
        val payload = line.toByteArray(StandardCharsets.UTF_8)
        val deadClients = mutableListOf<Socket>()
        clients.forEach { socket ->
            try {
                socket.getOutputStream().write(payload)
                socket.getOutputStream().flush()
            } catch (error: Exception) {
                Log.w(TAG, "USB NMEA client write failed", error)
                deadClients += socket
            }
        }
        deadClients.forEach { socket ->
            clients.remove(socket)
            try {
                socket.close()
            } catch (_: Exception) {
                // Ignore repeated close on dead client.
            }
        }
    }

    private fun acceptLoop(socket: ServerSocket) {
        try {
            while (!socket.isClosed) {
                val client =
                    try {
                        socket.accept()
                    } catch (error: SocketException) {
                        if (socket.isClosed) {
                            break
                        }
                        throw error
                    }
                client.tcpNoDelay = true
                clients += client
                Log.i(TAG, "USB NMEA client connected (${clients.size} total)")
            }
        } catch (error: Exception) {
            if (!isExpectedShutdown(error)) {
                lastError = error.message ?: error.javaClass.simpleName
                Log.e(TAG, "USB NMEA accept loop failed", error)
            }
        } finally {
            // Only reset the running flag when this thread still owns the server socket.
            // A rapid stop()+start() may have already bound a new socket; clearing
            // isRunning in that case would silently kill the new server's healthy state.
            if (serverSocket === socket) {
                isRunning = false
            }
            try {
                socket.close()
            } catch (_: Exception) {
                // Ignore shutdown close failures.
            }
        }
    }

    private fun isExpectedShutdown(error: Exception): Boolean =
        error is SocketException &&
            (
                error.message?.contains("Socket closed", ignoreCase = true) == true ||
                    error.message?.contains("closed", ignoreCase = true) == true
            )

    companion object {
        private const val TAG = "UsbNmeaServer"
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 47393
    }
}
