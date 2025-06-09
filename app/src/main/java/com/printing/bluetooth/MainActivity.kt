package com.printing.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothPrinterHelper: BluetoothPrinterHelper
    private var selectedPrinter: BluetoothDevice? = null

    private lateinit var printTextEditText: EditText
    private lateinit var selectPrinterButton: Button
    private lateinit var printButton: Button
    private lateinit var disconnectButton: Button

    private lateinit var requestBluetoothPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothPrinterHelper = BluetoothPrinterHelper(this)

        printTextEditText = findViewById(R.id.printTextEditText)
        selectPrinterButton = findViewById(R.id.selectPrinterButton)
        printButton = findViewById(R.id.printButton)
        disconnectButton = findViewById(R.id.disconnectButton)

        requestBluetoothPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allPermissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[android.Manifest.permission.BLUETOOTH_SCAN] == true &&
                        permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
            } else {
                permissions[android.Manifest.permission.BLUETOOTH] == true &&
                        permissions[android.Manifest.permission.BLUETOOTH_ADMIN] == true &&
                        permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "Izin Bluetooth diberikan", Toast.LENGTH_SHORT).show()
                checkAndSelectPrinter()
            } else {
                Toast.makeText(this, "Izin Bluetooth tidak diberikan. Fungsi cetak tidak akan berfungsi.", Toast.LENGTH_LONG).show()
            }
        }

        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth diaktifkan", Toast.LENGTH_SHORT).show()
                checkAndSelectPrinter()
            } else {
                Toast.makeText(this, "Bluetooth tidak diaktifkan", Toast.LENGTH_SHORT).show()
            }
        }

        selectPrinterButton.setOnClickListener {
            checkAndSelectPrinter()
        }

        printButton.setOnClickListener {
            val textToPrint = printTextEditText.text.toString()
            if (textToPrint.isNotBlank()) {
                printData(textToPrint)
            } else {
                Toast.makeText(this, "Masukkan teks yang ingin dicetak", Toast.LENGTH_SHORT).show()
            }
        }

        disconnectButton.setOnClickListener {
            bluetoothPrinterHelper.closeConnection()
            selectedPrinter = null
            updateUiState()
        }

        updateUiState()
    }

    override fun onStart() {
        super.onStart()
        if (!bluetoothPrinterHelper.checkBluetoothPermissions()) {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            requestBluetoothPermissionsLauncher.launch(permissionsToRequest)
        }
    }

    private fun checkAndSelectPrinter() {
        if (!bluetoothPrinterHelper.isBluetoothAvailable()) {
            Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothPrinterHelper.checkBluetoothPermissions()) {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            requestBluetoothPermissionsLauncher.launch(permissionsToRequest)
            return
        }

        if (!bluetoothPrinterHelper.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                enableBluetoothLauncher.launch(enableBtIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal mengaktifkan Bluetooth: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "Error enabling Bluetooth: ${e.message}", e)
            }
            return
        }

        showPairedDevicesDialog()
    }

    private fun showPairedDevicesDialog() {
        // PERBAIKAN: Pemeriksaan izin BLUETOOTH_CONNECT secara eksplisit
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin BLUETOOTH_CONNECT diperlukan untuk melihat nama perangkat.", Toast.LENGTH_LONG).show()
            // Kita bisa meminta izin lagi jika diperlukan, tetapi di sini kita asumsikan
            // checkAndSelectPrinter() atau onStart() sudah menanganinya.
            // Jika Anda ingin lebih agresif, Anda bisa panggil requestBluetoothPermissionsLauncher.launch() di sini.
            return
        }
        // END PERBAIKAN

        val pairedDevices = bluetoothPrinterHelper.getPairedDevices()
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(this, "Tidak ada perangkat Bluetooth yang dipasangkan. Pastikan printer Anda sudah dipasangkan di pengaturan perangkat.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceNames = pairedDevices.map { it.name ?: it.address }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pilih Printer Bluetooth")
            .setItems(deviceNames) { dialog, which ->
                val deviceArray = pairedDevices.toTypedArray()
                val deviceToConnect = deviceArray[which]
                connectToPrinter(deviceToConnect)
            }
            .show()
    }

    private fun connectToPrinter(device: BluetoothDevice) {
        // PERBAIKAN: Pemeriksaan izin BLUETOOTH_CONNECT secara eksplisit sebelum mencoba koneksi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin BLUETOOTH_CONNECT diperlukan untuk terhubung ke printer.", Toast.LENGTH_LONG).show()
            // Di sini kita bisa juga meminta izin lagi jika mau,
            // atau cukup mengandalkan alur checkAndSelectPrinter() untuk menanganinya.
            requestBluetoothPermissionsLauncher.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT)) // Minta izin lagi
            return
        }
        // END PERBAIKAN

        Toast.makeText(this, "Mencoba terhubung ke ${device.name ?: device.address}...", Toast.LENGTH_SHORT).show()
        bluetoothPrinterHelper.connectToPrinter(device) { success, errorMessage ->
            if (success) {
                selectedPrinter = device
                Toast.makeText(this, "Berhasil terhubung ke ${device.name ?: device.address}", Toast.LENGTH_SHORT).show()
            } else {
                selectedPrinter = null
                Toast.makeText(this, "Gagal terhubung: $errorMessage", Toast.LENGTH_LONG).show()
            }
            updateUiState()
        }
    }

    private fun printData(data: String) {
        if (selectedPrinter == null) {
            Toast.makeText(this, "Pilih printer terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothPrinterHelper.printText(data) { success, errorMessage ->
            if (success) {
                // Toast sudah ada di helper
            } else {
                Toast.makeText(this, "Gagal mencetak: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUiState() {
        val isConnected = (selectedPrinter != null)
        printButton.isEnabled = isConnected
        disconnectButton.isEnabled = isConnected
        selectPrinterButton.isEnabled = !isConnected
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothPrinterHelper.closeConnection()
    }
}