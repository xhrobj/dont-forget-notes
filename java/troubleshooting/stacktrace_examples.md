Ниже — эталонные примеры. Смотри на **первую строку** (тип+сообщение) и на **первые кадры твоего кода**.

**1) `NullPointerException` (имплицитное разыменование `null`)**
```
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
    at com.acme.user.UserService.fullNameLen(UserService.java:17)   ← твой код
    at com.acme.user.UserController.handle(UserController.java:11)  ← твой код
    at com.acme.App.main(App.java:7)                                ← твой код
```
**Куда смотреть:** первая строка уже говорит, что `name` был `null`. Открой `UserService.java:17`.

**2) `NullPointerException` (явная проверка через `Objects.requireNonNull`)**
```
Exception in thread "main" java.lang.NullPointerException: userId must not be null
    at java.util.Objects.requireNonNull(Objects.java:... )
    at com.acme.user.UserService.load(UserService.java:22)          ← твой код
    at com.acme.App.main(App.java:7)                                ← твой код
```
**Куда смотреть:** сообщение формируешь сам — видно, **какой аргумент** нарушил контракт.

**3) `IllegalArgumentException` (значение неподходящее — твоя валидация)**
```
Exception in thread "main" java.lang.IllegalArgumentException: limit must be > 0
    at com.acme.search.QueryValidator.checkLimit(QueryValidator.java:14)  ← место throw
    at com.acme.search.SearchService.find(SearchService.java:28)          ← call-site
    at com.acme.App.main(App.java:7)
```
**Куда смотреть:** первая строка — IAE с понятным текстом. Исправляй вызывающий код/входные данные.

**4) `IllegalArgumentException` (IAE из JDK/библиотеки)**
```
Exception in thread "main" java.lang.IllegalArgumentException: Illegal capacity: -1
    at java.util.ArrayList.<init>(ArrayList.java:... )
    at com.acme.util.Lists.newListWithCap(Lists.java:9)              ← твой код (call-site)
    at com.acme.App.main(App.java:7)
```
**Куда смотреть:** первый «пользовательский» кадр показывает, **откуда вызван API** с плохим значением.

**5) `IllegalStateException` (не аргументы, а состояние объекта)**
```
Exception in thread "main" java.lang.IllegalStateException: service already stopped
    at com.acme.core.Lifecycle.ensureRunning(Lifecycle.java:31)     ← место throw/проверка
    at com.acme.core.Service.doWork(Service.java:18)
    at com.acme.App.main(App.java:7)
```
**Куда смотреть:** контракт жизненного цикла нарушен — чини порядок вызовов.

> Памятка: **NPE** — про разыменование `null`/автораспаковку; **IAE** — метод сам бросил из-за плохого аргумента; **ISE** — неверное состояние объекта. В IDEA: *Analyze → Stack Trace…* и кликай по первым кадрам твоего кода. Для часто бросаемых NPE без стека — временно добавь `-XX:-OmitStackTraceInFastThrow`.