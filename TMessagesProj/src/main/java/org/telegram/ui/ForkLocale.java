package org.telegram.ui;

import android.content.Context;
import android.content.res.Configuration;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.fork.text.ForkLocalePolicy;

/** Uses Telegram's chosen language for BerryGram's own Android resources. */
public final class ForkLocale {
    private ForkLocale() {}
    public static String get(int id, Object... arguments) {
        Context context = ApplicationLoader.applicationContext;
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(ForkLocalePolicy.select(LocaleController.getInstance().getCurrentLocale(), config.locale));
        Context localized = context.createConfigurationContext(config);
        return arguments.length == 0 ? localized.getString(id) : localized.getString(id, arguments);
    }
}
