# Pilates Salonu — Masaüstü Arayüz (Java Swing) Talimatları

## 1. Sorumluluk sınırı

Bu modül yalnızca görsel katmandır:
- Kullanıcı girdilerini toplar ve Service katmanına iletir.
- Service'ten dönen DTO'ları ekranda gösterir.
- İş mantığı, doğrulama kuralı veya veritabanı erişimi içermez.
- Tüm iş kuralları `service` modülünde bulunur; burada tekrarlanmaz.

## 2. Teknoloji ve kütüphaneler

- **Java Swing** — standart bileşenler tercih edilir
- **FlatLaf** — modern görünüm için (`com.formdev:flatlaf`)
- **MigLayout** veya **GroupLayout** — form düzeni için
- **JCalendar** veya özel takvim bileşeni — rezervasyon takvimi için
- **Spring IoC** — tüm panel/frame sınıfları `@Component` ile Spring'e kayıtlı
- ResourceBundle — tüm etiket ve mesajlar buradan; kod içine Türkçe string yazma

## 3. Paket yapısı
ui/
├── main/
│ ├── MainFrame.java # Ana pencere, navigasyon menüsü
│ └── MainMenuPanel.java # Sol sidebar veya üst menü
├── studio/
│ ├── StudioPanel.java
│ └── RoomFormDialog.java
├── instructor/
│ ├── InstructorListPanel.java
│ └── InstructorFormDialog.java
├── student/
│ ├── StudentListPanel.java
│ ├── StudentFormDialog.java
│ └── PackageDialog.java
├── reservation/
│ ├── CalendarPanel.java
│ ├── LessonFormDialog.java
│ └── ReservationDialog.java
├── finance/
│ ├── PaymentPanel.java
│ └── DailySummaryPanel.java
├── report/
│ └── ReportPanel.java
└── shared/
├── TableFactory.java # Ortak JTable konfigürasyonu
├── FormValidator.java # UI seviyesi basit boşluk kontrolü
├── NotificationBar.java # Başarı/hata mesajı bandı
└── SwingHelper.java # EDT yardımcıları



## 4. Swing kuralları

### EDT (Event Dispatch Thread)
- Tüm Swing bileşenleri EDT'de oluşturulur ve güncellenir.
- Service çağrısı gibi arka plan işlemleri için `SwingWorker` kullan:
  ```java
  new SwingWorker<List<StudentDto>, Void>() {
      protected List<StudentDto> doInBackground() {
          return studentService.findAll();
      }
      protected void done() {
          table.setModel(new StudentTableModel(get()));
      }
  }.execute();
SwingUtilities.invokeLater() veya invokeAndWait() gerektiğinde kullan.
Uzun işlemler sırasında UI’ı bloke etme; loading göstergesi ekle.
Bileşen kuralları
Her ekran kendi JPanel alt sınıfıdır; doğrudan JFrame kullanma.
Dialog’lar JDialog olarak açılır; MainFrame parent olarak geçilir.
JTable için her zaman özel AbstractTableModel yaz; DefaultTableModel’a veri doldurma.
Tablo satırı seçiminde entity ID’si modelden alınır; satır indexiyle mapping yapma.
Form validasyonu için FormValidator kullan; Service’e boş/hatalı veri gönderme.
Spring ile entegrasyon
Panel’ler @Component, dialog’lar @Scope("prototype") ile işaretlenir.
MainFrame diğer panel’leri constructor injection ile alır.
ApplicationContext doğrudan UI sınıflarında kullanılmaz; bean’leri inject et.
5. Görsel standartlar
   Tema: FlatLaf Light veya FlatLaf IntelliJ — başlangıçta FlatLightLaf.setup() çağrılır.
   Font: Varsayılan sistem fontu; boyutlar sabit sayı yerine UIManager üzerinden.
   Renk: Sabit renk kodu yok; FlatLaf CSS değişkenleri veya UIManager property kullan.
   Minimum pencere boyutu: 1024x768.
   Tüm formlar: etiket + alan çifti, MigLayout "label, field, wrap" düzeniyle.
   Zorunlu alanlar: etikette * işareti, boş bırakıldığında kırmızı kenarlık.
   Başarı/hata geri bildirimi: JOptionPane yerine NotificationBar (üst bantta kayan mesaj).
6. Metinler ve yerelleştirme
   Tüm kullanıcıya gösterilen metinler messages_tr.properties dosyasından gelir.
   Kod içine "Kayıt başarılı", "Hata oluştu" gibi string yazma.
   Key formatı: ekran.bileşen.mesaj — örn. student.form.save.success
   Tarih/saat formatı: dd.MM.yyyy HH:mm, DateTimeFormatter sabit olarak SharedConstants’ta tanımlanır.
7. Hata yönetimi
   Service’ten fırlayan BusinessException yakalanır; mesajı NotificationBar’da gösterilir.
   Beklenmeyen Exception loglanır ve genel bir hata mesajı gösterilir; stack trace kullanıcıya açılmaz.
   Bağlantı/DB hatası için uygulama başlangıcında kontrol yapılır; hata varsa anlaşılır mesajla kapanır.
8. Claude ile çalışma kuralları
   Yeni ekran eklerken önce paket yapısını ve mevcut panel’leri incele.
   MainFrame’e bağlantı gerektiren her yeni panel için navigasyon menüsünü güncelle.
   Yeni JTable eklerken mutlaka özel TableModel yaz.
   İş mantığı UI’a sızmasın; kural kontrolü gerekiyorsa Service’e taşı.
   EDT ihlali yaratacak kod yazma; arka plan işlemi varsa SwingWorker öner.
   ResourceBundle key’i eklenmeden string doğrudan koda yazma.
   Değiştirilen her dosyayı görev sonunda listele.
9. Görev sonu raporu
   Yapılanlar: Kısa özet
   Dosyalar: Eklenen / değiştirilen
   Doğrulama: Derleme ve varsa UI testi sonucu
   Sonraki adım: Tek önerilen iş