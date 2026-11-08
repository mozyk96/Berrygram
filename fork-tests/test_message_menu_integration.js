const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const chat = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java'), 'utf8');
const menuSettings = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/ForkMessageMenuSettingsActivity.java'), 'utf8');

function check(condition, message) {
  if (!condition) throw new Error(message);
}

// The custom "More" page and its gear row must be gone from the message menu.
check(!chat.includes('ForkMenuEditorMore'), 'ChatActivity still contains the custom More row');
check(!chat.includes('forkMorePage'), 'ChatActivity still builds the custom More page');
check(!chat.includes('forkHiddenIndices'), 'ChatActivity still routes hidden actions to a More page');
check(!chat.includes('hasHidden(currentAccount)'), 'Hidden actions must not force the swipe-back container');

// All menu actions must be rendered directly in the main popup.
check(chat.includes('final java.util.ArrayList<Integer> forkMenuIndices = new java.util.ArrayList<>(forkMenuPlan.mainIndices);'),
  'ChatActivity must render mainIndices directly');
check(chat.includes('scrimPopupWindowItems[position] = cell;'), 'Menu items are not indexed for theming');
check(chat.includes('popupLayout.addView(cell);'), 'Menu items are not added to the main popup');

// Every appearance mode must select a distinct, real renderer.
check(chat.includes('MenuSurfacePolicy.surface(forkMenuStyle)'), 'Message menu does not resolve the appearance mode');
check(chat.includes('MenuSurfacePolicy.OPAQUE'), 'Message menu has no opaque No Effects path');
check(chat.includes('ForkGlassSurface.provider(resourceProvider)'), 'Glass 2.0 provider is not used by the message menu');
check(chat.includes('BlurredBackgroundProviderImpl.messageMenuBackground(resourceProvider)'), 'Blur provider is missing');
check(!chat.includes('.setColorProvider(BlurredBackgroundProviderImpl.messageMenuBackground(resourceProvider))'),
  'Glass mode must not fall back to the stock matte provider');
check(chat.includes('forkMenuSurface == org.telegram.messenger.fork.panels.MenuSurfacePolicy.GLASS'),
  'Glass mode is not distinguished for the reaction bar');
check(chat.includes('if (forkMenuSurface != org.telegram.messenger.fork.panels.MenuSurfacePolicy.OPAQUE)'),
  'No Effects must skip the reaction blur factory');

// The editor preview must not advertise a More section that no longer exists.
check(!menuSettings.includes('plan.moreIndices'), 'Message menu editor still previews the removed More section');
check(!menuSettings.includes('ForkMenuEditorMore'), 'Message menu editor still references the removed More section');

console.log('Message menu integration test passed (no More page, all three appearance modes)');
