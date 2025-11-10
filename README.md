# 💻 Java ☕

- [Книга: Spring. Start Here](java/book_spring-start-here/spring-start-here.md)

## Troubleshooting Java

- [Troubleshooting Java](java/troubleshooting/java-troubleshooting-cheatsheet.md)
- [jcmd](java/troubleshooting/jcmd.md)
- [jmap](java/troubleshooting/jmap.md)
- [Книга: Troubleshooting Java](java/book_java-troubleshooting/troubleshooting-java.md)






# 🧠 LLM

- [Книга: Промпт-инжиниринг для LLM](llm/prompt-engineering-for-llms.md)

## 🤖 Агенты LLM

- Агенты LLM
    - [Агенты LLM: протоколы MCP, A2](https://vkvideo.ru/playlist/-49378_13/video-49378_456239261)

        - Агенты - системы, независимо от пользователя, выполняющие задачи от его имени.
          Технически, можно сказать, что это "LLM'ка с тулзами" - ReAct (Reasoning + Acting)
        
        - Агенты могут взаимодействовать с внешним миром, используя определенные API

        Для доступа к API можно использовать протоколы:

        - OpenAi API (проприетарный)
        - MCP (Model Context Protocol) от Antropic - открытый стандарт подключения ИИ-приложений к внешним системам; т.е. позволяет получить ИИ-приложению доступ к ресурсам и интрументам (tools) для решения задач (сама Antropic сравнивает его с разъемом USB type C)

        Но MCP работает только с одним пользователем/агентом/приложением, что делать если мы создаем не агентрую систему, а мультиагентную систему, где агентам нужно взаимодействовать между собой .. и для этого тоже есть протокол 

        - A2A (Agent 2 Agent) от Google

        MCP и A2A дополняют друг друга

    - [lookatme](https://vkvideo.ru/playlist/-49378_13/video-49378_456239254)

# 🔌 API

- [API](api/api.md)






# ☸️ k8s

- [k8s](k8s/k8s.md)

## Istio

- [Service-Mesh](k8s/istio/service-mesh.md)
    
## Unimon 

- [Unimon](k8s/unimon/unimon.md)






# 🐧 Linux

- [Что такое *nix-системы?](linux/unix-like-systems.md)
- [Популярные дистрибутивы Linux: Сводная схема](linux/linux-distributions-summary.md)
- [Шпаргалка по командам Linux](linux/linux_commands_cheatsheet.md)





# 🔐 Криптография

- [DH & RSA](crypto/dh_vs_rsa.md)

## TLS 

- [TLS](crypto/tls/tls.md)
    - [Разбор статьи на Хабре](crypto/tls/habr-https-tls-notes.md)
    - [Разбор статьи на misterpki](crypto/tls/curl-authentication.md)

## OTT

- [OTT](crypto/ott/ott.md)

## Wong Book

- [Wong Book](crypto/wong_book/wong.md)






# 🧩 Кодирование

- [Кодирование](encoding/encoding.md)
