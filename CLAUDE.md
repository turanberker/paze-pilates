# Pilates Salonu Yönetim Uygulaması — Ana Proje Talimatları

## 1. Proje vizyonu

Bu uygulama, pilates stüdyolarına özel bir masaüstü yönetim yazılımıdır. Stüdyo sahipleri ve resepsiyonistler için tasarlanmıştır.

Temel işlevler:
- Stüdyo ve seans odası tanımı
- Ders ve seans planlaması (takvim görünümü)
- Öğrenci kaydı ve takibi
- Hoca yönetimi (birden fazla hoca desteği)
- Rezervasyon ve iptal yönetimi
- Paket satışı ve bakiye takibi
- Finansal raporlama (günlük/aylık gelir, tahsilat)
- Diyetisyen modülü (ileriki aşama, şimdilik kapsam dışı)

Hedef kullanıcı: Küçük-orta ölçekli pilates stüdyosu yöneticisi ve resepsiyonisti.

## 2. Teknoloji mimarisi

- **Masaüstü arayüz:** Java Swing (FlatLaf veya Darcula teması tercih edilir)
- **Servis katmanı:** Spring Boot (embedded, web sunucusu kapalı — `spring.main.web-application-type=none`)
- **Veritabanı (Faz 1):** H2 dosya tabanlı (taşınabilir, EXE içinde çalışır)
- **Veritabanı (Faz 2 — web):** PostgreSQL
- **ORM:** Spring Data JPA + Hibernate
- **Build:** Maven multi-modül (`pilates-dto` → `pilates-service-common` → `pilates-backend` → `pilates-desktop`)
- **Migrasyon:** Liquibase (XML format, `db/changelog/v1.0.0/`)
- **Paketleme:** jpackage (JRE dahil, native EXE)
- **Raporlama:** JasperReports veya Apache POI (Excel export)
- **Metinler:** ResourceBundle ile dışa alınacak; kod içine Türkçe/İngilizce string gömülmeyecek

## 3. Katman sorumlulukları

### 3.1  Swing (UI katmanı)
Formlar, tablolar, takvim bileşenleri, diyaloglar ve kullanıcı etkileşimleri. İş mantığı içermez; yalnızca Service katmanını çağırır.dto kütüpanesini ve service-common veya feign-service-common kütüpanelerini bilir. 

## 3.2 pilates-dto (`com.paze.pilates.dto`)
- Ortak DTO kayıt sınıfları (`RoomDto`, `StudioDto`, `AppUserDto`) ve `BusinessException` burada tanımlıdır.
- Sadece `jakarta.validation-api` bağımlılığı vardır; Spring veya JPA bağımlılığı içermez.
- İş mantığı olmaz; yalnızca alan tanımları ve doğrulama kısıtlamaları yer alır.

## 3.3 pilates-service-common (`com.paze.pilates.service`)
- `AuthService` ve `StudioService` gibi servis arayüzlerini barındırır; uygulamalar `pilates-backend` modülündedir.
- Monolit desktop modunda `pilates-desktop` derleme bağımlılığı olarak, ilerleyen web fazında ise `feign-service-common` alternatifi olarak kullanılır.
- Yalnızca `pilates-dto` bağımlılığını bilir; backend veya JPA sınıflarına erişimi yoktur.

## 3.4 http-service-common
Frontend backend ile http isteği üzerinden anlaşmak isterse bu kütüpane kullanılacaktır.dto paketini bilir. 

### 3.5 Backend (Servis katmanı)
Rezervasyon kuralları, çakışma kontrolü, paket/bakiye hesaplamaları, ödeme kaydı, raporlama mantığı.

### Spring Data JPA (Repository katmanı)
Tüm veritabanı erişimi. Sorgular Repository arayüzleri veya `@Query` ile yapılır; SQL doğrudan Swing'e yazılmaz.

### H2 (Faz 1 DB)
Tek dosyalı, kurulum gerektirmeyen yerel veritabanı (`data/studio.db`). Yedekleme basit dosya kopyalamasıyla yapılır.

### jpackage
Maven plugin veya script ile native Windows EXE üretir. JRE pakete dahildir; kullanıcı Java kurmak zorunda kalmaz.

## 4. Temel mimari kurallar

1. Spring context Swing `main` metodundan başlatılır; tüm Swing bileşenleri Spring bean'lerini inject eder.
2. Swing EDT (Event Dispatch Thread) bloklama işlemleri için `SwingWorker` kullan.
3. Tüm iş kuralları Service katmanında; UI katmanı yalnızca sunum ve yönlendirme yapar.
4. Repository'den dönen entity'leri doğrudan Swing tablosuna bağlama; DTO kullan.
5. Kritik işlemler (ödeme, rezervasyon, iptal) `@Transactional` ile korunmalıdır.
6. Çift rezervasyon ve çift ödeme oluşmaması için çakışma kontrolü Service'te yapılır.
7. Faz 1'de web desteği yok; kod gelecekteki REST katmanı için hazır olmalı ama şimdi eklenmemeli.
8. Test: Service katmanı için JUnit + Mockito; kritik iş kuralları birim testiyle doğrulanır.
9. Gereksiz soyutlamadan kaçın; özellikle MVP aşamasında.
10. Ekrana yazdırılan her metin ResourceBundle üzerinden gelir.
11. Katmanlı mimari uygula. Frontend ve backend ayrı projeler aç. MultiModule oluştur.
12. **Servis kuralı:** Durum değiştiren iş işlemleri (ödeme alma, iptal, pasife alma vb.) genel `save`/`update` ucu üzerinden yapılmaz; her biri için amaca özel servis metodu yazılır.
    - Yanlış: UI'da `dto.setPaid(true); dto.setPaymentDate(today); service.update(dto)`
    - Doğru: UI `service.completePayment(id, paymentDate)` çağırır; servis kaydı `findById` ile getirir, kuralları kontrol eder, alanları set eder ve kaydeder.
## 5. Uygulama kapsamı (MVP)

### Stüdyo tanımı
- Ders odası (isim, kapasite, ekipmanlar)
- Stüdyo bilgileri (ad, adres, telefon)

### Hoca yönetimi
- Hoca kaydı (ad, branş, müsait saatler)
- Birden fazla hoca desteği
- Hocanın ders takvimi

### Öğrenci yönetimi
- Öğrenci kaydı (ad, telefon, e-posta, notlar)
- Paket tanımı (ör. 8 seans, 12 seans)
- Öğrenci paketi ve kalan seans bakiyesi

### Rezervasyon ve takvim
- Haftalık/günlük takvim görünümü
- Ders oluşturma (oda, hoca, tarih-saat, kapasite, ders tipi)
- Öğrenci rezervasyonu ve iptal
- Çakışma kontrolü (oda + hoca + zaman)

### Finans
- Paket satışı ve ödeme kaydı (nakit, kart)
- Günlük kasa özeti
- Aylık gelir raporu
- Borçlu öğrenci listesi

### Raporlama
- Doluluk raporu
- Hoca ders/seans raporu
- Gelir-gider özeti

## 6. Kapsam dışı (MVP için)

Açık karar alınmadıkça ekleme:
- Diyetisyen modülü
- Online rezervasyon (web/mobil)
- SMS / e-posta bildirim
- Muhasebe entegrasyonu (ör. Logo, Mikro)
- Çoklu şube yönetimi
- Personel maaş hesaplama
- Üyelik abonelik sistemi (aylık otomasyon)

## 7. Güvenlik ve veri bütünlüğü

- Uygulama başlangıcında basit kullanıcı adı/şifre girişi (yerel, DB'de bcrypt hash)
- Şifreler plain-text saklanmaz
- H2 DB dosyası uygulama dizininde; yedek alma kolaylığı için yol konfigüre edilebilir
- Finansal işlemler `@Transactional`; yarım kalan ödeme kaydı oluşmaz
- Gizli bilgiler `application.yml`'e konmaz; `application-local.yml` gitignore'da

## 8. Claude ile çalışma kuralları

Her görevde:

1. İlgili `CLAUDE.md` dosyalarını oku.
2. Mevcut kod ve paket yapısını incele; var olmayan sınıfları varmış gibi varsayma.
3. Swing EDT kurallarına dikkat et; arka plan işlemi varsa `SwingWorker` öner.
4. Service → Repository ayrımını koru; UI'a iş mantığı yazma.
5. Değişiklikleri küçük ve anlaşılır tut.
6. Çalıştırmadığın testleri başarılı olarak bildirme.
7. Her görev sonunda değişen dosyaları ve amaçlarını özetle.
8. Kapsam dışı özellik ekleme.
9. Büyük mimari değişikliklerden önce gerekçeyi açıkla.
10. Belirsiz ürün kararlarında seçenekler sun; küçük teknik kararlar için onay isteme.

## 9. Varsayılan geliştirme sırası

1. Maven proje iskeleti (Spring Boot + Swing + H2 + FlatLaf)
2. Veritabanı şeması ve entity'ler (Liquibase XML — `db/changelog/v1.0.0/`)
3. Temel ana pencere ve navigasyon
4. Stüdyo ve oda tanımı ekranları
5. Hoca yönetimi
6. Öğrenci yönetimi ve paket tanımı
7. Takvim ve rezervasyon ekranı
8. Finans: paket satışı, ödeme kaydı
9. Raporlar ve Excel/PDF export
10. jpackage ile EXE paketleme
11. (İleride) REST API katmanı + PostgreSQL geçişi


Bu sıra mevcut proje durumuna göre değişebilir.

## 10. Görev sonu raporu

Her görev sonunda:

- **Yapılanlar:** Kısa değişiklik özeti
- **Dosyalar:** Eklenen ve değiştirilen dosyalar
- **Doğrulama:** Çalıştırılan test ve build sonuçları
- **Sonraki adım:** Önerilen tek sonraki iş

## 11. Git kuralları
- Değişiklikler tamamlanıp derleme başarılı olduktan sonra otomatik olarak commit ve push yap.
- Commit mesajı: değişikliğin kısa özeti (Türkçe veya İngilizce).
  Branch: mevcut aktif branch'e push yap, hiçbir zaman branch değiştirme.