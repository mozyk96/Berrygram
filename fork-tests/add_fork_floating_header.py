# -*- coding: utf-8 -*-
"""Gives the archive screens the shared floating navigation header.

The archive screens used the plain Telegram action bar, every other Berrygram
screen uses the floating header with a round back button. The archive screens
without a menu now use the same header, the screen with the search keeps its
action bar.
"""
import io
import os

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
ui = os.path.join(root, 'TMessagesProj', 'src', 'main', 'java', 'org', 'telegram', 'ui')

PICKER = os.path.join(ui, 'ForkArchiveChatPickerActivity.kt')
MESSAGES = os.path.join(ui, 'ForkArchiveMessagesActivity.kt')

PICKER_OLD = """        actionBar.setTitle(ForkLocale.get(R.string.ForkArchiveImportTitle))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })

"""
PICKER_NEW = ""

MESSAGES_OLD = """        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(resolveDialogName(dialogId))
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })

"""
MESSAGES_NEW = ""


def patch(path, old, new, footer_old, footer_new):
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    if old not in text:
        print('header block not found in ' + os.path.basename(path))
        return False
    text = text.replace(old, new)
    if footer_old not in text:
        print('footer block not found in ' + os.path.basename(path))
        return False
    text = text.replace(footer_old, footer_new)
    with io.open(path, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)
    print('patched ' + os.path.basename(path))
    return True


patch(PICKER, PICKER_OLD, PICKER_NEW,
      """        loadChats()
        fragmentView = frame
        return frame
    }""",
      """        loadChats()
        fragmentView = ForkFloatingHeader.wrap(
            context,
            actionBar,
            frame,
            ForkLocale.get(R.string.ForkArchiveImportTitle)
        ) { finishFragment() }
        return fragmentView
    }""")

patch(MESSAGES, MESSAGES_OLD, MESSAGES_NEW,
      """        loadMessages()
        return frameLayout
    }""",
      """        loadMessages()
        fragmentView = ForkFloatingHeader.wrap(
            context,
            actionBar,
            frameLayout,
            resolveDialogName(dialogId)
        ) { finishFragment() }
        return fragmentView
    }""")
