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
- Push notification Firebase secara real-time untuk berita terbaru, transfer masuk, serta Berita Acara baru, revisi, persetujuan, dan penolakan.
- Pemeriksaan berkala tetap aktif sebagai cadangan untuk pengingat Daily dan pemulihan ketika perangkat sempat offline.
- Ikon kecil notifikasi memakai kotak rounded dengan huruf `BI` agar tetap jelas pada status bar Android.
- Notification dot/badge dan badge belum dibaca pada ikon lonceng Dashboard.
- Notification Center berisi pengaturan kategori, suara, dan riwayat maksimal 100 notifikasi.
- Update aplikasi internal dari GitHub Release, tanpa Play Store.

## Signing permanen dan update

Workflow release membutuhkan GitHub Secrets berikut:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `FIREBASE_GOOGLE_SERVICES_JSON_BASE64`

`FIREBASE_GOOGLE_SERVICES_JSON_BASE64` adalah isi file `google-services.json` dari Firebase yang sudah dikonversi ke Base64. File asli tidak boleh disimpan di repository.

Sisi Apps Script juga membutuhkan Script Properties berikut agar dapat mengirim FCM HTTP v1:

- `FCM_PROJECT_ID`
- `FCM_CLIENT_EMAIL`
- `FCM_PRIVATE_KEY`

Nilainya berasal dari service account Firebase/Google Cloud yang memiliki izin mengirim pesan FCM. Gunakan private key lengkap; bila ditempel satu baris, pertahankan karakter `\n`. Setelah Apps Script terbaru diterapkan dan pengguna login ke APK versi 1.5.0, perangkat otomatis didaftarkan pada sheet `APP_PUSH_TOKENS`.

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
