import io
import os

base = os.path.join('TMessagesProj', 'src', 'main', 'res')

blocks = {
    'values': """    <string name="ForkPerformanceAffectsInfo">The performance profile controls the Berrygram effects only: glass and blur surfaces, the fluid popup animation and the embedded round-video preview. On the Low profile these effects are disabled and the preview is not started. Telegram itself keeps its own Lite mode setting, which is applied on top.</string>
""",
    'values-uk': """    <string name="ForkPerformanceAffectsInfo">Профіль продуктивності керує лише ефектами Berrygram: поверхнями glass і blur, анімацією спливаючих меню та вбудованим круглим відеопереглядом. У профілі Low ці ефекти вимкнені, а перегляд не запускається. Телеграм зберігає власне налаштування Lite mode, яке застосовується поверх.</string>
""",
    'values-ru': """    <string name="ForkPerformanceAffectsInfo">Профиль производительности управляет только эффектами Berrygram: поверхностями glass и blur, анимацией всплывающих меню и встроенным круглым видеопревью. В профиле Low эти эффекты отключены, а превью не запускается. Телеграм сохраняет собственную настройку Lite mode, которая применяется поверх.</string>
""",
}

for folder, block in blocks.items():
    path = os.path.join(base, folder, 'fork_custom_text.xml')
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    if 'ForkPerformanceAffectsInfo' not in text:
        text = text.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    with io.open(path, 'r', encoding='utf-8') as handle:
        check = handle.read()
    print(path, '->', check.count('ForkPerformanceAffectsInfo'))
