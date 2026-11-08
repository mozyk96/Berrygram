import io
import os

base = os.path.join('TMessagesProj', 'src', 'main', 'res')

blocks = {
    'values': """    <string name="ForkArchiveRetention">Retention</string>
    <string name="ForkArchiveRetention7">7 days</string>
    <string name="ForkArchiveRetention30">30 days</string>
    <string name="ForkArchiveRetention90">90 days</string>
    <string name="ForkArchiveRetentionForever">Forever</string>
    <string name="ForkArchiveRetentionInfo">Archived messages, deleted-message markers and edit history older than the selected period are removed from this device. Everything is stored encrypted with a device-bound key and never leaves the phone.</string>
""",
    'values-uk': """    <string name="ForkArchiveRetention">Термін зберігання</string>
    <string name="ForkArchiveRetention7">7 днів</string>
    <string name="ForkArchiveRetention30">30 днів</string>
    <string name="ForkArchiveRetention90">90 днів</string>
    <string name="ForkArchiveRetentionForever">Завжди</string>
    <string name="ForkArchiveRetentionInfo">Заархівовані повідомлення, позначки видалених повідомлень та історія редагувань старші за вибраний строк видаляються з цього пристрою. Усе зберігається в зашифрованому вигляді ключем, прив’язаним до пристрою, і не залишає телефона.</string>
""",
    'values-ru': """    <string name="ForkArchiveRetention">Срок хранения</string>
    <string name="ForkArchiveRetention7">7 дней</string>
    <string name="ForkArchiveRetention30">30 дней</string>
    <string name="ForkArchiveRetention90">90 дней</string>
    <string name="ForkArchiveRetentionForever">Навсегда</string>
    <string name="ForkArchiveRetentionInfo">Заархивированные сообщения, отметки удалённых сообщений и история правок старше выбранного срока удаляются с этого устройства. Всё хранится в зашифрованном виде ключом, привязанным к устройству, и не покидает телефон.</string>
""",
}

for folder, block in blocks.items():
    path = os.path.join(base, folder, 'fork_custom_text.xml')
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    if 'ForkArchiveRetention"' not in text:
        text = text.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    with io.open(path, 'r', encoding='utf-8') as handle:
        check = handle.read()
    print(path, '->', check.count('ForkArchiveRetention'))
