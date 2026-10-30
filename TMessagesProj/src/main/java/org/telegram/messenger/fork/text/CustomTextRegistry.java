package org.telegram.messenger.fork.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.telegram.messenger.R;

/** Explicit allowlist: never includes login, payment, security or destructive prompts. */
public final class CustomTextRegistry {
    public enum Category { ONLINE, LAST_SEEN, TYPING, RECORDING, SENDING, NAVIGATION, MEDIA }
    public static final class Entry {
        public final String key;
        public final int resource;
        public final boolean status;
        public final Category category;
        public final String fallback;
        Entry(String key, int resource, Category category, boolean status) { this(key, resource, category, status, null); }
        Entry(String key, int resource, Category category, boolean status, String fallback) {
            this.key = key; this.resource = resource; this.category = category; this.status = status; this.fallback = fallback;
        }
    }
    public static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
        new Entry("Online", R.string.Online, Category.ONLINE, true),
        new Entry("Lately", R.string.Lately, Category.LAST_SEEN, true),
        new Entry("WithinAWeek", R.string.WithinAWeek, Category.LAST_SEEN, true),
        new Entry("WithinAMonth", R.string.WithinAMonth, Category.LAST_SEEN, true),
        new Entry("Typing", R.string.Typing, Category.TYPING, true),
        new Entry("IsTyping", R.string.IsTyping, Category.TYPING, true),
        new Entry("AreTyping", R.string.AreTyping, Category.TYPING, true),
        new Entry("IsTypingGroup", R.string.IsTypingGroup, Category.TYPING, true),
        new Entry("AreTypingGroup", R.string.AreTypingGroup, Category.TYPING, true),
        new Entry("IsRecordingAudio", R.string.IsRecordingAudio, Category.RECORDING, true),
        new Entry("RecordingAudio", R.string.RecordingAudio, Category.RECORDING, true),
        new Entry("IsRecordingRound", R.string.IsRecordingRound, Category.RECORDING, true),
        new Entry("RecordingRound", R.string.RecordingRound, Category.RECORDING, true),
        new Entry("IsRecordingVideo", R.string.IsRecordingVideo, Category.RECORDING, true),
        new Entry("RecordingVideoStatus", R.string.RecordingVideoStatus, Category.RECORDING, true),
        new Entry("IsSendingAudio", R.string.IsSendingAudio, Category.SENDING, true),
        new Entry("SendingAudio", R.string.SendingAudio, Category.SENDING, true),
        new Entry("IsSendingPhoto", R.string.IsSendingPhoto, Category.SENDING, true),
        new Entry("SendingPhoto", R.string.SendingPhoto, Category.SENDING, true),
        new Entry("IsSendingVideo", R.string.IsSendingVideo, Category.SENDING, true),
        new Entry("SendingVideoStatus", R.string.SendingVideoStatus, Category.SENDING, true),
        new Entry("IsSendingFile", R.string.IsSendingFile, Category.SENDING, true),
        new Entry("SendingFile", R.string.SendingFile, Category.SENDING, true),
        new Entry("Contacts", R.string.Contacts, Category.NAVIGATION, false),
        new Entry("Chats", R.string.Chats, Category.NAVIGATION, false),
        new Entry("SavedMessages", R.string.SavedMessages, Category.NAVIGATION, false),
        new Entry("ArchivedChats", R.string.ArchivedChats, Category.NAVIGATION, false),
        new Entry("SearchMessages", R.string.SearchMessages, Category.NAVIGATION, false),
        new Entry("SharedMedia", R.string.SharedMedia, Category.NAVIGATION, false),
        new Entry("Gallery", R.string.Gallery, Category.NAVIGATION, false),
        new Entry("SearchContacts", R.string.SearchContacts, Category.NAVIGATION, false),
        new Entry("SearchChats", R.string.SearchChats, Category.NAVIGATION, false),
        new Entry("SearchTopics", R.string.SearchTopics, Category.NAVIGATION, false),
        new Entry("SearchMembers", R.string.SearchMembers, Category.NAVIGATION, false),
        new Entry("SearchChannels", R.string.SearchChannels, Category.NAVIGATION, false),
        new Entry("SearchForChats", R.string.SearchForChats, Category.NAVIGATION, false),
        new Entry("SearchAllChatsShort", R.string.SearchAllChatsShort, Category.NAVIGATION, false),
        new Entry("SearchEmptyViewTitle", R.string.SearchEmptyViewTitle, Category.NAVIGATION, false),
        new Entry("NoContacts", R.string.NoContacts, Category.NAVIGATION, false),
        new Entry("SearchEmptyViewFilteredSubtitleMusic", R.string.SearchEmptyViewFilteredSubtitleMusic, Category.MEDIA, false),
        new Entry("SearchEmptyViewFilteredSubtitleFiles", R.string.SearchEmptyViewFilteredSubtitleFiles, Category.MEDIA, false),
        new Entry("SearchEmptyViewFilteredSubtitleLinks", R.string.SearchEmptyViewFilteredSubtitleLinks, Category.MEDIA, false),
        new Entry("SearchEmptyViewFilteredSubtitleVoice", R.string.SearchEmptyViewFilteredSubtitleVoice, Category.MEDIA, false),
        new Entry("SearchEmptyViewFilteredSubtitleMedia", R.string.SearchEmptyViewFilteredSubtitleMedia, Category.MEDIA, false),
        new Entry("MediaTab", R.string.MediaTab, Category.MEDIA, false),
        new Entry("Files", R.string.Files, Category.MEDIA, false),
        new Entry("Music", R.string.Music, Category.MEDIA, false),
        new Entry("Voice", R.string.Voice, Category.MEDIA, false),
        new Entry("ChatSticker", R.string.ChatSticker, Category.MEDIA, false),
        new Entry("ChatLink", R.string.ChatLink, Category.MEDIA, false),
        new Entry("ChatDocument", R.string.ChatDocument, Category.MEDIA, false),
        new Entry("NoMessages", R.string.NoMessages, Category.MEDIA, false),
        new Entry("NoScheduledMessages", R.string.NoScheduledMessages, Category.MEDIA, false),
        new Entry("Members_zero", 0, Category.MEDIA, false, "Members_other"),
        new Entry("Members_one", R.string.Members_one, Category.MEDIA, false, "Members_other"),
        new Entry("Members_two", 0, Category.MEDIA, false, "Members_other"),
        new Entry("Members_few", 0, Category.MEDIA, false, "Members_other"),
        new Entry("Members_many", 0, Category.MEDIA, false, "Members_other"),
        new Entry("Members_other", R.string.Members_other, Category.MEDIA, false, "Members_other"),
        new Entry("Subscribers_zero", 0, Category.MEDIA, false, "Subscribers_other"),
        new Entry("Subscribers_one", R.string.Subscribers_one, Category.MEDIA, false, "Subscribers_other"),
        new Entry("Subscribers_two", 0, Category.MEDIA, false, "Subscribers_other"),
        new Entry("Subscribers_few", 0, Category.MEDIA, false, "Subscribers_other"),
        new Entry("Subscribers_many", 0, Category.MEDIA, false, "Subscribers_other"),
        new Entry("Subscribers_other", R.string.Subscribers_other, Category.MEDIA, false, "Subscribers_other"),
        new Entry("Photos_zero", 0, Category.MEDIA, false, "Photos_other"),
        new Entry("Photos_one", R.string.Photos_one, Category.MEDIA, false, "Photos_other"),
        new Entry("Photos_two", 0, Category.MEDIA, false, "Photos_other"),
        new Entry("Photos_few", 0, Category.MEDIA, false, "Photos_other"),
        new Entry("Photos_many", 0, Category.MEDIA, false, "Photos_other"),
        new Entry("Photos_other", R.string.Photos_other, Category.MEDIA, false, "Photos_other"),
        new Entry("Videos_zero", 0, Category.MEDIA, false, "Videos_other"),
        new Entry("Videos_one", R.string.Videos_one, Category.MEDIA, false, "Videos_other"),
        new Entry("Videos_two", 0, Category.MEDIA, false, "Videos_other"),
        new Entry("Videos_few", 0, Category.MEDIA, false, "Videos_other"),
        new Entry("Videos_many", 0, Category.MEDIA, false, "Videos_other"),
        new Entry("Videos_other", R.string.Videos_other, Category.MEDIA, false, "Videos_other"),
        new Entry("SavedMessagesCount_zero", 0, Category.MEDIA, false, "SavedMessagesCount_other"),
        new Entry("SavedMessagesCount_one", R.string.SavedMessagesCount_one, Category.MEDIA, false, "SavedMessagesCount_other"),
        new Entry("SavedMessagesCount_two", 0, Category.MEDIA, false, "SavedMessagesCount_other"),
        new Entry("SavedMessagesCount_few", 0, Category.MEDIA, false, "SavedMessagesCount_other"),
        new Entry("SavedMessagesCount_many", 0, Category.MEDIA, false, "SavedMessagesCount_other"),
        new Entry("SavedMessagesCount_other", R.string.SavedMessagesCount_other, Category.MEDIA, false, "SavedMessagesCount_other"),
        new Entry("AndMoreTypingGroup_zero", 0, Category.TYPING, true, "AndMoreTypingGroup_other"),
        new Entry("AndMoreTypingGroup_one", R.string.AndMoreTypingGroup_one, Category.TYPING, true, "AndMoreTypingGroup_other"),
        new Entry("AndMoreTypingGroup_two", 0, Category.TYPING, true, "AndMoreTypingGroup_other"),
        new Entry("AndMoreTypingGroup_few", 0, Category.TYPING, true, "AndMoreTypingGroup_other"),
        new Entry("AndMoreTypingGroup_many", 0, Category.TYPING, true, "AndMoreTypingGroup_other"),
        new Entry("AndMoreTypingGroup_other", R.string.AndMoreTypingGroup_other, Category.TYPING, true, "AndMoreTypingGroup_other")
    ));
    private static final Map<String, Entry> BY_KEY;
    private static final Map<Integer, Entry> BY_RESOURCE;
    static {
        Map<String, Entry> keys = new HashMap<>();
        Map<Integer, Entry> resources = new HashMap<>();
        for (Entry entry : ENTRIES) {
            if (!keys.containsKey(entry.key)) keys.put(entry.key, entry);
            if (entry.resource != 0 && !resources.containsKey(entry.resource)) resources.put(entry.resource, entry);
        }
        BY_KEY = Collections.unmodifiableMap(keys);
        BY_RESOURCE = Collections.unmodifiableMap(resources);
    }
    public static Entry find(String key, int resource) {
        return key != null ? BY_KEY.get(key) : resource != 0 ? BY_RESOURCE.get(resource) : null;
    }
    private CustomTextRegistry() {}
}
