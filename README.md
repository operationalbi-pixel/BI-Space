# BI-Space Android

Aplikasi Android WebView untuk membuka:

`https://operationalbi-pixel.github.io/form/`

## Fitur wrapper

- Nama aplikasi: **BI-Space**.
- Logo launcher dan splash memakai logo yang diberikan.
- Warna splash `#BD4B49`, yaitu warna rata-rata seluruh pixel logo baru.
- Safe area sistem Android: halaman tidak digambar di belakang notch, status bar, atau navigation bar.
- Mendukung pemilihan satu atau beberapa file dari elemen upload website.
- Mendukung download HTTP/HTTPS dan download Blob yang dibuat website.
- Tombol Back Android kembali ke halaman WebView sebelumnya.

## Fitur native

- Kamera Absensi Break dengan izin Android yang aman.
- Notifikasi bersuara untuk berita terbaru, transfer masuk, dan pengingat Daily.
- Notification dot/badge dan badge belum dibaca pada ikon lonceng Dashboard.
- Notification Center berisi pengaturan kategori, suara, dan riwayat maksimal 100 notifikasi.
- Pemeriksaan background berkala menggunakan sesi login BI-Space yang sama (interval minimum Android sekitar 15 menit).
- Update aplikasi internal dari GitHub Release, tanpa Play Store.

## Signing permanen dan update

Workflow release membutuhkan GitHub Secrets berikut:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Gunakan satu signing key yang sama untuk seluruh versi. Kehilangan atau mengganti key membuat APK berikutnya tidak dapat memperbarui aplikasi yang sudah terpasang.

Setelah push ke `main`, workflow akan membangun signed release APK, mengunggah artifact, dan menerbitkan `BI-Space.apk` pada GitHub Release. Aplikasi memeriksa release terbaru dan meminta persetujuan pengguna sebelum download serta pemasangan.
- Link di luar domain aplikasi dibuka pada browser/peramban yang sesuai.
- Scroll vertikal Stock Card dan Showcase Log diteruskan ke seluruh halaman; tabel tetap dapat digeser horizontal.
- Hanya menggunakan koneksi HTTPS.

## Cara membuat APK melalui GitHub

> GitHub Actions tidak dapat membaca workflow dari dalam file ZIP. Ekstrak ZIP terlebih dahulu, lalu upload **seluruh isi folder ini**, termasuk folder tersembunyi `.github`, ke root repository.

1. Buat repository GitHub baru, misalnya `bi-space-android`.
2. Ekstrak `BI-Space-Android.zip`.
3. Upload semua isi folder hasil ekstrak ke root repository, lalu commit ke branch `main`.
4. Buka tab **Actions** pada repository.
5. Pilih workflow **Build BI-Space APK**.
6. Klik **Run workflow** bila workflow belum berjalan otomatis.
7. Setelah status hijau, buka hasil run.
8. Pada bagian **Artifacts**, download **BI-Space-APK**.
9. Ekstrak artifact tersebut untuk memperoleh `BI-Space.apk`.

APK yang dihasilkan adalah build **debug** yang telah ditandatangani otomatis oleh Android dan dapat langsung dipasang untuk pengujian. Android mungkin meminta izin **Install unknown apps**.

## Membuat versi produksi Play Store

Versi Play Store membutuhkan keystore privat dan sebaiknya menghasilkan Android App Bundle (`.aab`). Jangan menyimpan password atau file keystore langsung di repository publik; gunakan GitHub Actions Secrets.
