Proyek Cetak Bluetooth Android (Modul Native)

Proyek ini menyediakan implementasi dasar modul pencetakan Bluetooth native untuk aplikasi Android. Modul ini memungkinkan aplikasi Anda untuk memindai perangkat Bluetooth yang dipasangkan, terhubung ke printer Bluetooth (biasanya POS printer yang mendukung Serial Port Profile / SPP), dan mengirim data teks untuk dicetak.

Daftar Isi
Fitur Utama
Persyaratan Sistem
Pemasangan
Penggunaan
Izin Android
Penanganan Perizinan Runtime
Struktur Kode
Pengembangan Lebih Lanjut
Lisensi
Kontribusi
Fitur Utama
Pencarian Perangkat Bluetooth: Mendapatkan daftar perangkat Bluetooth yang sudah dipasangkan.
Koneksi Bluetooth: Membuat koneksi ke printer Bluetooth menggunakan Serial Port Profile (SPP).
Pencetakan Teks: Mengirim data teks sederhana ke printer Bluetooth.
Penanganan Izin Android: Mengelola izin Bluetooth dan lokasi yang diperlukan sesuai dengan versi Android (dari API 23 hingga API 35).
Desain Modular: Logika Bluetooth dipisahkan dalam kelas BluetoothPrinterHelper untuk penggunaan yang mudah.
Persyaratan Sistem
Android Studio
Perangkat Android (fisik atau emulator) dengan Bluetooth diaktifkan.
Printer Bluetooth yang mendukung Serial Port Profile (SPP) dan sudah dipasangkan dengan perangkat Android Anda.
compileSdk dan targetSdk diatur ke 35 (atau versi stabil terbaru).
Bahasa pemrograman: Kotlin.
Pemasangan
Kloning Repositori (Opsional): Jika ini adalah proyek baru Anda, Anda bisa membuat proyek "Empty Activity" baru di Android Studio.

Tambahkan Izin ke AndroidManifest.xml:
Buka app/src/main/AndroidManifest.xml dan tambahkan izin berikut di dalam tag <manifest> (di luar tag <application>):

XML

<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
Perbarui build.gradle (Module :app):
Pastikan compileSdk dan targetSdk Anda diatur ke 35.

Gradle

android {
    compileSdk 35 // Atau versi stabil terbaru
    defaultConfig {
        targetSdk 35 // Atau versi stabil terbaru
        // ...
    }
    // ...
}
Tambahkan File Sumber:

Buat file BluetoothPrinterHelper.kt di direktori paket utama Anda (misalnya, app/src/main/java/com/your/package/name/).
Perbarui file MainActivity.kt di direktori yang sama.
Buat atau perbarui file layout activity_main.xml di app/src/main/res/layout/.
Pastikan untuk mengganti com.your.package.name dengan nama paket aplikasi Anda yang sebenarnya di semua file sumber.

Penggunaan
Pasangkan Printer Bluetooth:
Pastikan printer Bluetooth Anda sudah dipasangkan dengan perangkat Android Anda melalui pengaturan Bluetooth sistem sebelum menjalankan aplikasi.

Jalankan Aplikasi:

Instal dan jalankan aplikasi di perangkat Android Anda.
Aplikasi akan meminta izin Bluetooth dan Lokasi. Pastikan Anda mengizinkan semua permintaan.
Jika Bluetooth tidak aktif, aplikasi akan meminta Anda untuk mengaktifkannya.
Pilih Printer:

Tekan tombol "Pilih Printer Bluetooth".
Dialog akan muncul menampilkan daftar perangkat Bluetooth yang sudah dipasangkan.
Pilih printer Anda dari daftar.
Cetak Teks:

Setelah printer terhubung (Anda akan melihat toast konfirmasi), masukkan teks di kolom input.
Tekan tombol "Cetak Teks" untuk mengirim teks ke printer.
Putuskan Koneksi:

Tekan tombol "Putuskan Koneksi" untuk menutup koneksi Bluetooth dengan printer.
Izin Android
Proyek ini memerlukan izin Bluetooth dan Lokasi untuk berfungsi dengan baik.

android.permission.BLUETOOTH
android.permission.BLUETOOTH_ADMIN
android.permission.BLUETOOTH_SCAN (Untuk API 31+)
android.permission.BLUETOOTH_CONNECT (Untuk API 31+)
android.permission.ACCESS_FINE_LOCATION (Untuk API 23+ untuk memindai perangkat Bluetooth)
android.permission.ACCESS_COARSE_LOCATION (Untuk API 23+ sebagai alternatif ACCESS_FINE_LOCATION)
Penanganan Perizinan Runtime
Untuk perangkat yang menjalankan Android 6.0 (API 23) atau lebih baru, aplikasi harus meminta izin sensitif (seperti lokasi dan izin Bluetooth baru) secara runtime. Proyek ini mengimplementasikan penanganan perizinan ini menggunakan ActivityResultLauncher untuk API 30+ dan ActivityCompat.requestPermissions untuk versi sebelumnya.

Struktur Kode
MainActivity.kt:

Aktivitas utama yang bertanggung jawab untuk UI dan interaksi pengguna.
Menginisialisasi dan menggunakan BluetoothPrinterHelper.
Menangani permintaan izin runtime dan mengaktifkan Bluetooth menggunakan ActivityResultLauncher.
Menampilkan dialog perangkat yang dipasangkan dan memicu koneksi/pencetakan.
BluetoothPrinterHelper.kt:

Kelas utilitas yang berisi sebagian besar logika inti Bluetooth.
Menangani pemeriksaan status Bluetooth, pemeriksaan dan permintaan izin Bluetooth.
Menyediakan metode untuk mendapatkan perangkat yang dipasangkan, terhubung ke printer, mengirim data cetak (teks), dan memutuskan koneksi.
Menggunakan UUID standar untuk Serial Port Profile (SPP).
activity_main.xml:

File layout XML untuk MainActivity, berisi input teks, dan tombol untuk memilih printer, mencetak, dan memutuskan koneksi.
Pengembangan Lebih Lanjut
Proyek ini menyediakan fondasi dasar. Anda dapat mengembangkannya lebih lanjut dengan fitur-fitur berikut:

Pencetakan Gambar/Grafis: Implementasikan pengiriman data gambar ke printer (mungkin memerlukan konversi ke format yang didukung printer).
Perintah ESC/POS: Kirim perintah raw ESC/POS untuk kontrol printer yang lebih canggih (misalnya, bold teks, kode batang, feed kertas, dll.).
Penemuan Perangkat Bluetooth Baru: Tambahkan fungsionalitas untuk memindai dan memasangkan dengan perangkat Bluetooth yang belum dipasangkan.
UI yang Lebih Baik: Tingkatkan UI untuk pemilihan perangkat, indikator status koneksi, dan feedback pencetakan.
Dukungan Multi-Printer: Memungkinkan aplikasi untuk mengingat dan beralih antara beberapa printer.
Error Handling yang Lebih Granular: Tangani jenis kesalahan koneksi dan pencetakan yang lebih spesifik.
