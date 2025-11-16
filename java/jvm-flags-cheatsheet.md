# JVM-флаги: `-D`, `-X`, `-XX` — краткий гид

## Быстрое различие
- **`-D`** — *Define system property*: задаёт **системные свойства** для приложения. Видны в коде через `System.getProperty(...)`.
- **`-X`** — *eXtended options*: **нестандартные** опции запуска JVM (зависят от реализации/версии лаунчера).
- **`-XX`** — **внутренние (HotSpot)**, продвинутые/диагностические/экспериментальные опции JVM.

> Запоминалка: **D = Define**, **X = eXtended (нестандартные)**, **XX = eXtra‑eXtended (внутренности HotSpot)**.

---

## `-D` — системные свойства
**Назначение:** конфигурация приложения и библиотек на уровне JVM.

**Примеры:**
```bash
-Dspring.profiles.active=prod
-Dfile.encoding=UTF-8
-Duser.timezone=Europe/Moscow
```

**Доступ из кода:**
```java
String profile = System.getProperty("spring.profiles.active");
```

**Посмотреть живьём:**
```bash
jcmd <pid> VM.system_properties
```

**Характеристики:**
- Переносимы (часть стандарта Java SE).
- Удобны для конфигурирования без перекомпиляции.
- Не влияют на внутренние подсистемы JVM напрямую (GC/JIT и т.п.).

---

## `-X` — нестандартные (расширенные) опции JVM
**Назначение:** контролировать поведение лаунчера/JVM, что не входит в спецификацию Java SE.

**Частые примеры:**
```bash
-Xms1g -Xmx2g         # начальный/максимальный heap
-Xss512k              # размер стека на поток
-Xint                 # запуск без JIT (интерпретатор)
-Xcomp                # компилировать при первом вызове (стресс JIT)
-XshowSettings:vm     # показать ключевые настройки при старте
```

**Список, доступный именно в вашей JVM:**
```bash
java -X
java -XshowSettings:all -version
```

**Характеристики:**
- Нестандартны: могут отличаться/меняться между реализациями/версиями.
- Широко поддерживаются в HotSpot, но не гарантированы в OpenJ9/других JVM.

---

## `-XX` — продвинутые/внутренние опции HotSpot
**Назначение:** тонкая настройка HotSpot (GC, JIT, Code Cache, Metaspace, диагностика).

**Форматы:**
- Булевы флаги: `-XX:+UseG1GC`, `-XX:-TieredCompilation`
- Числовые: `-XX:MaxGCPauseMillis=200`, `-XX:ReservedCodeCacheSize=256m`
- Иногда требуют «разлочить» диагностические или экспериментальные опции:
  ```bash
  -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining
  -XX:+UnlockExperimentalVMOptions -XX:+UseZGC
  ```

**Примеры по тематикам:**
```bash
# GC
-XX:+UseG1GC -XX:MaxGCPauseMillis=200

# JIT/компиляция
-XX:+TieredCompilation -XX:TieredStopAtLevel=3
-XX:ReservedCodeCacheSize=256m

# Метаспейс/директ‑память
-XX:MaxMetaspaceSize=512m
-XX:MaxDirectMemorySize=512m

# Диагностика
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps
```

**Посмотреть активные флаги у процесса:**
```bash
jcmd <pid> VM.flags
jcmd <pid> VM.flags -all   # с неявными значениями по умолчанию
```

**Характеристики:**
- Специфичны для HotSpot; в OpenJ9/других JVM эквиваленты отличаются.
- Могут исчезать/меняться между версиями JDK; не все считаются «стабильными».

---

## Когда какой тип использовать
- **Конфигурация приложения/библиотек** → `-D...`
- **Общие поведенческие тумблеры JVM** (heap/stack/JIT‑режимы/показы настроек) → `-X...`
- **Тонкий тюнинг и диагностика внутренностей HotSpot** (GC/JIT/CodeCache/Metaspace) → `-XX:...`

---

## Быстрые команды для проверки
```bash
# Сводка настроек на старте (heap, GC, версия JVM)
java -XshowSettings:vm -version

# Все system properties (те самые -D)
jcmd <pid> VM.system_properties

# Активные -X/-XX флаги
jcmd <pid> VM.flags -all
```

---

## Практические заметки
- В контейнерах Java 11+ лучше задавать heap долей от лимита:
  ```bash
  -XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=70
  ```
- Считывайте бюджет памяти целиком: Heap (`-Xmx`) + Metaspace + стеки (`-Xss × threads`) + Direct/NIO + CodeCache + нативная часть.
- Не полагайтесь на экспериментальные `-XX` в проде без тестов: проверяйте на вашей версии JDK.
