# Власний Android-клієнт: специфікація v1

## 1. Summary
Чистий форк офіційного Telegram Android, upstream 3de37c8cfb039452be7398909461da6288babf15. Робоча назва «Fork» — тимчасова, не бренд; власний package ID і логотип обираються до публікації. Пріоритет — надійність основних функцій та поступові зміни. Збереження сумісності не доведене, поки збірка та сценарії не перевірені.

## 2. Architecture analysis
- `TMessagesProj`: основний Android-код, UI, ресурси та native/JNI; `TMessagesProj_App*`: варіанти застосунку; `TMessagesProj_Modules/media` та інші залежності — git submodules.
- `ui/ActionBar/Theme.java` — токени, drawables та механізм тем; `ui/Components` і `ui/Cells` — наявні компоненти. `SharedConfig.bubbleRadius` використовується для малювання і вимірювань у `ChatMessageCell`, тому адаптер має оновлювати обидва шляхи та кеші.
- `ui/SettingsActivity.java` — існуючий кореневий екран та пошук; `ui/DialogsActivity.java`, `ui/Cells/DialogCell.java` — список чатів; `ui/ChatActivity.java` та `ui/Cells/ChatMessageCell.java` — повідомлення; `ui/ProfileActivity.java` — профілі.
- `messenger/LocaleController.java` та ресурси локалізації — локалізація; фрази підміняти лише через явний allowlist презентаційних статусів, з fallback до upstream, збереженням форматних аргументів і plural rules. Не перехоплювати всі getString.
- `messenger/TranslateController.java` уже реалізує переклад upstream. Зовнішній адаптер має бути окремим opt-in шляхом із явною згодою на передачу тексту, не ламати поточний контролер. Вимкнено за замовчуванням.
- Плагіни: спочатку типізовані, вбудовані extension points та декларативні пакети з перевіркою схеми. Жодного стороннього виконуваного коду без окремої моделі ізоляції, дозволів і ревʼю.

## 3. Design system v1
Принципи: стриманість, стабільна ієрархія, читабельність, доступність, один акцент, компактна інформаційна щільність. Токени — початкові проєктні значення, візуальна та контрастна перевірка ще потрібна.

| Токен | Light | Dark |
|---|---|---|
| background | #F5F6F8 | #101215 |
| surface | #FFFFFF | #1B1E22 |
| primary text | #171A1F | #F1F3F5 |
| secondary text | #5D6670 | #B1BAC4 |
| divider | #DDE1E5 | #343A41 |
| accent | #2869B2 | #8BBDF6 |

Типографіка: Android system sans як безпечний default, 24/30 sp заголовок, 17/22 основний текст, 15/20 вторинний, 13/18 метадані, 12/16 підпис; підтримувати системне масштабування та кирилицю, без вбудовування Apple-шрифтів без ліцензії. Мінімум 48 dp touch targets, контраст тексту перевірити при тестуванні.

Сітка 4 dp: 4/8/12/16/24/32; горизонтальні поля 16 dp, компактні списки 12–16 dp. Радіуси: sharp 4, balanced 10 для кнопок/12 для bubble, rounded 16, extra 22 dp; локальний override 0–24 dp, аватари окремо з безпечним default. Не застосовувати радіуси до геометрії вкладеного медіа до перевірки.

Bubble presets: Classic зберігає upstream; Compact зменшує вертикальні відступи, iOS-inspired має чисту геометрію, Ultra clean зменшує декоративні маркери, Bubble-heavy підсилює фон, Flat зменшує заливку. Поведінка базового Classic не змінюється до інтеграційних тестів.

Іконки: 24 dp viewport, однакова товщина 1.75–2 dp, filled лише для активного стану; перевірка RTL. Анімації: 120 ms прості зміни, 180 ms навігаційні переходи, 240 ms складні; системна настройка тривалості анімації має пріоритет. Тіні: 0 для списків, 1–3 dp тільки для overlay. Blur лише для тимчасових overlay на здатних пристроях, непрозорий fallback, Lite Mode і reduced motion підтримуються.

Settings IA: Appearance (preview, presets, Colors, Bubbles, Shapes, Fonts, Icons); Chat List; Chat Screen; Media; Profile; Navigation & Gestures; Text & Phrases; Translation; Effects; Advanced; Labs. Пошук охоплює ключі та локалізовані назви; reset кожного розділу та повного профілю з підтвердженням. Спочатку базові налаштування, advanced — явне розкриття.

## 4. Feasibility
| Запит | Статус | Умови |
|---|---|---|
| локальні теми, радіуси, профілі, жести | можливо | поступова інтеграція і тести |
| phrase packs | можливо частково | allowlist, локалі, валідація форматів, fallback |
| зовнішній переклад | можливо частково | ключі, квоти/вартість, передача стороннім сервісам, підтримка форматування |
| локальні версії вже отриманих повідомлень | частково / ризиковано | окремий opt-in, строки зберігання, захист, очищення, secret chats та політики потрібно окремо дослідити; невідомі повідомлення відновити неможливо |
| серверний Premium | неможливо розблокувати локально | сервер визначає права |
| довільні сторонні плагіни | відкладено | немає доведеної sandbox-моделі |

## 5. MVP та roadmap
- MVP: збірка upstream і smoke-тести; власна безпечна app identity; ізольовані налаштування appearance з міграцією, radius/bubble adapter, один екран налаштувань з preview; мінімальні UI toggles; allowlisted phrase packs; контракт перекладу, без мережевого провайдера за замовчуванням. Зміни дозувати після перевірки upstream baseline.
- v0.2: live previews, обмежені style presets, локальна персистенція та пошук, accessibility/RTL.
- v0.3: версійований імпорт/експорт із валідацією та обмеженням розмірів, фрази, opt-in зовнішні провайдери з приватністю, кешем і квотами.
- v0.5: жести, font/icon packs, декларативні plugin extensions, бюджет продуктивності та regression tests.
- v1.0: аудит захисту, стабільні міграції та оновлення upstream, CI, release signing, тестування багатьох пристроїв і належна публікація вихідного коду.

## 6. Step-by-step implementation plan
1. Зафіксувати commit upstream, рекурсивно завантажити submodules. Встановити рекомендовані SDK/NDK/JDK, власні тестові credentials і перевірити чисту debug build та ключові сценарії.
2. Визначити власний applicationId, Firebase config, deep links, authorities, підпис, назву та ресурси як єдину зміну; перевірити паралельну установку і входи.
3. Окрема модель налаштувань: immutable snapshot, schema version, bounds, migration, defaults без зміни upstream; тести на bounds, override і backward compatibility.
4. Адаптер радіусів для одного контрольованого елемента; мапування bubble на наявний SharedConfig без регресії layout; порівняти скріншоти, RTL та медіа.
5. Окремий екран appearance, збереження та preview; тестування відновлення після перезапуску, dynamic scale і accessibility.
6. Фрази: allowlist та типізовані placeholders, відхилення некоректних imports; ніколи не змінювати security/consent тексти.
7. Переклад: асинхронний контракт із cancellation, provider id, результатом/помилками, consent перед мережею, ізоляція ключів; адаптери, ліміти і кеш окремими задачами.
8. Прогін авторизації, чатів, медіа, дзвінків, нотифікацій, синхронізації та startup/performance на реальних пристроях.

## 7. Risks
Підмодулі, native-збірка та приватні credentials блокують відтворення; оновлення upstream може конфліктувати з великими UI-класами; package ID змінює authorities, links і push; локальна історія створює приватнісні ризики; translation витікає до стороннього сервісу; оптимізації та контраст потребують реальних вимірювань. GPL-2.0 та ліцензії залежностей потребують дотримання, включно з наданням вихідного коду відповідно до їх умов.

## 8. Concrete file/module change plan
- Новий `org/telegram/messenger/fork/appearance/AppearanceSettings.java` — модель радіусів, незалежна від Android; `fork-tests/AppearanceSettingsTest.java` — автономний тест.
- Надалі `fork/appearance/AppearanceStore.java` — versioned persistence, `AppearanceAdapter.java` — контрольована інтеграція в Theme/SharedConfig/Cells; зміни в `SettingsActivity` лише через окремий пункт.
- `fork/phrases/` — allowlist, resolver і валідатор; `fork/translation/` — provider contract, consent і адаптери; `fork/plugins/` — декларативні інтерфейси без виконання коду.
- `gradle.properties`, manifest, resources і app modules — identity лише після комплексної перевірки Firebase/links/authorities.

## 9. First coding step
Побудувати та перевірити ізольовану immutable модель радіусів, не підключаючи її до UI до підтвердження upstream baseline. Це фундамент, а не готовий MVP.


## 10. Phrase Override Engine — конкретний план
Мета: локальне перевизначення тільки дозволених некритичних фраз незалежно від поточної мови. Зберігання не залежить від `LocaleController`; окремий адаптер на вибраних місцях UI отримує фактичний стан та локалізований upstream fallback. Ключі — стабільні семантичні ідентифікатори, наприклад `presence.online`, а не оригінальний текст і не Android resource ID. Зміна локалі не стирає overrides.

Модулі: `fork/phrases/PhraseKey.java` (явний реєстр), `PhraseSchema.java` (дозволені типи аргументів, plural categories, accessibility та допустимі стани), `PhraseStore.java` (локальне versioned зберігання і міграції), `PhraseResolver.java` (пріоритет locale-specific → загальний override → upstream fallback), `PhraseValidator.java` (обмеження довжини, placeholder і plural validation), `ui/ForkPhraseSettingsActivity.java` (редактор, live preview з тестовими аргументами, reset окремого ключа та всіх ключів). Не використовувати перехоплення всіх `getString`; інтегрувати по одному семантичному ключу в chat list, профіль, chat header і лише потім інші поверхні. Вибір мови зберігає всі записи; plural — категорії поточної локалі, відсутні форми fallback до upstream. Текст accessibility береться з того самого фактичного стану та валідованої фрази; dynamic text, bidi, стилі/спани та аргументи мають безпечні fallback. Відхиляти невідомі ключі, некоректні типи/число placeholder, надмірні рядки та неповні обов'язкові форми.

Точність важливіша за кастомний напис: стан online/offline/last-seen/typing та read status повинен залишатися правдивим і зрозумілим. Довільні заміни статусів не включаються без перевірки відповідності API Terms і збереження фактичного статусу; за неможливості гарантувати це ключ не дозволений. Критичні security, consent, authorization, billing та системні помилки виключено. Перший інкремент — реєстр, validator, resolver і tests на зміну мови, аргументи, plural, RTL/accessibility, invalid inputs; потім реальний редактор та одна безпечна інтеграція з живим preview і збереженням після перезапуску.

## 11. Local Message Archive — дослідження, без реалізації
Стан: тільки дослідження; за замовчуванням вимкнений. Офіційні [Telegram API Terms §§1.1–1.4](https://core.telegram.org/api/terms) вимагають захисту приватності і забороняють перешкоджати зникненню self-destructing content та базовій функціональності. Ці положення явно не визначають усі сценарії локального збереження звичайних видалених повідомлень; відповідність такого архіву не підтверджена. До реалізації: письмово розібрати Telegram API Terms, Privacy/Security Guidelines та локальне право, зокрема server deletion, user deletion, clear history, protected content, TTL, view-once і очікування інших учасників; за потреби отримати роз'яснення Telegram. До завершення перевірки не збирати і не зберігати копії видалених повідомлень.

Точки дослідження upstream: `MessagesController` обробляє `TL_updateDeleteMessages` та `TL_updateEditMessage`; `MessagesStorage` зберігає/видаляє дані; `NotificationCenter.messagesDeleted` і `replaceMessagesObjects` — UI-події, але їх недостатньо як єдиного надійного джерела: можуть бути повтори, незавершені транзакції, пропущені події, видалення з push, каналів і кількох аккаунтів. Перед вибором integration point дослідити порядок транзакцій, reconciliation після reconnect, ідентичність (account + dialog/channel + message ID), race conditions, TTL та всі deletion paths. Архів не замінює стандартне видалення/синхронізацію; marker події і текст архіву відокремлені від основного чату й ніколи не видаються за актуальний серверний стан.

Якщо дозволеність підтверджена: окреме per-account шифроване сховище, AES-GCM з окремим випадковим data key, обгорнутим Android Keystore key; authenticated metadata, захист від підміни запису і перевірка перезапуску/втрати ключа. Ключі та архів виключити з Android backup/export, визначити поведінку при logout, видаленні облікового запису, зміні пристрою та uninstall. Механізм retention — явно обраний користувачем строк, гарантоване очищення expired даних і «очистити все»; пояснення обмежень secure deletion на flash. Додатковий opt-in і чітке пояснення локального зберігання. Відхиляти secret chats, будь-який self-destruct/TTL/view-once або сумнівний тип, media за замовчуванням виключити. Захист content із обмеженнями forwarding окремо оцінюється, default deny. Не відновлювати контент, який пристрій не отримав. Погрожувальні моделі: shared device, backup, compromised device, notifications/previews, видалення ключа. Тести: повний deny-list, перехід до TTL, дедуплікація, обробка всіх delete paths, retention/очищення, key loss, migration, офлайн/reconnect, logout, multi-account; privacy/security review до включення labs.

## 12. Credentials і пріоритет наступного інкремента
Вхід лише через власні `FORK_TELEGRAM_API_ID` / `FORK_TELEGRAM_API_HASH`; сценарій `build-with-credentials.ps1` просить їх приховано у локальному терміналі, передає в дочірній Gradle-процес і не створює файл секретів або лог. `api_hash` все одно вбудовується в APK та Gradle build intermediates і не є таємницею від власника APK. Каталоги build та `.fork-local/` ігноруються Git; build не слід запускати з `--debug`, публікувати APK/intermediates з особистими credentials або зберігати термінальний transcript із секретами. Тестова збірка без credentials показує явне повідомлення і не може авторизуватися. Firebase конфігурується окремо власними параметрами.

Першочергово: локальний ввід credentials → новий ARM64 debug APK → встановлення на підключений телефон за згодою → авторизація лише самим користувачем або на його явно дозволеному тестовому акаунті → перевірка чату, радіуса, збереження, медіа та перезапуску. Фразовий engine — після підтвердження стабільного основного сценарію; архів — тільки дослідження до вирішення питань Terms та приватності.
