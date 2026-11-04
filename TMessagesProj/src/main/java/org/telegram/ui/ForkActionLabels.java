package org.telegram.ui;

import org.telegram.messenger.R;
import org.telegram.messenger.LocaleController;

/** One UI adapter for stable registry IDs used by all three editors. */
public final class ForkActionLabels {
 private ForkActionLabels() { }
 public static CharSequence label(String id) {
  switch(id) {
   case "reply": return LocaleController.getString(R.string.Reply);
   case "forward": return LocaleController.getString(R.string.Forward);
   case "copy": return LocaleController.getString(R.string.Copy);
   case "edit": return LocaleController.getString(R.string.Edit);
   case "delete": return LocaleController.getString(R.string.Delete);
   case "pin": return LocaleController.getString(R.string.PinMessage);
   case "save": return ForkLocale.get(R.string.ForkActionSave);
   case "reaction": return LocaleController.getString(R.string.Reactions);
   case "none": return ForkLocale.get(R.string.ForkActionNone);
   case "menu": return ForkLocale.get(R.string.ForkActionMenu);
   default: return ForkLocale.get(R.string.ForkActionNative);
  }
 }
 public static int icon(String id) {
  switch(id) {
   case "reply": return R.drawable.menu_reply;
   case "forward": return R.drawable.msg_forward;
   case "copy": return R.drawable.msg_copy;
   case "edit": return R.drawable.msg_edit;
   case "delete": return R.drawable.msg_delete;
   case "pin": return R.drawable.msg_pin;
   case "save": return R.drawable.msg_gallery;
   case "reaction": return R.drawable.msg_reactions;
   case "none": return R.drawable.msg_cancel;
   default: return R.drawable.msg_settings;
  }
 }
}
