# Android implementation map
## Registry
ChatActivity.fillMessageMenu / processSelectedOption: native options are eligibility truth. New messenger/fork/actions/ActionRegistry and ActionSettings; UI label/icon adapter. Registry option IDs verified against ChatActivity constants. Unsupported/unavailable actions never synthesized; delete/pin retain native dialogs. Ready: IDs/options/permissions tests and actual integration.
## Gestures
ChatActivity.onItemClickListener.hasDoubleTap/onDoubleTap/onItemClick; onItemLongClickListener; ChatMessageCell delegate.didLongPress; native chat RecyclerView.processTouchEvent swipe tracking. Reuse native tap delay, cancellation, dominant-horizontal threshold, action-mode guards. Configure only plain, noninteractive message bodies for tap overrides; retain links/media/buttons and selection/accessibility. Map per account; reset native. No destructive automatic action without native confirmation. Ready: executable mapping/dispatch/conflict tests, Android compilation and owner gesture testing.
## Reactions
Components/ReactionsContainerLayout.setVisibleReactionsList/setMessage and ChatActivity.onDoubleTap/selectReaction. Apply ordered1..4 favorites only after native allowed list computed; keep full native list/expander. Standard supported emoji; Premium/custom constraints remain native. Separate double-tap emoji; native validation before selectReaction. Ready: selection/order/count/restriction/persistence tests and manual chat test.
## Menu editor
ChatActivity.fillMessageMenu / createMenu builds parallel icons/items/options. Transform presentation together after native availability; hidden items remain in a reachable More/native fallback. Pin and drag reorder shared registry IDs; never expose unavailable options. New theme-aware editor with preview/reset. Ready: tuple alignment/unknown/hidden-all/safe access tests, Android compile and manual menu test.
## Settings
ForkSettingsActivity entry points; new compact native-themed editors using ForkChoicePopup, existing settings layout and resource strings in en/uk/ru. No ActionBar redesign. Account-local preferences. Test slot isolation/restart/reset/corrupt data and editor navigation.
