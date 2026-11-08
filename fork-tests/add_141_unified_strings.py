import io
import os

base = os.path.join('TMessagesProj', 'src', 'main', 'res')

blocks = {
    'values': """    <string name="ForkUnifiedPrivacyTitle">Berrygram privacy</string>
    <string name="ForkUnifiedPrivacyInfo">Ghost mode, deleted messages, edit history and archive capture</string>
    <string name="ForkUnifiedEffectsTitle">Berrygram effects</string>
    <string name="ForkUnifiedEffectsInfo">Visual mode, blur, liquid glass and animations</string>
    <string name="ForkUnifiedMediaTitle">Media and input</string>
    <string name="ForkUnifiedMediaInfo">Camera and video, voice, gestures, reactions, message menu and custom text</string>
    <string name="ForkUnifiedDiagnosticsTitle">Notification diagnostics</string>
    <string name="ForkUnifiedDiagnosticsInfo">Permission, channels, push registration and a local test</string>
    <string name="ForkUnifiedTranslationTitle">Translation</string>
    <string name="ForkUnifiedTranslationInfo">Target language and automatic translation</string>
    <string name="ForkUnifiedArchiveTitle">Berry archive</string>
    <string name="ForkUnifiedArchiveInfo">Archived chats, create archive, update archive and storage usage</string>
    <string name="ForkUnifiedPerformanceTitle">Performance and device analyzer</string>
    <string name="ForkUnifiedPerformanceInfo">Automatic and manual performance profiles</string>
    <string name="ForkUnifiedUpdateTitle">Update Center</string>
    <string name="ForkUnifiedUpdateInfo">Check for updates, download and install</string>
""",
    'values-uk': """    <string name="ForkUnifiedPrivacyTitle">Приватність Berrygram</string>
    <string name="ForkUnifiedPrivacyInfo">Ghost Mode, видалені повідомлення, історія редагувань і захоплення архіву</string>
    <string name="ForkUnifiedEffectsTitle">Ефекти Berrygram</string>
    <string name="ForkUnifiedEffectsInfo">Режим visuals, blur, liquid glass та анімації</string>
    <string name="ForkUnifiedMediaTitle">Медіа та ввід</string>
    <string name="ForkUnifiedMediaInfo">Камера й відео, голос, жести, реакції, меню повідомлень і власні тексти</string>
    <string name="ForkUnifiedDiagnosticsTitle">Діагностика сповіщень</string>
    <string name="ForkUnifiedDiagnosticsInfo">Дозвіл, канали, реєстрація push і локальний тест</string>
    <string name="ForkUnifiedTranslationTitle">Переклад</string>
    <string name="ForkUnifiedTranslationInfo">Цільова мова та автоматичний переклад</string>
    <string name="ForkUnifiedArchiveTitle">Архів Berrygram</string>
    <string name="ForkUnifiedArchiveInfo">Архівовані чати, створення й оновлення архіву та використання сховища</string>
    <string name="ForkUnifiedPerformanceTitle">Продуктивність і аналізатор пристрою</string>
    <string name="ForkUnifiedPerformanceInfo">Автоматичний і ручний профілі продуктивності</string>
    <string name="ForkUnifiedUpdateTitle">Центр оновлень</string>
    <string name="ForkUnifiedUpdateInfo">Перевірка, завантаження та встановлення оновлень</string>
""",
    'values-ru': """    <string name="ForkUnifiedPrivacyTitle">Приватность Berrygram</string>
    <string name="ForkUnifiedPrivacyInfo">Ghost Mode, удалённые сообщения, история правок и захват архива</string>
    <string name="ForkUnifiedEffectsTitle">Эффекты Berrygram</string>
    <string name="ForkUnifiedEffectsInfo">Режим visuals, blur, liquid glass и анимации</string>
    <string name="ForkUnifiedMediaTitle">Медиа и ввод</string>
    <string name="ForkUnifiedMediaInfo">Камера и видео, голос, жесты, реакции, меню сообщений и свои тексты</string>
    <string name="ForkUnifiedDiagnosticsTitle">Диагностика уведомлений</string>
    <string name="ForkUnifiedDiagnosticsInfo">Разрешение, каналы, регистрация push и локальный тест</string>
    <string name="ForkUnifiedTranslationTitle">Перевод</string>
    <string name="ForkUnifiedTranslationInfo">Целевой язык и автоматический перевод</string>
    <string name="ForkUnifiedArchiveTitle">Архив Berrygram</string>
    <string name="ForkUnifiedArchiveInfo">Архивированные чаты, создание и обновление архива и использование хранилища</string>
    <string name="ForkUnifiedPerformanceTitle">Производительность и анализатор устройства</string>
    <string name="ForkUnifiedPerformanceInfo">Автоматический и ручной профили производительности</string>
    <string name="ForkUnifiedUpdateTitle">Центр обновлений</string>
    <string name="ForkUnifiedUpdateInfo">Проверка, загрузка и установка обновлений</string>
""",
}

for folder, block in blocks.items():
    path = os.path.join(base, folder, 'fork_custom_text.xml')
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    if 'ForkUnifiedPrivacyTitle' not in text:
        text = text.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    with io.open(path, 'r', encoding='utf-8') as handle:
        check = handle.read()
    print(path, '->', check.count('ForkUnified'))
