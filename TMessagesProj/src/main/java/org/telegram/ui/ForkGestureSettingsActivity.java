package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.R;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.fork.actions.ActionRegistry;
import org.telegram.messenger.fork.actions.ActionSettings;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Cells.*;

/** Uses native confirmed taps, body-only overrides and per-account bindings. */
public final class ForkGestureSettingsActivity extends ForkSettingsFragment {
 private LinearLayout content;
 @Override public View createView(Context context) {
  actionBar.setTitle(ForkLocale.get(R.string.ForkGestureTitle));
  actionBar.setBackButtonImage(R.drawable.ic_ab_back);
  actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
  content=ForkSettingsLayout.page(context); rebuild(context);
  ScrollView scroll=new ScrollView(context);scroll.addView(content);fragmentView=scroll;return fragmentView;
 }
 private void rebuild(Context context) {
  content.removeAllViews();
  int[] titles={R.string.ForkGestureTap,R.string.ForkGestureDoubleTap,R.string.ForkGestureLongPress,R.string.ForkGestureLeft,R.string.ForkGestureRight};
  for(int i=0;i<ActionSettings.GESTURES.length;i++) {
   final String gesture=ActionSettings.GESTURES[i];
   TextSettingsCell row=new TextSettingsCell(context);
   row.setTextAndValue(ForkLocale.get(titles[i]),ForkActionLabels.label(ActionSettings.gesture(currentAccount,gesture)).toString(),false);
   ForkSettingsLayout.interactive(row);
   row.setOnClickListener(v->{
    CharSequence[] labels=new CharSequence[ActionRegistry.IDS.size()];int[] icons=new int[labels.length];
    for(int n=0;n<labels.length;n++){String id=ActionRegistry.IDS.get(n);labels[n]=ForkActionLabels.label(id);icons[n]=ForkActionLabels.icon(id);}
    ForkChoicePopup.show(this,row,labels,icons,ActionRegistry.IDS.indexOf(ActionSettings.gesture(currentAccount,gesture)),n->{ActionSettings.setGesture(currentAccount,gesture,ActionRegistry.IDS.get(n));rebuild(context);});
   });
   ForkSettingsLayout.addCard(content,row);
  }
  TextInfoPrivacyCell info=new TextInfoPrivacyCell(context);info.setText(ForkLocale.get(R.string.ForkGestureInfo));content.addView(info);
  TextSettingsCell reset=new TextSettingsCell(context);reset.setText(LocaleController.getString(R.string.Reset),false);ForkSettingsLayout.interactive(reset);reset.setOnClickListener(v->{ActionSettings.resetGestures(currentAccount);rebuild(context);});ForkSettingsLayout.addCard(content,reset);
 }
}
