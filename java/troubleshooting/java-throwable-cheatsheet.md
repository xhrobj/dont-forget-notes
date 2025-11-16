# Что хранится в Java-исключении (`Throwable`) и как это использовать

## Базовая модель
Все исключения наследуются от **`Throwable`**. И у всех исключений есть общая часть, унаследованная от Throwable. В базовых исключениях: `Exception`, `RuntimeException` и `Error` есть только эта общая часть и нет никаких дополнительных полей — только то, что определено в `Throwable`, а именно:

- **Сообщение** (`detailMessage`) — строка с описанием ошибки.
- **Причина** (`cause`) — «вложенное» исключение для чейнинга.
- **Стек‑трейс** (`stackTrace`) — массив `StackTraceElement[]` (кадры вызовов).
- **Подавленные исключения** (`suppressedExceptions`) — список «спутников» при `try-with-resources` и др.

---

## Message (и `getLocalizedMessage()`)
- Задаётся в конструкторе и доступно через `getMessage()`.
- `getLocalizedMessage()` по умолчанию **возвращает то же**, что и `getMessage()`. В собственных классах можно переопределить для локализации.

**Совет:** пишите сообщение с контекстом: *что* делали, *какой* инпут, *ожидание vs факт*. Не дублируйте то, что уже есть в cause.

```java
throw new IllegalStateException("Failed to load user id=%s from %s".formatted(id, region), cause);
```

---

## Cause (цепочка исключений)
- «Первопричина», по которой возникла текущая ошибка.
- Устанавливается **один раз** — либо конструктором с `cause`, либо методом **`initCause(Throwable)`** (это **не сеттер**; повторный вызов бросит `IllegalStateException`).

```java
try {
  doIo();
} catch (IOException e) {
  throw new UncheckedIOException("IO while syncing", e);
}
```

**Правило:** *не теряйте cause*. Если оборачиваете — передавайте `e` дальше.

---

## Stack Trace (стек вызовов)
- Захватывается в момент **конструирования** исключения (через `fillInStackTrace()`), а **не** в момент `throw`, т.е. это стек вызовов методов потока до того места, где сообщение было сконструировано (не выброшено!)
- JVM **может опустить** часть кадров из‑за инлайна/оптимизаций JIT. Для повторяющихся быстрых исключений HotSpot может **урезать** трассу (опция `-XX:+OmitStackTraceInFastThrow`; чтобы видеть полные трассы — запустите с `-XX:-OmitStackTraceInFastThrow`).

**Управление стеком:**
- Конструктор `Throwable(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace)`.  
  Если `writableStackTrace = false`, стек не заполняется (дешевле по CPU/памяти) и его нельзя позже задать.
- Метод `setStackTrace(StackTraceElement[])` — заменяет стек (если он «разрешён»).  
- Метод `fillInStackTrace()` — перезаполняет стек **текущим** вызовом (редко нужно, но можно, если объект создавали раньше, а кидаете позже).

**Печать:**
```java
ex.printStackTrace();               // в stderr
logger.error("Boom", ex);           // логгер сам распечатает стек
```

---

## Suppressed (подавленные исключения)
- Список «спутников», которые возникли **пока основное исключение шло наверх**, чаще всего — ошибки во время `close()` в `try-with-resources`.
- Управляется методами `addSuppressed(Throwable)` и `getSuppressed()`.
- Можно **отключить** подавление, создав исключение конструктором с `enableSuppression = false`.

Пример, иллюстрирующий подавление в `try-with-resources`:
```java
try (var in = newInput(); var out = newOutput()) {
  copy(in, out);                     // бросает IOException
}                                    // если out.close() тоже бросит, оно попадёт в suppressed
```

В печати увидите:
```
java.io.IOException: copy failed
  at ...
Suppressed: java.io.IOException: close failed
  at ...
```

---

## Быстрый API‑шпаргалка
```java
Throwable t;
t.getMessage();
t.getLocalizedMessage();

t.getCause();
t.initCause(cause);                  // только 1 раз

t.getStackTrace();
t.setStackTrace(new StackTraceElement[]{ ... });
t.fillInStackTrace();                // обновить стек на «сейчас»

t.addSuppressed(other);
for (var s : t.getSuppressed()) { ... }
```

---

## Производительность и диагностика
- Создание исключения = **дорогая операция** (съём стека); **не используйте исключения для нормального контроля потока**.
- Для многократно бросаемых «горячих» исключений полезно проверить флаг `-XX:-OmitStackTraceInFastThrow`, чтобы видеть полноценные трассы.
- В JDK 14+ полезные сообщения у `NullPointerException` включены по умолчанию. Отключение: `-XX:-ShowCodeDetailsInExceptionMessages`.

---

## Хорошие практики
1. **Чейните, не теряйте cause.**
2. Сообщение делайте **достаточно информативным**, но без приватных данных.
3. В слоях интеграции **оборачивайте** checked→runtime с сохранением контекста.
4. Для «тяжёлых» путей можно создавать исключение с `writableStackTrace=false` (если стек не важен).
5. В `try-with-resources` **не глушите подавленные** — они часто несут ключ к первопричине.

---

## Резюме
В `Throwable` есть 4 ключевых составляющих: **message**, **cause**, **stack trace**, **suppressed**.  
Они дают контекст, цепочку, место возникновения и «побочные» ошибки. Умение их правильно заполнять и читать — основа эффективного траблшутинга.
