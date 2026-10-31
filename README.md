# HeavenPM (HeavenChat)

[![Paper](https://img.shields.io/badge/Paper-1.21.11-2b2b2b?logo=paper&logoColor=white)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-JUnit-informational)](src/test/java)

Приватные сообщения, упоминания и красивые входы для **Purpur/Paper 1.21.11**.
Никаких зависимостей: всё, что нужно, плагин читает сам.

## Что умеет

- **`/w` и `/r`** — личные сообщения с кликабельной кнопкой «Ответить» прямо в строке чата.
- **Упоминания `@ник`** — подсветка ника, звук, защита от спама (кулдаун на игрока). Работает по границе ника: `@Kai` не сработает на `Kaito`, а самое длинное совпадение побеждает.
- **Hover-карточка игрока** — при наведении на ник видно игровое время и активный титул. Данные читаются из `HeavenTitles` напрямую, без жёсткой зависимости: нет плагина — нет карточки, чат работает.
- **Вход / первый вход / выход** — свои шаблоны с `&`-цветами, hex (`<#RRGGBB>`) и MiniMessage (`<gradient:#7AD9FF:#FFD76A>`). Плейсхолдеры `%player%`, `%online%`, `%max%`.
- **Анти-спам** — отдельные кулдауны на `/w` и на пинг упоминанием.
- **LuckPerms** — мягкая интеграция для префиксов, не обязательна.

## Команды

| Команда | Что делает | Право |
|---|---|---|
| `/w <ник> <текст>` | Написать игроку | `heavenchat.use` |
| `/r <текст>` | Ответить последнему собеседнику | `heavenchat.use` |

Клик по нику в сообщении подставляет `/w Ник `, клик по «Ответить» — `/r `.

## Права

| Право | По умолчанию | Назначение |
|---|---|---|
| `heavenchat.use` | всем | Личные сообщения и упоминания |
| `heavenchat.admin` | оператор | Административные действия |

## Конфиг

`plugins/HeavenChat/config.yml`:

```yaml
general:
  prefix: '<gradient:#7AD9FF:#FFD76A>[Чат]</gradient> <gray>'
  refresh-minutes: 10          # как часто перечитывать playtime/титулы

mentions:
  enabled: true
  highlight: true               # false = звук остаётся, подсветка выключается
  sound: BLOCK_NOTE_BLOCK_PLING
  cooldown-seconds: 5

pm:
  sound: ENTITY_EXPERIENCE_ORB_PICKUP
  cooldown-seconds: 2

welcome:
  enabled: true
  first-join: '<gradient:#FFD76A:#7AD9FF>Добро пожаловать, %player%!</gradient>'
  join: '<gray>%player% зашёл. Онлайн: %online%/%max%'
  quit: '<gray>%player% вышел.'
```

Конфиг применяется при старте сервера. Если в чате когда-нибудь начнут печататься
сырые теги — поставьте `mentions.highlight: false`, звук продолжит работать.

## Установка

1. Скачайте `HeavenChat-1.0.0.jar` из раздела **Releases** (или соберите сами).
2. Положите в `plugins/`.
3. Перезапустите сервер.

Сборка из исходников:

```bash
./gradlew build     # Windows: gradlew.bat build
# готовый jar: build/libs/HeavenChat-1.0.0.jar
```

Нужен JDK 21.

## Совместимость

- Purpur/Paper **1.21.11**, API 1.21, Java 21.
- Мягкие зависимости: `LuckPerms`, `HeavenTitles` (для hover-карточки).
- Конфликтов с другими чат-плагинами нет, пока они не перехватывают `AsyncChatEvent` раньше.

## Тесты

Чистая логика (разбор упоминаний, формат ЛС, шаблоны входа) вынесена из Bukkit и покрыта JUnit:

```bash
./gradlew test
```

## Структура

```
src/main/java/com/vitalis/chat/
  HeavenChat.java     — регистрация, события, планировщик
  WCommand.java       — /w и /r
  Mention.java        — поиск @ника (pure logic)
  PmFormat.java       — сборка компонентов ЛС (pure logic)
  WelcomeText.java    — шаблоны входа (pure logic)
  PlaytimeIndex.java  — playtime из HeavenTitles
  TitlesIndex.java    — активные титулы из HeavenTitles
```

## Лицензия

MIT — см. [LICENSE](LICENSE).

## Problem
Minecraft chat plugin (Bukkit/Spigot). Provides chat formatting, channels, and moderation tools.

## Architecture
Gradle + Java 17. Event-driven chat listeners, channel manager, permission checks.

## Trade-offs
- Simplicity over feature richness; targeted at single-server/single-tenant use.
- Java 17 baseline; no support for older runtimes.

## Benchmarks
- (add measurements here)

## Limitations
- Minecraft-version specific; requires matching server API.
- No automated integration tests against a live server.
