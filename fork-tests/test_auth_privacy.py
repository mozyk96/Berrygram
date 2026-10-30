from pathlib import Path
root=Path(__file__).resolve().parents[1]
login=(root/'TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java').read_text(encoding='utf-8')
vars=(root/'TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java').read_text(encoding='utf-8')
gradle=(root/'TMessagesProj/build.gradle').read_text(encoding='utf-8')
assert 'settings.allow_firebase = false;' in login and 'settings.allow_app_hash = settings.allow_firebase =' not in login, 'third-party client must not request official Firebase SMS'
assert 'login token to check' not in login and '"; phone=" + phone' not in login, 'authentication logs expose secrets'
assert 'LOGS_ENABLED = DEBUG_VERSION ||' not in vars, 'debug logs enabled by default'
debug=gradle.split('        debug {',1)[1].split('        HA_private {',1)[0]
assert 'buildConfigField "boolean", "DEBUG_PRIVATE_VERSION", "false"' in debug, 'raw protocol dumps enabled in debug'
helper=(root/'TMessagesProj/src/main/java/org/telegram/messenger/AuthTokensHelper.java').read_text(encoding='utf-8')
assert 'saveLogInToken " + new String(token.future_auth_token' not in helper, 'login token must not be logged'
print('Auth privacy policy checks passed')
