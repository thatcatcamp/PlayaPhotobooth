package com.capricallctx.playaphotobooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
    
    private var serverSocket: BluetoothServerSocket? = null
    private var isServerRunning = false
    
    // Enable Bluetooth discovery
    fun enableDiscoverability(activity: ComponentActivity) {
        if (bluetoothAdapter?.isEnabled == true) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            activity.startActivity(discoverableIntent)
        }
    }
    
    // Start server to share photos
    suspend fun startSharingServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (bluetoothAdapter?.isEnabled != true) {
                Log.e("BluetoothManager", "Bluetooth not enabled")
                return@withContext false
            }
            
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("PhotoShare", serviceUUID)
            isServerRunning = true
            
            Log.d("BluetoothManager", "Bluetooth server started, waiting for connections...")
            true
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Failed to start server", e)
            false
        }
    }
    
    // Accept incoming connections and send photo
    suspend fun sharePhoto(photoFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = serverSocket?.accept()
            socket?.let { clientSocket ->
                sendFile(clientSocket, photoFile)
                clientSocket.close()
                return@withContext true
            }
            false
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Failed to share photo", e)
            false
        }
    }
    
    // Connect to another device and receive photo
    suspend fun connectToDevice(device: BluetoothDevice): BluetoothSocket? = withContext(Dispatchers.IO) {
        try {
            val socket = device.createRfcommSocketToServiceRecord(serviceUUID)
            socket.connect()
            socket
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Failed to connect to device", e)
            null
        }
    }
    
    // Send file over Bluetooth
    private suspend fun sendFile(socket: BluetoothSocket, file: File) = withContext(Dispatchers.IO) {
        try {
            val outputStream: OutputStream = socket.outputStream
            val fileInputStream = FileInputStream(file)
            
            // Send file size first
            val fileSize = file.length().toString()
            outputStream.write(fileSize.toByteArray())
            outputStream.write("\n".toByteArray())
            
            // Send file data
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            fileInputStream.close()
            outputStream.flush()
            
            Log.d("BluetoothManager", "File sent successfully: ${file.name}")
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Failed to send file", e)
        }
    }
    
    // Get list of paired devices
    fun getPairedDevices(): Set<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices
    }
    
    // Check if Bluetooth is available and enabled
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
    
    // Stop the server
    fun stopServer() {
        try {
            isServerRunning = false
            serverSocket?.close()
            serverSocket = null
            Log.d("BluetoothManager", "Bluetooth server stopped")
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Failed to stop server", e)
        }
    }
}