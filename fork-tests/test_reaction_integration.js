const fs = require('fs');
const assert = require('assert');
const base = require('path').resolve(__dirname, '../TMessagesProj/src/main/java/org/telegram') + '/';
const src = fs.readFileSync(base + 'ui/Components/ReactionsContainerLayout.java', 'utf8');
const start = src.indexOf('private void setVisibleReactionsList');
const method = src.slice(start, src.indexOf('private BlurredBackgroundDrawable', start));
assert(method.includes('type == TYPE_DEFAULT && !hitLimit && !hasStar'));
assert(method.includes('ForkReactionSettings.customized(currentAccount)'));
assert(method.includes('reaction.documentId == 0 && !reaction.isStar && reaction.emojicon != null'));
assert(method.includes('ReactionFilterPolicy.indices('));
assert(method.includes('ReactionFilterPolicy.narrowed('));
assert(method.includes('allReactionsList.addAll(nativeReactions)'));
assert(method.includes('visibleReactionsList == this.visibleReactionsList'));
assert(method.includes('this.visibleReactionsList.size() + (showCustomEmojiReaction() ? 1 : 0)'));
assert(src.includes('new CustomEmojiReactionsWindow(type, fragment, allReactionsList,'));
assert(src.indexOf('filterReactions(visibleReactions);') < src.indexOf('setVisibleReactionsList(visibleReactions, animated);'));
const ui = fs.readFileSync(base + 'ui/ForkReactionSettingsActivity.java', 'utf8');
assert(ui.includes('getEnabledReactionsList()'));
assert(ui.includes('ForkChoicePopup.show'));
assert(ui.includes('ForkReactionSettings.move(currentAccount'));
assert(ui.includes('ForkActionLabels.label(binding)'));
for (const locale of ['values', 'values-uk', 'values-ru']) {
  const xml = fs.readFileSync(require("path").resolve(__dirname, `../TMessagesProj/src/main/res/${locale}/fork_reactions.xml`), 'utf8');
  assert(xml.includes('name="ForkReactionInfo"'));
  assert(xml.includes('name="ForkReactionPosition"'));
}
console.log('Reaction native integration source checks passed');
