import io
import os

base = os.path.join('TMessagesProj', 'src', 'main', 'res')

blocks = {
    'values': """    <string name="ForkNotificationFirebaseSetupHint">Remote push needs a Firebase project owned by this fork. Create an Android app with the package name org.example.forkclient, download google-services.json and place it into .fork-local/google-services.json of the project, then rebuild. The file stays on your machine and is never committed. Until then, notifications only arrive while the application process is alive.</string>
""",
    'values-uk': """    <string name="ForkNotificationFirebaseSetupHint">Для віддаленого push потрібен власний Firebase-проєкт Berrygram. Створіть Android-додаток з package name org.example.forkclient, завантажте google-services.json і покладіть його у .fork-local/google-services.json у проєкті, після чого перебудуйте застосунок. Файл залишається на вашому комп\'ютері і ніколи не потрапляє в Git. До того часу сповіщення надходитимуть лише поки процес застосунку живий.</string>
""",
    'values-ru': """    <string name="ForkNotificationFirebaseSetupHint">Для удалённого push нужен собственный Firebase-проект Berrygram. Создайте Android-приложение с package name org.example.forkclient, скачайте google-services.json и положите его в .fork-local/google-services.json проекта, после чего пересоберите приложение. Файл остаётся на вашем компьютере и никогда не попадает в Git. До этого момента уведомления приходят только пока процесс приложения жив.</string>
""",
}

for folder, block in blocks.items():
    path = os.path.join(base, folder, 'fork_custom_text.xml')
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    if 'ForkNotificationFirebaseSetupHint' not in text:
        text = text.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    with io.open(path, 'r', encoding='utf-8') as handle:
        check = handle.read()
    print(path, '->', check.count('ForkNotificationFirebaseSetupHint'))
