# -*- coding: utf-8 -*-
"""Adds the Berrygram settings section headers to the fork string resources.

The headers are used by the settings blocks that are embedded into the
Telegram settings screens. Every locale receives the same resource names, so
a missing translation can never silently fall back to an empty label.
"""
import io
import os

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
res_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'res')

STRINGS = {
    'values': [
        ('ForkSectionAppearance', 'Appearance'),
        ('ForkSectionEffects', 'Effects'),
        ('ForkSectionMessages', 'Messages'),
        ('ForkSectionMedia', 'Media and input'),
        ('ForkSectionGhostMode', 'Ghost Mode'),
        ('ForkSectionPrivacy', 'Privacy'),
        ('ForkSectionArchive', 'Archive'),
        ('ForkSectionDiagnostics', 'Diagnostics'),
        ('ForkSectionTranslation', 'Translation'),
        ('ForkSectionPerformance', 'Performance'),
        ('ForkSectionUpdates', 'Updates'),
    ],
    'values-uk': [
        ('ForkSectionAppearance', 'Вигляд'),
        ('ForkSectionEffects', 'Ефекти'),
        ('ForkSectionMessages', 'Повідомлення'),
        ('ForkSectionMedia', 'Медіа та введення'),
        ('ForkSectionGhostMode', 'Ghost Mode'),
        ('ForkSectionPrivacy', 'Конфіденційність'),
        ('ForkSectionArchive', 'Архів'),
        ('ForkSectionDiagnostics', 'Діагностика'),
        ('ForkSectionTranslation', 'Переклад'),
        ('ForkSectionPerformance', 'Продуктивність'),
        ('ForkSectionUpdates', 'Оновлення'),
    ],
    'values-ru': [
        ('ForkSectionAppearance', 'Внешний вид'),
        ('ForkSectionEffects', 'Эффекты'),
        ('ForkSectionMessages', 'Сообщения'),
        ('ForkSectionMedia', 'Медиа и ввод'),
        ('ForkSectionGhostMode', 'Ghost Mode'),
        ('ForkSectionPrivacy', 'Конфиденциальность'),
        ('ForkSectionArchive', 'Архив'),
        ('ForkSectionDiagnostics', 'Диагностика'),
        ('ForkSectionTranslation', 'Перевод'),
        ('ForkSectionPerformance', 'Производительность'),
        ('ForkSectionUpdates', 'Обновления'),
    ],
}


def main():
    for folder, entries in STRINGS.items():
        path = os.path.join(res_root, folder, 'fork_custom_text.xml')
        if not os.path.exists(path):
            raise SystemExit('missing resource file: ' + path)
        with io.open(path, 'r', encoding='utf-8') as handle:
            text = handle.read()
        missing = []
        for name, value in entries:
            if ('name="%s"' % name) in text:
                continue
            missing.append('    <string name="%s">%s</string>' % (name, value))
        if not missing:
            continue
        index = text.rindex('</resources>')
        text = text[:index] + '\n'.join(missing) + '\n' + text[index:]
        with io.open(path, 'w', encoding='utf-8') as handle:
            handle.write(text)
        print('updated %s with %d strings' % (folder, len(missing)))


if __name__ == '__main__':
    main()
