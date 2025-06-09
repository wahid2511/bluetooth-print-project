package com.printing.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID standar untuk Serial Port Profile (SPP)
    // Sebagian besar printer Bluetooth menggunakan ini
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        private const val TAG = "BluetoothPrinterHelper"
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_BLUETOOTH_PERMISSIONS = 2
    }

    //region Permission and Bluetooth Status Check

    /**
     * Memeriksa apakah aplikasi memiliki semua izin Bluetooth yang diperlukan
     * berdasarkan versi SDK Android.
     * @return true jika semua izin diberikan, false jika tidak.
     */
    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31) ke atas (termasuk SDK 35)
            val bluetoothScanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val bluetoothConnectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            // BLUETOOTH_ADVERTISE tidak diperlukan untuk koneksi klien (printer)
            bluetoothScanPermission && bluetoothConnectPermission
        } else { // Android 6.0 (API 23) hingga Android 11 (API 30)
            // Diperlukan izin lokasi untuk memindai perangkat Bluetooth
            val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    fineLocationPermission
        }
    }

    /**
     * Meminta izin Bluetooth yang diperlukan dari pengguna.
     * Harus dipanggil dari Activity.
     * @param activity Activity yang memanggil permintaan izin.
     */
    fun requestBluetoothPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31) ke atas
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        } else { // Android 6.0 (API 23) hingga Android 11 (API 30)
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION // Untuk memindai dan menemukan perangkat
                ),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }
    }

    /**
     * Memeriksa apakah perangkat memiliki adaptor Bluetooth.
     * @return true jika Bluetooth tersedia, false jika tidak.
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Memeriksa apakah Bluetooth diaktifkan di perangkat.
     * @return true jika Bluetooth aktif, false jika tidak.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Meminta pengguna untuk mengaktifkan Bluetooth jika belum aktif.
     * @param activity Activity yang memanggil permintaan.
     */
    fun enableBluetooth(activity: Activity) {
        if (!isBluetoothEnabled()) {
            // Tambahkan pemeriksaan izin BLUETOOTH_CONNECT secara eksplisit di sini
            // untuk memuaskan peringatan lint dan memastikan izin tersedia.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    Log.w(TAG, "Tidak dapat mengaktifkan Bluetooth: Izin BLUETOOTH_CONNECT tidak diberikan.")
                    // Opsional: berikan feedback ke pengguna atau minta izin lagi
                    Toast.makeText(context, "Tidak dapat mengaktifkan Bluetooth: Izin BLUETOOTH_CONNECT diperlukan.", Toast.LENGTH_LONG).show()
                    requestBluetoothPermissions(activity) // Minta izin lagi
                }
            } else {
                // Untuk API level < S, cukup izin BLUETOOTH (yang sudah dicek oleh checkBluetoothPermissions)
                // Peringatan ini sering muncul di API < S karena 'BLUETOOTH' juga dicentang.
                // Asumsi: checkBluetoothPermissions() sudah dipanggil dan disetujui.
                // Namun, untuk keamanan, kita bisa tambahkan cek eksplisit di sini juga.
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    Log.w(TAG, "Tidak dapat mengaktifkan Bluetooth: Izin BLUETOOTH tidak diberikan.")
                    Toast.makeText(context, "Tidak dapat mengaktifkan Bluetooth: Izin BLUETOOTH diperlukan.", Toast.LENGTH_LONG).show()
                    requestBluetoothPermissions(activity) // Minta izin lagi
                }
            }
        }
    }

    //endregion

    //region Device Discovery and Pairing

    /**
     * Mendapatkan daftar perangkat Bluetooth yang sudah dipasangkan dengan perangkat ini.
     * Membutuhkan izin BLUETOOTH_CONNECT di Android 12+ dan BLUETOOTH sebelumnya.
     * @return Set<BluetoothDevice>? atau null jika izin tidak ada/Bluetooth tidak tersedia.
     */
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (!isBluetoothAvailable() || !isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth tidak tersedia atau tidak diaktifkan.")
            return null
        }
        // Periksa izin koneksi Bluetooth sebelum mengakses bondedDevices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Izin BLUETOOTH_CONNECT tidak diberikan.")
                return null
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Izin BLUETOOTH tidak diberikan.")
                return null
            }
        }
        return bluetoothAdapter?.bondedDevices
    }

    //endregion

    //region Connection and Printing

    /**
     * Menghubungkan ke printer Bluetooth yang ditentukan.
     * Koneksi dilakukan di background thread.
     * @param device Perangkat Bluetooth printer.
     * @param callback Callback untuk memberitahu hasil koneksi (sukses/gagal).
     */
    fun connectToPrinter(device: BluetoothDevice, callback: (Boolean, String?) -> Unit) {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Izin Bluetooth tidak diberikan.")
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Izin Bluetooth tidak diberikan.", Toast.LENGTH_SHORT).show()
            }
            callback(false, "Izin Bluetooth tidak diberikan.")
            return
        }

        // Batalkan penemuan jika sedang berjalan, karena dapat memperlambat koneksi
        // Membutuhkan izin BLUETOOTH_SCAN di Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter?.cancelDiscovery()
            } else {
                Log.w(TAG, "BLUETOOTH_SCAN permission not granted, cannot cancel discovery.")
            }
        } else {
            bluetoothAdapter?.cancelDiscovery()
        }


        Thread {
            try {
                // Pastikan soket sebelumnya ditutup jika ada
                closeConnection()

                // Membuat RFCOMM socket
                // Membutuhkan izin BLUETOOTH_CONNECT di Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        throw SecurityException("BLUETOOTH_CONNECT permission not granted for createRfcommSocketToServiceRecord.")
                    }
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect() // Ini adalah panggilan yang memblokir
                outputStream = bluetoothSocket?.outputStream
                Log.d(TAG, "Terhubung ke printer: ${device.name ?: device.address}")
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Terhubung ke ${device.name ?: device.address}", Toast.LENGTH_SHORT).show()
                }
                callback(true, null)
            } catch (e: SecurityException) {
                // Tangani khusus jika izin BLUETOOTH_CONNECT hilang saat createRfcommSocketToServiceRecord
                Log.e(TAG, "Izin Bluetooth hilang saat mencoba koneksi: ${e.message}", e)
                closeConnection()
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Gagal terhubung: Izin Bluetooth hilang.", Toast.LENGTH_LONG).show()
                }
                callback(false, "Gagal terhubung: Izin Bluetooth hilang.")
            }
            catch (e: IOException) {
                Log.e(TAG, "Gagal terhubung ke printer: ${e.message}", e)
                closeConnection() // Tutup soket jika koneksi gagal
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Gagal terhubung: ${e.message}", Toast.LENGTH_LONG).show()
                }
                callback(false, "Gagal terhubung: ${e.message}")
            }
            catch (e: Exception) {
                Log.e(TAG, "Terjadi kesalahan tidak terduga saat mencoba koneksi: ${e.message}", e)
                closeConnection()
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Gagal terhubung: Kesalahan tak terduga.", Toast.LENGTH_LONG).show()
                }
                callback(false, "Gagal terhubung: Kesalahan tak terduga.")
            }
        }.start()
    }

    /**
     * Mengirim teks ke printer yang terhubung.
     * Pencetakan dilakukan di background thread.
     * @param text Teks yang akan dicetak.
     * @param callback Callback untuk memberitahu hasil pencetakan (sukses/gagal).
     */
    fun printText(text: String, callback: (Boolean, String?) -> Unit) {
        if (outputStream == null) {
            val errorMessage = "Printer tidak terhubung atau output stream tidak tersedia."
            Log.e(TAG, errorMessage)
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
            callback(false, errorMessage)
            return
        }

        Thread {
            try {
                outputStream?.write(text.toByteArray())
                outputStream?.flush() // Pastikan semua data terkirim
                Log.d(TAG, "Teks berhasil dicetak: $text")
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Teks berhasil dicetak", Toast.LENGTH_SHORT).show()
                }
                callback(true, null)
            } catch (e: IOException) {
                val errorMessage = "Gagal mencetak teks: ${e.message}"
                Log.e(TAG, errorMessage, e)
                closeConnection() // Tutup koneksi jika ada error saat menulis
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                callback(false, errorMessage)
            }
            catch (e: Exception) {
                val errorMessage = "Terjadi kesalahan tidak terduga saat mencetak: ${e.message}"
                Log.e(TAG, errorMessage, e)
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                callback(false, errorMessage)
            }
        }.start()
    }

    /**
     * Menutup koneksi Bluetooth ke printer.
     */
    fun closeConnection() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
            Log.d(TAG, "Koneksi Bluetooth ditutup.")
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Koneksi ditutup", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error menutup koneksi: ${e.message}", e)
        }
    }

    //endregion
}