# TV Online Indonesia

Project Android sederhana untuk memutar playlist M3U milik Anda.

## Fitur
- Import file playlist M3U dari penyimpanan HP
- Pemutar streaming HLS/MP4 yang didukung Media3
- Pencarian channel
- Daftar favorit yang tersimpan di HP
- Mode layar penuh

## Build APK dari HP melalui GitHub
1. Buat repository GitHub baru.
2. Upload seluruh isi folder project ini (jangan hanya file ZIP).
3. Buka tab Actions pada repository.
4. Pilih workflow "Build APK", lalu tekan "Run workflow".
5. Setelah selesai, buka hasil run dan unduh artifact `TV-Online-Indonesia-APK`.
6. Ekstrak ZIP artifact, lalu instal `app-debug.apk`.

## Playlist
Tekan tombol Import M3U di aplikasi lalu pilih file `.m3u` atau `.m3u8` playlist.
Format dasar:
#EXTM3U
#EXTINF:-1,Nama Channel
https://alamat-streaming-resmi/channel.m3u8

Gunakan hanya URL streaming yang sah dan Anda berhak akses. File contoh berisi URL placeholder dan bukan siaran aktif.
