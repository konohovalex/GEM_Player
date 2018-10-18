/* Class to hold all common Strings, to get easy access to them from any class */
package com.owlmanners.gem;

final class CommonStrings {
    // Some various Strings
    public static final String BY = "\nby\n";
    public static final String FROM = "\nfrom\n";

    // Toast texts
    public static final String PLEASE_GRANT_PERMISSIONS = "You should grant permission " +
            "to storage access, because otherwise #GEM won't be able to load " +
            "your songs and edit them on your wish, " +
            "please, grant it next time you start #GEM or in " +
            "Preferences/Applications/#GEM/Permissions.";

    public static final String NO_SONGS_ON_DEVICE = "You have no songs on your device, " +
            "please, add some, restart the app and rescan your device for them.\n" +
            "Playlists Menu and Search Menu are unavailable, but you can open any other menu.";

    public static final String DEFAULT_PLAYLISTS_CREATED = "Songs library was created.";

    public static final String DEFAULT_PLAYLISTS_RECREATED = "Songs library was updated.";

    public static final String NOTHING_TO_UPDATE = "Nothing to update.";

    public static final String AT_FIRST_SONG = "Already at first song in playlist, " +
            "click again to play last playlist's song or toggle Repeat All mode.";

    public static final String PLAYLIST_END = "Reached the end of playlist, " +
            "click again to play first playlist's song or toggle Repeat All mode.";

    public static final String SONG_REMOVED =
            "Seems like you removed that song during or before #GEM session.\n" +
                    "Please, choose another song to play " +
                    "or rescan songs on your device by performing one of following:\n" +
                    "1. rescan songs on your device by confirming forthcoming rescan dialog " +
                    "or from Preferences Menu any time you want;\n" +
                    "2. try to play this song again and confirm rescan dialog;\n" +
                    "3. turn on \"Show songs rescan request on each start?\" in Preferences Menu " +
                    "and restart #GEM.";

    public static final String EMPTY_SEARCH_FIELD = "Search field is empty, " +
            "please, input something.";

    public static final String NOTHING_IS_FOUND = "Nothing is found, try something else.";

    public static final String USER_PLAYLIST_SUCCESSFULLY_CREATED =
            "Your new User Playlist successfully created!";

    public static final String NEW_PLAYLIST_NAME_IS_NULL = "Please, input something and try again.";

    public static final String PLAYLIST_ALREADY_EXISTS = "Playlist under this name " +
            "is already exists, please, input other name.";

    public static final String NO_ONE_SONG_WERE_SELECTED = "Please, select at least one song.";

    public static final String NO_ONE_ITEM_WERE_SELECTED = "Please, select at least one item.";

    public static final String NOTHING_TO_ADD = "Seems like chosen User Playlist already " +
            "contains all the songs, that you have on device.";

    public static final String CURRENT_IS_THE_ONLY_USER = "You have only one User playlist, " +
            "and it's your CURRENT PLAYLIST now.\n" +
            "Want to add new one and add some of you current's songs to it?";

    public static final String NO_USER_PLAYLISTS_EXISTS = "You have no User playlists.\n" +
            "To proceed next you need to add one";

    public static final String INPUT_CORRECT_AMOUNT = "Please, input correct amount.";

    public static final String CHECKPOINT_WAS_SET = "Checkpoint position was set, " +
            "now this song will always be played from it.";

    public static final String CHECKPOINT_WAS_RESET = "Checkpoint position was reset to 0.";

    // Unknown buddys
    public static final String UNKNOWN_ARTIST = "UNKNOWN\tARTIST";
    public static final String UNKNOWN_ALBUM  = "UNKNOWN\tALBUM";
    public static final String UNKNOWN_TITLE  = "UNKNOWN\tTITLE";
    public static final String UNKNOWN_GENRE  = "UNKNOWN\tGENRE";
    public static final String UNKNOWN_YEAR   = "UNKNOWN\tYEAR";
    public static final String UNKNOWN_TRACK  = "UNKNOWN\tTRACK";
}