# Paze Pilates — Yol Haritası

Durum simgeleri: ✅ Tamamlandı · 🔄 Devam ediyor · ⬜ Planlandı

_Son güncelleme: 2026-10-14_

## ✅ Tamamlananlar

- ✅ **Maven multi-modül yapısı** — `pilates-dto` → `pilates-service-common` → `pilates-backend` → `pilates-desktop`
- ✅ **Flyway → Liquibase geçişi** — XML changelog, `db/changelog/v1.0.0/`
- ✅ **Stüdyo ve oda ekranları** — stüdyo bilgileri, ders odası tanımı (isim, kapasite, ekipman)
- ✅ **Hoca tanımlama ekranı** — PR #1

## 🔄 Devam edenler

- 🔄 **Rol, yetki ve kullanıcı yönetimi** (`feature/user-roles-albng0`, kullanıcı testi bekleniyor)
  - ✅ Sabit roller: Yönetici ve Hoca; yetkiler servis katmanında `PermissionGuard` ile kontrol edilir
  - ✅ İlk açılışta Kurulum ekranı (stüdyo, ilk yönetici); kullanıcı varken kurulum reddedilir
  - ✅ Tanımlamalar > Kullanıcılar: hoca için kullanıcı aç, geçici şifre, ilk girişte şifre değiştirme, şifre sıfırlama, rol değiştirme, pasife alma
  - ✅ Son aktif yönetici korunur; hoca pasife alınınca kullanıcısı da pasife alınır
  - ✅ Sabit (static) admin: `paze.security.static-admin.*`, bcrypt hash `application-local.yml` veya `PAZE_STATIC_ADMIN_PASSWORD_HASH`; hash üretmek için `--hash-password=<şifre>`
  - ✅ Şifre kuralları `paze.security.password.*` ile yml'den ayarlanır (uzunluk, büyük/küçük harf, rakam, özel karakter)
  - ✅ Hoca tüm dersleri ve katılımcıları görür; kendi adına ders açabilir, yalnızca kendi dersinde işlem yapabilir; öğrenci tanımlayabilir

## ⬜ Planlananlar

- 🔄 **Ders planlama**
  - ✅ Ders açma: hoca + oda + gün + saat aralığı, haftalık takvimden çift tıklayarak
  - ✅ Çakışma kontrolü (oda ve hoca için saat aralığı) servis katmanında
  - ✅ Haftalık takvim görünümü
  - ✅ Takvimde sağ tık menüsü; derse öğrenci ekleme, katılımcıları gösterme ve çıkarma (kapasite = oda kapasitesi)
  - ⬜ Ders düzenleme/iptal, paket bakiyesi düşümü, ders tipi, tekrarlayan dersler
- ⬜ **Öğrenci yönetimi**
  - Öğrenci kaydı (ad, telefon, e-posta, notlar)
  - Rezervasyon ve iptal
- ⬜ **Paket yönetimi**
  - Paket tanımı (ör. 8 seans, 12 seans)
  - Öğrenci paketi ve kalan seans bakiyesi
  - Paket satışı ve ödeme kaydı (nakit, kart)
  - Ödemesi alınmış paketin iptali: kullanılan seansa göre kalan bakiye ve iade hesabı
- ⬜ **Raporlar**
  - Günlük kasa özeti, aylık gelir raporu, borçlu öğrenci listesi
  - Doluluk raporu, hoca ders/seans raporu
  - Excel/PDF export
- ⬜ **Multi-tenant web sürümü**
  - REST API katmanı (`http-service-common`)
  - PostgreSQL geçişi
  - Çoklu stüdyo (tenant) desteği
