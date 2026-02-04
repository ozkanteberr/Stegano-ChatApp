# Stegano-ChatApp
# SteganoChat: Steganografi Tabanlı Güvenli Mesajlaşma

Bu proje, güvenli haberleşme sağlamak amacıyla **Steganografi (Resim içine veri gizleme)** ve **DES (Data Encryption Standard)** algoritmalarını birleştiren Java tabanlı bir sohbet uygulamasıdır.

Kullanıcılar sisteme giriş yaparken parolalarını metin olarak girmek yerine, parolalarının içine gizlendiği bir **resim dosyası** yüklerler. Sunucu, resimden parolayı çözer ve bu parolayı o oturumun şifreleme anahtarı olarak kullanır.

## Özellikler

* **Resim Tabanlı Kimlik Doğrulama:** Klasik parola girişi yerine, şifrenin gizlendiği bir "görsel anahtar" (Cover Image) kullanılır.
* **Steganografi:** Kullanıcının belirlediği gizli anahtar (Secret Key), seçilen bir PNG resminin piksellerine gömülür.
* **Uçtan Uca Şifreleme (E2EE Simülasyonu):** Mesajlar ağ üzerinde asla açık metin (plain-text) olarak dolaşmaz.
    * Mesaj gönderilirken göndericinin anahtarıyla DES ile şifrelenir.
    * Sunucu mesajı çözer ve alıcının anahtarıyla tekrar şifreleyerek iletir.
* **Çevrimdışı Mesajlaşma:** Hedef kullanıcı çevrimdışı olsa bile mesajlar sunucuda şifreli olarak saklanır ve kullanıcı giriş yaptığında iletilir.
* **Modern Swing Arayüzü:** Kullanıcı dostu, karanlık mod (Dark Theme) destekli GUI.

## Kullanılan Teknolojiler

* **Dil:** Java (JDK 8+)
* **Arayüz:** Java Swing (JFrame, JPanel, Custom Borders)
* **Ağ:** Java Sockets (TCP/IP)
* **Güvenlik:**
    * `javax.crypto` (DES Şifreleme)
    * `java.awt.image` & `javax.imageio` (Görüntü İşleme)
* **Veri Yapıları:** ConcurrentHashMap (Thread-safe sunucu yönetimi için)

## Proje Yapısı

* `MainServer.java`: Sunucu tarafı. İstemcileri dinler, anahtarları yönetir ve mesaj trafiğini yönlendirir.
* `ClientHandler.java`: Her bir istemci için sunucu tarafında çalışan thread.
* `RegisterForm.java`: İstemci giriş ekranı. Kullanıcı buradan resim seçer ve sisteme kaydolur.
* `ChatScreen.java`: Ana sohbet arayüzü.
* `CryptoHelper.java`: DES şifreleme ve şifre çözme yardımcı sınıfı.
* `SteganoManager.java`: Resim içine veri gizleme (Encode) ve resimden veri okuma (Decode) işlemlerini yapar.

## Kurulum ve Çalıştırma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyin.

### 1. Gereksinimler
* Bilgisayarınızda **Java Development Kit (JDK)** kurulu olmalıdır.
* Terminal veya Komut İstemi (CMD).

### 2. Derleme (Compile)

Tüm `.java` dosyalarını derlemek için proje dizininde şu komutu çalıştırın:

```bash
javac *.java
```
### 3. Sunucuyu Başlatma

Önce sunucuyu ayağa kaldırmanız gerekir. Varsayılan olarak 5555 portunu kullanır.

``` bash
java Server
```
### 4. İstemciyi (Client) Başlatma
``` bash
java RegisterForm
```
