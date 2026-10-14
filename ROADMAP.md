# Paze Pilates — Yol Haritası

Durum simgeleri: ✅ Tamamlandı · 🔄 Devam ediyor · ⬜ Planlandı

_Son güncelleme: 2026-10-14_

## ✅ Tamamlananlar

- ✅ **Maven multi-modül yapısı** — `pilates-dto` → `pilates-service-common` → `pilates-backend` → `pilates-desktop`
- ✅ **Flyway → Liquibase geçişi** — XML changelog, `db/changelog/v1.0.0/`
- ✅ **Stüdyo ve oda ekranları** — stüdyo bilgileri, ders odası tanımı (isim, kapasite, ekipman)
- ✅ **Hoca tanımlama ekranı** — PR #1

## 🔄 Devam edenler

_Şu an aktif iş yok._

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
