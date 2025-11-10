# jcmd — выжимка (cheat sheet)

**jcmd** — универсальный консольный «пульт» диагностики **HotSpot JVM**. Через единый интерфейс выполняет сотни команд: память/GC, треды, флаги VM, JFR, NMT и пр. На современных JDK это **рекомендуемый** инструмент для live-диагностики.

---

## Быстрый старт
```bash
jcmd -l                     # список Java-процессов (PID и main class)
jcmd <PID> help             # список доступных команд для данного процесса
jcmd <PID> help GC.*        # справка по командам конкретной области
```

---

## Частые сценарии

### Память и GC
```bash
jcmd <PID> GC.heap_info                 # сводка по куче/GC
jcmd <PID> GC.class_histogram           # гистограмма классов
jcmd <PID> GC.heap_dump filename=heap.hprof   # heap dump (HPROF)
jcmd <PID> GC.run                        # принудительный GC
```

### Потоки и блокировки
```bash
jcmd <PID> Thread.print                  # тред-дамп со стек-трейсами
jcmd <PID> Thread.dump_to_file filename=threads.txt  # (если доступно)
```

### Флаги JVM, свойства, аптайм
```bash
jcmd <PID> VM.flags
jcmd <PID> VM.system_properties
jcmd <PID> VM.uptime
jcmd <PID> VM.version
```

### JFR (Java Flight Recorder)
```bash
# старт записи (минимальная нагрузка, удобно для продакшена)
jcmd <PID> JFR.start name=diag settings=profile disk=true filename=recording.jfr

# проверка статуса
jcmd <PID> JFR.check

# дамп текущей записи в файл (не останавливая)
jcmd <PID> JFR.dump name=diag filename=recording.jfr

# остановка записи
jcmd <PID> JFR.stop name=diag
```
> Полезные пресеты: `settings=default` (ниже детализация) и `settings=profile` (больше событий). Можно задать `duration=60s`/`5m`, `maxsize=250m`, `maxage=30m` и т.д.

### NMT (Native Memory Tracking)
```bash
# Требуется запуск JVM с -XX:NativeMemoryTracking=summary|detail и -XX:+UnlockDiagnosticVMOptions
jcmd <PID> VM.native_memory summary
jcmd <PID> VM.native_memory detail
```

### Другое полезное
```bash
jcmd <PID> JVMTI.agent_load library=/path/agent.so options=...   # загрузка агента JVMTI
jcmd <PID> PerfCounter.print                                     # счётчики производительности
```

---

## Когда jcmd лучше jmap/jstack/jinfo
- Практически все операции jmap/jstack/jinfo есть в jcmd с **единым синтаксисом**.
- Более «безопасная» и расширяемая диагностика live-процессов.
- Удобная справка `help`, единые имена команд, интеграция с **JFR** и **NMT**.

### Соответствия
| Задача | jmap/jstack/jinfo | jcmd (предпочтительно) |
|---|---|---|
| Heap info | `jmap -heap` | `GC.heap_info` |
| Class histogram | `jmap -histo` | `GC.class_histogram` |
| Heap dump | `jmap -dump:...` | `GC.heap_dump filename=...` |
| Thread dump | `jstack` | `Thread.print` |
| Флаги/свойства VM | `jinfo` | `VM.flags` / `VM.system_properties` |
| JFR | — | `JFR.start|dump|stop` |
| NMT | — | `VM.native_memory` |

---

## Влияние и осторожность
- **Overhead зависит от операции**, а не от инструмента. `GC.heap_dump` и `GC.class_histogram` могут инициировать маркировку → **STW-паузы**.
- **Heap dump** создаёт крупный файл (≈ объём живых данных). Проверьте место на диске.
- **Права/контейнеры**: требуется attach-доступ (тот же UID, доступ к `/proc/<PID>`). В Docker/K8s иногда нужны доп. привилегии, например `CAP_SYS_PTRACE` и неймспейсы PID.

---

## Быстрые рецепты

### Снять heap dump с меткой времени
```bash
TS=$(date +%F-%H%M%S)
jcmd <PID> GC.heap_dump filename=/tmp/heap-$TS.hprof
```

### «Быстрый» профайл (JFR) на 2 минуты и автосохранение
```bash
jcmd <PID> JFR.start name=quick settings=profile duration=2m filename=/tmp/quick.jfr
# по окончании файл появится автоматически
```

### Тред-дамп + гистограмма классов для корелляции
```bash
jcmd <PID> Thread.print > /tmp/threads.txt
jcmd <PID> GC.class_histogram > /tmp/classes.txt
```

---

## Советы эксплуатации
- Начинайте с **ненавязчивых** команд чтения (`help`, `VM.*`, `Thread.print`, `GC.heap_info`).  
- Для долгих расследований предпочитайте **JFR**: минимальная нагрузка и богатая телеметрия.  
- Для утечек памяти: снимите **heap dump**, анализируйте в **Eclipse MAT** (Dominator Tree, Retained Size).  
- В проде — предупреждайте SRE/DevOps и выполняйте тяжёлые операции вне пиков.

---

## Мини-шпаргалка команд
```
jcmd -l
jcmd <PID> help
jcmd <PID> GC.heap_info
jcmd <PID> GC.class_histogram
jcmd <PID> GC.heap_dump filename=heap.hprof
jcmd <PID> Thread.print
jcmd <PID> VM.flags
jcmd <PID> VM.system_properties
jcmd <PID> JFR.start|JFR.check|JFR.dump|JFR.stop
jcmd <PID> VM.native_memory summary
```
