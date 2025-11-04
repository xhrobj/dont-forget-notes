# Разбор статьи на Хабре  
**«Основы HTTPS, TLS, SSL. Создание собственных X.509 сертификатов. Пример настройки TLSv1.2 в Spring Boot»**  
https://habr.com/ru/articles/593507/

---

## 1. Выпускаем приватный ключ для CA

**Вариант из статьи:**
```bash
openssl genrsa \
	-aes256 \
	-passout pass:god \
	-out CA-private-key.key 4096 
```

**Более актуальный вариант:**
```bash
openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:4096 \
  -aes-256-cbc \
  -pass pass:god \
  -out CA-private-key.key
```

---

## 2. Выпускаем запрос на подпись (Certificate Signing Request) для нашего CA
```bash
openssl req -new \
  -key CA-private-key.key \
  -passin pass:god \
  -subj "/C=RU/ST=Moscow/O=M1 Company/CN=M1 Company Root Certificate Authority" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical, keyCertSign, cRLSign" \
  -out CA-certificate-signing-request.csr
```

🔹 В `-subj` указываем все основные поля (C, ST, L, O, OU, CN).  
🔹 Через `-addext` (OpenSSL ≥1.1.1) добавляем критичные расширения, чтобы сертификат стал полноценным CA.

**CSR (Certificate Signing Request)** — это ещё не сертификат, а «заявка».  

🚦 Сравнение по шагам:
- **Приватный ключ**: генерируешь у себя (`openssl genrsa …`), никому не показываешь.  
- **CSR**: формируешь из приватного ключа публичный ключ + subject, это данные субъекта (CN, O, C и т.д.). Подписываешь своим приватным ключом → доказательство владения.  

👉 Результат: «Я хочу сертификат на вот этот публичный ключ и имя».

---

## 3. Ключевой момент — из «полуфабриката» (CSR) делаем настоящий сертификат нашего CA 🔑
```bash
openssl x509 -req \
  -in CA-certificate-signing-request.csr \
  -signkey CA-private-key.key \
  -passin pass:god \
  -days 3 \
  -out CA-self-signed-certificate.pem
```

🔹 **CA (Certificate Authority)** проверяет CSR и подписывает его своим корневым приватным ключом.  
🔹 На выходе получаем **сертификат (.crt / .pem)** — финальный документ, который сервер будет показывать клиентам.

📌 Таким образом:
- CSR = заявка на выпуск  
- Сертификат = готовый документ, подтверждённый доверенным CA  

---

## 4. Сертификаты для сервера и клиента

### 4.1 Сервер

**4.1.1 Приватный ключ**
```bash
openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:4096 \
  -aes-256-cbc \
  -pass pass:love \
  -out Server-private-key.key
```

**4.1.2 Запрос на сертификат (src)**
```bash
openssl req -new \
	-key Server-private-key.key \
	-passin pass:love \
	-subj "/CN=localhost" \
	-addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
	-out Server-certificate-signing-request.csr
```

**4.1.3 Подпись сертификата у CA**
```bash
openssl x509 -req \
  -in Server-certificate-signing-request.csr \
  -CA CA-self-signed-certificate.pem \
  -CAkey CA-private-key.key \
  -passin pass:god \
  -days 3 \
  -CAcreateserial \
  -out Server-certificate.pem
```

---

### 4.2 Клиент

**4.2.1 Приватный ключ**
```bash
openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:4096 \
  -aes-256-cbc \
  -pass pass:secret \
  -out Client-private-key.key
```

**4.2.2 Запрос на сертификат**
```bash
openssl req -new \
  -key Client-private-key.key \
  -passin pass:secret \
  -subj "/CN=Client" \
  -addext "keyUsage=critical, digitalSignature" \
  -addext "extendedKeyUsage=clientAuth" \
  -out Client-certificate-signing-request.csr
```

**4.2.3 Подпись у CA**
```bash
openssl x509 -req \
  -in Client-certificate-signing-request.csr \
  -CA CA-self-signed-certificate.pem \
  -CAkey CA-private-key.key \
  -passin pass:god \
  -days 3 \
  -out Client-certificate.pem
```

---

## 5. Так можно проверить цепочку доверия
```bash
openssl verify -CAfile CA-self-signed-certificate.pem Server-certificate.pem
openssl verify -CAfile CA-self-signed-certificate.pem Client-certificate.pem
```

NOTE: выдает ошибки, кстати

---

## 6. После этого необходимо создать хранилище ключей с сертификатами (**Keystore**) Server-keystore.p12 для использования в нашем приложении. Положим туда сертификат сервера, приватный ключ сервера и защитим хранилище паролем "keystore-password":
```bash
openssl pkcs12 -export \
  -in Server-certificate.pem \
  -inkey Server-private-key.key \
  -passin pass:love \
  -passout pass:keystore-password \
  -out Server-keystore.p12
```
📌 Это собирает серверный сертификат + приватный ключ в `.p12`, который можно использовать в Java (Spring Boot, Tomcat).

---

## 7. Осталось только создать хранилище доверенных сертификатов (**Truststore**): сервер будет доверять всем клиентам, в цепочке подписания которых есть сертификат из truststore. К сожалению, для Java сертификаты в truststore должны содержать специальный object identifier, а OpenSSL пока не поддерживает их добавление. Поэтому здесь мы прибегнем к поставляемому вместе с JDK keytool:
```bash
keytool -import \
  -file CA-self-signed-certificate.pem \
  -storetype PKCS12 \
  -storepass truststore-password \
  -noprompt \
  -keystore Server-truststore.p12
```

### Keystore и Truststore в Java
- **Keystore**: приватный ключ + сертификат сервера → чтобы предъявлять серверный сертификат.  
- **Truststore**: доверенные CA → чтобы проверять сертификаты клиентов.  

📌 Для HTTPS: нужен только keystore.  
📌 Для mTLS: нужны оба (сервер проверяет клиента по truststore).

---

## 8. Настройка TLS в Spring Boot

Основой для нашего проекта послужит шаблон с https://start.spring.io/ с одной лишь зависимостью Spring Web.

**Контроллер**
```java
@RestController
public class TlsController {
    @GetMapping
    public String helloWorld() {
        return "Hello, world!";
    }
}
```

Запускаем, обращаемся по http://localhost:8080 в браузере или через curl и получаем Hello, World! в ответ

## 9. Теперь кладем Server-keystore.p12 в папку ресурсов и добавляем в application.properties:

**application.properties (обычный TLS)**
```properties
server.port=443
server.ssl.enabled=true

# Keystore (серверный ключ + сертификат)
server.ssl.key-store-type=PKCS12
server.ssl.key-store=classpath:Server-keystore.p12
server.ssl.key-store-password=keystore-pass
```

Через браузер при обращении
```
https://localhost:443
```
получаем предупреждение о самоподписанном сертификате, через curl выдает ошибку - нет доверия сертификату сервера.

В vm options можно добавит
```
-Djavax.net.debug=ssl,handshake
```
системное свойство jvm, которое включает детализированный отладочный вывод работы TLS/SSL в java-консоли
опция -v для curl тоже даст дополнительную информацию, но значительно меннее детальную.

Добавлем к curl --cacer и получаем наш Hello, World!

**Проверка**
```bash
curl -v https://localhost \
	--cacert CA-self-signed-certificate.pem
```

---

## 9. Теперь попробуем в mTLS. Кладем в Server-truststore.p12 в ресурсы. Дописываем в application.properties:

**application.properties**
```properties
# Truststore (CA, по которому доверяем клиентам)
server.ssl.client-auth=need
server.ssl.trust-store-type=PKCS12
server.ssl.trust-store=classpath:Server-truststore.p12
server.ssl.trust-store-password=truststore-password
```

Команда
```bash
curl -v https://localhost \
	--cacert CA-self-signed-certificate.pem
```
больше не работает - bad request. Дополним запрос клиентским сертификатом и его закрытым ключом:

**Проверка**
```bash
curl -v https://localhost \
	--cacert CA-self-signed-certificate.pem \
	--cert Client-certificate.pem:secret \
	--key Client-private-key.key 
```

Кстати, не знаю почему это команда записана в статье именно так - в клиентском сертификате нет закрытого ключа, зачем передавать к нему пароль от закрытого ключа клиента? Скорее должно быть так:

```bash
curl -v https://localhost \
	--cacert CA-self-signed-certificate.pem \
	--cert Client-certificate.pem \
	--key Client-private-key.key \
  --pass secret
```

⚠️ В статье получилось, у меня нет... на сервере Java при проверке клиентского сертификата валится:
```
ValidatorException: TrustAnchor ... is not a CA certificate
```
Видимо, в CA-self-signed-certificate.pem нет нужных X.509 расширений, поэтому Java не считает его центром сертификации и нужно создать его как-то по-другому ... [вернуться к этому позже]
