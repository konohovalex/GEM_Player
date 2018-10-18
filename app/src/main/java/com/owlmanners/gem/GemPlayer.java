package com.owlmanners.gem;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.Gson;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jp.wasabeef.blurry.Blurry;
import rm.com.audiowave.AudioWaveView;
import rm.com.audiowave.OnProgressListener;

public class GemPlayer extends AppCompatActivity {
    /*tbd put in right place*/
    private boolean                    isNotificationShown = false;
    private static final int           NOTIFICATION_ID     = 1;
    private static final String        PLAY_PREVIOUS_SONG  = "PLAY_PREVIOUS_SONG";
    private static final String        PLAY_PAUSE_SONG     = "PLAY_PAUSE_SONG";
    private static final String        PLAY_NEXT_SONG      = "PLAY_NEXT_SONG";

    private NotificationManager        notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationReceiver       notificationReceiver;
    private RemoteViews                notificationViews;

    public class NotificationReceiver extends BroadcastReceiver {
        public NotificationReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case PLAY_PREVIOUS_SONG:
                        playPreviousSong(null);
                        break;
                    case PLAY_PAUSE_SONG:
                        onPlayPauseClick(null);
                        createOrUpdateNotification();
                        break;
                    case PLAY_NEXT_SONG:
                        playNextSong(null);
                        break;
                }
            }
        }
    }

    public Bitmap drawableToBitmap (Drawable drawable) {
        return ((BitmapDrawable) drawable).getBitmap();
    }

    void createOrUpdateNotification() {
        Intent        notificationIntent = new Intent(this, GemPlayer.class);
        PendingIntent contentIntent      = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent previousSongIntent = PendingIntent.getBroadcast(
                GemPlayer.this, 1,
                new Intent(PLAY_PREVIOUS_SONG),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent playPauseIntent    = PendingIntent.getBroadcast(
                GemPlayer.this, 1,
                new Intent(PLAY_PAUSE_SONG),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextSongIntent     = PendingIntent.getBroadcast(
                GemPlayer.this, 1,
                new Intent(PLAY_NEXT_SONG),
                PendingIntent.FLAG_UPDATE_CURRENT);

        notificationViews = new RemoteViews(getPackageName(), R.layout.gem_notification_panel);

        notificationViews.setImageViewBitmap(
                R.id.notification_cover,
                drawableToBitmap(cover_button_small.getDrawable()));

        notificationViews.setTextViewText(
                R.id.notification_song_title,
                current_song_title_player.getText());
        notificationViews.setTextColor(R.id.notification_song_title, themeColor1);

        notificationViews.setTextViewText(
                R.id.notification_song_artist,
                current_song_artist_player.getText());
        notificationViews.setTextColor(R.id.notification_song_artist, themeColor1);
        notificationViews.setTextViewText(
                R.id.notification_song_album,
                current_song_album_player.getText());
        notificationViews.setTextColor(R.id.notification_song_album, themeColor1);

        notificationViews.setImageViewBitmap(
                R.id.notification_previous_song_button,
                drawableToBitmap(player_screen_previous_song_button.getDrawable()));
        notificationViews.setOnClickPendingIntent(
                R.id.notification_previous_song_button,
                previousSongIntent);

        if (isPlaying) {
            notificationViews.setImageViewBitmap(
                    R.id.notification_play_pause_song_button_button,
                    drawableToBitmap(getImageFromAssets(imageNames[8])));
        }
        else {
            notificationViews.setImageViewBitmap(
                    R.id.notification_play_pause_song_button_button,
                    drawableToBitmap(getImageFromAssets(imageNames[7])));
        }
        notificationViews.setOnClickPendingIntent(
                R.id.notification_play_pause_song_button_button,
                playPauseIntent);

        notificationViews.setImageViewBitmap(
                R.id.notification_next_song_button,
                drawableToBitmap(player_screen_next_song_button.getDrawable()));
        notificationViews.setOnClickPendingIntent(
                R.id.notification_next_song_button,
                nextSongIntent);

        notificationBuilder = new NotificationCompat.Builder(
                GemPlayer.this, "#GEM_NOTIFICATION")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(themeColor1)
                .setCustomContentView(notificationViews)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(contentIntent)
                .setShowWhen(false)
                .setAutoCancel(false);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        isNotificationShown = true;
    }
    /*tbd put in right place*/

    class GemMediaPlayer extends MediaPlayer implements
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnSeekCompleteListener,
            MediaPlayer.OnInfoListener,
            AudioManager.OnAudioFocusChangeListener {


        private boolean            ongoingCall = false;
        private PhoneStateListener phoneStateListener;
        private TelephonyManager   telephonyManager;
        // Handle incoming phone calls
        private void callStateListener() {
            // Get the telephony manager
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            // Starting listening for PhoneState changes
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    switch (state) {
                        // If at least one call exists or the phone is ringing - pause the MediaPlayer
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                        case TelephonyManager.CALL_STATE_RINGING:
                            pauseMedia();
                            ongoingCall = true;
                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            // Phone idle. Start playing.
                            if (ongoingCall) {
                                ongoingCall = false;
                                playMedia();
                            }
                            break;
                    }
                }
            };
            // Register the listener with the telephony manager
            // Listen for changes to the device call state
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        //"Becoming noisy" actions - user removed headphones
        private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pauseMedia();
            }
        };
        // Register NotificationReceiver after getting audio focus
        private void registerBecomingNoisyReceiver() {
            registerReceiver(
                    becomingNoisyReceiver,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        // An instance of AudioManager, used to handle AudioFocus
        private AudioManager       audioManager;
        // Initialize audioManager, request AudioFocus and check it's status
        private boolean requestAudioFocus() {
            int result = 0;
            if (audioManager != null) {
                result = audioManager.requestAudioFocus(
                        this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
            // returns focus gained or not
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        // Remove AudioFocus, then can be used to check if it's removed
        private boolean removeAudioFocus() {
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                    audioManager.abandonAudioFocus(this);
        }
        @Override
        public void onAudioFocusChange(int focusState) {
            switch (focusState) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // Resume playback
                    if (!isPlaying()) {
                        start();
                    }
                    setVolume(1.0f, 1.0f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // Lost focus for an unbounded amount of time:
                    // remember resumePosition and stop playback
                    if (isPlaying()) {
                        resumePosition = getCurrentPosition();
                        stopMedia();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Lost focus for a short time, but we have to stop playback
                    // We don't release the media player because playback is likely to resume
                    if (isPlaying()) {
                        pauseMedia();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // Lost focus for a short time,
                    // but it's ok to keep playing at an attenuated level
                    if (isPlaying()) {
                        setVolume(0.1f, 0.1f);
                    }
                    break;
            }
        }

        GemMediaPlayer() {
            super();
            setOnCompletionListener(this);
            setOnErrorListener(this);
            setOnPreparedListener(this);
            setOnSeekCompleteListener(this);
            setOnInfoListener(this);
            setAudioStreamType(AudioManager.STREAM_MUSIC);
            callStateListener();
            registerBecomingNoisyReceiver();
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            int whereToSeek = resumePosition == CHECKPOINT_PRIORITY ?
                    (int) currentlyPlayingSong.getCheckpointPosition() :
                    (resumePosition == SEEK_TO_SONG_START ? 0 : resumePosition);
            seekTo(whereToSeek);
            resumePosition = CHECKPOINT_PRIORITY;
            refreshSeekbarState();
            refreshTimerScore();
            gemMediaPlayerIsPrepared = true;
        }
        @Override
        public void onCompletion(MediaPlayer mp) {
            playNextSong(null);
        }
        @Override
        public void onSeekComplete(MediaPlayer mp) {
        }

        // Playback control methods
        // Start playback
        public void playMedia() {
            try {
                if (requestAudioFocus()) {
                    reset();

                    isPlaying = true;

                    setDataSource(currentlyPlayingSongName);
                    prepare();

                    switchPlayPauseButtonImage();

                    createOrUpdateNotification();

                    start();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Pause playback
        public void pauseMedia() {
            resumePosition = getCurrentPosition();
            pause();

            isPlaying = false;
            switchPlayPauseButtonImage();

            createOrUpdateNotification();
        }
        // Stop playback
        public void stopMedia() {
            if (isPlaying()) {
                stop();
            }

            isPlaying = false;
            switchPlayPauseButtonImage();

            gemMediaPlayerIsPrepared = false;

            createOrUpdateNotification();
        }
        // Release actions
        @Override
        public void release() {
            resumePosition = getCurrentPosition();
            if (isPlaying) {
                stopMedia();
            }

            if (phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            unregisterReceiver(becomingNoisyReceiver);
            removeAudioFocus();

            notificationManager.cancelAll();
            super.release();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    Log.d("MediaPlayer Error",
                            "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    Log.d("MediaPlayer Error",
                            "MEDIA ERROR SERVER DIED " + extra);
                    break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    Log.d("MediaPlayer Error",
                            "MEDIA ERROR UNKNOWN " + extra);
                    break;
            }
            return false;
        }
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            return false;
        }
    }

    // Resources instance
    private Resources                res                                    = null;
    
    // Screen metrics for setting cover metrics
    private DisplayMetrics           displayMetrics                         = null;

    // LayoutInflater instance
    private LayoutInflater           inflater                               = null;
    
    // Handles soft-keyboard hideout
    private InputMethodManager       imm                                    = null;

    // Boolean for necessary permissions
    private volatile boolean         arePermissionsGranted                  = false;

    // First boot flag and usage instructions coarse flags
    private boolean                  isFirstTimeBoot                        = true;
    /*tbd - usage guide*/

    // User preferences object and name
    private SharedPreferences        userPrefs                              = null;
    private final String             prefsFileName                          = "com.owlmanners.gem";

    // Gson instance to serialize stuff
    private Gson                     gson                                   = null;

    // Playback variables
    // GemMediaPlayer instance for audio control
    private GemMediaPlayer           gemMediaPlayer                         = null;
    // Miscellaneous Playback related stuff
    private boolean                  gemMediaPlayerIsPrepared               = false;
    private boolean                  isPlaying                              = false;
    private boolean                  isShuffled                             = false;
    private int          amountOfGuaranteedNonRepetitiveSongsWhileShuffling = 0;
    private ArrayList<Integer>       songsAlreadyPlayedInShuffleMode        = null;
    private RepeatState              repeatState                            = RepeatState.NO_REPEAT;
    private static final int         CHECKPOINT_PRIORITY                    = -1;
    private static final int         SEEK_TO_SONG_START                     = -2;
    private int                      resumePosition                         = CHECKPOINT_PRIORITY;

    // Handler constants, responding to it's actions
    // Loading screen constants
    private static final int         SHOW_LOADING_SCREEN                    = 0;
    private static final int         HIDE_LOADING_SCREEN                    = 1;

    // PLAYLISTS MENU CONSTANTS
    private static final int         FILL_PLAYLISTS_OPTIONS                 = 2;
    // "CURRENT PLAYLIST" OPTION ACTIONS
    private static final int         FILL_CURRENT_PLAYLIST_SONGS            = 3;
    // "ALL SONGS" OPTION ACTIONS
    private static final int         FILL_ALL_SONGS                         = 4;
    // "ALL ARTISTS" & "ALL ALBUMS" OPTIONS' ACTIONS
    private static final int         FILL_ALL_ARTIST_NAMES                  = 5;
    private static final int         FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST = 6;
    private static final int         FILL_ALL_ARTIST_SONGS                  = 7;
    private static final int         FILL_ALL_ARTIST_ALBUMS                 = 8;
    private static final int         FILL_SPECIFIC_ALBUM_SONGS              = 9;
    private static final int         FILL_ALL_ALBUM_TITLES                  = 10;
    // "ALL GENRES" OPTION ACTIONS
    private static final int         FILL_ALL_GENRE_NAMES                   = 11;
    private static final int         FILL_SPECIFIC_GENRE_SONGS              = 12;
    // "ALL USER" OPTION ACTIONS
    private static final int         FILL_ALL_USER_PLAYLISTS_NAMES          = 13;
    private static final int         ADD_NEW_USER_PLAYLIST_OR_MERGE_SOME    = 14;
    private static final int         MERGE_USER_PLAYLISTS                   = 15;
    private static final int         FILL_CHOSEN_USER_PLAYLIST_SONGS        = 16;
    private static final int         CHOSEN_USER_PLAYLIST_IS_EMPTY          = 17;
    private static final int         ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST    = 18;
    private static final int       DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST = 19;

    // SEARCH MENU ACTIONS
    private static final int         FILL_SEARCH_RESULTS                    = 20;
    private static final int         SEARCH_AGAIN                           = 21;

    // PREFERENCES MENU CONSTANTS
    private static final int         SHOW_ALL_GEM_THEMES                    = 22;
    private static final int         SHOW_ADDITIONAL_INFO_LINE_OPTIONS      = 23;

    // Main menu variables
    // Responsible for Main menu visibility
    private boolean                  isMainMenuShown                        = false;
    // Responsible for switching menu button images and hiding/showing menus
    private boolean                  isHelpMenuToggled                      = false;
    private boolean                  isEqualizerMenuToggled                 = false;
    private boolean                  isPlaylistsMenuToggled                 = false;
    private boolean                  isPreferencesMenuToggled               = false;
    private boolean                  isSearchMenuToggled                    = false;
    // Responsible for showing large Main menu submenus
    private boolean                  isEqualizerMenuEnlarged                = false;
    private boolean                  isHelpMenuEnlarged                     = false;
    private boolean                  isPlaylistsMenuEnlarged                = false;
    private boolean                  isPreferencesMenuEnlarged              = false;
    private boolean                  isSearchMenuEnlarged                   = false;
    // Responsible for showing large additional_info_line
    private boolean                  isEnlargedAdditionalInfoLineShown      = false;
    // Responsible for showing large element description in Help menu
    private boolean                  isHelpMenuLargeElementDescriptionShown = false;

    // Back button flags
    private boolean                  backInsteadOfExit                      = false;
    private boolean                  isBackButtonHasLongClick               = false;
    private boolean                  deepInPreferences                      = false;

    // Equalizer variables
    private String[]                 equalizerPresetNames                   = null;
    private String                   currentEqualizerPresetName             = "";

    // Help menu variables
    private int                      checkedHelpMenuTopicPosition           = -1;
    private int                      checkedHelpMenuGroupPosition           = -1;
   private BaseExpandableListAdapter help_menu_topicsAdapter                = null;
    private String                   currentHelpMenuImageName               = "";

    // Playlists menu variables - navigation
    private volatile int             whereAreWe                             = 0;
    private boolean                  fromAllAlbums                          = false;
    private int                      backOption                             = 0;
    private String                   artistToShow                           = "";
    private String                   albumToShow                            = "";
    private String                   genreToShow                            = "";
    private String                   userPlaylistToShow                     = "";
    // Playlists-related variables
    private String[]                 playlistsOptionsNames                  = null;
    private final HashMap<String, Integer>
                                     prefixesWithIndexes                    = new HashMap<>();
    // Main songs library array
    private ArrayList<Song>          allSongs                               = null;
    // and CURRENT PLAYLIST array (which contains only indices of Songs allSongs)
    private ArrayList<Integer>       currentPlaylist                        = null;
    // Holds all User Playlist names
    private ArrayList<String>        allUserPlaylistsNames                  = null;
    // Adapter-related variables
    private BaseAdapter              defaultPlaylistsMenuAdapter            = null;
    private BaseAdapter              playlists_menu_objectsAdapter          = null;
    private SparseBooleanArray       checkedElements                        = null;
    private ArrayList<String>        adapterList                            = null;
    private ArrayList<Integer>       songsMediator                          = null;
    private int                      order                                  = 0;
    private final String[]           playlistNameMediator                   = new String[1];
    // currentlyPlayingSong-related variables
    private String                   currentPlaylistName                    = "";
    private Song                     currentlyPlayingSong                   = null;
    private int                      currentlyPlayingSongOrder              = 0;
    private String                   currentlyPlayingSongName               = "";
    private long                     currentlyPlayingSongDuration           = 0;
    private boolean               currentlyPlayingSongDurationContainsHours = false;
    private boolean             currentlyPlayingSongDurationContainsMinutes = false;
    private boolean                  playFirstSongAfterLastOnce             = false;
    private boolean                  playLastSongAfterFirstOnce             = false;
    private String                   complexAlbumInfoForRecovery            = "";
    // MERGE_USER_PLAYLISTS varibales
    private String                   newUserPlaylistNameMerge               = "";
    private boolean                  needToMerge                            = false;
    // Used, when User want to delete Songs from User Playlist or want to add something to it,
    // only for highlighting selected item
    private int                      checkedSongUserPlaylistSongsAddition   = 0;
    private int                      checkedSongUserPlaylistSongsDeletion   = 0;
    // Songs addition procedure-related variables and objects
    private boolean                  inSongsAdditionProcedure               = false;
    private ArrayList<Object>        whatToAdd                              = null;
    private ArrayList<String>        whereToAdd                             = null;
    private int                      whereAreWeAddition                     = 0;
    private final int                SHORT_WAY                              = 1;
    private final int                STANDART_WAY_STEP_1                    = 2;
    private final int                STANDART_WAY_STEP_2                    = 3;
    private final int                ADDITION_COMPLETED                     = 4;
    private boolean                  isShortWayAvailable                    = false;
    private boolean                  needToGoToUserPlaylists                = false;
    private ArrayList<Integer>       songsToAddToPlaylist                   = null;
    private ArrayList<String>        allUserPlaylistsNamesCopy              = null;
    // Playlists Menu State-recovery system
    private Parcelable               playlists_menu_objectsState            = null;
    private BaseAdapter              playlists_menu_objectsCopy             = null;
    // Used only while walkthrough FILL_SPECIFIC_ALBUM_SONGS's "SHOW ALL ALBUMS"
    private SparseIntArray           firstVisiblePositions                  = new SparseIntArray();
    private SparseIntArray           firstVisiblePositionsUser              = new SparseIntArray();
    private boolean                  needToBackToSpecificAlbumSongs         = false;
    private String                   albumToShowCopy                        = "";
    private int                      firstVisiblePositionBackup             = -1;

    // Preferences flags and other stuff
    private volatile int             whereAreWePreferences                  = 0;
    // Arrays, taken from res, used to hold names of all theme-related images and theme names and
    // res id's for Playlists Menu icons
    private String[]                 imageNames                             = null;
    private String[]                 themeNames                             = null;
    private int[]                    playlistsIconsIds                      = null;
    // Variable flags
    private boolean                  needToShowRescanRequestAtStart         = false;
    private boolean                  needToShowSongDetailsOnMainMenu        = true;
    private boolean                  textTickerEffectManuallyDisabling      = false;
    private boolean                  needToShowPlaybackControlsOnMainMenu   = true;
    private boolean                  isCheckPointSystemEnabled              = true;
    // Theme-related variables
    private BaseAdapter              gem_themesAdapter                      = null;
    private String                   currentThemeName                       = "";
    private int                      themeColor1                            = -1;
    private int                      themeColor2                            = -1;
    private int                      themeColorSeekbar                      = -1;
    // additional_info_line-related variables
    private BaseAdapter              additional_info_line_optionsAdapter    = null;
    private String[]                 additional_info_line_optionsTexts      = null;
    private int                      current_additional_info_lineOption     = -1;

    // Search menu variables
   private BaseExpandableListAdapter search_menu_found_matchesAdapter       = null;
    private ExpandableListView       found_artists_inner_list               = null;
    private ExpandableListView       found_albums_inner_list                = null;
    private ExpandableListView       found_genres_inner_list                = null;
    private ArrayList<String>        resultGroups                           = null;
    private ArrayList<Integer>       foundSongs                             = null;
    private ArrayList<String>        foundArtistNames                       = null;
    private ArrayList<ArrayList<Integer>> foundArtistSongs                  = null;
    private ArrayList<String>        foundAlbumTitles                       = null;
    private ArrayList<ArrayList<Integer>> foundAlbumSongs                   = null;
    private ArrayList<String>        foundGenreNames                        = null;
    private ArrayList<ArrayList<Integer>> foundGenreSongs                   = null;
    private String                   foundSongsGroupName                    = "";
    private String                   foundArtistsGroupName                  = "";
    private String                   foundAlbumsGroupName                   = "";
    private String                   foundGenresGroupName                   = "";
    // Responsible for remembering position to highlighting
    private int                      checkedSearchMenuGroupPosition         = -1;
    private int                      checkedSearchMenuItemPosition          = -1;
    private String                   checkedSearchMenuGroupName             = "";
    private int                      checkedSearchMenuGroupPositionInner    = -1;
    private int                      checkedSearchMenuItemPositionInner     = -1;

    // Player screen flags
    private boolean                  isCoverEnlarged                        = false;

    // UI Objects
    // Player screen for hideout/showdown animation
    private RelativeLayout     background_layout;
    private LinearLayout       player_screen;
    // Player screen elements: left-to-right and top-to-bottom
    // Top layout
    private LinearLayout       player_top_layout;
    private ImageButton        player_screen_equalizer_button;
    private ImageButton        player_screen_help_button;
    private ImageButton        player_screen_preferences_button;
    private ImageButton        cover_button_small;
    private ImageButton        player_screen_playlists_button;
    private TextView           player_screen_additional_info_line;
    // Middle layout
    private LinearLayout       player_middle_layout;
    private TextView           current_song_artist_player;
    private TextView           current_song_order_player;
    private TextView           current_song_album_player;
    private TextView           timer;
    private TextView           timer_divider;
    private TextView           currently_playing_song_duration_view;
    private TextView           currently_playing_song_checkpoint_time;
    private ImageButton        checkpoint_system_button;
    private AudioWaveView      seekbar;
    private TextView           current_song_title_player;
    // Bottom layout
    private LinearLayout       player_bottom_layout;
    private ImageButton        player_screen_previous_song_button;
    private ImageButton        player_screen_play_pause_song_button;
    private Drawable           play_button_image;
    private Drawable           pause_button_image;
    private ImageButton        player_screen_next_song_button;
    private ImageButton        player_screen_shuffle_button;
    private Drawable           straight_button_image;
    private Drawable           shuffle_button_image;
    private ImageButton        player_screen_search_button;
    private ImageButton        player_screen_repeat_button;
    private Drawable           no_repeat_button_image;
    private Drawable           repeat_one_button_image;
    private Drawable           repeat_all_button_image;

    // Enlarged cover button object
    private ImageButton        cover_button_large;

    // Main menu and it's elements
    private ImageView          shadow; // Always shown under current Main menu, can close it onClick

    // Main menu elements
    private LinearLayout       main_menu;
    private LinearLayout       main_menu_menu_s_buttons;
    private ImageView          main_menu_top_divider;
    private ImageButton        main_menu_help_button;
    private ImageButton        main_menu_equalizer_button;
    private ImageButton        main_menu_playlists_button;
    private ImageButton        main_menu_preferences_button;
    private ImageButton        main_menu_search_button;
    private LinearLayout       main_menu_song_details;
    private LinearLayout       main_menu_playback_controls_1;
    private ImageButton        main_menu_previous_song_button;
    private ImageButton        main_menu_play_pause_song_button;
    private ImageButton        main_menu_next_song_button;
    private LinearLayout       main_menu_playback_controls_2;
    private Space              main_menu_playback_controls_2_space_1;
    private ImageButton        main_menu_shuffle_button;
    private Space              main_menu_playback_controls_2_space_2;
    private ImageButton        main_menu_exit_and_back_button;
    private Space              main_menu_playback_controls_2_space_3;
    private ImageButton        main_menu_repeat_button;
    private Space              main_menu_playback_controls_2_space_4;
    private ImageView          main_menu_bottom_divider;
    // Current song Main menu attributes
    private TextView           current_song_artist_main_menu;
    private TextView           current_song_title_main_menu;
    private TextView           current_song_album_main_menu;
    private Button             main_menu_s_enlarged_submenu_decrease_button;

    // Help menu's elements
    private LinearLayout       help_menu;
    private ImageView          help_menu_image_view;
    private ExpandableListView help_menu_topics;
    private TextView           help_menu_element_description_small_text;
    private LinearLayout       help_menu_element_description_large_layout;
    private TextView           help_menu_element_description_large_text;
    private ImageView          help_menu_element_description_large_layout_image_view;
    private Button             help_menu_element_description_large_layout_decrease_button;

    // Equalizer menu elements
    private LinearLayout       equalizer_menu;
    private ImageButton        equalizer_menu_delete_current_preset_button;
    private TextView           equalizer_menu_current_preset_name;
    private ImageButton        equalizer_menu_add_new_preset_button;

    // Playlists menu elements
    private LinearLayout       playlists_menu;
    private TextView           playlists_menu_additional_info_line;
    private ListView           playlists_menu_objects;
    private ImageButton        playlists_menu_confirm_button;
    private ImageView          playlists_menu_divider;

    // Preferences menu elements
    private LinearLayout       preferences_menu;
    private TextView           preferences_menu_header;
    private ImageView          preferences_menu_header_divider;
    private ScrollView         preferences_menu_elements;
    private TextView           show_all_themes_text;
    private ImageButton        show_all_themes_button;
    private ListView           gem_themes;
    private TextView           show_additional_info_line_options_text;
    private ImageButton        show_additional_info_line_options_button;
    private ListView           additional_info_line_options;
    private Switch             songs_rescan_request_on_start_check;
    private Button             songs_rescan_button;
    private Switch             main_menu_song_details_check;
    private Switch             text_ticker_effect_manually_disabling_check;
    private Switch             main_menu_playback_controls_check;
    private Switch             checkpoint_system_check;
    private TextView           gnr_amount_text;
    private EditText           gnr_amount;
    private ImageView          preferences_menu_divider1;
    private ImageView          preferences_menu_divider2;
    private ImageView          preferences_menu_divider3;
    private ImageView          preferences_menu_divider4;
    private ImageView          preferences_menu_divider5;
    private ImageView          preferences_menu_divider6;
    private ImageView          preferences_menu_divider7;
    private ImageView          preferences_menu_divider8;

    // Search menu elements
    private LinearLayout       search_menu;
    private EditText           search_menu_search_field;
    private ImageButton        search_menu_confirm_button;
    private ExpandableListView search_menu_found_matches;

    // Enlarged additional_info_line elements and flags
    private LinearLayout       enlarged_additional_info_line_layout;
    private TextView           enlarged_additional_info_line_text;
    private Button             enlarged_additional_info_line_decrease_button;

    // Loading screen elements
    private RelativeLayout     loading_screen;
    private ImageView          loading_screen_icon;
    private TextView           loading_screen_text;

    // Custom Toast and Custom Dialog elements
    private boolean            isCustomToastShown = false;
    private RelativeLayout     custom_toast;
    private ImageView          custom_toast_shadow;
    private RelativeLayout     custom_toast_frame_outer;
    private LinearLayout       custom_toast_frame_inner;
    private TextView           custom_toast_message;
    private Button             custom_toast_close_button;

    private boolean            isCustomDialogShown = false;
    private RelativeLayout     custom_dialog;
    private ImageView          custom_dialog_shadow;
    private RelativeLayout     custom_dialog_frame_outer;
    private Switch             custom_dialog_songs_rescan_request_on_start_check;
    private EditText           custom_dialog_user_input;
    private LinearLayout       custom_dialog_frame_inner;
    private TextView           custom_dialog_message;
    private Button             custom_dialog_cancel_button;
    private Button             custom_dialog_confirm_button;

    // Method, that returns first letter of String
    char returnFirstLetter(String item) {
        for (Map.Entry<String, Integer> items : prefixesWithIndexes.entrySet()) {
            if (item.startsWith(items.getKey())) {
                item = item.substring(items.getValue());
            }
        }

        return Character.toUpperCase(item.charAt(0));
    }
    // Method, that returns unprefixed items
    String[] returnUnprefixedItems(String item1, String item2) {
        String[] titles = new String[] { item1, item2};

        for (Map.Entry<String, Integer> items : prefixesWithIndexes.entrySet()) {
            if (item1.startsWith(items.getKey())) {
                titles[0] = item1.substring(items.getValue());
            }
            if (item2.startsWith(items.getKey())) {
                titles[1] = item2.substring(items.getValue());
            }
        }

        return titles;
    }
    // Custom comparator for Song objects by track for songs in albums
    Comparator<Integer> songsInAlbumComparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer index1, Integer index2) {
            Song o1 = allSongs.get(index1);
            Song o2 = allSongs.get(index2);
            String[] withoutPrefixes = returnUnprefixedItems(o1.getTitle(), o2.getTitle());

            if (o1.getFictiveTrack().contains("/") & o2.getFictiveTrack().contains("/")) {
                int order1           = Integer.parseInt(o1.getFictiveTrack().split("/")[0]);
                int order2           = Integer.parseInt(o2.getFictiveTrack().split("/")[0]);
                int comparisonResult = Integer.compare(order1, order2);
                if (comparisonResult != 0) {
                    return comparisonResult;
                }
                else {
                    return withoutPrefixes[0].compareToIgnoreCase(withoutPrefixes[1]);
                }
            }
            else {
                if (o1.getFictiveTrack().contains("/") & !o2.getFictiveTrack().contains("/")) {
                    int order1 = Integer.parseInt(o1.getFictiveTrack().split("/")[0]);
                    try {
                        return Integer.compare(order1, Integer.parseInt(o2.getFictiveTrack()));
                    }
                    catch (NumberFormatException e) {
                        return String.valueOf(order1).compareTo(o2.getFictiveTrack());
                    }
                }
                if (!o1.getFictiveTrack().contains("/") & o2.getFictiveTrack().contains("/")) {
                    int order2 = Integer.parseInt(o2.getFictiveTrack().split("/")[0]);
                    try {
                        return Integer.compare(Integer.parseInt(o1.getFictiveTrack()), order2);
                    }
                    catch (NumberFormatException e) {
                        return o1.getFictiveTrack().compareTo(String.valueOf(order2));
                    }
                }
                else {
                    try {
                        return Integer.compare(Integer.parseInt(o1.getFictiveTrack()),
                                Integer.parseInt(o2.getFictiveTrack()));
                    }
                    catch (NumberFormatException e) {
                        return o1.getFictiveTrack().compareTo(o2.getFictiveTrack());
                    }
                }
            }
        }
    };
    // Custom comparator for albums by year
    Comparator<String> albumsByYearComparator = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            String[] withoutPrefixes = returnUnprefixedItems(o1, o2);
            String   title1          = withoutPrefixes[0];
            String   title2          = withoutPrefixes[1];

            if (title1.endsWith("]") & title2.endsWith("]")) {
                int year1 = -1;
                int year2 = -1;
                String presumablyYear1 = title1.substring(title1.lastIndexOf("[") + 1,
                        title1.lastIndexOf("]"));
                String presumablyYear2 = title2.substring(title2.lastIndexOf("[") + 1,
                        title2.lastIndexOf("]"));
                try {
                    if (presumablyYear1.length() == 4) {
                        year1 = Integer.parseInt(presumablyYear1);
                    }
                    if (presumablyYear2.length() == 4) {
                        year2 = Integer.parseInt(presumablyYear2);
                    }
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                if (year1 != -1 & year2 != -1) {
                    int comparisonResult = Integer.compare(year1, year2);

                    if (comparisonResult != 0) {
                        return comparisonResult;
                    }
                    else {
                        return title1.substring(
                                0, title1.lastIndexOf("[") - 1)
                                .compareToIgnoreCase(title2.substring(
                                        0, title2.lastIndexOf("[") - 1));
                    }
                }
                else {
                    if (year1 != -1) {
                        return -1;
                    }
                    if (year2 != -1) {
                        return 1;
                    }
                    return title1.compareToIgnoreCase(title2);
                }
            }
            else {
                if (title1.endsWith("]")) {
                    int year1 = -1;
                    String presumablyYear1 = title1.substring(title1.lastIndexOf("[") + 1,
                            title1.lastIndexOf("]"));
                    try {
                        if (presumablyYear1.length() == 4) {
                            year1 = Integer.parseInt(presumablyYear1);
                        }
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (year1 != -1) {
                        return -1;
                    }
                    else {
                        return title1.compareToIgnoreCase(title2);
                    }
                }
                if (title2.endsWith("]")) {
                    int year2 = -1;
                    String presumablyYear2 = title2.substring(title2.lastIndexOf("[") + 1,
                            title2.lastIndexOf("]"));
                    try {
                        if (presumablyYear2.length() == 4) {
                            year2 = Integer.parseInt(presumablyYear2);
                        }
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (year2 != -1) {
                        return 1;
                    }
                    else {
                        return title1.compareToIgnoreCase(title2);
                    }
                }
                return title1.compareToIgnoreCase(title2);
            }
        }
    };

    // Custom comparator by unprefixed Song title for songs outside albums
    Comparator<Song> songTitlesComparator = new Comparator<Song>() {
        @Override
        public int compare(Song o1, Song o2) {
            String[] withoutPrefixes = returnUnprefixedItems(o1.getTitle(), o2.getTitle());
            int comparison1 = withoutPrefixes[0].compareToIgnoreCase(withoutPrefixes[1]);

            if (comparison1 == 0) {
                int comparison2 = o1.getArtist().compareToIgnoreCase(o2.getArtist());
                if (comparison2 == 0) {
                    int comparison3 = albumsByYearComparator.compare(
                            o1.getAlbum() + o1.checkYear(), o2.getAlbum() + o2.checkYear());
                    if (comparison3 == 0) {
                        int comparison4 = o1.checkTrack().compareToIgnoreCase(o2.checkTrack());
                        if (comparison4 == 0) {
                            return o1.getGenre().compareToIgnoreCase(o2.getGenre());
                        }
                        else {
                            return comparison4;
                        }
                    }
                    else {
                        return comparison3;
                    }
                }
                else {
                    return comparison2;
                }
            }
            else {
                return comparison1;
            }
        }
    };
    // Custom comparator by unprefixed items
    Comparator<String> unprefixedItemsComparator = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            String[] withoutPrefixes = returnUnprefixedItems(o1, o2);
            return withoutPrefixes[0].compareToIgnoreCase(withoutPrefixes[1]);
        }
    };

    // Methods to reset playlists objects (just for code clarity)
    void resetAllPlaylistsRelativeObjectsToEmpty() {
        allSongs                     = new ArrayList<>();
        currentPlaylist              = new ArrayList<>();
        currentPlaylistName          = "";
        currentlyPlayingSong         = null;
        currentlyPlayingSongOrder    = 0;
        currentlyPlayingSongName     = "";
        complexAlbumInfoForRecovery  = "";
        currentlyPlayingSongDuration = 0;
        resumePosition               = CHECKPOINT_PRIORITY;

        allUserPlaylistsNames        = new ArrayList<>();
        firstVisiblePositions        = new SparseIntArray();
        firstVisiblePositionsUser    = new SparseIntArray();

        for (int i = 2; i < 16; i++) {
            if (i != 6 & i != 14 & i != 15) {
                firstVisiblePositions.put(i, -1);
            }
        }
    }
    void resetCurrentPlaylistToAllSongs() {
        currentPlaylist = new ArrayList<>();
        for (int i = 0; i < allSongs.size(); i++) {
            currentPlaylist.add(i);
        }
        firstVisiblePositions.removeAt(FILL_CURRENT_PLAYLIST_SONGS);
    }
    void resetCurrentPlaylistRelativeObjectsToAllSongs() {
        currentPlaylistName          = playlistsOptionsNames[1] + "\n";
        currentlyPlayingSong         = allSongs.get(0);
        currentlyPlayingSongOrder    = 0;
        currentlyPlayingSongName     = currentlyPlayingSong.getData();
        complexAlbumInfoForRecovery  = "";
        currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
        resumePosition               = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;
        seekbar.setProgress(100 *
                (resumePosition == CHECKPOINT_PRIORITY ?
                        currentlyPlayingSong.getCheckpointPosition() : resumePosition) /
                currentlyPlayingSongDuration);
    }

    // Request permissions methods:
    // Request permissions code
    void requestPermissions() {
        // Ensure, that user uses Marshmallow and check, if necessary permissions granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!arePermissionsGranted) {
                requestPermissions(new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }
    }
    // Request permissions callback
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                arePermissionsGranted = true;
            }
            else {
                showCustomToast(CommonStrings.PLEASE_GRANT_PERMISSIONS);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Method, that checks, if user deleted all the stuff, that should be shown
    // (full artist deletion, full album deletion etc.) during #GEM session,
    // if yes - step back at Playlists menu up to FILL_PLAYLISTS_OPTIONS
    boolean checkIfThereAnythingToShow() {
        switch (whereAreWe) {
            case FILL_CURRENT_PLAYLIST_SONGS:
                if (currentPlaylist.isEmpty()) {
                    uiHandler.sendEmptyMessage(FILL_PLAYLISTS_OPTIONS);
                    return false;
                }
                else {
                    return true;
                }
            case FILL_ALL_SONGS:
                if (allSongs.isEmpty()) {
                    uiHandler.sendEmptyMessage(FILL_PLAYLISTS_OPTIONS);
                    return false;
                }
                else {
                    return true;
                }
            default:
                if (adapterList.isEmpty() & songsMediator.isEmpty()) {
                    if (allSongs.isEmpty()) {
                        backOption = FILL_PLAYLISTS_OPTIONS;
                    }
                    uiHandler.sendEmptyMessage(backOption);
                    return false;
                }
                else {
                    return true;
                }
        }
    }

    // Method, that requests songs on device rescan
    void requestSongsRescan(final boolean isAtStart) {
        if (!isFirstTimeBoot) {
            showCustomDialog(
                    res.getString(R.string.request_audio_rescan),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isAtStart) {
                                songs_rescan_request_on_start_check.setChecked(
                                        custom_dialog_songs_rescan_request_on_start_check
                                                .isChecked());
                            }

                            hideCustomDialog();

                            loadAudio(isAtStart);
                        }
                    },
                    null,
                    isAtStart,
                    false);
        }
        else {
            loadAudio(isAtStart);
        }
    }
    // Method, used to receive all the songs from all the storages, proceed in side thread
    void loadAudio(final boolean isAtStart) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!arePermissionsGranted) {
                    try {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (isFirstTimeBoot) {
                    loading_screen_text.setText(R.string.please_wait_until_songs_library_created);
                }
                else {
                    loading_screen_text.setText(R.string.please_wait_until_songs_library_updated);
                }
                uiHandler.sendEmptyMessage(SHOW_LOADING_SCREEN);

                ContentResolver contentResolver    = getContentResolver();

                Uri             externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String          selection          =
                        MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.IS_ALARM + " = 0 AND " +
                        MediaStore.Audio.Media.IS_NOTIFICATION + " = 0 AND " +
                        MediaStore.Audio.Media.IS_RINGTONE + " = 0";

                String          sortOrder          = MediaStore.Audio.Media.TITLE + " ASC";

                Cursor          externalCursor     = contentResolver.query(
                        externalContentUri, null, selection, null, sortOrder);

                ArrayList<Song> foundSongs         = new ArrayList<>();

                if (externalCursor != null && externalCursor.getCount() > 0) {
                    while (externalCursor.moveToNext()) {
                        String                 data      = externalCursor.getString
                                (externalCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));

                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(data);
                        }
                        catch (IllegalArgumentException e) {
                            continue;
                        }

                        String artist = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String album  = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        String title  = externalCursor.getString
                                (externalCursor.getColumnIndex(
                                        MediaStore.Audio.AudioColumns.TITLE));
                        String genre  = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_GENRE);
                        String year   = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_YEAR);
                        String track  = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                        long duration = Long.parseLong
                                (retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_DURATION));

                        foundSongs.add(new Song(
                                data, artist, album, title, genre, year, track, duration));
                    }
                }

                if (foundSongs.isEmpty()) {
                    resetAllPlaylistsRelativeObjectsToEmpty();

                    Log.i("State", CommonStrings.NO_SONGS_ON_DEVICE);
                }
                else {
                    Collections.sort(foundSongs, songTitlesComparator);

                    //Check if playlist changed
                    boolean needToUpdateAllSongs = false;
                    if (!isFirstTimeBoot) {
                        if (foundSongs.size() == allSongs.size()) {
                            for (int i = 0; i < allSongs.size(); i++) {
                                if (!foundSongs.get(i).equals(allSongs.get(i))) {
                                    needToUpdateAllSongs = true;
                                    Log.i("State", "Same size, different songs" + i);
                                    break;
                                }
                            }
                        }
                        else {
                            needToUpdateAllSongs = true;
                        }
                    }
                    else {
                        needToUpdateAllSongs = true;
                    }

                    if (needToUpdateAllSongs) {
                        for (int i = 2; i < 16; i++) {
                            if (i != 6 & i != 14 & i != 15) {
                                firstVisiblePositions.put(i, -1);
                            }
                        }

                        if (!isFirstTimeBoot) {
                            ArrayList<Song> removedSongs = new ArrayList<>(allSongs);
                            removedSongs.removeAll(foundSongs);
                            foundSongs.removeAll(allSongs);

                            for (int i = allSongs.size() - 1; i >= 0; i--) {
                                Song iterableSong = allSongs.get(i);
                                if (removedSongs.contains(iterableSong)) {
                                    allSongs.remove(i);
                                    if (currentPlaylist.contains(i)) {
                                        currentPlaylist.remove(i);
                                    }
                                }
                            }
                            allSongs.addAll(foundSongs);

                            if (allSongs.isEmpty()) {
                                resetAllPlaylistsRelativeObjectsToEmpty();

                                Log.i("State", CommonStrings.NO_SONGS_ON_DEVICE);
                            }
                            else {
                                Collections.sort(allSongs, songTitlesComparator);

                                if (currentPlaylist.isEmpty()) {
                                    resetCurrentPlaylistToAllSongs();
                                    resetCurrentPlaylistRelativeObjectsToAllSongs();
                                }
                                else {
                                    if (currentPlaylistName.equals(
                                            playlistsOptionsNames[1] + "\n")) {
                                        resetCurrentPlaylistToAllSongs();
                                    }

                                    if (!complexAlbumInfoForRecovery.equals("")) {
                                        Collections.sort(currentPlaylist);
                                    }

                                    boolean isPreviousCurrentlyPlayingSongFound = false;
                                    for (int i = 0; i < currentPlaylist.size(); i++) {
                                        if (allSongs.get(currentPlaylist.get(i)).getData()
                                                .equals(currentlyPlayingSongName)) {
                                            currentlyPlayingSongOrder   = i;
                                            isPreviousCurrentlyPlayingSongFound = true;
                                            break;
                                        }
                                    }
                                    if (!isPreviousCurrentlyPlayingSongFound) {
                                        currentlyPlayingSongOrder = 0;
                                        resumePosition = isCheckPointSystemEnabled ?
                                                CHECKPOINT_PRIORITY : 0;
                                    }

                                    seekbar.setProgress(100 *
                                            (resumePosition == CHECKPOINT_PRIORITY ?
                                                    currentlyPlayingSong.getCheckpointPosition() :
                                                    resumePosition) /
                                            currentlyPlayingSongDuration);
                                    currentlyPlayingSong         = allSongs.get(
                                            currentPlaylist.get(currentlyPlayingSongOrder));
                                    currentlyPlayingSongName     = currentlyPlayingSong.getData();
                                    currentlyPlayingSongDuration =
                                            currentlyPlayingSong.getDuration();
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showCustomToast(CommonStrings.DEFAULT_PLAYLISTS_RECREATED);
                                }
                            });

                            Log.i("State", CommonStrings.DEFAULT_PLAYLISTS_RECREATED);
                        }
                        else {
                            allSongs = foundSongs;
                            resetCurrentPlaylistToAllSongs();
                            resetCurrentPlaylistRelativeObjectsToAllSongs();

                            for (int i = 0; i < allSongs.size(); i++) {
                                Log.i("State", allSongs.get(i).toString());
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showCustomToast(CommonStrings.DEFAULT_PLAYLISTS_CREATED);
                                }
                            });

                            Log.i("State", "Audio loaded for the first time");
                        }
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showCustomToast(CommonStrings.NOTHING_TO_UPDATE);
                            }
                        });

                        Log.i("State", "Nothing to update");
                    }
                }

                if (isFirstTimeBoot) {
                    isFirstTimeBoot = false;
                }

                if (externalCursor != null) {
                    externalCursor.close();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setSongDetails(new File(currentlyPlayingSongName).exists());
                    }
                });

                try {
                    if (!playlists_menu_objects.getAdapter().isEmpty()) {
                        if (whereAreWe == FILL_CHOSEN_USER_PLAYLIST_SONGS) {
                            if (allSongs.isEmpty()) {
                                whereAreWe = FILL_PLAYLISTS_OPTIONS;
                            }
                        }
                        uiHandler.sendEmptyMessage(whereAreWe);
                    }
                    if (!search_menu_found_matches.getAdapter().isEmpty()) {
                        uiHandler.sendEmptyMessage(SEARCH_AGAIN);
                    }
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                }

                savePrefs();

                uiHandler.sendEmptyMessage(HIDE_LOADING_SCREEN);

                if (isAtStart) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            player_screen.animate().alpha(1).setDuration(500).start();
                        }
                    });
                }
            }
        }).start();
    }

    // Method, that creates default adapter for Playlists menu (PLAYLISTS_OPTIONS)
    // to speed up the loading time
    void createDefaultAdapterForPlaylistsMenu() {
        defaultPlaylistsMenuAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return playlistsOptionsNames.length;
            }
            @Override
            public Object getItem(int position) {
                return position;
            }
            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.listview_item_with_image_and_textview,
                            parent, false);
                }

                ImageView item_icon = item.findViewById(R.id.item_icon);
                TextView  item_name = item.findViewById(R.id.item_name);
                item_name.setTextColor(themeColor2);

                if (position == 0) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[0]));
                }
                if (position == 1) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[1]));
                }
                if (position == 2) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[2]));
                }
                if (position == 3) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[3]));
                }
                if (position == 4) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[4]));
                }
                if (position == 5) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[5]));
                }
                item_icon.setColorFilter(themeColor2);

                item_name.setText(playlistsOptionsNames[position]);

                return item;
            }
        };
    }

    // Method, that creates adapter for gem_themes
    void create_gem_themesAdapter() {
        gem_themesAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return themeNames.length;
            }
            @Override
            public Object getItem(int position) {
                return position;
            }
            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.listview_item_with_image_and_textview,
                            parent, false);
                }
                applySelector(item);

                ImageView item_icon = item.findViewById(R.id.item_icon);
                TextView  item_name = item.findViewById(R.id.item_name);
                item_name.setTextColor(themeColor2);

                try (InputStream ims = getAssets().open(
                        "theme-related images/" +
                                themeNames[position].toLowerCase() + "/" +
                                imageNames[20] + ".png")) {
                    item_icon.setImageDrawable(Drawable.createFromStream(ims, null));
                    ims.close();
                }
                catch(IOException ex) {
                    item_icon.setImageDrawable(null);
                }

                item_name.setText(themeNames[position]);

                if (themeNames[position].equals(currentThemeName)) {
                    gem_themes.setItemChecked(position, true);
                }
                return item;
            }
        };
        gem_themes.setAdapter(gem_themesAdapter);

        gem_themes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View item, int position, long id) {
                gem_themes.setItemChecked(position, true);
                gem_themesAdapter.notifyDataSetChanged();
                applyTheme(themeNames[position]);
            }
        });

        gem_themes.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    }

    // Method, that creates SimpleAdapter for additional_info_line_options
    void create_additional_info_line_optionsAdapter() {
        additional_info_line_optionsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return additional_info_line_optionsTexts.length;
            }
            @Override
            public Object getItem(int position) {
                return position;
            }
            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.listview_item_with_image_and_textview,
                            parent, false);
                }
                applySelector(item);

                LinearLayout holder    = item.findViewById(R.id.holder);
                holder.getLayoutParams().height = 64 * (int) displayMetrics.density;

                ImageView    item_icon = item.findViewById(R.id.item_icon);
                TextView     item_name = item.findViewById(R.id.item_name);
                item_name.setTextColor(themeColor2);

                if (position == 0) {
                    item_icon.setImageDrawable(getImageFromAssets(imageNames[0]));
                }
                if (position == 1) {
                    item_icon.setImageDrawable(getImageFromAssets(imageNames[4]));
                }
                if (position == 2) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[4]));
                    item_icon.setColorFilter(themeColor2);
                }
                if (position == 3) {
                    item_icon.setImageDrawable(getImageFromAssets(imageNames[22]));
                }

                item_name.setText(additional_info_line_optionsTexts[position]);

                if (position == current_additional_info_lineOption) {
                    additional_info_line_options.setItemChecked(position, true);
                }
                return item;
            }
        };
        additional_info_line_options.setAdapter(additional_info_line_optionsAdapter);

        additional_info_line_options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View item, int position, long id) {
                additional_info_line_options.setItemChecked(position, true);
                current_additional_info_lineOption = position;
                change_show_additional_info_line_options_buttonImage();
                set_additional_info_lineText(new File(currentlyPlayingSongName).exists());
            }
        });

        additional_info_line_options.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    }
    // Method to change additional_info_line_options_button image in Preferences menu
    void change_show_additional_info_line_options_buttonImage() {
        switch (current_additional_info_lineOption) {
            case 0:
                show_additional_info_line_options_button
                        .setImageDrawable(getImageFromAssets(imageNames[0]));
                break;
            case 1:
                show_additional_info_line_options_button
                        .setImageDrawable(getImageFromAssets(imageNames[4]));
                break;
            case 2:
                show_additional_info_line_options_button
                        .setImageDrawable(getDrawable(playlistsIconsIds[4]));
                if (!currentThemeName.equals(themeNames[1])) {
                    show_additional_info_line_options_button.setColorFilter(themeColor2);
                }
                break;
            case 3:
                show_additional_info_line_options_button
                        .setImageDrawable(getImageFromAssets(imageNames[22]));
                break;
        }
    }
    // Method to change additional_info_line mode
    void set_additional_info_lineText(boolean isFileExists) {
        if (isFileExists) {
            switch (current_additional_info_lineOption) {
                case 0:
                    player_screen_additional_info_line.setText(
                            res.getString(R.string.current_equalizer_preset,
                                    currentEqualizerPresetName));
                    break;
                case 1:
                    player_screen_additional_info_line.setText(
                            res.getString(R.string.current_playlist,
                                    currentPlaylistName
                                            .replace("\n", "")
                                            .replace("\t", "")));
                    break;
                case 2:
                    player_screen_additional_info_line.setText(
                            res.getString(R.string.currently_playing_song_genre,
                                    currentlyPlayingSong.getGenre()));
                    break;
                case 3:
                    loadLyrics();
                    break;
            }
        }
        else {
            player_screen_additional_info_line.setText(CommonStrings.SONG_REMOVED);
        }
    }

    // Method to create adapter for Help menu
    void createHelpMenuAdapter() {
        final String[] helpMenuGroups                = res.getStringArray(
                R.array.help_menu_groups);
        final String[] helpMenuTopics_Buttons        = res.getStringArray(
                R.array.help_menu_topics_buttons);
        final String[] helpMenuTopics_SongInfo       = res.getStringArray(
                R.array.help_menu_topics_song_info);
        final String[] helpMenuTopics_WidgetElements = res.getStringArray(
                R.array.help_menu_topics_widget_elements);
        help_menu_topicsAdapter = new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                return helpMenuGroups.length;
            }
            @Override
            public int getChildrenCount(int groupPosition) {
                return groupPosition == 0 ?
                        helpMenuTopics_Buttons.length :
                        groupPosition == 1 ?
                                helpMenuTopics_SongInfo.length :
                                helpMenuTopics_WidgetElements.length;
            }
            @Override
            public Object getGroup(int groupPosition) {
                return groupPosition == 0 ?
                        helpMenuTopics_Buttons :
                        groupPosition == 1 ?
                                helpMenuTopics_SongInfo :
                                helpMenuTopics_WidgetElements;
            }
            @Override
            public Object getChild(int groupPosition, int childPosition) {
                return groupPosition == 0 ?
                        helpMenuTopics_Buttons[childPosition] :
                        groupPosition == 1 ?
                                helpMenuTopics_SongInfo[childPosition] :
                                helpMenuTopics_WidgetElements[childPosition];
            }
            @Override
            public long getGroupId(int groupPosition) {
                return groupPosition;
            }
            @Override
            public long getChildId(int groupPosition, int childPosition) {
                return childPosition;
            }
            @Override
            public boolean hasStableIds() {
                return true;
            }
            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }

            @Override
            public View getGroupView(int groupPosition, boolean isExpanded,
                                     View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.expandable_list_group_layout,
                            parent, false);
                }

                ImageView indicator_icon = item.findViewById(R.id.indicator_icon);
                TextView group_name      = item.findViewById(R.id.group_name);
                ImageView item_icon      = item.findViewById(R.id.group_icon);
                item_icon.setVisibility(View.GONE);
                applyGroupIndicator(indicator_icon, isExpanded);
                group_name.setTextColor(themeColor2);

                group_name.setText(helpMenuGroups[groupPosition]);

                if (groupPosition == checkedHelpMenuGroupPosition) {
                    item.setBackground(new ColorDrawable(themeColor1));
                }
                else {
                    item.setBackground(null);
                }
                return item;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                     View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(R.layout.help_menu_topic, parent, false);
                }
                applySelector(item);

                TextView help_menu_topic_text = item.findViewById(R.id.help_menu_topic_text);
                help_menu_topic_text.setTextColor(themeColor2);

                switch (groupPosition) {
                    case 0:
                        help_menu_topic_text.setText(helpMenuTopics_Buttons[childPosition]);
                        break;
                    case 1:
                        help_menu_topic_text.setText(helpMenuTopics_SongInfo[childPosition]);
                        break;
                    case 2:
                        help_menu_topic_text.setText(helpMenuTopics_WidgetElements[childPosition]);
                        break;
                }

                return item;
            }

            @Override
            public void onGroupExpanded(int groupPosition) {
                super.onGroupExpanded(groupPosition);
                if (groupPosition == checkedHelpMenuGroupPosition) {
                    help_menu_topics.setItemChecked(checkedHelpMenuTopicPosition, true);
                }
            }
        };

        help_menu_element_description_small_text.setText(
                R.string.help_menu_default_description);
        help_menu_element_description_large_text.setText(
                help_menu_element_description_small_text.getText());
        help_menu_image_view.setVisibility(View.GONE);
        help_menu_element_description_large_layout_image_view.setVisibility(View.GONE);

        help_menu_topics.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                checkedHelpMenuTopicPosition = parent.getFlatListPosition(
                        ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                help_menu_topics.setItemChecked(checkedHelpMenuTopicPosition, true);
                checkedHelpMenuGroupPosition = groupPosition;

                if (groupPosition == 0) {
                    if (childPosition == 0) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_buttons_important_information);
                        currentHelpMenuImageName = "";
                    }
                    if (childPosition == 1) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_checkpoint_system_button);
                        currentHelpMenuImageName = imageNames[25];
                    }
                    if (childPosition == 2) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_equalizer_menu_description);
                        currentHelpMenuImageName = imageNames[0];
                    }
                    if (childPosition == 3) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_main_menu_exit_and_back_button_description);
                        currentHelpMenuImageName = imageNames[14];
                    }
                    if (childPosition == 4) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_help_menu_description);
                        currentHelpMenuImageName = imageNames[2];
                    }
                    if (childPosition == 5) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_next_song_button_description);
                        currentHelpMenuImageName = imageNames[9];
                    }
                    if (childPosition == 6) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_play_pause_button_description);
                        currentHelpMenuImageName = imageNames[7];
                    }
                    if (childPosition == 7) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_playlists_menu_description);
                        currentHelpMenuImageName = imageNames[4];
                    }
                    if (childPosition == 8) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_preferences_description);
                        currentHelpMenuImageName = "preferences";
                    }
                    if (childPosition == 9) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_previous_song_button_description);
                        currentHelpMenuImageName = imageNames[6];
                    }
                    if (childPosition == 10) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_repeat_button_description);
                        currentHelpMenuImageName = imageNames[26];
                    }
                    if (childPosition == 11) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_search_button_description);
                        currentHelpMenuImageName = imageNames[12];
                    }
                    if (childPosition == 12) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_shadow_description);
                        currentHelpMenuImageName = "";
                    }
                    if (childPosition == 13) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_shuffle_button_description);
                        currentHelpMenuImageName = imageNames[27];
                    }
                    if (childPosition == 14) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_song_cover_description);
                        currentHelpMenuImageName = "songCover";
                    }
                }
                if (groupPosition == 1) {
                    if (childPosition == 0) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_additional_info_line_description);
                        currentHelpMenuImageName = "";
                    }
                    if (childPosition == 1) {
                        help_menu_element_description_small_text.setText(
                                R.string.help_menu_song_details_description);
                        currentHelpMenuImageName = "";
                    }
                }
                if (groupPosition == 2) {
                    help_menu_element_description_small_text.setText(
                            R.string.help_menu_status_bar_widget_description);
                    currentHelpMenuImageName = "";
                }
                help_menu_element_description_large_text.setText(
                        help_menu_element_description_small_text.getText());
                switch (currentHelpMenuImageName) {
                    case "":
                        help_menu_image_view.setImageDrawable(null);
                        break;
                    case "preferences":
                        help_menu_image_view.setImageDrawable(
                                player_screen_preferences_button.getDrawable());
                        break;
                    case "songCover":
                        help_menu_image_view.setImageDrawable(cover_button_small.getDrawable());
                        break;
                    default:
                        help_menu_image_view.setImageDrawable(
                                getImageFromAssets(currentHelpMenuImageName));
                        break;
                }
                help_menu_element_description_large_layout_image_view.setImageDrawable(
                        help_menu_image_view.getDrawable());
                if (help_menu_image_view.getDrawable() == null) {
                    help_menu_image_view.setVisibility(View.GONE);
                    help_menu_element_description_large_layout_image_view
                            .setVisibility(View.GONE);
                }
                else {
                    help_menu_image_view.setVisibility(View.VISIBLE);
                    help_menu_element_description_large_layout_image_view
                            .setVisibility(View.VISIBLE);
                }
                return true;
            }
        });

        help_menu_topics.setAdapter(help_menu_topicsAdapter);

        help_menu_topics.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    // Handler, that handles all side-thread UI updates
    final Handler uiHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_LOADING_SCREEN:
                    loading_screen.setVisibility(View.VISIBLE);
                    loading_screen.animate().alpha(1).setDuration(500).start();
                    ObjectAnimator animation = ObjectAnimator.ofFloat(
                            loading_screen_icon, "rotationY", 0.0f, 360f);
                    animation.setDuration(1000);
                    animation.setRepeatCount(ObjectAnimator.INFINITE);
                    animation.setInterpolator(new AccelerateDecelerateInterpolator());
                    animation.start();
                    break;

                case HIDE_LOADING_SCREEN:
                    loading_screen_icon.clearAnimation();
                    loading_screen.animate().alpha(0).setDuration(500)
                            .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            loading_screen.setVisibility(View.GONE);
                        }
                    }).start();
                    break;

                case FILL_PLAYLISTS_OPTIONS:
                    disableTextTickerEffect();

                    switchBackButtonToExitButton();
                    backInsteadOfExit = false;
                    whereAreWe        = FILL_PLAYLISTS_OPTIONS;

                    playlists_menu_objects.setAdapter(defaultPlaylistsMenuAdapter);

                    final int firstVisiblePosition = firstVisiblePositions.get(whereAreWe);
                    if (firstVisiblePosition != -1) {
                        playlists_menu_objects.setSelection(firstVisiblePosition);
                    }

                    playlists_menu_objects.setOnItemClickListener
                            (new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick
                                        (AdapterView<?> parent, View itemClicked,
                                         int position, long id) {
                                    if (allSongs.isEmpty()) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);
                                            }
                                        });
                                    }
                                    else {
                                        firstVisiblePositions.put(
                                                whereAreWe,
                                                playlists_menu_objects.getFirstVisiblePosition());

                                        if (position == 0) {
                                            uiHandler.sendEmptyMessage(FILL_CURRENT_PLAYLIST_SONGS);
                                        }
                                        if (position == 1) {
                                            uiHandler.sendEmptyMessage(FILL_ALL_SONGS);
                                        }
                                        if (position == 2) {
                                            uiHandler.sendEmptyMessage(FILL_ALL_ARTIST_NAMES);
                                        }
                                        if (position == 3) {
                                            uiHandler.sendEmptyMessage(FILL_ALL_ALBUM_TITLES);
                                        }
                                        if (position == 4) {
                                            uiHandler.sendEmptyMessage(FILL_ALL_GENRE_NAMES);
                                        }
                                        if (position == 5) {
                                            uiHandler.sendEmptyMessage(
                                                    FILL_ALL_USER_PLAYLISTS_NAMES);
                                        }
                                        switchExitButtonToBackButton();
                                    }
                                }
                            });

                    playlists_menu_objects.setOnItemLongClickListener(null);

                    playlists_menu_objects.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

                    if (allSongs.isEmpty()) {
                        playlists_menu_additional_info_line
                                .setText(CommonStrings.NO_SONGS_ON_DEVICE);
                    }
                    else {
                        playlists_menu_additional_info_line.setText(
                                res.getString(R.string.current_playlist,
                                        currentPlaylistName
                                                .replace("\n", "")
                                                .replace("\t", "")));
                    }
                    break;

                // "CURRENT PLAYLIST" OPTION ACTIONS
                case FILL_CURRENT_PLAYLIST_SONGS:
                    disableTextTickerEffect();

                    backOption = FILL_PLAYLISTS_OPTIONS;
                    whereAreWe = FILL_CURRENT_PLAYLIST_SONGS;

                    if (checkIfThereAnythingToShow()) {
                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playAudioFromPlaylist(
                                                null,
                                                "",
                                                position,
                                                complexAlbumInfoForRecovery,
                                                false);
                                    }
                                });

                        playlists_menu_objects
                                .setOnItemLongClickListener(requestUserPlaylistsAddition);
                    }
                    break;

                // "ALL SONGS" OPTION ACTIONS
                case FILL_ALL_SONGS:
                    disableTextTickerEffect();

                    backOption = FILL_PLAYLISTS_OPTIONS;
                    whereAreWe = FILL_ALL_SONGS;

                    if (checkIfThereAnythingToShow()) {
                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playAudioFromPlaylist(
                                                new ArrayList<Integer>(),
                                                playlistsOptionsNames[1],
                                                position,
                                                "",
                                                false);
                                    }
                                });
                        playlists_menu_objects
                                .setOnItemLongClickListener(requestUserPlaylistsAddition);

                        playlists_menu_additional_info_line.setText(playlistsOptionsNames[1]);
                    }
                    break;

                // "ALL ARTISTS" and "ALL ALBUMS" OPTIONS ACTIONS
                case FILL_ALL_ARTIST_NAMES:
                    disableTextTickerEffect();

                    fromAllAlbums = false;
                    backOption    = FILL_PLAYLISTS_OPTIONS;
                    whereAreWe    = FILL_ALL_ARTIST_NAMES;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();

                    boolean needToAddUnknown = false;
                    for (int i = 0; i < allSongs.size(); i++) {
                        Song   iterableSong       = allSongs.get(i);
                        String iterableSongArtist = iterableSong.getArtist();

                        if (!iterableSongArtist.equals(CommonStrings.UNKNOWN_ARTIST)) {
                            if (!adapterList.contains(iterableSongArtist)) {
                                adapterList.add(iterableSongArtist);
                            }
                        }
                        else {
                            needToAddUnknown = true;
                        }
                    }
                    if (needToAddUnknown) {
                        adapterList.add(CommonStrings.UNKNOWN_ARTIST);
                    }

                    if (checkIfThereAnythingToShow()) {
                        Collections.sort(adapterList, unprefixedItemsComparator);
                        if (adapterList.contains(CommonStrings.UNKNOWN_ARTIST)) {
                            adapterList.remove(CommonStrings.UNKNOWN_ARTIST);
                            adapterList.add(CommonStrings.UNKNOWN_ARTIST);
                        }

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playlists_menu_objects
                                                .setItemChecked(position, false);
                                        artistToShow = adapterList.get(position);

                                        firstVisiblePositions.put(
                                                whereAreWe,
                                                playlists_menu_objects.getFirstVisiblePosition());

                                        uiHandler.sendEmptyMessage(
                                                FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST);

                                        addOnLongClickListenerToBackButton();
                                    }
                                });

                        playlists_menu_objects
                                .setOnItemLongClickListener(requestUserPlaylistsAddition);

                        playlists_menu_additional_info_line.setText(playlistsOptionsNames[2]);
                    }
                    break;

                case FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST:
                    disableTextTickerEffect();

                    backOption    = FILL_ALL_ARTIST_NAMES;
                    whereAreWe    = FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();
                    adapterList.add(playlistsOptionsNames[1]);
                    adapterList.add(playlistsOptionsNames[3]);

                    apply_playlists_menu_objectsAdapter();

                    playlists_menu_objects.setOnItemClickListener
                            (new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick
                                        (AdapterView<?> parent, View itemClicked,
                                         int position, long id) {
                                    playlists_menu_objects.setItemChecked(position, false);
                                    if (position == 0) {
                                        uiHandler.sendEmptyMessage(FILL_ALL_ARTIST_SONGS);
                                    }
                                    if (position == 1) {
                                        uiHandler.sendEmptyMessage(FILL_ALL_ARTIST_ALBUMS);
                                    }
                                }
                            });

                    playlists_menu_additional_info_line.setText(artistToShow);
                    break;

                case FILL_ALL_ARTIST_SONGS:
                    disableTextTickerEffect();

                    if (fromAllAlbums) {
                        backOption = FILL_SPECIFIC_ALBUM_SONGS;
                    }
                    else {
                        backOption = FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST;
                    }
                    whereAreWe              = FILL_ALL_ARTIST_SONGS;

                    adapterList             = new ArrayList<>();
                    songsMediator           = new ArrayList<>();

                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        if (iterableSong.getArtist().equals(artistToShow)) {
                            songsMediator.add(i);
                        }
                    }

                    if (checkIfThereAnythingToShow()) {
                        playlistNameMediator[0] = res.getString(
                                R.string.all_songs_by_artist, artistToShow);

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playAudioFromPlaylist(
                                                songsMediator,
                                                playlistNameMediator[0],
                                                position,
                                                "",
                                                false);
                                    }
                                });

                        playlists_menu_additional_info_line.setText(playlistNameMediator[0]);
                    }
                    break;

                case FILL_ALL_ARTIST_ALBUMS:
                    disableTextTickerEffect();

                    if (fromAllAlbums) {
                        backOption = FILL_SPECIFIC_ALBUM_SONGS;
                    }
                    else {
                        backOption = FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST;
                    }
                    whereAreWe    = FILL_ALL_ARTIST_ALBUMS;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();

                    for (int i = 0; i < allSongs.size(); i++) {
                        Song   iterableSong      = allSongs.get(i);
                        String iterableSongAlbum = iterableSong.getAlbum();
                        iterableSongAlbum       += iterableSong.checkYear();

                        if (iterableSong.getArtist().equals(artistToShow)) {
                            if (!adapterList.contains(iterableSongAlbum)) {
                                adapterList.add(iterableSongAlbum);
                            }
                        }
                    }

                    if (checkIfThereAnythingToShow()) {
                        Collections.sort(adapterList, albumsByYearComparator);

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playlists_menu_objects
                                                .setItemChecked(position, false);
                                        albumToShow = adapterList.get(position);

                                        firstVisiblePositions.put(
                                                whereAreWe,
                                                playlists_menu_objects.getFirstVisiblePosition());

                                        uiHandler.sendEmptyMessage(FILL_SPECIFIC_ALBUM_SONGS);
                                    }
                                });

                        playlists_menu_additional_info_line.setText(
                                res.getString(R.string.all_albums_by_artist, artistToShow));
                    }
                    break;

                case FILL_SPECIFIC_ALBUM_SONGS:
                    disableTextTickerEffect();


                    if (fromAllAlbums) {
                        backOption = FILL_ALL_ALBUM_TITLES;
                    }
                    else {
                        backOption = FILL_ALL_ARTIST_ALBUMS;
                    }
                    whereAreWe = FILL_SPECIFIC_ALBUM_SONGS;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();
                    order         = 0;

                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        String iterableTrack = iterableSong.getTrack();
                        if (iterableSong.getComplexAlbumInfo().equals(
                                albumToShow + "\n" + artistToShow)) {
                            if (iterableTrack.equals(CommonStrings.UNKNOWN_TRACK)) {
                                ++order;
                                iterableSong.setFictiveTrack(String.valueOf(order));
                            }
                            else {
                                iterableSong.setFictiveTrack(iterableTrack);
                            }
                            songsMediator.add(i);
                        }
                    }

                    if (checkIfThereAnythingToShow()) {
                        Collections.sort(songsMediator, songsInAlbumComparator);

                        playlistNameMediator[0] = albumToShow + CommonStrings.BY + artistToShow;

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        if (fromAllAlbums && position == songsMediator.size()) {
                                            playlists_menu_objects.setItemChecked(
                                                    position, false);
                                            fromAllAlbums = true;

                                            firstVisiblePositions.put(
                                                    whereAreWe,
                                                    playlists_menu_objects
                                                            .getFirstVisiblePosition());

                                            uiHandler.sendEmptyMessage(FILL_ALL_ARTIST_SONGS);
                                        }
                                        else if (fromAllAlbums &&
                                                position == songsMediator.size() + 1) {
                                            playlists_menu_objects.setItemChecked(
                                                    position, false);

                                            firstVisiblePositions.put(
                                                    whereAreWe,
                                                    playlists_menu_objects
                                                            .getFirstVisiblePosition());

                                            if (!needToBackToSpecificAlbumSongs) {
                                                needToBackToSpecificAlbumSongs = true;
                                                albumToShowCopy                = albumToShow;
                                                firstVisiblePositionBackup     =
                                                        playlists_menu_objects.
                                                                getFirstVisiblePosition();
                                            }

                                            uiHandler.sendEmptyMessage(FILL_ALL_ARTIST_ALBUMS);
                                        }
                                        else {
                                            playAudioFromPlaylist(
                                                    songsMediator,
                                                    playlistNameMediator[0],
                                                    position,
                                                    playlistNameMediator[0],
                                                    false);
                                        }
                                    }
                                });

                        playlists_menu_additional_info_line.setText(playlistNameMediator[0]);
                    }
                    break;

                case FILL_ALL_ALBUM_TITLES:
                    disableTextTickerEffect();

                    fromAllAlbums = true;
                    backOption    = FILL_PLAYLISTS_OPTIONS;
                    whereAreWe    = FILL_ALL_ALBUM_TITLES;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();
                    ArrayList<String> unknowns = new ArrayList<>();

                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);

                        String iterableSongComplexAlbumInfo = iterableSong.getComplexAlbumInfo();
                        if (iterableSongComplexAlbumInfo.contains(CommonStrings.UNKNOWN_ALBUM)) {
                            if (!unknowns.contains(iterableSongComplexAlbumInfo)) {
                                unknowns.add(iterableSongComplexAlbumInfo);
                            }
                        }
                        else {
                            if (!adapterList.contains(iterableSongComplexAlbumInfo)) {
                                adapterList.add(iterableSongComplexAlbumInfo);
                            }
                        }
                    }
                    Collections.sort(adapterList, unprefixedItemsComparator);
                    Collections.sort(unknowns, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            String artist1 = o1.split("\n")[1];
                            String artist2 = o2.split("\n")[1];
                            if (artist1.equals(CommonStrings.UNKNOWN_ARTIST)) {
                                return Integer.MAX_VALUE;
                            }
                            else if (artist2.equals(CommonStrings.UNKNOWN_ARTIST)) {
                                return Integer.MIN_VALUE;
                            }
                            else {
                                return artist1.compareTo(artist2);
                            }
                        }
                    });
                    adapterList.addAll(unknowns);

                    if (checkIfThereAnythingToShow()) {
                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playlists_menu_objects
                                                .setItemChecked(position, false);

                                        String[] split = adapterList.get(position)
                                                .split("\n");
                                        albumToShow  = split[0];
                                        artistToShow = split[1];

                                        firstVisiblePositions.put(
                                                whereAreWe,
                                                playlists_menu_objects.getFirstVisiblePosition());

                                        uiHandler.sendEmptyMessage(FILL_SPECIFIC_ALBUM_SONGS);

                                        addOnLongClickListenerToBackButton();
                                    }
                                });

                        playlists_menu_objects
                                .setOnItemLongClickListener(requestUserPlaylistsAddition);

                        playlists_menu_additional_info_line.setText(playlistsOptionsNames[3]);
                    }
                    break;

                // "ALL GENRES" OPTION ACTIONS
                case FILL_ALL_GENRE_NAMES:
                    disableTextTickerEffect();

                    backOption    = FILL_PLAYLISTS_OPTIONS;
                    whereAreWe    = FILL_ALL_GENRE_NAMES;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();

                    for (int i = 0; i < allSongs.size(); i++) {
                        String iterableSongGenre = allSongs.get(i).getGenre();
                        if (!adapterList.contains(iterableSongGenre)) {
                            adapterList.add(iterableSongGenre);
                        }
                    }

                    if (checkIfThereAnythingToShow()) {
                        Collections.sort(adapterList, unprefixedItemsComparator);
                        if (adapterList.contains(CommonStrings.UNKNOWN_GENRE)) {
                            adapterList.remove(CommonStrings.UNKNOWN_GENRE);
                            adapterList.add(CommonStrings.UNKNOWN_GENRE);
                        }

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playlists_menu_objects
                                                .setItemChecked(position, false);
                                        genreToShow = adapterList.get(position);

                                        addOnLongClickListenerToBackButton();

                                        firstVisiblePositions.put(
                                                whereAreWe,
                                                playlists_menu_objects.getFirstVisiblePosition());

                                        uiHandler.sendEmptyMessage(FILL_SPECIFIC_GENRE_SONGS);
                                    }
                                });

                        playlists_menu_objects
                                .setOnItemLongClickListener(requestUserPlaylistsAddition);

                        playlists_menu_additional_info_line.setText(playlistsOptionsNames[4]);
                    }
                    break;

                case FILL_SPECIFIC_GENRE_SONGS:
                    disableTextTickerEffect();

                    backOption    = FILL_ALL_GENRE_NAMES;
                    whereAreWe    = FILL_SPECIFIC_GENRE_SONGS;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();

                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        if (iterableSong.getGenre().equals(genreToShow)) {
                            songsMediator.add(i);
                        }
                    }

                    if (checkIfThereAnythingToShow()) {
                        playlistNameMediator[0] =
                                res.getString(R.string.all_specific_genre_songs, genreToShow);

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick
                                            (AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        playAudioFromPlaylist(
                                                songsMediator,
                                                playlistNameMediator[0],
                                                position,
                                                "",
                                                false);
                                    }
                                });

                        playlists_menu_additional_info_line.setText(playlistNameMediator[0]);
                    }
                    break;

                // "USER PLAYLISTS" OPTION ACTIONS
                case FILL_ALL_USER_PLAYLISTS_NAMES:
                    disableTextTickerEffect();

                    backOption    = FILL_PLAYLISTS_OPTIONS;
                    whereAreWe    = FILL_ALL_USER_PLAYLISTS_NAMES;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();

                    adapterList.add(0, res.getString(R.string.add_new_user_playlist));
                    if (!allUserPlaylistsNames.isEmpty()) {
                        adapterList.add(1, res.getString(R.string.merge_user_playlists));
                    }
                    adapterList.addAll(allUserPlaylistsNames);

                    apply_playlists_menu_objectsAdapter();

                    playlists_menu_objects.setOnItemClickListener
                            (new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick
                                        (AdapterView<?> parent, View itemClicked,
                                         int position, long id) {
                                    playlists_menu_objects
                                            .setItemChecked(position, false);
                                    if (position == 0) {
                                        uiHandler.sendEmptyMessage(
                                                ADD_NEW_USER_PLAYLIST_OR_MERGE_SOME);
                                    }
                                    else if (adapterList.size() > 2 & position == 1) {
                                        needToMerge = true;
                                        uiHandler.sendEmptyMessage(
                                                ADD_NEW_USER_PLAYLIST_OR_MERGE_SOME);
                                    }
                                    else {
                                        userPlaylistToShow = adapterList.get(position);

                                        addOnLongClickListenerToBackButton();

                                        firstVisiblePositions.put(
                                                whereAreWe,
                                                playlists_menu_objects.getFirstVisiblePosition());

                                        uiHandler.sendEmptyMessage(FILL_CHOSEN_USER_PLAYLIST_SONGS);
                                    }
                                }
                            });

                    playlists_menu_objects.setOnItemLongClickListener(deleteUserPlaylist);

                    playlists_menu_additional_info_line.setText(playlistsOptionsNames[5]);
                    break;

                case ADD_NEW_USER_PLAYLIST_OR_MERGE_SOME:
                    String message = "";
                    if (needToMerge) {
                        message = getString(R.string.merge_question_text);
                    }

                    showCustomDialog(
                            message,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String newPlaylistName = custom_dialog_user_input.getText()
                                            .toString();
                                    if (newPlaylistName.equals("")) {
                                        showCustomToast(CommonStrings.NEW_PLAYLIST_NAME_IS_NULL);
                                    }
                                    else {
                                        if (!allUserPlaylistsNames.contains(newPlaylistName)) {
                                            custom_dialog_user_input.setVisibility(View.GONE);

                                            hideCustomDialog();

                                            hideKeyboard();

                                            if (needToMerge) {
                                                addOnLongClickListenerToBackButton();

                                                newUserPlaylistNameMerge = newPlaylistName;
                                                uiHandler.sendEmptyMessage(MERGE_USER_PLAYLISTS);
                                            }
                                            else {
                                                allUserPlaylistsNames.add(newPlaylistName);

                                                showCustomToast(CommonStrings
                                                        .USER_PLAYLIST_SUCCESSFULLY_CREATED);

                                                if (inSongsAdditionProcedure) {
                                                    checkUserPlaylistsAmount();
                                                }
                                                else {
                                                    uiHandler.sendEmptyMessage(
                                                            FILL_ALL_USER_PLAYLISTS_NAMES);
                                                }
                                            }
                                        }
                                        else {
                                            showCustomToast(CommonStrings.PLAYLIST_ALREADY_EXISTS);
                                        }
                                    }
                                }
                            },
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (needToMerge) {
                                        needToMerge = false;
                                    }
                                }
                            },
                            false,
                            true);
                    break;

                case MERGE_USER_PLAYLISTS:
                    disableTextTickerEffect();

                    backOption = FILL_ALL_USER_PLAYLISTS_NAMES;
                    whereAreWe = MERGE_USER_PLAYLISTS;

                    apply_playlists_menu_objectsAdapter();

                    playlists_menu_objects.setOnItemClickListener(
                            new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(
                                        AdapterView<?> parent, View view,
                                        int position, long id) {
                                }
                            });
                    playlists_menu_objects.setOnItemLongClickListener(selectAll);

                    playlists_menu_confirm_button.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final ArrayList<String> playlistsToMerge = new ArrayList<>();
                            checkedElements = playlists_menu_objects.getCheckedItemPositions();
                            for (int i = 0; i < checkedElements.size(); i++) {
                                if (checkedElements.get(checkedElements.keyAt(i))) {
                                    playlistsToMerge.add(allUserPlaylistsNames.get(i));
                                }
                            }

                            if (playlistsToMerge.isEmpty()) {
                                showCustomToast(CommonStrings.NO_ONE_ITEM_WERE_SELECTED);
                            }
                            else {
                                showCustomDialog(
                                        getString(R.string.merge_confirmation_text),
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                hideCustomDialog();

                                                loading_screen_text.setText(
                                                        R.string.please_wait_user_playlists_merge);
                                                uiHandler.sendEmptyMessage(SHOW_LOADING_SCREEN);

                                                allUserPlaylistsNames.add(newUserPlaylistNameMerge);
                                                addMergedPlaylistToSongs(playlistsToMerge);
                                            }
                                        },
                                        null,
                                        false,
                                        false);
                            }
                        }
                    });
                    playlists_menu_confirm_button.setVisibility(View.VISIBLE);
                    playlists_menu_confirm_button.animate().alpha(1).start();

                    playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                    playlists_menu_additional_info_line.setText(
                            res.getString(R.string.merge_selection_text));
                    break;

                case FILL_CHOSEN_USER_PLAYLIST_SONGS:
                    disableTextTickerEffect();

                    backOption    = FILL_ALL_USER_PLAYLISTS_NAMES;

                    adapterList   = new ArrayList<>();
                    songsMediator = new ArrayList<>();

                    if (!songsToAddToPlaylist.isEmpty()) {
                        int currentPlaylistSizeBefore = currentPlaylist.size();

                        int lastIndex = 0;
                        for (int i = 0; i < songsToAddToPlaylist.size(); i++) {
                            Song iterableSong1 = allSongs.get(songsToAddToPlaylist.get(i));
                            for (int j = lastIndex; j < allSongs.size(); j++) {
                                Song iterableSong2 = allSongs.get(j);
                                if (iterableSong1.equals(iterableSong2)) {
                                    iterableSong2.addRelatedUserPlaylist(userPlaylistToShow);

                                    if (currentPlaylistName.equals(userPlaylistToShow + "\t\n")) {
                                        currentPlaylist.add(j);
                                    }

                                    lastIndex = j + 1;
                                    break;
                                }
                            }
                        }

                        if (currentPlaylistSizeBefore != currentPlaylist.size()) {
                            Collections.sort(currentPlaylist);

                            currentlyPlayingSongOrder = allSongs.indexOf(currentlyPlayingSong);
                            String replacement = String.valueOf(currentlyPlayingSongOrder + 1) +
                                    " / " + currentPlaylist.size();
                            current_song_order_player.setText(replacement);
                        }

                        songsToAddToPlaylist = new ArrayList<>();
                    }

                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        ArrayList<String> iterableSongRelatedPlaylists =
                                iterableSong.getRelatedUserPlaylists();

                        if (iterableSongRelatedPlaylists.contains(userPlaylistToShow)) {
                            songsMediator.add(i);
                        }
                    }

                    if (songsMediator.isEmpty()) {
                        whereAreWe = CHOSEN_USER_PLAYLIST_IS_EMPTY;

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener(
                                new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(
                                    AdapterView<?> parent, View view, int position, long id) {
                            }
                        });

                        playlists_menu_objects.setOnItemLongClickListener(selectAll);

                        playlists_menu_confirm_button.setOnClickListener(
                                new Button.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                checkedElements = playlists_menu_objects.getCheckedItemPositions();
                                boolean isAtLeastOneSongSelected = false;
                                for (int i = 0; i < checkedElements.size(); i++) {
                                    if (checkedElements.get(checkedElements.keyAt(i))) {
                                        isAtLeastOneSongSelected = true;
                                        break;
                                    }
                                }

                                if (!isAtLeastOneSongSelected) {
                                    showCustomToast(CommonStrings.NO_ONE_SONG_WERE_SELECTED);
                                }
                                else {
                                    showUserPlaylistSongsAdditionDialog(0);
                                }
                            }
                        });
                        playlists_menu_confirm_button.setVisibility(View.VISIBLE);
                        playlists_menu_confirm_button.animate().alpha(1).start();

                        playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                        playlists_menu_additional_info_line.setText(
                                res.getString(R.string.confirm_songs_text, userPlaylistToShow));
                    }
                    else {
                        whereAreWe              = FILL_CHOSEN_USER_PLAYLIST_SONGS;
                        playlistNameMediator[0] = userPlaylistToShow + "\t";

                        apply_playlists_menu_objectsAdapter();

                        playlists_menu_objects.setOnItemClickListener
                                (new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View itemClicked,
                                             int position, long id) {
                                        if (position == 0) {
                                            playlists_menu_objects
                                                    .setItemChecked(position, false);
                                            ArrayList<Integer> songsMediatorBackup = songsMediator;
                                            songsMediator = new ArrayList<>();

                                            for (int i = 0; i < allSongs.size(); i++) {
                                                Song iterableSong = allSongs.get(i);

                                                if (!iterableSong.getRelatedUserPlaylists()
                                                        .contains(userPlaylistToShow)) {
                                                    songsMediator.add(i);
                                                }
                                            }

                                            if (songsMediator.isEmpty()) {
                                                showCustomToast(CommonStrings.NOTHING_TO_ADD);
                                                songsMediator = songsMediatorBackup;
                                            }
                                            else {
                                                uiHandler.sendEmptyMessage(
                                                        ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST);
                                            }
                                        }
                                        else {
                                            playAudioFromPlaylist(
                                                    songsMediator,
                                                    playlistNameMediator[0],
                                                    position - 1,
                                                    "",
                                                    false);
                                        }
                                    }
                                });

                        playlists_menu_objects.setOnItemLongClickListener(
                                new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                                           final int position, long id) {
                                showCustomDialog(
                                        getString(
                                        R.string.delete_songs_from_user_playlist_long_click_text,
                                        userPlaylistToShow),
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                hideCustomDialog();

                                                checkedSongUserPlaylistSongsDeletion = position - 1;

                                                uiHandler.sendEmptyMessage(
                                                        DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST);
                                            }
                                        },
                                        null,
                                        false,
                                        false);
                                return true;
                            }
                        });

                        playlists_menu_additional_info_line.setText(userPlaylistToShow);
                    }
                    break;

                case ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST:
                    disableTextTickerEffect();

                    backOption = FILL_CHOSEN_USER_PLAYLIST_SONGS;
                    whereAreWe = ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST;

                    apply_playlists_menu_objectsAdapter();

                    playlists_menu_objects.setOnItemClickListener(
                            new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(
                                        AdapterView<?> parent, View view,
                                        int position, long id) {
                                }
                            });

                    playlists_menu_objects.setOnItemLongClickListener(selectAll);

                    playlists_menu_confirm_button.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            checkedElements = playlists_menu_objects.getCheckedItemPositions();
                            boolean isAtLeastOneSongSelected = false;
                            for (int i = 0; i < checkedElements.size(); i++) {
                                if (checkedElements.get(checkedElements.keyAt(i))) {
                                    isAtLeastOneSongSelected = true;
                                    break;
                                }
                            }

                            if (!isAtLeastOneSongSelected) {
                                showCustomToast(CommonStrings.NO_ONE_SONG_WERE_SELECTED);
                            }
                            else {
                                showUserPlaylistSongsAdditionDialog(1);
                            }
                        }
                    });
                    playlists_menu_confirm_button.setVisibility(View.VISIBLE);
                    playlists_menu_confirm_button.animate().alpha(1).start();

                    playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                    playlists_menu_additional_info_line.setText(
                            res.getString(R.string.confirm_songs_text, userPlaylistToShow));
                    break;

                case DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST:
                    disableTextTickerEffect();

                    backOption = FILL_CHOSEN_USER_PLAYLIST_SONGS;
                    whereAreWe = DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST;

                    apply_playlists_menu_objectsAdapter();

                    playlists_menu_objects.setOnItemClickListener(
                            new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(
                                        AdapterView<?> parent, View view,
                                        int position, long id) {
                                }
                            });

                    playlists_menu_objects.setOnItemLongClickListener(selectAll);

                    playlists_menu_confirm_button.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showCustomDialog(
                                    getString(
                                    R.string.delete_songs_from_user_playlist_confirmation_text,
                                    userPlaylistToShow),
                                    deleteUserPlaylistFromMultipleSongs,
                                    null,
                                    false,
                                    false);
                        }
                    });
                    playlists_menu_confirm_button.setVisibility(View.VISIBLE);
                    playlists_menu_confirm_button.animate().alpha(1).start();

                    playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                    playlists_menu_additional_info_line.setText(
                            res.getString(R.string.delete_songs_text, userPlaylistToShow));
                    break;

                // SEARCH MENU ACTIONS
                case FILL_SEARCH_RESULTS:
                    String input = search_menu_search_field.getText().toString().toUpperCase();
                    order        = 0;

                    if (input.length() != 0) {
                        resultGroups     = new ArrayList<>();
                        foundSongs       = new ArrayList<>();
                        TreeMap<String, ArrayList<Integer>> foundArtistsMap = new TreeMap<>();
                        foundArtistNames = null;
                        foundArtistSongs = null;
                        TreeMap<String, ArrayList<Integer>> foundAlbumsMap = new TreeMap<>();
                        foundAlbumTitles = null;
                        foundAlbumSongs  = null;
                        TreeMap<String, ArrayList<Integer>> foundGenresMap = new TreeMap<>();
                        foundGenreNames  = null;
                        foundGenreSongs  = null;

                        for (int i = 0; i < allSongs.size(); i++) {
                            Song   iterableSong   = allSongs.get(i);
                            String iterableTitle  = iterableSong.getTitle();
                            String iterableArtist = iterableSong.getArtist();
                            String iterableAlbum  = iterableSong.getComplexAlbumInfo();
                            String iterableGenre  = iterableSong.getGenre();

                            if (iterableTitle.toUpperCase().contains(input)) {
                                foundSongs.add(i);
                            }

                            if (iterableArtist.toUpperCase().contains(input) &&
                                    !foundArtistsMap.containsKey(iterableArtist)) {
                                ArrayList<Integer> foundArtistSongs = new ArrayList<>();
                                for (int j = 0; j < allSongs.size(); j++) {
                                    if (allSongs.get(j).getArtist().equals(iterableArtist)) {
                                        foundArtistSongs.add(j);
                                    }
                                }

                                foundArtistsMap.put(iterableArtist, foundArtistSongs);
                            }

                            if (iterableAlbum.split("\n")[0].toUpperCase().contains(input) &&
                                    !foundAlbumsMap.containsKey(iterableAlbum)) {
                                ArrayList<Integer> foundAlbumSongs = new ArrayList<>();
                                order           = 0;
                                for (int j = 0; j < allSongs.size(); j++) {
                                    Song   iterableAlbumSong      = allSongs.get(j);
                                    String iterableAlbumSongTrack = iterableAlbumSong.getTrack();
                                    if (iterableAlbumSong.getComplexAlbumInfo()
                                            .equals(iterableAlbum)) {
                                        foundAlbumSongs.add(j);
                                    }
                                    if (iterableAlbumSongTrack
                                            .equals(CommonStrings.UNKNOWN_TRACK)) {
                                        ++order;
                                        iterableAlbumSong.setFictiveTrack(String.valueOf(order));
                                    }
                                    else {
                                        iterableAlbumSong.setFictiveTrack(iterableAlbumSongTrack);
                                    }
                                }
                                Collections.sort(foundAlbumSongs, songsInAlbumComparator);

                                foundAlbumsMap.put(iterableAlbum, foundAlbumSongs);
                            }

                            if (iterableGenre.toUpperCase().contains(input) &&
                                    !foundGenresMap.containsKey(iterableGenre)) {
                                ArrayList<Integer> foundGenresSongs = new ArrayList<>();
                                for (int j = 0; j < allSongs.size(); j++) {
                                    if (allSongs.get(j).getComplexAlbumInfo()
                                            .equals(iterableAlbum)) {
                                        foundGenresSongs.add(j);
                                    }
                                }

                                foundGenresMap.put(iterableGenre, foundGenresSongs);
                            }
                        }

                        if (!foundSongs.isEmpty()) {
                            resultGroups.add(foundSongsGroupName);
                        }
                        if (!foundArtistsMap.isEmpty()) {
                            foundArtistNames = new ArrayList<>(foundArtistsMap.keySet());
                            foundArtistSongs = new ArrayList<>(foundArtistsMap.values());
                            resultGroups.add(foundArtistsGroupName);
                        }
                        if (!foundAlbumsMap.isEmpty()) {
                            foundAlbumTitles = new ArrayList<>(foundAlbumsMap.keySet());
                            foundAlbumSongs  = new ArrayList<>(foundAlbumsMap.values());
                            resultGroups.add(foundAlbumsGroupName);
                        }
                        if (!foundGenresMap.isEmpty()) {
                            foundGenreNames  = new ArrayList<>(foundGenresMap.keySet());
                            foundGenreSongs  = new ArrayList<>(foundGenresMap.values());
                            resultGroups.add(foundGenresGroupName);
                        }

                        if (resultGroups.isEmpty()) {
                            search_menu_found_matchesAdapter = null;
                            search_menu_found_matches.setAdapter(search_menu_found_matchesAdapter);
                            showCustomToast(CommonStrings.NOTHING_IS_FOUND);
                        }
                        else {
                            search_menu_search_field.clearFocus();
                            hideKeyboard();

                            apply_search_menu_found_matchesAdapter();
                        }
                    }
                    else {
                        showCustomToast(CommonStrings.EMPTY_SEARCH_FIELD);
                    }
                    break;

                case SEARCH_AGAIN:
                    disableTextTickerEffect();
                    if (!search_menu_found_matches.getAdapter().isEmpty()) {
                        search_menu_confirm_button.callOnClick();
                    }
                    break;
            }
            return true;
        }
    });

    // Method, that creates variative playlists_menu_objectsAdapter,
    // based on whereAreWe or whereAreWeAddition, and applies it to playlists_menu_objects
    void apply_playlists_menu_objectsAdapter() {
        playlists_menu_objectsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                switch (whereAreWe) {
                    case FILL_CURRENT_PLAYLIST_SONGS:
                        return currentPlaylist.size();

                    case FILL_ALL_SONGS:
                    case CHOSEN_USER_PLAYLIST_IS_EMPTY:
                        return allSongs.size();

                    case FILL_ALL_ARTIST_NAMES:
                    case FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST:
                    case FILL_ALL_ARTIST_ALBUMS:
                    case FILL_ALL_ALBUM_TITLES:
                    case FILL_ALL_GENRE_NAMES:
                    case FILL_ALL_USER_PLAYLISTS_NAMES:
                        return adapterList.size();

                    case FILL_ALL_ARTIST_SONGS:
                    case FILL_SPECIFIC_GENRE_SONGS:
                    case ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST:
                    case DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST:
                        return songsMediator.size();

                    case FILL_SPECIFIC_ALBUM_SONGS:
                        return fromAllAlbums & !inSongsAdditionProcedure ?
                                songsMediator.size() + 2 : songsMediator.size();

                    case MERGE_USER_PLAYLISTS:
                        return allUserPlaylistsNames.size();

                    case FILL_CHOSEN_USER_PLAYLIST_SONGS:
                        return songsMediator.size() + 1;
                }
                return -1;
            }
            @Override
            public long getItemId(int position) {
                return position;
            }
            @Override
            public Object getItem(int position)
            {
                return position;
            }

            @Override
            public View getView(int position, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.playlist_items_with_additional_info,
                            parent, false);
                }
                applySelector(item);

                TextView item_description = item.findViewById(R.id.item_description);
                item_description.setTextColor(themeColor2);
                TextView additional_info  = item.findViewById(R.id.additional_info);
                additional_info.setTextColor(themeColor2);

                Song   iterableSong;
                String iterableElement       = "";
                char   firstLetter;
                File   file;
                String additional_infoString = "";

                switch (whereAreWe) {
                    case FILL_CURRENT_PLAYLIST_SONGS:
                        iterableSong = allSongs.get(currentPlaylist.get(position));
                        if (complexAlbumInfoForRecovery.equals("")) {
                            iterableElement = iterableSong.getFullSongDescription();

                            file = new File(iterableSong.getData());
                            if (file.exists()) {
                                firstLetter = returnFirstLetter(iterableElement);
                            }
                            else {
                                firstLetter = '!';
                            }
                            additional_infoString = (position + 1) + "\n" + firstLetter;
                        }
                        else {
                            iterableElement = iterableSong.getTitle();

                            String ifNotExist;
                            file = new File(iterableSong.getData());
                            if (file.exists()) {
                                ifNotExist = "";
                            }
                            else {
                                ifNotExist = "\n!";
                            }
                            additional_infoString = iterableSong.getFictiveTrack() + ifNotExist;
                        }

                        if (!inSongsAdditionProcedure &
                                currentlyPlayingSongName.equals(iterableSong.getData())) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_SONGS:
                        iterableSong = allSongs.get(position);
                        iterableElement = iterableSong.getFullSongDescription();

                        file = new File(iterableSong.getData());
                        if (file.exists()) {
                            firstLetter = returnFirstLetter(iterableElement);
                        }
                        else {
                            firstLetter = '!';
                        }
                        additional_infoString = (position + 1) + "\n" + firstLetter;

                        if (!inSongsAdditionProcedure &
                                currentlyPlayingSongName.equals(iterableSong.getData()) &
                                currentPlaylistName.equals(playlistsOptionsNames[1] + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_ARTIST_NAMES:
                        iterableElement = adapterList.get(position);

                        if (iterableElement.equals(CommonStrings.UNKNOWN_ARTIST)) {
                            additional_infoString = "-----";
                        }
                        else {
                            additional_infoString = (position + 1) + "\n" +
                                    returnFirstLetter(iterableElement);
                        }

                        if (!inSongsAdditionProcedure &
                                iterableElement.equals(currentlyPlayingSong.getArtist()) &
                                currentPlaylistName.endsWith(iterableElement + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST:
                        additional_info.setVisibility(View.GONE);

                        iterableElement = adapterList.get(position);
                        if (position == 0 & currentPlaylistName.equals(
                                res.getString(R.string.all_songs_by_artist,
                                        artistToShow) + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        if (position == 1 & !currentPlaylistName.equals(
                                res.getString(R.string.all_songs_by_artist,
                                        artistToShow) + "\n") &
                                currentPlaylistName.endsWith(artistToShow + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_ARTIST_SONGS:
                        iterableSong    = allSongs.get(songsMediator.get(position));
                        iterableElement = iterableSong.getAllSongsBy_SongDescription();

                        file = new File(iterableSong.getData());
                        if (file.exists()) {
                            firstLetter = returnFirstLetter(iterableElement);
                        }
                        else {
                            firstLetter = '!';
                        }
                        additional_infoString = (position + 1) + "\n" + firstLetter;

                        if (!inSongsAdditionProcedure &
                                currentlyPlayingSongName.equals(iterableSong.getData()) &
                                currentPlaylistName.equals(playlistNameMediator[0] + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_ARTIST_ALBUMS:
                        additional_info.setVisibility(View.GONE);

                        iterableElement = adapterList.get(position);

                        String albumComparison = currentlyPlayingSong.getAlbum() +
                                currentlyPlayingSong.checkYear();
                        if (!inSongsAdditionProcedure &
                                iterableElement.equals(albumComparison) &
                                currentPlaylistName.equals(iterableElement +
                                        CommonStrings.BY + artistToShow + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_SPECIFIC_ALBUM_SONGS:
                        if (!inSongsAdditionProcedure &&
                                fromAllAlbums && position == songsMediator.size()) {
                            iterableElement = res.getString(
                                    R.string.all_songs_by_artist, artistToShow);
                            additional_infoString = "-----";
                            if (currentPlaylistName.equals(
                                    res.getString(R.string.all_songs_by_artist,
                                            artistToShow) + "\n")) {
                                playlists_menu_objects.setItemChecked(position, true);
                            }
                        }
                        else if (!inSongsAdditionProcedure &&
                                fromAllAlbums && position == songsMediator.size() + 1) {
                            iterableElement = res.getString(
                                    R.string.all_albums_by_artist, artistToShow);
                            additional_infoString = "-----";
                            if (!currentPlaylistName.equals(
                                    res.getString(R.string.all_songs_by_artist,
                                            artistToShow) + "\n") &
                                    !currentPlaylistName.startsWith(albumToShow) &
                                    currentPlaylistName.endsWith(artistToShow + "\n")) {
                                playlists_menu_objects.setItemChecked(position, true);
                            }
                        }
                        else {
                            iterableSong    = allSongs.get(songsMediator.get(position));
                            iterableElement = iterableSong.getTitle();

                            String ifNotExist;
                            file = new File(iterableSong.getData());
                            if (file.exists()) {
                                ifNotExist = "";
                            }
                            else {
                                ifNotExist = "\n!";
                            }
                            additional_infoString = iterableSong.getFictiveTrack() + ifNotExist;

                            if (currentlyPlayingSongName.equals(iterableSong.getData()) &
                                    currentPlaylistName.equals(
                                            playlistNameMediator[0] + "\n")) {
                                playlists_menu_objects.setItemChecked(position, true);
                            }
                        }
                        break;

                    case FILL_ALL_ALBUM_TITLES:
                        iterableElement = adapterList.get(position);

                        if (iterableElement.contains(CommonStrings.UNKNOWN_ALBUM)) {
                            additional_infoString = "-----";
                        }
                        else {
                            additional_infoString = String.valueOf(
                                    returnFirstLetter(iterableElement));
                        }

                        if (!inSongsAdditionProcedure &
                                iterableElement.equals(currentlyPlayingSong.getComplexAlbumInfo()) &
                                currentPlaylistName.equals(iterableElement.replace(
                                        "\n",
                                        CommonStrings.BY) + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_GENRE_NAMES:
                        iterableElement = adapterList.get(position);

                        if (iterableElement.equals(CommonStrings.UNKNOWN_GENRE)) {
                            additional_infoString = "-----";
                        }
                        else {
                            additional_infoString = String.valueOf(
                                    returnFirstLetter(iterableElement));
                        }

                        if (!inSongsAdditionProcedure &
                                iterableElement.equals(currentlyPlayingSong.getGenre()) &
                                currentPlaylistName.equals(
                                        res.getString(R.string.all_specific_genre_songs,
                                                iterableElement) + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_SPECIFIC_GENRE_SONGS:
                        iterableSong    = allSongs.get(songsMediator.get(position));
                        iterableElement = iterableSong.getFullSongDescription();

                        file = new File(iterableSong.getData());
                        if (file.exists()) {
                            firstLetter = returnFirstLetter(iterableElement);
                        }
                        else {
                            firstLetter = '!';
                        }
                        additional_infoString = (position + 1) + "\n" + firstLetter;

                        if (!inSongsAdditionProcedure &
                                currentlyPlayingSongName.equals(iterableSong.getData()) &
                                currentPlaylistName.equals(playlistNameMediator[0] + "\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case FILL_ALL_USER_PLAYLISTS_NAMES:
                        iterableElement = adapterList.get(position);

                        if (position == 0 || iterableElement.equals(
                                res.getString(R.string.merge_user_playlists))) {
                            additional_infoString = "-----";
                        }
                        else {
                            additional_infoString = String.valueOf(
                                    returnFirstLetter(iterableElement));
                        }

                        if (currentPlaylistName.equals(iterableElement + "\t\n")) {
                            playlists_menu_objects.setItemChecked(position, true);
                        }
                        break;

                    case MERGE_USER_PLAYLISTS:
                        iterableElement = allUserPlaylistsNames.get(position);

                        additional_infoString = String.valueOf(returnFirstLetter(iterableElement));
                        break;

                    case CHOSEN_USER_PLAYLIST_IS_EMPTY:
                        iterableSong = allSongs.get(position);
                        iterableElement = iterableSong.getFullSongDescription();

                        file = new File(iterableSong.getData());
                        if (file.exists()) {
                            firstLetter = returnFirstLetter(iterableElement);
                        }
                        else {
                            firstLetter = '!';
                        }
                        additional_infoString = (position + 1) + "\n" + firstLetter;
                        break;

                    case FILL_CHOSEN_USER_PLAYLIST_SONGS:
                        if (position == 0) {
                            iterableElement = res.getString(
                                    R.string.add_new_songs_to_user_playlist);
                            additional_infoString = "-----";
                        }
                        else {
                            iterableSong    = allSongs.get(songsMediator.get(position - 1));
                            iterableElement = iterableSong.getFullSongDescription();

                            file = new File(iterableSong.getData());
                            if (file.exists()) {
                                firstLetter = returnFirstLetter(iterableElement);
                            }
                            else {
                                firstLetter = '!';
                            }
                            additional_infoString = (position) + "\n" + firstLetter;

                            if (currentlyPlayingSongName.equals(iterableSong.getData()) &
                                    currentPlaylistName.equals(playlistNameMediator[0] + "\n")) {
                                playlists_menu_objects.setItemChecked(position, true);
                            }
                        }
                        break;

                    case ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST:
                    case DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST:
                        iterableSong    = allSongs.get(songsMediator.get(position));
                        iterableElement = iterableSong.getFullSongDescription();

                        file = new File(iterableSong.getData());
                        if (file.exists()) {
                            firstLetter = returnFirstLetter(iterableElement);
                        }
                        else {
                            firstLetter = '!';
                        }
                        additional_infoString = (position + 1) + "\n" + firstLetter;
                        break;
                }

                item_description.setText(iterableElement);
                additional_info.setText(additional_infoString);

                return item;
            }
        };
        playlists_menu_objects.setAdapter(playlists_menu_objectsAdapter);

        int firstVisiblePosition;
        if (whereAreWe == FILL_CHOSEN_USER_PLAYLIST_SONGS) {
            firstVisiblePosition = firstVisiblePositionsUser
                    .get(allUserPlaylistsNames.indexOf(userPlaylistToShow), -1);
            if (firstVisiblePosition != -1) {
                playlists_menu_objects.setSelection(firstVisiblePosition);
            }
        }
        else {
            firstVisiblePosition = firstVisiblePositions.get(whereAreWe);
            if (firstVisiblePosition != -1) {
                playlists_menu_objects.setSelection(firstVisiblePosition);
            }
        }

        if (whereAreWe == DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST) {
            playlists_menu_objects.setSelection(checkedSongUserPlaylistSongsDeletion);
            playlists_menu_objects.setItemChecked(
                    checkedSongUserPlaylistSongsDeletion, true);
        }
    }
    // Method, used in MERGE_USER_PLAYLISTS, that add newly merged User playlist to all needed songs
    void addMergedPlaylistToSongs(ArrayList<String> playlistsToMerge) {
        for (int i = 0; i < allSongs.size(); i++) {
            Song              iterableSong                 = allSongs.get(i);
            ArrayList<String> iterableSongRelatedPlaylists = iterableSong.getRelatedUserPlaylists();
            if (!iterableSongRelatedPlaylists.isEmpty()) {
                for (int j = 0; j < playlistsToMerge.size(); j++) {
                    String iterablePlaylist = playlistsToMerge.get(j);
                    if (iterableSongRelatedPlaylists.contains(iterablePlaylist)) {
                        allSongs.get(i).addRelatedUserPlaylist(newUserPlaylistNameMerge);
                        break;
                    }
                }
            }
        }

        confirmButtonHideoutHandler.sendEmptyMessage(0);
        removeOnLongClickListenerFromBackButton();
        uiHandler.sendEmptyMessage(FILL_ALL_USER_PLAYLISTS_NAMES);

        uiHandler.sendEmptyMessage(HIDE_LOADING_SCREEN);
    }
    // Just for code clarity, used only once in project -
    // while in User playlist multiple songs deletion procedure
    View.OnClickListener deleteUserPlaylistFromMultipleSongs =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loading_screen_text.setText(R.string.please_wait_songs_deletion);
                    uiHandler.sendEmptyMessage(SHOW_LOADING_SCREEN);

                    checkedElements = playlists_menu_objects.getCheckedItemPositions();
                    boolean isAtLeastOneSongSelected = false;
                    for (int i = 0; i < checkedElements.size(); i++) {
                        if (checkedElements.get(checkedElements.keyAt(i))) {
                            isAtLeastOneSongSelected = true;
                            break;
                        }
                    }

                    if (!isAtLeastOneSongSelected) {
                        showCustomToast(CommonStrings.NO_ONE_SONG_WERE_SELECTED);
                    }
                    else {
                        checkedElements = playlists_menu_objects.getCheckedItemPositions();

                        int lastIndex = 0;
                        for (int i = 0; i < checkedElements.size(); i++) {
                            order = checkedElements.keyAt(i);

                            if (checkedElements.get(order)) {
                                for (int j = lastIndex; j < allSongs.size(); j++) {
                                    Song iterableSong = allSongs.get(j);
                                    if (iterableSong.equals(
                                            allSongs.get(songsMediator.get(order)))) {
                                        iterableSong.removeRelatedUserPlaylist(userPlaylistToShow);

                                        lastIndex = j + 1;
                                        break;
                                    }
                                }
                            }
                        }

                        if (currentPlaylistName.equals(userPlaylistToShow + "\t\n")) {
                            boolean isDeletedSongPlaying = false;

                            for (int i = checkedElements.size() - 1; i >= 0; i--) {
                                order = checkedElements.keyAt(i);

                                if (checkedElements.get(order)) {
                                    if (currentlyPlayingSongName.equals(
                                            allSongs.get(currentPlaylist.get(order)).getData())) {
                                        isDeletedSongPlaying = true;
                                    }
                                    currentPlaylist.remove(order);
                                }
                            }

                            if (currentPlaylist.isEmpty()) {
                                if (isPlaying) {
                                    gemMediaPlayer.stopMedia();
                                }

                                resetCurrentPlaylistToAllSongs();
                                resetCurrentPlaylistRelativeObjectsToAllSongs();

                                backOption = FILL_ALL_USER_PLAYLISTS_NAMES;

                                setSongDetails(new File(currentlyPlayingSongName).exists());
                            }
                            else {
                                if (isDeletedSongPlaying) {
                                    if (isPlaying) {
                                        gemMediaPlayer.stopMedia();
                                    }

                                    currentlyPlayingSong         = allSongs.get(
                                            currentPlaylist.get(0));
                                    currentlyPlayingSongOrder    = 0;
                                    currentlyPlayingSongName     = currentlyPlayingSong.getData();
                                    currentlyPlayingSongDuration =
                                            currentlyPlayingSong.getDuration();
                                    resumePosition = isCheckPointSystemEnabled ?
                                            CHECKPOINT_PRIORITY : 0;

                                    setSongDetails(new File(currentlyPlayingSongName).exists());
                                }
                                else {
                                    currentlyPlayingSongOrder = allSongs
                                            .indexOf(currentlyPlayingSong);
                                    String replacement = String.valueOf(
                                            currentlyPlayingSongOrder + 1) +
                                            " / " + currentPlaylist.size();
                                    current_song_order_player.setText(replacement);
                                }
                            }
                        }

                        hideCustomDialog();

                        confirmButtonHideoutHandler.sendEmptyMessage(0);
                        playlists_menu_objects.setOnItemClickListener(null);
                        playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                        uiHandler.sendEmptyMessage(backOption);

                        uiHandler.sendEmptyMessage(HIDE_LOADING_SCREEN);
                    }
                }
            };
    // playlists_menu_objects OnItemLongClickListener, used to completely delete User playlist
    private AdapterView.OnItemLongClickListener deleteUserPlaylist =
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               final int position, long id) {
                    if (adapterList.size() > 2 & position != 0 & position != 1) {
                        showCustomDialog(
                                getString(R.string.delete_user_playlist_confirmation_text,
                                        adapterList.get(position)),
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        loading_screen_text.setText(
                                                R.string.please_wait_user_playlist_deletion);
                                        uiHandler.sendEmptyMessage(SHOW_LOADING_SCREEN);

                                        String userPlaylistToDelete = adapterList.get(position);

                                        firstVisiblePositions.put(FILL_ALL_USER_PLAYLISTS_NAMES, -1);
                                        firstVisiblePositionsUser.delete(allUserPlaylistsNames
                                                .indexOf(userPlaylistToDelete));

                                        allUserPlaylistsNames.remove(userPlaylistToDelete);
                                        for (int i = 0; i < allSongs.size(); i++) {
                                            Song iterableSong = allSongs.get(i);
                                            if (iterableSong.getRelatedUserPlaylists()
                                                    .contains(userPlaylistToDelete)) {
                                                allSongs.get(i).removeRelatedUserPlaylist
                                                        (userPlaylistToDelete);
                                            }
                                        }

                                        if (currentPlaylistName.equals(
                                                userPlaylistToDelete + "\t\n")) {
                                            if (isPlaying) {
                                                gemMediaPlayer.stopMedia();
                                            }

                                            resetCurrentPlaylistToAllSongs();
                                            resetCurrentPlaylistRelativeObjectsToAllSongs();

                                            setSongDetails(
                                                    new File(currentlyPlayingSongName).exists());
                                        }

                                        hideCustomDialog();

                                        uiHandler.sendEmptyMessage(FILL_ALL_USER_PLAYLISTS_NAMES);

                                        uiHandler.sendEmptyMessage(HIDE_LOADING_SCREEN);
                                    }
                                },
                                null,
                                false,
                                false);
                    }
                    return true;
                }
            };
    // Method to show User playlist addition confirmation dialog
    void showUserPlaylistSongsAdditionDialog(final int option) {
        showCustomDialog(
                getString(R.string.add_new_user_playlist_confirm_song_selection_text),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkedElements = playlists_menu_objects.getCheckedItemPositions();
                        for (int i = 0; i < checkedElements.size(); i++) {
                            if (checkedElements.get(checkedElements.keyAt(i))) {
                                if (option == 0) {
                                    songsToAddToPlaylist.add(checkedElements.keyAt(i));
                                }
                                else {
                                    songsToAddToPlaylist.add(
                                            songsMediator.get(checkedElements.keyAt(i)));
                                }
                            }
                        }

                        hideCustomDialog();

                        confirmButtonHideoutHandler.sendEmptyMessage(0);
                        playlists_menu_objects.setOnItemClickListener(null);
                        playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                        uiHandler.sendEmptyMessage(FILL_CHOSEN_USER_PLAYLIST_SONGS);
                    }
                },
                null,
                false,
                false);
    }
    // OnItemLongClickListener, used when playlists_menu_objects is in ChoiceMode.Multiple
    // either to select or deselect all elements
    private AdapterView.OnItemLongClickListener selectAll =
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               int position, long id) {
                    checkedElements = playlists_menu_objects.getCheckedItemPositions();
                    int songsAlreadySelected = 0;
                    for (int i = 0; i < checkedElements.size(); i++) {
                        if (checkedElements.get(checkedElements.keyAt(i))) {
                            ++songsAlreadySelected;
                        }
                    }

                    if (songsAlreadySelected == playlists_menu_objectsAdapter.getCount() &&
                            checkedElements.size() != 0) {
                        for (int i = 0; i < playlists_menu_objectsAdapter.getCount(); i++) {
                            playlists_menu_objects.setItemChecked(i, false);
                        }
                    }
                    else {
                        for (int i = 0; i < playlists_menu_objectsAdapter.getCount(); i++) {
                            playlists_menu_objects.setItemChecked(i, true);
                        }
                    }
                    return true;
                }
            };

    // Method to create search_menu_found_matches adapter
    void apply_search_menu_found_matchesAdapter() {
        search_menu_found_matchesAdapter = new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                return resultGroups.size();
            }
            @Override
            public int getChildrenCount(int groupPosition) {
                if (resultGroups.get(groupPosition).equals(foundSongsGroupName)) {
                    return foundSongs.size();
                }
                if (resultGroups.get(groupPosition).equals(foundArtistsGroupName)) {
                    return 1;
                }
                if (resultGroups.get(groupPosition).equals(foundAlbumsGroupName)) {
                    return 1;
                }
                if (resultGroups.get(groupPosition).equals(foundGenresGroupName)) {
                    return 1;
                }
                return -1;
            }
            @Override
            public Object getGroup(int groupPosition) {
                if (resultGroups.get(groupPosition).equals(foundSongsGroupName)) {
                    return foundSongs;
                }
                if (resultGroups.get(groupPosition).equals(foundArtistsGroupName)) {
                    return found_artists_inner_list;
                }
                if (resultGroups.get(groupPosition).equals(foundAlbumsGroupName)) {
                    return found_albums_inner_list;
                }
                if (resultGroups.get(groupPosition).equals(foundGenresGroupName)) {
                    return found_genres_inner_list;
                }
                return null;
            }
            @Override
            public Object getChild(int groupPosition, int childPosition) {
                if (resultGroups.get(groupPosition).equals(foundSongsGroupName)) {
                    return foundSongs.get(childPosition);
                }
                if (resultGroups.get(groupPosition).equals(foundArtistsGroupName)) {
                    return foundArtistNames.get(childPosition);
                }
                if (resultGroups.get(groupPosition).equals(foundAlbumsGroupName)) {
                    return foundAlbumTitles.get(childPosition);
                }
                if (resultGroups.get(groupPosition).equals(foundGenresGroupName)) {
                    return foundGenreNames.get(childPosition);
                }
                return null;
            }
            @Override
            public long getGroupId(int groupPosition) {
                return groupPosition;
            }
            @Override
            public long getChildId(int groupPosition, int childPosition) {
                return childPosition;
            }
            @Override
            public boolean hasStableIds() {
                return true;
            }
            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }

            @Override
            public View getGroupView(
                    int groupPosition, boolean isExpanded, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.expandable_list_group_layout,
                            parent, false);
                }
                ImageView indicator_icon = item.findViewById(R.id.indicator_icon);
                TextView group_name      = item.findViewById(R.id.group_name);
                ImageView item_icon      = item.findViewById(R.id.group_icon);
                applyGroupIndicator(indicator_icon, isExpanded);
                group_name.setTextColor(themeColor2);

                group_name.setText(resultGroups.get(groupPosition));

                if (resultGroups.get(groupPosition).equals(foundSongsGroupName)) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[1]));
                }
                if (resultGroups.get(groupPosition).equals(foundArtistsGroupName)) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[2]));
                }
                if (resultGroups.get(groupPosition).equals(foundAlbumsGroupName)) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[3]));
                }
                if (resultGroups.get(groupPosition).equals(foundGenresGroupName)) {
                    item_icon.setImageDrawable(getDrawable(playlistsIconsIds[4]));
                }
                item_icon.setColorFilter(themeColor2);

                if (groupPosition == checkedSearchMenuGroupPosition) {
                    item.setBackground(new ColorDrawable(themeColor1));
                }
                else {
                    item.setBackground(null);
                }
                return item;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition,
                                     boolean isLastChild, View item, ViewGroup parent) {
                if (resultGroups.get(groupPosition).equals(foundSongsGroupName)) {
                    item = inflater.inflate(
                            R.layout.playlist_items_with_additional_info,
                            parent, false);
                    TextView item_description = item.findViewById(R.id.item_description);
                    TextView additional_info  = item.findViewById(R.id.additional_info);
                    item_description.setTextColor(themeColor2);
                    additional_info.setTextColor(themeColor2);
                    applySelector(item);

                    Song   iterableSong    = allSongs.get(foundSongs.get(childPosition));
                    String iterableElement = iterableSong.getFullSongDescription();
                    File   file            = new File(iterableSong.getData());
                    char   firstLetter;
                    if (file.exists()) {
                        firstLetter = returnFirstLetter(iterableElement);
                    }
                    else {
                        firstLetter = '!';
                    }
                    String additional_infoString = (foundSongs.get(childPosition) + 1) +
                            "\n" + firstLetter;

                    item_description.setText(iterableElement);
                    additional_info.setText(additional_infoString);
                }
                else {
                    item = inflater.inflate(
                            R.layout.search_menu_inner_list,
                            parent, false);
                    int minimumHeight = 134 * (int) displayMetrics.density;

                    if (resultGroups.get(groupPosition).equals(foundArtistsGroupName)) {
                        found_artists_inner_list = item.findViewById(R.id.inner_list);

                        found_artists_inner_list.setDivider(new ColorDrawable(themeColor1));
                        found_artists_inner_list.setChildDivider(new ColorDrawable(themeColor2));
                        found_artists_inner_list.setDividerHeight((int) displayMetrics.density * 2);

                        found_artists_inner_list.setAdapter(
                                create_inner_listAdapter(foundArtistsGroupName));

                        found_artists_inner_list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

                        found_artists_inner_list.setOnChildClickListener(
                                new SearchMenuOnChildClickListener(foundArtistsGroupName));

                        int height = (int) (foundArtistNames.size() * displayMetrics.density * 32 +
                                (foundArtistNames.size() - 1) * 2 * displayMetrics.density);
                        if (height > minimumHeight) {
                            found_artists_inner_list.setLayoutParams(new AbsListView.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, height));
                        }
                        else {
                            found_artists_inner_list.setLayoutParams(new AbsListView.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, minimumHeight));
                        }

                        changeScrollbarColor(found_artists_inner_list);
                    }
                    if (resultGroups.get(groupPosition).equals(foundAlbumsGroupName)) {
                        found_albums_inner_list = item.findViewById(R.id.inner_list);

                        found_albums_inner_list.setDivider(new ColorDrawable(themeColor1));
                        found_albums_inner_list.setChildDivider(new ColorDrawable(themeColor2));
                        found_albums_inner_list.setDividerHeight((int) displayMetrics.density * 2);

                        found_albums_inner_list.setAdapter(
                                create_inner_listAdapter(foundAlbumsGroupName));

                        found_albums_inner_list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

                        found_albums_inner_list.setOnChildClickListener(
                                new SearchMenuOnChildClickListener(foundAlbumsGroupName));

                        int height = (int) (foundAlbumTitles.size() * displayMetrics.density * 32 +
                                (foundAlbumTitles.size() - 1) * 2 * displayMetrics.density);
                        if (height > minimumHeight) {
                            found_albums_inner_list.setLayoutParams(new AbsListView.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, height));
                        }
                        else {
                            found_albums_inner_list.setLayoutParams(new AbsListView.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, minimumHeight));
                        }

                        changeScrollbarColor(found_albums_inner_list);
                    }
                    if (resultGroups.get(groupPosition).equals(foundGenresGroupName)) {
                        found_genres_inner_list = item.findViewById(R.id.inner_list);

                        found_genres_inner_list.setDivider(new ColorDrawable(themeColor1));
                        found_genres_inner_list.setChildDivider(new ColorDrawable(themeColor2));
                        found_genres_inner_list.setDividerHeight((int) displayMetrics.density * 2);

                        found_genres_inner_list.setAdapter(
                                create_inner_listAdapter(foundGenresGroupName));

                        found_genres_inner_list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

                        found_genres_inner_list.setOnChildClickListener(
                                new SearchMenuOnChildClickListener(foundGenresGroupName));

                        int height = (int) (foundGenreNames.size() * displayMetrics.density * 32 +
                                (foundGenreNames.size() - 1) * 2 * displayMetrics.density);
                        if (height > minimumHeight) {
                            found_genres_inner_list.setLayoutParams(new AbsListView.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, height));
                        }
                        else {
                            found_genres_inner_list.setLayoutParams(new AbsListView.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, minimumHeight));
                        }

                        changeScrollbarColor(found_genres_inner_list);
                    }
                }
                return item;
            }

            @Override
            public void onGroupExpanded(int groupPosition) {
                super.onGroupExpanded(groupPosition);
                if (checkedSearchMenuGroupPosition != -1 &&
                        resultGroups.get(checkedSearchMenuGroupPosition)
                                .equals(foundSongsGroupName)) {
                    search_menu_found_matches.setItemChecked(
                            checkedSearchMenuItemPosition, true);
                }
            }
        };
        search_menu_found_matches.setAdapter(search_menu_found_matchesAdapter);

        search_menu_found_matches.setOnChildClickListener(
                new SearchMenuOnChildClickListener(foundSongsGroupName));
    }
    // Methods to create search_menu_found_matches inner_list's
    BaseExpandableListAdapter create_inner_listAdapter(final String foundMatchesGroupName) {
        return new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                if (foundMatchesGroupName.equals(foundArtistsGroupName)) {
                    return foundArtistNames.size();
                }
                if (foundMatchesGroupName.equals(foundAlbumsGroupName)) {
                    return foundAlbumTitles.size();
                }
                if (foundMatchesGroupName.equals(foundGenresGroupName)) {
                    return foundGenreNames.size();
                }
                return -1;
            }
            @Override
            public int getChildrenCount(int groupPosition) {
                if (foundMatchesGroupName.equals(foundArtistsGroupName)) {
                    return foundArtistSongs.get(groupPosition).size();
                }
                if (foundMatchesGroupName.equals(foundAlbumsGroupName)) {
                    return foundAlbumSongs.get(groupPosition).size();
                }
                if (foundMatchesGroupName.equals(foundGenresGroupName)) {
                    return foundGenreSongs.get(groupPosition).size();
                }
                return -1;
            }
            @Override
            public Object getGroup(int groupPosition) {
                if (foundMatchesGroupName.equals(foundArtistsGroupName)) {
                    return foundArtistSongs;
                }
                if (foundMatchesGroupName.equals(foundAlbumsGroupName)) {
                    return foundAlbumSongs;
                }
                if (foundMatchesGroupName.equals(foundGenresGroupName)) {
                    return foundGenreSongs;
                }
                return null;
            }
            @Override
            public Object getChild(int groupPosition, int childPosition) {
                if (foundMatchesGroupName.equals(foundArtistsGroupName)) {
                    return foundArtistSongs.get(groupPosition).get(childPosition);
                }
                if (foundMatchesGroupName.equals(foundAlbumsGroupName)) {
                    return foundAlbumSongs.get(groupPosition).get(childPosition);
                }
                if (foundMatchesGroupName.equals(foundGenresGroupName)) {
                    return foundGenreSongs.get(groupPosition).get(childPosition);
                }
                return null;
            }
            @Override
            public long getGroupId(int groupPosition) {
                return groupPosition;
            }
            @Override
            public long getChildId(int groupPosition, int childPosition) {
                return childPosition;
            }
            @Override
            public boolean hasStableIds() {
                return true;
            }
            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }

            @Override
            public View getGroupView(int groupPosition, boolean isExpanded,
                                     View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.inner_list_group_layout,
                            parent, false);
                }
                item.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ImageView indicator_icon = item.findViewById(R.id.indicator_icon);
                TextView  group_name     = item.findViewById(R.id.group_name);
                group_name.setTextColor(themeColor2);
                applyGroupIndicator(indicator_icon, isExpanded);

                if (foundMatchesGroupName.equals(foundArtistsGroupName)) {
                    group_name.setText(foundArtistNames.get(groupPosition));
                }
                if (foundMatchesGroupName.equals(foundAlbumsGroupName)) {
                    group_name.setText(foundAlbumTitles.get(groupPosition));
                }
                if (foundMatchesGroupName.equals(foundGenresGroupName)) {
                    group_name.setText(foundGenreNames.get(groupPosition));
                }

                if (checkedSearchMenuGroupPositionInner == groupPosition &&
                        foundMatchesGroupName.equals(checkedSearchMenuGroupName)) {
                    item.setBackground(new ColorDrawable(themeColor1));
                }
                else {
                    item.setBackground(null);
                }
                return item;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition,
                                     boolean isLastChild, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.playlist_items_with_additional_info,
                            parent, false);
                }
                TextView item_description = item.findViewById(R.id.item_description);
                TextView additional_info  = item.findViewById(R.id.additional_info);
                item_description.setTextColor(themeColor2);
                additional_info.setTextColor(themeColor2);
                applySelector(item);

                int    index;
                Song   iterableSong;
                String iterableElement       = "";
                File   file;
                char   firstLetter;
                String additional_infoString = "";

                if (foundMatchesGroupName.equals(foundArtistsGroupName)) {
                    index           = foundArtistSongs.get(groupPosition).get(childPosition);
                    iterableSong    = allSongs.get(index);
                    iterableElement = iterableSong.getAllSongsBy_SongDescription();
                    file            = new File(iterableSong.getData());
                    if (file.exists()) {
                        firstLetter = returnFirstLetter(iterableElement);
                    }
                    else {
                        firstLetter = '!';
                    }
                    additional_infoString = (index + 1) + "\n" + firstLetter;
                }
                if (foundMatchesGroupName.equals(foundAlbumsGroupName)) {
                    index = foundAlbumSongs.get(groupPosition).get(childPosition);
                    iterableSong    = allSongs.get(index);
                    iterableElement = iterableSong.getTitle();
                    String ifNotExist;
                    file = new File(iterableSong.getData());
                    if (file.exists()) {
                        ifNotExist = "";
                    }
                    else {
                        ifNotExist = "\n!";
                    }
                    additional_infoString = iterableSong.getFictiveTrack() + ifNotExist;
                }
                if (foundMatchesGroupName.equals(foundGenresGroupName)) {
                    index = foundGenreSongs.get(groupPosition).get(childPosition);
                    iterableSong    = allSongs.get(index);
                    iterableElement = iterableSong.getFullSongDescription();
                    file = new File(iterableSong.getData());
                    if (file.exists()) {
                        firstLetter = returnFirstLetter(iterableElement);
                    }
                    else {
                        firstLetter = '!';
                    }
                    additional_infoString = (index + 1) + "\n" + firstLetter;
                }

                item_description.setText(iterableElement);
                additional_info.setText(additional_infoString);
                return item;
            }

            @Override
            public void onGroupExpanded(int groupPosition) {
                super.onGroupExpanded(groupPosition);
                if (checkedSearchMenuGroupPositionInner == groupPosition) {
                    if (checkedSearchMenuGroupName.equals(foundArtistsGroupName)) {
                        found_artists_inner_list.setItemChecked(
                                checkedSearchMenuItemPositionInner, true);
                    }
                    if (checkedSearchMenuGroupName.equals(foundAlbumsGroupName)) {
                        found_albums_inner_list.setItemChecked(
                                checkedSearchMenuItemPositionInner, true);
                    }
                    if (checkedSearchMenuGroupName.equals(foundGenresGroupName)) {
                        found_genres_inner_list.setItemChecked(
                                checkedSearchMenuItemPositionInner, true);
                    }
                }
            }
        };
    }
    // An instance of ExpandableListView.OnChildClickListener, used to play songs from Search menu
    class SearchMenuOnChildClickListener implements ExpandableListView.OnChildClickListener {
        private String option;

        SearchMenuOnChildClickListener(String option) {
            this.option = option;
        }

        @Override
        public boolean onChildClick(final ExpandableListView parent, View item,
                                    final int groupPosition, int childPosition, long id) {
            final int    checkedSearchMenuGroupPositionBackup      =
                    checkedSearchMenuGroupPosition;
            final int    checkedSearchMenuItemPositionBackup       =
                    checkedSearchMenuItemPosition;
            final String checkedSearchMenuGroupNameBackup          =
                    checkedSearchMenuGroupName;
            final int    checkedSearchMenuGroupPositionInnerBackup =
                    checkedSearchMenuGroupPositionInner;
            final int    checkedSearchMenuItemPositionInnerBackup  =
                    checkedSearchMenuItemPositionInner;

            if (option.equals(foundSongsGroupName)) {
                checkedSearchMenuGroupPosition = groupPosition;
                checkedSearchMenuItemPosition  = parent.getFlatListPosition(
                        ExpandableListView.getPackedPositionForChild(
                                groupPosition, childPosition));
            }
            else {
                checkedSearchMenuGroupName          = option;
                checkedSearchMenuGroupPositionInner = groupPosition;
                checkedSearchMenuItemPositionInner  = parent.getFlatListPosition(
                        ExpandableListView.getPackedPositionForChild(
                                groupPosition, childPosition));
                checkedSearchMenuGroupPosition      = resultGroups.indexOf(option);
            }

            final ArrayList<Integer> songsMediatorSearch = new ArrayList<>();

            String                   itemName            = "";
            final int[]              index               = new int[1];
            final String[]           playlistName        = new String[1];
            final String[]           complexAlbumInfo    = new String[1];

            complexAlbumInfo[0] = "";

            if (option.equals(foundSongsGroupName)) {
                index[0] = foundSongs.get(childPosition);
                itemName = allSongs.get(index[0]).getFullSongDescription();
                playlistName[0] = playlistsOptionsNames[1];
            }
            if (option.equals(foundArtistsGroupName)) {
                songsMediatorSearch.addAll(foundArtistSongs.get(groupPosition));

                index[0]        = childPosition;
                itemName        = allSongs.get(
                        songsMediatorSearch.get(childPosition)).getAllSongsBy_SongDescription()
                        .replace("\n", CommonStrings.FROM);
                playlistName[0] = res.getString(
                        R.string.all_songs_by_artist,
                        foundArtistNames.get(groupPosition));
            }
            if (option.equals(foundAlbumsGroupName)) {
                songsMediatorSearch.addAll(foundAlbumSongs.get(groupPosition));

                index[0]            = childPosition;
                itemName            = allSongs.get(
                        songsMediatorSearch.get(childPosition)).getTitle();
                playlistName[0]     = foundAlbumTitles.get(groupPosition)
                        .replace("\n", CommonStrings.BY);
                complexAlbumInfo[0] = playlistName[0];

            }
            if (option.equals(foundGenresGroupName)) {
                songsMediatorSearch.addAll(foundGenreSongs.get(groupPosition));

                index[0]        = childPosition;
                itemName        = allSongs.get(
                        songsMediatorSearch.get(childPosition)).getFullSongDescription();
                playlistName[0] = res.getString(
                        R.string.all_specific_genre_songs,
                        foundGenreNames.get(groupPosition));
            }

            showCustomDialog(
                    getString(R.string.play_song_from_search_results,
                            playlistName[0], itemName),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (option.equals(foundSongsGroupName)) {
                                parent.setItemChecked(
                                        checkedSearchMenuItemPosition, true);
                            }
                            else {
                                parent.setItemChecked(
                                        checkedSearchMenuItemPositionInner, true);
                            }
                            playAudioFromPlaylist(
                                    songsMediatorSearch,
                                    playlistName[0],
                                    index[0],
                                    complexAlbumInfo[0],
                                    true);

                            hideCustomDialog();
                        }
                    },
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            checkedSearchMenuGroupPosition      =
                                    checkedSearchMenuGroupPositionBackup;
                            checkedSearchMenuItemPosition       =
                                    checkedSearchMenuItemPositionBackup;
                            checkedSearchMenuGroupName          =
                                    checkedSearchMenuGroupNameBackup;
                            checkedSearchMenuGroupPositionInner =
                                    checkedSearchMenuGroupPositionInnerBackup;
                            checkedSearchMenuItemPositionInner  =
                                    checkedSearchMenuItemPositionInnerBackup;
                        }
                    },
                    false,
                    false);

            return true;
        }
    }

    // Long click listener to add multiple playlist songs to chosen user playlist
    private AdapterView.OnItemLongClickListener requestUserPlaylistsAddition =
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               final int position, long id) {
                    String message;
                    switch (whereAreWe) {
                        case FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST:
                            message = res.getString(R.string.add_wish_2, artistToShow);
                            isShortWayAvailable = true;
                            break;
                        case FILL_SPECIFIC_ALBUM_SONGS:
                            if (fromAllAlbums &
                                    (position == songsMediator.size() ||
                                            position == songsMediator.size() + 1)) {
                                message = res.getString(R.string.add_wish_2, artistToShow);
                                isShortWayAvailable = true;
                            }
                            else {
                                message = res.getString(R.string.add_wish_1);
                            }
                            break;
                        default:
                            message = res.getString(R.string.add_wish_1);
                            break;
                    }

                    showCustomDialog(
                            message,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    inSongsAdditionProcedure = true;

                                    hideCustomDialog();

                                    if (isShortWayAvailable) {
                                        checkUserPlaylistsAmount();
                                    }
                                    else {
                                        checkedSongUserPlaylistSongsAddition = position;
                                        transform_playlists_menu_objects_inSongsAdditionProcedure();
                                    }
                                }
                            },
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    isShortWayAvailable = false;
                                }
                            },
                            false,
                            false);
                    return true;
                }
            };
    // Method, that transforms playlists_menu_objects inSongsAdditionProcedure
    void transform_playlists_menu_objects_inSongsAdditionProcedure() {
        disableTextTickerEffect();

        whereAreWeAddition = STANDART_WAY_STEP_1;

        if (whereAreWe == FILL_SPECIFIC_ALBUM_SONGS) {
            playlists_menu_objectsAdapter.notifyDataSetChanged();
        }
        int checkedItemPosition = playlists_menu_objects.getCheckedItemPosition();
        if (checkedItemPosition != ListView.INVALID_POSITION) {
            playlists_menu_objects.setItemChecked(checkedItemPosition, false);
        }

        playlists_menu_objects.setItemChecked(checkedSongUserPlaylistSongsAddition, true);

        playlists_menu_objects.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view,
                            int position, long id) {
                    }
                });
        playlists_menu_objects.setOnItemLongClickListener(selectAll);

        playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        playlists_menu_confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkedElements = playlists_menu_objects.getCheckedItemPositions();
                boolean isAtLeastOneItemSelected = false;
                for (int i = 0; i < checkedElements.size(); i++) {
                    if (checkedElements.get(checkedElements.keyAt(i))) {
                        isAtLeastOneItemSelected = true;
                        break;
                    }
                }

                if (!isAtLeastOneItemSelected) {
                    showCustomToast(CommonStrings.NO_ONE_ITEM_WERE_SELECTED);
                }
                else {
                    showCustomDialog(
                            getString(R.string.items_addition_confirmation),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    hideCustomDialog();

                                    checkUserPlaylistsAmount();
                                }
                            },
                            null,
                            false,
                            false);
                }
            }
        });
        playlists_menu_confirm_button.setVisibility(View.VISIBLE);
        playlists_menu_confirm_button.animate().alpha(1).start();

        if (!isBackButtonHasLongClick) {
            addOnLongClickListenerToBackButton();
        }

        playlists_menu_additional_info_line.setText(R.string.select_what_to_add_text);
    }
    // Method to fill whatToAdd inSongsAdditionProcedure according to whereAreWe
    void fillWhatToAdd() {
        whatToAdd = new ArrayList<>();
        switch (whereAreWe) {
            case FILL_CURRENT_PLAYLIST_SONGS:
                for (int i = 0; i < checkedElements.size(); i++) {
                    order = checkedElements.keyAt(i);
                    if (checkedElements.get(order)) {
                        whatToAdd.add(currentPlaylist.get(order));
                    }
                }
                break;
            case FILL_ALL_SONGS:
                for (int i = 0; i < checkedElements.size(); i++) {
                    order = checkedElements.keyAt(i);
                    if (checkedElements.get(checkedElements.keyAt(i))) {
                        whatToAdd.add(order);
                    }
                }
                break;
            case FILL_ALL_ARTIST_NAMES:
            case FILL_ALL_ARTIST_ALBUMS:
            case FILL_ALL_ALBUM_TITLES:
            case FILL_ALL_GENRE_NAMES:
                for (int i = 0; i < checkedElements.size(); i++) {
                    order = checkedElements.keyAt(i);
                    if (checkedElements.get(checkedElements.keyAt(i))) {
                        whatToAdd.add(adapterList.get(order));
                    }
                }
                break;
            case FILL_ALL_ARTIST_SONGS:
            case FILL_SPECIFIC_ALBUM_SONGS:
            case FILL_SPECIFIC_GENRE_SONGS:
                for (int i = 0; i < checkedElements.size(); i++) {
                    order = checkedElements.keyAt(i);
                    if (checkedElements.get(checkedElements.keyAt(i))) {
                        whatToAdd.add(songsMediator.get(order));
                    }
                }
                break;
        }
    }
    // Method to check, is there at least one User playlist exists
    void checkUserPlaylistsAmount() {
        if (allUserPlaylistsNames.isEmpty()) {
            showCustomToast(CommonStrings.NO_USER_PLAYLISTS_EXISTS);
            uiHandler.sendEmptyMessage(ADD_NEW_USER_PLAYLIST_OR_MERGE_SOME);
        }
        else {
            fillWhatToAdd();

            allUserPlaylistsNamesCopy = new ArrayList<>();
            allUserPlaylistsNamesCopy.addAll(allUserPlaylistsNames);

            String ifUserIsCurrent = currentPlaylistName
                    .replace("\n", "")
                    .replace("\t", "");
            if (whereAreWe == FILL_CURRENT_PLAYLIST_SONGS &&
                    allUserPlaylistsNamesCopy.contains(ifUserIsCurrent)) {
                allUserPlaylistsNamesCopy.remove(ifUserIsCurrent);
            }
            if (allUserPlaylistsNamesCopy.isEmpty()) {
                showCustomToast(CommonStrings.CURRENT_IS_THE_ONLY_USER);
                uiHandler.sendEmptyMessage(ADD_NEW_USER_PLAYLIST_OR_MERGE_SOME);
            }
            else {
                showAllUserPlaylists();
            }
        }
    }
    // Method, that shows all User playlists, which can be added to previously selected songs
    void showAllUserPlaylists() {
        playlists_menu_objectsCopy  = playlists_menu_objectsAdapter;
        playlists_menu_objectsState = playlists_menu_objects.onSaveInstanceState();

        disableTextTickerEffect();

        if (isShortWayAvailable) {
            whereAreWeAddition = SHORT_WAY;

            playlists_menu_objects.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(
                                AdapterView<?> parent, View view,
                                int position, long id) {
                        }
                    });
            playlists_menu_objects.setOnItemLongClickListener(selectAll);

            playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }
        else {
            whereAreWeAddition = STANDART_WAY_STEP_2;
        }

        playlists_menu_objectsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return allUserPlaylistsNamesCopy.size();
            }
            @Override
            public Object getItem(int position) {
                return position;
            }
            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View item, ViewGroup parent) {
                if (item == null) {
                    item = inflater.inflate(
                            R.layout.playlist_items_with_additional_info,
                            parent, false);
                }
                applySelector(item);

                TextView item_description    = item.findViewById(R.id.item_description);
                item_description.setTextColor(themeColor2);
                TextView additional_info     = item.findViewById(R.id.additional_info);
                additional_info.setTextColor(themeColor2);

                String iterableElement       = allUserPlaylistsNamesCopy.get(position);
                String additional_infoString = String.valueOf(returnFirstLetter(iterableElement));

                item_description.setText(iterableElement);
                additional_info.setText(additional_infoString);
                return item;
            }
        };
        playlists_menu_objects.setAdapter(playlists_menu_objectsAdapter);

        playlists_menu_confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkedElements = playlists_menu_objects.getCheckedItemPositions();
                whereToAdd = new ArrayList<>();
                for (int i = 0; i < checkedElements.size(); i++) {
                    order = checkedElements.keyAt(i);
                    if (checkedElements.get(order)) {
                        whereToAdd.add(allUserPlaylistsNamesCopy.get(order));
                    }
                }

                if (whereToAdd.isEmpty()) {
                    showCustomToast(CommonStrings.NO_ONE_ITEM_WERE_SELECTED);
                }
                else {
                    showCustomDialog(
                            getString(R.string.user_playlists_to_add_to_confirmation),
                            songsAddition,
                            null,
                            false,
                            false);
                }
            }
        });
        if (isShortWayAvailable) {
            playlists_menu_confirm_button.setVisibility(View.VISIBLE);
            playlists_menu_confirm_button.animate().alpha(1).start();
        }

        playlists_menu_additional_info_line.setText(
                R.string.select_where_to_add_text);
    }
    // Instance of DialogInterface.OnClickListener, that handles songs addition process
    // (just for code clarity)
    View.OnClickListener songsAddition = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            loading_screen_text.setText(R.string.please_wait_songs_addition);
            uiHandler.sendEmptyMessage(SHOW_LOADING_SCREEN);

            int     currentPlaylistSizeBefore           = currentPlaylist.size();
            boolean userIsCurrent                       = whereToAdd.contains(
                    currentPlaylistName
                            .replace("\n", "")
                            .replace("\t", ""));
            int     songsAddedBefore                    = 0;
            int     currentlyPlayingSongOrderInAllSongs = allSongs.indexOf(currentlyPlayingSong);
            switch (whereAreWe) {
                case FILL_CURRENT_PLAYLIST_SONGS:
                    for (int i = 0; i < whatToAdd.size(); i++) {
                        Song iterableSong = allSongs.get(
                                currentPlaylist.get((Integer) whatToAdd.get(i)));
                        for (int j = 0; j < whereToAdd.size(); j++) {
                            String iterableUserPlaylist = whereToAdd.get(j);
                            if (!iterableSong.getRelatedUserPlaylists()
                                    .contains(iterableUserPlaylist)) {
                                iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                            }
                        }
                    }
                    break;
                case FILL_ALL_SONGS:
                    for (int i = 0; i < whatToAdd.size(); i++) {
                        Song iterableSong = allSongs.get((Integer) whatToAdd.get(i));
                        for (int j = 0; j < whereToAdd.size(); j++) {
                            String iterableUserPlaylist = whereToAdd.get(j);
                            if (!iterableSong.getRelatedUserPlaylists()
                                    .contains(iterableUserPlaylist)) {
                                iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                            }
                            if (userIsCurrent) {
                                order = (Integer) whatToAdd.get(i);
                                if (!currentPlaylist.contains(order)) {
                                    currentPlaylist.add(order);
                                }
                                if (order < currentlyPlayingSongOrderInAllSongs) {
                                    ++songsAddedBefore;
                                }
                            }
                        }
                    }
                    break;
                case FILL_ALL_ARTIST_NAMES:
                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        for (int j = 0; j < whatToAdd.size(); j++) {
                            if (iterableSong.getArtist().equals(whatToAdd.get(j))) {
                                for (int k = 0; k < whereToAdd.size(); k++) {
                                    String iterableUserPlaylist = whereToAdd.get(k);
                                    if (!iterableSong.getRelatedUserPlaylists()
                                            .contains(iterableUserPlaylist)) {
                                        iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                    }
                                }
                                if (userIsCurrent) {
                                    if (!currentPlaylist.contains(i)) {
                                        currentPlaylist.add(i);
                                    }
                                    if (i < currentlyPlayingSongOrderInAllSongs) {
                                        ++songsAddedBefore;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case FILL_ALL_SONGS_OR_ALL_ALBUMS_BY_ARTIST:
                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        if (iterableSong.getArtist().equals(artistToShow)) {
                            for (int j = 0; j < whereToAdd.size(); j++) {
                                String iterableUserPlaylist = whereToAdd.get(j);
                                if (!iterableSong.getRelatedUserPlaylists()
                                        .contains(iterableUserPlaylist)) {
                                    iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                }
                            }
                            if (userIsCurrent) {
                                if (!currentPlaylist.contains(i)) {
                                    currentPlaylist.add(i);
                                }
                                if (i < currentlyPlayingSongOrderInAllSongs) {
                                    ++songsAddedBefore;
                                }
                            }
                        }
                    }
                    break;
                case FILL_ALL_ARTIST_ALBUMS:
                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);

                        String iterableSongAlbum = iterableSong.getAlbum();
                        iterableSongAlbum       += iterableSong.checkYear();
                        for (int j = 0; j < whatToAdd.size(); j++) {
                            if (iterableSong.getArtist().equals(artistToShow) &
                                    iterableSongAlbum.equals(whatToAdd.get(j))) {
                                for (int k = 0; k < whereToAdd.size(); k++) {
                                    String iterableUserPlaylist = whereToAdd.get(k);
                                    if (!iterableSong.getRelatedUserPlaylists()
                                            .contains(iterableUserPlaylist)) {
                                        iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                    }
                                }
                                if (userIsCurrent) {
                                    if (!currentPlaylist.contains(i)) {
                                        currentPlaylist.add(i);
                                    }
                                    if (i < currentlyPlayingSongOrderInAllSongs) {
                                        ++songsAddedBefore;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case FILL_SPECIFIC_ALBUM_SONGS:
                    if (isShortWayAvailable) {
                        for (int i = 0; i < allSongs.size(); i++) {
                            Song iterableSong = allSongs.get(i);
                            if (iterableSong.getArtist().equals(artistToShow)) {
                                for (int j = 0; j < whereToAdd.size(); j++) {
                                    String iterableUserPlaylist = whereToAdd.get(j);
                                    if (!iterableSong.getRelatedUserPlaylists()
                                            .contains(iterableUserPlaylist)) {
                                        iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                    }
                                }
                                if (userIsCurrent) {
                                    if (!currentPlaylist.contains(i)) {
                                        currentPlaylist.add(i);
                                    }

                                    if (i < currentlyPlayingSongOrderInAllSongs) {
                                        ++songsAddedBefore;
                                    }
                                }
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < whatToAdd.size(); i++) {
                            Song iterableSong = allSongs.get((Integer) whatToAdd.get(i));
                            for (int j = 0; j < whereToAdd.size(); j++) {
                                String iterableUserPlaylist = whereToAdd.get(j);
                                if (!iterableSong.getRelatedUserPlaylists()
                                        .contains(iterableUserPlaylist)) {
                                    iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                }
                            }
                            if (userIsCurrent) {
                                order = (Integer) whatToAdd.get(i);
                                if (!currentPlaylist.contains(order)) {
                                    currentPlaylist.add(order);
                                }
                                if (order < currentlyPlayingSongOrderInAllSongs) {
                                    ++songsAddedBefore;
                                }
                            }
                        }
                    }
                    break;
                case FILL_ALL_ALBUM_TITLES:
                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        for (int j = 0; j < whatToAdd.size(); j++) {
                            if (iterableSong.getComplexAlbumInfo().equals(whatToAdd.get(j))) {
                                for (int k = 0; k < whereToAdd.size(); k++) {
                                    String iterableUserPlaylist = whereToAdd.get(k);
                                    if (!iterableSong.getRelatedUserPlaylists()
                                            .contains(iterableUserPlaylist)) {
                                        iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                    }
                                }
                                if (userIsCurrent) {
                                    if (!currentPlaylist.contains(i)) {
                                        currentPlaylist.add(i);
                                    }
                                    if (i < currentlyPlayingSongOrderInAllSongs) {
                                        ++songsAddedBefore;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case FILL_ALL_GENRE_NAMES:
                    for (int i = 0; i < allSongs.size(); i++) {
                        Song iterableSong = allSongs.get(i);
                        for (int j = 0; j < whatToAdd.size(); j++) {
                            if (iterableSong.getGenre().equals(whatToAdd.get(j))) {
                                for (int k = 0; k < whereToAdd.size(); k++) {
                                    String iterableUserPlaylist = whereToAdd.get(k);
                                    if (!iterableSong.getRelatedUserPlaylists()
                                            .contains(iterableUserPlaylist)) {
                                        iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                                    }
                                }
                                if (userIsCurrent) {
                                    if (!currentPlaylist.contains(i)) {
                                        currentPlaylist.add(i);
                                    }
                                    if (i < currentlyPlayingSongOrderInAllSongs) {
                                        ++songsAddedBefore;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case FILL_SPECIFIC_GENRE_SONGS:
                case FILL_ALL_ARTIST_SONGS:
                    for (int i = 0; i < whatToAdd.size(); i++) {
                        Song iterableSong = allSongs.get((Integer) whatToAdd.get(i));
                        for (int j = 0; j < whereToAdd.size(); j++) {
                            String iterableUserPlaylist = whereToAdd.get(j);
                            if (!iterableSong.getRelatedUserPlaylists()
                                    .contains(iterableUserPlaylist)) {
                                iterableSong.addRelatedUserPlaylist(iterableUserPlaylist);
                            }
                        }
                        if (userIsCurrent) {
                            order = (Integer) whatToAdd.get(i);
                            if (!currentPlaylist.contains(order)) {
                                currentPlaylist.add(order);
                            }
                            if (order < currentlyPlayingSongOrderInAllSongs) {
                                ++songsAddedBefore;
                            }
                        }
                    }
                    break;
            }

            if (currentPlaylistSizeBefore != currentPlaylist.size()) {
                Collections.sort(currentPlaylist);

                currentlyPlayingSongOrder = currentlyPlayingSongOrder + songsAddedBefore;
                String replacement = String.valueOf(currentlyPlayingSongOrder + 1) +
                        " / " + currentPlaylist.size();
                current_song_order_player.setText(replacement);
            }

            hideCustomDialog();

            uiHandler.sendEmptyMessage(HIDE_LOADING_SCREEN);

            showCustomDialog(
                    getString(R.string.go_to_user_playlists_confirmation),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideCustomDialog();

                            needToGoToUserPlaylists = true;
                            whereAreWeAddition = ADDITION_COMPLETED;
                            main_menu_exit_and_back_button.callOnClick();
                        }
                    },
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            needToGoToUserPlaylists = false;
                            whereAreWeAddition = ADDITION_COMPLETED;
                            main_menu_exit_and_back_button.callOnClick();
                        }
                    },
                    false,
                    false);
        }
    };

    // Method to load lyrics from song
    void loadLyrics() {
        String lyrics = "";
        try {
            lyrics = AudioFileIO.read(new File(currentlyPlayingSongName))
                    .getTag().getFirst(FieldKey.LYRICS);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (lyrics.equals("")) {
            player_screen_additional_info_line.setText(
                    res.getString(R.string.no_lyrics_available));
        }
        else {
            player_screen_additional_info_line.setText(lyrics);
        }
    }

    // Initializes all needed for app's functionality elements
    void initAll() {
        // Init Player screen elements: left-to-right and top-to-bottom
        background_layout                                      = findViewById(
                R.id.background_layout);
        player_screen                                          = findViewById(
                R.id.player_screen);
        // Top layout
        player_top_layout                                      = findViewById(
                R.id.player_top_layout);
        player_screen_equalizer_button                         = findViewById(
                R.id.player_screen_equalizer_button);
        player_screen_equalizer_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleEqualizerMenu(v);
                if (!isEqualizerMenuEnlarged) {
                    main_menu_equalizer_button.performLongClick();
                }
                return true;
            }
        });
        player_screen_help_button                              = findViewById(
                R.id.player_screen_help_button);
        player_screen_help_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleHelpMenu(v);
                if (!isHelpMenuEnlarged) {
                    main_menu_help_button.performLongClick();
                }
                return true;
            }
        });
        player_screen_preferences_button                       = findViewById(
                R.id.player_screen_preferences_button);
        player_screen_preferences_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                togglePreferencesMenu(v);
                if (!isPreferencesMenuEnlarged) {
                    main_menu_preferences_button.performLongClick();
                }
                return true;
            }
        });
        cover_button_small                                     = findViewById(
                R.id.cover_button_small);
        player_screen_playlists_button                         = findViewById(
                R.id.player_screen_playlists_button);
        player_screen_playlists_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                togglePlaylistsMenu(v);
                if (!isPlaylistsMenuEnlarged) {
                    main_menu_playlists_button.performLongClick();
                }
                return true;
            }
        });
        player_screen_additional_info_line                     = findViewById(
                R.id.player_screen_additional_info_line);
        player_screen_additional_info_line.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                player_screen.animate().alpha(0).setDuration(500).start();
                shadow.setVisibility(View.VISIBLE);
                shadow.animate().alpha(1).setDuration(500).start();
                enlarged_additional_info_line_text
                        .setText(player_screen_additional_info_line.getText());
                enlarged_additional_info_line_layout.setVisibility(View.VISIBLE);
                enlarged_additional_info_line_layout.animate().alpha(1).setDuration(500).start();
                isEnlargedAdditionalInfoLineShown = true;
                return true;
            }
        });
        // Middle layout
        player_middle_layout                                   = findViewById(
                R.id.player_middle_layout);
        current_song_artist_player                             = findViewById(
                R.id.current_song_artist_player);
        current_song_order_player                              = findViewById(
                R.id.current_song_order_player);
        current_song_album_player                              = findViewById(
                R.id.current_song_album_player);
        current_song_title_player                              = findViewById(
                R.id.current_song_title_player);
        // Bottom layout
        player_bottom_layout                                   = findViewById(
                R.id.player_bottom_layout);
        player_screen_previous_song_button                     = findViewById(
                R.id.player_screen_previous_song_button);
        player_screen_previous_song_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!new File(currentlyPlayingSongName).exists()) {
                    setSongDetails(false);
                }
                else {
                    resumePosition = -2;
                    gemMediaPlayer.playMedia();
                }
                return true;
            }
        });
        player_screen_play_pause_song_button                   = findViewById(
                R.id.player_screen_play_pause_song_button);
        player_screen_next_song_button                         = findViewById(
                R.id.player_screen_next_song_button);
        player_screen_shuffle_button                           = findViewById(
                R.id.player_screen_shuffle_button);
        player_screen_search_button                            = findViewById(
                R.id.player_screen_search_button);
        player_screen_search_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleSearchMenu(v);
                if (!isSearchMenuEnlarged) {
                    main_menu_search_button.performLongClick();
                }
                return true;
            }
        });
        player_screen_repeat_button                            = findViewById(
                R.id.player_screen_repeat_button);

        // Init shadow and set it's OnClickListener
        shadow                                                 = findViewById(R.id.shadow);
        shadow.setOnClickListener(hideMainMenuOrCover);

        // Init large cover button
        cover_button_large                                     = findViewById(
                R.id.cover_button_large);

        // Init Main menu elements
        main_menu                                              = findViewById(R.id.main_menu);
        main_menu_menu_s_buttons                               = findViewById(
                R.id.main_menu_menu_s_buttons);
        main_menu_top_divider                                  = findViewById(
                R.id.main_menu_top_divider);
        main_menu_equalizer_button                             = findViewById(
                R.id.main_menu_equalizer_button);
        main_menu_equalizer_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isEqualizerMenuToggled) {
                    enlargeMainMenu_sSubmenu();
                    isEqualizerMenuEnlarged = true;
                }
                return true;
            }
        });
        main_menu_help_button                                  = findViewById(
                R.id.main_menu_help_button);
        main_menu_help_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isHelpMenuToggled) {
                    enlargeMainMenu_sSubmenu();
                    isHelpMenuEnlarged = true;
                }
                return true;
            }
        });
        main_menu_playlists_button                             = findViewById(
                R.id.main_menu_playlists_button);
        main_menu_playlists_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isPlaylistsMenuToggled) {
                    enlargeMainMenu_sSubmenu();
                    isPlaylistsMenuEnlarged = true;
                }
                return true;
            }
        });
        main_menu_preferences_button                           = findViewById(
                R.id.main_menu_preferences_button);
        main_menu_preferences_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isPreferencesMenuToggled) {
                    enlargeMainMenu_sSubmenu();
                    isPreferencesMenuEnlarged = true;
                }
                return true;
            }
        });
        main_menu_search_button                                = findViewById(
                R.id.main_menu_search_button);
        main_menu_search_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isSearchMenuToggled) {
                    enlargeMainMenu_sSubmenu();
                    isSearchMenuEnlarged = true;
                }
                return true;
            }
        });
        main_menu_playback_controls_1                          = findViewById(
                R.id.main_menu_playback_controls_1);
        main_menu_previous_song_button                         = findViewById(
                R.id.main_menu_previous_song_button);
        main_menu_play_pause_song_button                       = findViewById(
                R.id.main_menu_play_pause_song_button);
        main_menu_next_song_button                             = findViewById(
                R.id.main_menu_next_song_button);
        main_menu_playback_controls_2                          = findViewById(
                R.id.main_menu_playback_controls_2);
        main_menu_playback_controls_2_space_1                  = findViewById(
                R.id.main_menu_playback_controls_2_space_1);
        main_menu_shuffle_button                               = findViewById(
                R.id.main_menu_shuffle_button);
        main_menu_playback_controls_2_space_2                  = findViewById(
                R.id.main_menu_playback_controls_2_space_2);
        main_menu_exit_and_back_button                         = findViewById(
                R.id.main_menu_exit_and_back_button);
        main_menu_exit_and_back_button.setOnClickListener(hideMainMenuOrCover);
        main_menu_playback_controls_2_space_3                  = findViewById(
                R.id.main_menu_playback_controls_2_space_3);
        main_menu_repeat_button                                = findViewById(
                R.id.main_menu_repeat_button);
        main_menu_playback_controls_2_space_4                  = findViewById(
                R.id.main_menu_playback_controls_2_space_4);
        main_menu_song_details                                 = findViewById(
                R.id.main_menu_song_details);
        current_song_artist_main_menu                          = findViewById(
                R.id.current_song_artist_main_menu);
        current_song_title_main_menu                           = findViewById(
                R.id.current_song_title_main_menu);
        current_song_album_main_menu                           = findViewById(
                R.id.current_song_album_main_menu);
        main_menu_s_enlarged_submenu_decrease_button           = findViewById(
                R.id.main_menu_s_enlarged_submenu_decrease_button);
        main_menu_s_enlarged_submenu_decrease_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseMainMenu_sSubmenu();
                if (isEqualizerMenuEnlarged & isEqualizerMenuToggled) {
                    isEqualizerMenuEnlarged = false;
                }
                if (isHelpMenuEnlarged & isHelpMenuToggled) {
                    isHelpMenuEnlarged = false;
                }
                if (isPlaylistsMenuEnlarged & isPlaylistsMenuToggled) {
                    isPlaylistsMenuEnlarged = false;
                }
                if (isPreferencesMenuEnlarged & isPreferencesMenuToggled) {
                    isPreferencesMenuEnlarged = false;
                }
                if (isSearchMenuEnlarged & isSearchMenuToggled) {
                    isSearchMenuEnlarged = false;
                }
            }
        });

        // Init help menu elements
        help_menu                                              = findViewById(R.id.help_menu);
        help_menu_image_view                                   = findViewById(
                R.id.help_menu_image_view);
        help_menu_image_view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                help_menu_element_description_large_layout.setVisibility(View.VISIBLE);
                help_menu_element_description_large_layout
                        .animate().alpha(1).setDuration(500).start();
                isHelpMenuLargeElementDescriptionShown = true;
                return true;
            }
        });
        help_menu_topics                                       = findViewById(
                R.id.help_menu_topics);
        help_menu_element_description_small_text               = findViewById(
                R.id.help_menu_element_description_view);
        help_menu_element_description_small_text.setOnLongClickListener(
                new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                help_menu_element_description_large_layout.setVisibility(View.VISIBLE);
                help_menu_element_description_large_layout
                        .animate().alpha(1).setDuration(500).start();
                isHelpMenuLargeElementDescriptionShown = true;
                return true;
            }
        });
        // Init help menu enlarged item description layout
        help_menu_element_description_large_layout_image_view  = findViewById(
                        R.id.help_menu_element_description_large_layout_image_view);
        help_menu_element_description_large_layout             = findViewById(
                R.id.help_menu_element_description_large_layout);
        help_menu_element_description_large_text               = findViewById(
                R.id.help_menu_element_description_large_text);
    help_menu_element_description_large_layout_decrease_button = findViewById(
                R.id.help_menu_element_description_large_layout_decrease_button);
        help_menu_element_description_large_layout_decrease_button.setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                help_menu_element_description_large_layout.animate().alpha(0).setDuration(500)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                help_menu_element_description_large_layout.setVisibility(View.GONE);
                                isHelpMenuLargeElementDescriptionShown = false;
                            }
                        }).start();
            }
        });

        // Init equalizer menu elements
        equalizer_menu                                         = findViewById(R.id.equalizer_menu);
        equalizer_menu_delete_current_preset_button            = findViewById(
                R.id.equalizer_menu_delete_current_preset_button);
        equalizer_menu_current_preset_name                     = findViewById(
                R.id.equalizer_menu_current_preset_name);
        equalizer_menu_add_new_preset_button                   = findViewById(
                R.id.equalizer_menu_add_new_preset_button);

        // Init Playlists menu elements
        playlists_menu                                         = findViewById(R.id.playlists_menu);
        playlists_menu_additional_info_line                    = findViewById(
                R.id.playlists_menu_additional_info_line);
        playlists_menu_objects                                 = findViewById(
                R.id.playlists_menu_objects);
        playlists_menu_confirm_button                          = findViewById(
                R.id.playlists_menu_confirm_button);
        playlists_menu_divider                                 = findViewById(
                R.id.playlists_menu_divider);

        // Init preferences menu elements
        preferences_menu                                       = findViewById(
                R.id.preferences_menu);
        preferences_menu_header                                = findViewById(
                R.id.preferences_menu_header);
        preferences_menu_header_divider                        = findViewById(
                R.id.preferences_menu_header_divider);
        preferences_menu_elements                              = findViewById(
                R.id.preferences_menu_elements);
        show_all_themes_text                                   = findViewById(
                R.id.show_all_themes_text);
        show_all_themes_button                                 = findViewById(
                R.id.show_all_themes_button);
        gem_themes                                             = findViewById(R.id.gem_themes);
        show_additional_info_line_options_text                 = findViewById(
                R.id.show_additional_info_line_options_text);
        show_additional_info_line_options_button               = findViewById(
                R.id.show_additional_info_line_options_button);
        additional_info_line_options                           = findViewById(
                R.id.additional_info_line_options);
        songs_rescan_button                                    = findViewById(
                R.id.songs_rescan_button);
        songs_rescan_request_on_start_check                    = findViewById(
                R.id.songs_rescan_request_on_start_check);
        songs_rescan_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSongsRescan(false);
            }
        });
        main_menu_song_details_check                           = findViewById(
                R.id.main_menu_song_details_check);
        text_ticker_effect_manually_disabling_check            = findViewById(
                R.id.text_ticker_effect_manually_disabling_check);
        main_menu_playback_controls_check                      = findViewById(
                R.id.main_menu_playback_controls_check);
        checkpoint_system_check                                = findViewById(
                R.id.checkpoint_system_check);
        gnr_amount_text                                        = findViewById(R.id.gnr_amount_text);
        gnr_amount                                             = findViewById(R.id.gnr_amount);
        gnr_amount.setHint(String.valueOf(amountOfGuaranteedNonRepetitiveSongsWhileShuffling));
        gnr_amount.setText(String.valueOf(amountOfGuaranteedNonRepetitiveSongsWhileShuffling));
        gnr_amount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    int input = Integer.valueOf(gnr_amount.getText().toString().toUpperCase());
                    if (input >= 0 & input <= 250) {
                        amountOfGuaranteedNonRepetitiveSongsWhileShuffling = input;
                        hideKeyboard();
                    }
                    else {
                        showCustomToast(CommonStrings.INPUT_CORRECT_AMOUNT);
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        preferences_menu_divider1                              = findViewById(
                R.id.preferences_menu_divider1);
        preferences_menu_divider2                              = findViewById(
                R.id.preferences_menu_divider2);
        preferences_menu_divider3                              = findViewById(
                R.id.preferences_menu_divider3);
        preferences_menu_divider4                              = findViewById(
                R.id.preferences_menu_divider4);
        preferences_menu_divider5                              = findViewById(
                R.id.preferences_menu_divider5);
        preferences_menu_divider6                              = findViewById(
                R.id.preferences_menu_divider6);
        preferences_menu_divider7                              = findViewById(
                R.id.preferences_menu_divider7);
        preferences_menu_divider8                              = findViewById(
                R.id.preferences_menu_divider8);

        // Init search menu elements
        search_menu                                            = findViewById(R.id.search_menu);
        search_menu_search_field                               = findViewById(
                R.id.search_menu_search_field);
        search_menu_search_field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search_menu_confirm_button.callOnClick();
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        search_menu_confirm_button                             = findViewById(
                R.id.search_menu_confirm_button);
        search_menu_found_matches                              = findViewById(
                R.id.search_menu_found_matches);
        search_menu_found_matches.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        main_menu_bottom_divider                               = findViewById(
                R.id.main_menu_bottom_divider);

        // Init enlarged_lyrics_or_current_playlist_name elements
        enlarged_additional_info_line_layout                   = findViewById(
                R.id.enlarged_additional_info_line_layout);
        enlarged_additional_info_line_text                     = findViewById(
                R.id.enlarged_additional_info_line_text);
        enlarged_additional_info_line_decrease_button          = findViewById(
                R.id.enlarged_additional_info_line_decrease_button);
        enlarged_additional_info_line_decrease_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shadow.animate().alpha(0).setDuration(500)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        shadow.setVisibility(View.GONE);
                                    }
                                }).start();
                        hide_enlarged_additional_info_line_layout();
                        player_screen.animate().alpha(1).setDuration(500).start();
                    }
                });

        // At last we are setting cover buttons' metrics, according to displayMetrics dpi's
        int smallCoverEdgeSize = (int) ((displayMetrics.heightPixels / 6) * displayMetrics.density);
        int largeCoverEdgeSize = (displayMetrics.widthPixels >= displayMetrics.heightPixels ?
                (displayMetrics.widthPixels / 2) : (displayMetrics.heightPixels / 2));

        cover_button_small.getLayoutParams().height = smallCoverEdgeSize;
        cover_button_small.getLayoutParams().width  = smallCoverEdgeSize;
        cover_button_large.getLayoutParams().height = largeCoverEdgeSize;
        cover_button_large.getLayoutParams().width  = largeCoverEdgeSize;

        loading_screen      = findViewById(R.id.loading_screen);
        loading_screen_icon = findViewById(R.id.loading_screen_icon);
        loading_screen_icon.getLayoutParams().height = smallCoverEdgeSize / 2;
        loading_screen_icon.getLayoutParams().width  = smallCoverEdgeSize / 2;
        loading_screen_text = findViewById(R.id.loading_screen_text);

        timer                                  = findViewById(R.id.timer);
        timer_divider                          = findViewById(R.id.timer_divider);
        currently_playing_song_duration_view   = findViewById(
                R.id.currently_playing_song_duration_view);
        currently_playing_song_checkpoint_time = findViewById(
                R.id.currently_playing_song_checkpoint_time);
        currently_playing_song_checkpoint_time.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (currentlyPlayingSong != null) {
                            currentlyPlayingSong.setCheckpointPosition(0);
                            currently_playing_song_checkpoint_time.setText(
                                    returnTimeIn_HH_MM_SS(2));
                            showCustomToast(CommonStrings.CHECKPOINT_WAS_RESET);
                        }
                        return true;
                    }
                });
        checkpoint_system_button               = findViewById(
                R.id.checkpoint_system_button);
        // OnLongClickListener, given to checkpoint_system_button,
        // plays currentlyPlayingSong from checkpoint position
        checkpoint_system_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (currentlyPlayingSong != null) {
                    if (gemMediaPlayerIsPrepared) {
                        gemMediaPlayer.seekTo((int) currentlyPlayingSong.getCheckpointPosition());
                    }
                    else {
                        resumePosition = CHECKPOINT_PRIORITY;
                        gemMediaPlayer.playMedia();
                    }
                }
                return true;
            }
        });
        if (isCheckPointSystemEnabled) {
            currently_playing_song_checkpoint_time.setVisibility(View.VISIBLE);
            checkpoint_system_button.setVisibility(View.VISIBLE);
        }

        seekbar = findViewById(R.id.seekbar);
    }

    // Theme application methods
    void applyTheme(String themeName) {
        if (deepInPreferences) {
            loading_screen_text.setText(R.string.please_wait_theme_application);
            uiHandler.sendEmptyMessage(SHOW_LOADING_SCREEN);
        }

        currentThemeName = themeName;

        if (currentThemeName.equals(themeNames[0])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.amethyst1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.amethyst2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.amethystSeekbar, null);
        }
        if (currentThemeName.equals(themeNames[1])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.blackDiamond1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.blackDiamond2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.blackDiamondSeekbar, null);
        }
        if (currentThemeName.equals(themeNames[2])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.citrine1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.citrine2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.citrineSeekbar, null);
        }
        if (currentThemeName.equals(themeNames[3])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.emerald1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.emerald2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.emeraldSeekbar, null);
        }
        if (currentThemeName.equals(themeNames[4])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.fireOpal1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.fireOpal2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.fireOpalSeekbar, null);
        }
        if (currentThemeName.equals(themeNames[5])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.ruby1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.ruby2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.rubySeekbar, null);
        }
        if (currentThemeName.equals(themeNames[6])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.sapphire1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.sapphire2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.sapphireSeekbar, null);
        }
        if (currentThemeName.equals(themeNames[7])) {
            themeColor1       = ResourcesCompat.getColor(
                    res, R.color.topaz1, null);
            themeColor2       = ResourcesCompat.getColor(
                    res, R.color.topaz2, null);
            themeColorSeekbar = ResourcesCompat.getColor(
                    res, R.color.topazSeekbar, null);
        }

        setAllElementsColors();
        setAllElementsImages();

        if (isNotificationShown) {
            createOrUpdateNotification();
        }

        if (deepInPreferences) {
            defaultPlaylistsMenuAdapter.notifyDataSetChanged();
            additional_info_line_optionsAdapter.notifyDataSetChanged();
            if (playlists_menu_objectsAdapter != null) {
                playlists_menu_objectsAdapter.notifyDataSetChanged();
            }
            if (search_menu_found_matchesAdapter != null) {
                search_menu_found_matchesAdapter.notifyDataSetChanged();
            }
            if (help_menu_topicsAdapter != null) {
                help_menu_topicsAdapter.notifyDataSetChanged();
                if (!currentHelpMenuImageName.equals("") &
                        !currentHelpMenuImageName.equals("preferences") &
                        !currentHelpMenuImageName.equals("songCover")) {
                    help_menu_image_view.setImageDrawable(
                            getImageFromAssets(currentHelpMenuImageName));
                    help_menu_element_description_large_layout_image_view.setImageDrawable(
                            help_menu_image_view.getDrawable());
                }
            }
            uiHandler.sendEmptyMessage(HIDE_LOADING_SCREEN);
        }
    }
    // Get theme-related button image from assets
    Drawable getImageFromAssets(String imageName) {
        try {
            InputStream ims = getAssets().open(
                    "theme-related images/" +
                            currentThemeName.toLowerCase() + "/" + imageName + ".png");
            Drawable d = Drawable.createFromStream(ims, null);
            ims.close();
            return d;
        }
        catch(IOException ex) {
            return null;
        }
    }
    // Sets all text and background colors
    void setAllElementsColors() {
        player_screen_additional_info_line.setTextColor(themeColor2);
        playlists_menu_additional_info_line.setTextColor(themeColor2);
        current_song_artist_player.setTextColor(themeColor2);
        current_song_artist_main_menu.setTextColor(themeColor2);
        current_song_order_player.setTextColor(themeColor2);
        current_song_album_player.setTextColor(themeColor2);
        current_song_album_main_menu.setTextColor(themeColor2);
        current_song_title_player.setTextColor(themeColor2);
        current_song_title_main_menu.setTextColor(themeColor2);
        equalizer_menu_current_preset_name.setTextColor(themeColor2);
        help_menu_element_description_small_text.setTextColor(themeColor2);
        show_all_themes_text.setTextColor(themeColor2);
        show_additional_info_line_options_text.setTextColor(themeColor2);
        songs_rescan_request_on_start_check.setTextColor(themeColor2);
        main_menu_song_details_check.setTextColor(themeColor2);
        text_ticker_effect_manually_disabling_check.setTextColor(themeColor2);
        main_menu_playback_controls_check.setTextColor(themeColor2);
        checkpoint_system_check.setTextColor(themeColor2);
        gnr_amount_text.setTextColor(themeColor2);
        gnr_amount.setTextColor(themeColor2);
        gnr_amount.setHintTextColor(themeColor2);
        gnr_amount.setBackgroundTintList(ColorStateList.valueOf(themeColor1));
        search_menu_search_field.setTextColor(themeColor2);
        search_menu_search_field.setHintTextColor(themeColor2);
        search_menu_search_field.setBackgroundTintList(ColorStateList.valueOf(themeColor1));
        help_menu_element_description_large_text.setTextColor(themeColor2);
        help_menu_element_description_large_layout_decrease_button.setTextColor(themeColor2);
        enlarged_additional_info_line_text.setTextColor(themeColor2);
        enlarged_additional_info_line_decrease_button.setTextColor(themeColor2);
        main_menu_s_enlarged_submenu_decrease_button.setTextColor(themeColor2);
        main_menu_s_enlarged_submenu_decrease_button.setBackgroundColor(themeColor1);
        preferences_menu_header.setTextColor(themeColor2);

        cover_button_small.setBackgroundTintList(ColorStateList.valueOf(themeColor2));
        cover_button_large.setBackgroundTintList(ColorStateList.valueOf(themeColor1));

        main_menu_top_divider.setBackgroundColor(themeColor2);
        playlists_menu_divider.setBackgroundColor(themeColor2);
        preferences_menu_header_divider.setBackgroundColor(themeColor2);
        preferences_menu_divider1.setBackgroundColor(themeColor2);
        preferences_menu_divider2.setBackgroundColor(themeColor2);
        preferences_menu_divider3.setBackgroundColor(themeColor2);
        preferences_menu_divider4.setBackgroundColor(themeColor2);
        preferences_menu_divider5.setBackgroundColor(themeColor2);
        preferences_menu_divider6.setBackgroundColor(themeColor2);
        preferences_menu_divider7.setBackgroundColor(themeColor2);
        preferences_menu_divider8.setBackgroundColor(themeColor2);
        main_menu_bottom_divider.setBackgroundColor(themeColor2);

        help_menu_topics.setDivider(new ColorDrawable(themeColor2));
        help_menu_topics.setChildDivider(new ColorDrawable(themeColor2));
        help_menu_topics.setDividerHeight((int) displayMetrics.density * 2);
        playlists_menu_objects.setDivider(new ColorDrawable(themeColor2));
        playlists_menu_objects.setDividerHeight((int) displayMetrics.density * 2);
        gem_themes.setDivider(new ColorDrawable(themeColor2));
        gem_themes.setDividerHeight((int) displayMetrics.density * 2);
        additional_info_line_options.setDivider(new ColorDrawable(themeColor2));
        additional_info_line_options.setDividerHeight((int) displayMetrics.density * 2);
        search_menu_found_matches.setChildDivider(new ColorDrawable(themeColor2));
        search_menu_found_matches.setDivider(new ColorDrawable(themeColor2));
        search_menu_found_matches.setDividerHeight((int) displayMetrics.density * 2);

        ColorStateList buttonStates = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        themeColor2,
                        themeColor1,
                        themeColor2
                }
        );
        songs_rescan_request_on_start_check.getThumbDrawable().setTintList(buttonStates);
        main_menu_song_details_check.getThumbDrawable().setTintList(buttonStates);
        text_ticker_effect_manually_disabling_check.getThumbDrawable().setTintList(buttonStates);
        main_menu_playback_controls_check.getThumbDrawable().setTintList(buttonStates);
        checkpoint_system_check.getThumbDrawable().setTintList(buttonStates);

        songs_rescan_button.setTextColor(themeColor2);
        songs_rescan_button.setHintTextColor(themeColor2);

        loading_screen_text.setTextColor(themeColor2);

        seekbar.setWaveColor(themeColorSeekbar);

        timer.setTextColor(themeColor2);
        timer_divider.setTextColor(themeColor2);
        currently_playing_song_duration_view.setTextColor(themeColor2);
        currently_playing_song_checkpoint_time.setTextColor(themeColor2);
    }
    // Sets all images to all ImageButtons of project
    void setAllElementsImages() {
        player_screen_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[0]));
        main_menu_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[0]));
        player_screen_help_button.setImageDrawable(getImageFromAssets(imageNames[2]));
        main_menu_help_button.setImageDrawable(getImageFromAssets(imageNames[2]));
        player_screen_playlists_button.setImageDrawable(getImageFromAssets(imageNames[4]));
        main_menu_playlists_button.setImageDrawable(getImageFromAssets(imageNames[4]));
        player_screen_previous_song_button.setImageDrawable(getImageFromAssets(imageNames[6]));
        main_menu_previous_song_button.setImageDrawable(getImageFromAssets(imageNames[6]));
        player_screen_next_song_button.setImageDrawable(getImageFromAssets(imageNames[9]));
        main_menu_next_song_button.setImageDrawable(getImageFromAssets(imageNames[9]));
        player_screen_search_button.setImageDrawable(getImageFromAssets(imageNames[12]));
        main_menu_search_button.setImageDrawable(getImageFromAssets(imageNames[12]));
        main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[14]));
        playlists_menu_confirm_button.setImageDrawable(getImageFromAssets(imageNames[19]));
        search_menu_confirm_button.setImageDrawable(getImageFromAssets(imageNames[19]));
        show_all_themes_button.setImageDrawable(getImageFromAssets(imageNames[20]));
        background_layout.setBackground(getImageFromAssets(imageNames[21]));
        player_screen.setBackground(getImageFromAssets(imageNames[21]));
        main_menu_s_enlarged_submenu_decrease_button.setBackground(
                getImageFromAssets(imageNames[24]));
        help_menu_element_description_large_layout.setBackground(
                getImageFromAssets(imageNames[21]));
        help_menu_element_description_large_layout_decrease_button.setBackground(
                getImageFromAssets(imageNames[24]));
        enlarged_additional_info_line_layout.setBackground(
                getImageFromAssets(imageNames[21]));
        enlarged_additional_info_line_decrease_button.setBackground(
                getImageFromAssets(imageNames[24]));
        change_show_additional_info_line_options_buttonImage();
        songs_rescan_button.setBackground(
                getImageFromAssets(imageNames[24]));
        songs_rescan_button.setBackground(
                getImageFromAssets(imageNames[24]));
        checkpoint_system_button.setBackground(
                getImageFromAssets(imageNames[25]));

        loading_screen.setBackground(getImageFromAssets(imageNames[21]));
        try {
            InputStream ims = getAssets().open(
                    "theme-related images/" +
                            currentThemeName.toLowerCase() + "/" + imageNames[21] + ".png");
            Blurry
                    .with(GemPlayer.this)
                    .radius(11)
                    .sampling(2)
                    .color(Color.argb(15, 255, 255, 255))
                    .from(BitmapFactory.decodeStream(ims))
                    .into(shadow);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        play_button_image       = getImageFromAssets(imageNames[7]);
        pause_button_image      = getImageFromAssets(imageNames[8]);
        switchPlayPauseButtonImage();
        straight_button_image   = getImageFromAssets(imageNames[10]);
        shuffle_button_image    = getImageFromAssets(imageNames[11]);
        switchShuffleMode();
        no_repeat_button_image  = getImageFromAssets(imageNames[16]);
        repeat_one_button_image = getImageFromAssets(imageNames[17]);
        repeat_all_button_image = getImageFromAssets(imageNames[18]);
        switchRepeatMode();
    }
    // Applies theme-related selector to all needed ListView items
    void applySelector(View item) {
        if (currentThemeName.equals(themeNames[0])) {
            item.setBackground(getDrawable(R.drawable.song_selector_amethyst));
        }
        if (currentThemeName.equals(themeNames[1])) {
            item.setBackground(getDrawable(R.drawable.song_selector_black_diamond));
        }
        if (currentThemeName.equals(themeNames[2])) {
            item.setBackground(getDrawable(R.drawable.song_selector_citrine));
        }
        if (currentThemeName.equals(themeNames[3])) {
            item.setBackground(getDrawable(R.drawable.song_selector_emerald));
        }
        if (currentThemeName.equals(themeNames[4])) {
            item.setBackground(getDrawable(R.drawable.song_selector_fire_opal));
        }
        if (currentThemeName.equals(themeNames[5])) {
            item.setBackground(getDrawable(R.drawable.song_selector_ruby));
        }
        if (currentThemeName.equals(themeNames[6])) {
            item.setBackground(getDrawable(R.drawable.song_selector_sapphire));
        }
        if (currentThemeName.equals(themeNames[7])) {
            item.setBackground(getDrawable(R.drawable.song_selector_topaz));
        }
    }
    // Applies indicator_icon Drawable to group Views of ExpandableListView
    void applyGroupIndicator(ImageView indicator_icon, boolean isExpanded) {
        if (isExpanded) {
            if (currentThemeName.equals(themeNames[0])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_amethyst));
            }
            if (currentThemeName.equals(themeNames[1])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_black_diamond));
            }
            if (currentThemeName.equals(themeNames[2])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_citrine));
            }
            if (currentThemeName.equals(themeNames[3])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_emerald));
            }
            if (currentThemeName.equals(themeNames[4])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_fire_opal));
            }
            if (currentThemeName.equals(themeNames[5])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_ruby));
            }
            if (currentThemeName.equals(themeNames[6])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_sapphire));
            }
            if (currentThemeName.equals(themeNames[7])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_opened_topaz));
            }
        }
        else {
            if (currentThemeName.equals(themeNames[0])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_amethyst));
            }
            if (currentThemeName.equals(themeNames[1])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_black_diamond));
            }
            if (currentThemeName.equals(themeNames[2])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_citrine));
            }
            if (currentThemeName.equals(themeNames[3])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_emerald));
            }
            if (currentThemeName.equals(themeNames[4])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_fire_opal));
            }
            if (currentThemeName.equals(themeNames[5])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_ruby));
            }
            if (currentThemeName.equals(themeNames[6])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_sapphire));
            }
            if (currentThemeName.equals(themeNames[7])) {
                indicator_icon.setImageDrawable(
                        getDrawable(R.drawable.group_indicator_closed_topaz));
            }
        }
    }
    
    // GemPlayer lifecycle-methods
    // Creation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("State", "onCreate started");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gem_player);

        inflater = getLayoutInflater();

        prefixesWithIndexes.put("The ", 4);
        prefixesWithIndexes.put("the ", 4);
        prefixesWithIndexes.put("An ", 3);
        prefixesWithIndexes.put("an ", 3);
        prefixesWithIndexes.put("A ", 2);
        prefixesWithIndexes.put("a ", 2);

        res                               = getResources();

        displayMetrics                    = this.res.getDisplayMetrics();
        imm                               = (InputMethodManager)
                getSystemService(Activity.INPUT_METHOD_SERVICE);

        // Init Gson instance
        gson                              = new Gson();

        playlistsOptionsNames             = res.getStringArray(R.array.playlists_options_names);

        // Init preferences object
        userPrefs                         = this.getSharedPreferences(
                prefsFileName, Context.MODE_PRIVATE);

        allSongs                          = new ArrayList<>();
        currentPlaylist                   = new ArrayList<>();
        allUserPlaylistsNames             = new ArrayList<>();
        songsToAddToPlaylist              = new ArrayList<>();

        themeNames                        = res.getStringArray(R.array.theme_names);
        imageNames                        = res.getStringArray(R.array.image_names);
        playlistsIconsIds                 = new int[] {
                R.drawable.current_playlist_icon,
                R.drawable.all_songs_icon,
                R.drawable.all_artists_icon,
                R.drawable.all_albums_icon,
                R.drawable.all_genres_icon,
                R.drawable.user_playlists_icon
        };

        additional_info_line_optionsTexts = res
                .getStringArray(R.array.additional_info_line_options_texts);
        playlistsIconsIds                 = new int[] {
                R.drawable.current_playlist_icon,
                R.drawable.all_songs_icon,
                R.drawable.all_artists_icon,
                R.drawable.all_albums_icon,
                R.drawable.all_genres_icon,
                R.drawable.user_playlists_icon
        };
        equalizerPresetNames              = res
                .getStringArray(R.array.equalizer_preset_names);

        foundSongsGroupName               = res.getString(R.string.found_songs);
        foundArtistsGroupName             = res.getString(R.string.found_artists);
        foundAlbumsGroupName              = res.getString(R.string.found_albums);
        foundGenresGroupName              = res.getString(R.string.found_genres);

        if (isFirstTimeBoot) {
            amountOfGuaranteedNonRepetitiveSongsWhileShuffling = 0;
            currentThemeName                                   = themeNames[1];
            currentEqualizerPresetName                         = equalizerPresetNames[0];
            current_additional_info_lineOption                 = 1;
        }

        // Init all functional elements
        initAll();

        restorePrefs();

        applyTheme(currentThemeName);

        equalizer_menu_current_preset_name.setText(currentEqualizerPresetName);
        /*tbd - change eq preset here*/

        songs_rescan_request_on_start_check.setChecked(needToShowRescanRequestAtStart);
        songs_rescan_request_on_start_check
                .setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        needToShowRescanRequestAtStart = isChecked;
                    }
                });
        main_menu_song_details_check.setChecked(needToShowSongDetailsOnMainMenu);
        main_menu_song_details_check
                .setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        needToShowSongDetailsOnMainMenu = isChecked;
                        if (!isPreferencesMenuEnlarged) {
                            if (needToShowSongDetailsOnMainMenu) {
                                main_menu_song_details.setVisibility(View.VISIBLE);
                            }
                            else {
                                main_menu_song_details.setVisibility(View.GONE);
                            }
                        }
                    }
                });
        if (!needToShowSongDetailsOnMainMenu) {
            main_menu_song_details.setVisibility(View.GONE);
        }
        text_ticker_effect_manually_disabling_check.setChecked(textTickerEffectManuallyDisabling);
        text_ticker_effect_manually_disabling_check
                .setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        textTickerEffectManuallyDisabling = isChecked;
                    }
                });
        main_menu_playback_controls_check.setChecked(needToShowPlaybackControlsOnMainMenu);
        main_menu_playback_controls_check
                .setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        needToShowPlaybackControlsOnMainMenu = isChecked;
                        if (!isPreferencesMenuEnlarged) {
                            if (needToShowPlaybackControlsOnMainMenu) {
                                main_menu_playback_controls_1.setVisibility(View.VISIBLE);
                                main_menu_exit_and_back_button.setLayoutParams(new LinearLayout.LayoutParams(
                                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                                main_menu_shuffle_button.setVisibility(View.VISIBLE);
                                main_menu_repeat_button.setVisibility(View.VISIBLE);
                            }
                            else {
                                main_menu_playback_controls_1.setVisibility(View.GONE);
                                main_menu_exit_and_back_button.setLayoutParams(new LinearLayout.LayoutParams(
                                        0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
                                main_menu_shuffle_button.setVisibility(View.GONE);
                                main_menu_repeat_button.setVisibility(View.GONE);
                            }
                        }
                    }
                });
        if (!needToShowPlaybackControlsOnMainMenu) {
            main_menu_playback_controls_1.setVisibility(View.GONE);
            main_menu_exit_and_back_button.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
            main_menu_shuffle_button.setVisibility(View.GONE);
            main_menu_repeat_button.setVisibility(View.GONE);
        }
        checkpoint_system_check.setChecked(isCheckPointSystemEnabled);
        checkpoint_system_check
                .setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        isCheckPointSystemEnabled = !isCheckPointSystemEnabled;
                        if (isCheckPointSystemEnabled) {
                            checkpoint_system_button.setVisibility(View.VISIBLE);
                            currently_playing_song_checkpoint_time.setVisibility(View.VISIBLE);
                            currently_playing_song_checkpoint_time.setText(
                                    returnTimeIn_HH_MM_SS(2));
                        }
                        else {
                            checkpoint_system_button.setVisibility(View.GONE);
                            currently_playing_song_checkpoint_time.setVisibility(View.GONE);
                        }
                    }
                });
        gnr_amount.setHint(String.valueOf(amountOfGuaranteedNonRepetitiveSongsWhileShuffling));
        gnr_amount.setText(String.valueOf(amountOfGuaranteedNonRepetitiveSongsWhileShuffling));

        createHelpMenuAdapter();
        createDefaultAdapterForPlaylistsMenu();
        uiHandler.sendEmptyMessage(FILL_PLAYLISTS_OPTIONS);
        create_gem_themesAdapter();
        create_additional_info_line_optionsAdapter();

        gemMediaPlayer = new GemMediaPlayer();

        notificationReceiver                    = new NotificationReceiver();
        IntentFilter notificationReceiverFilter = new IntentFilter();
        notificationReceiverFilter.addAction(PLAY_PREVIOUS_SONG);
        notificationReceiverFilter.addAction(PLAY_PAUSE_SONG);
        notificationReceiverFilter.addAction(PLAY_NEXT_SONG);
        registerReceiver(notificationReceiver, notificationReceiverFilter);

        // Request permissions or ensure, that user don't revoked 'em
        requestPermissions();
        if (isFirstTimeBoot || needToShowRescanRequestAtStart) {
            requestSongsRescan(true);
        }
        else {
            if (currentlyPlayingSong != null) {
                currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
            }
            else {
                currentlyPlayingSongDuration = 0;
            }
            setSongDetails(new File(currentlyPlayingSongName).exists());

            player_screen.animate().alpha(1).setDuration(500).start();
        }

        Log.i("State", "onCreate completed");
    }
    // Method to configure seekbar depending on currentlyPlayingSong attributes
    void configureSeekbar(boolean isSongExists) {
        seekbar.setRawData(analyzeSong(isSongExists));
        if (isSongExists) {
            seekbar.setChunkWidth((int)
                    ((displayMetrics.widthPixels - 32 * displayMetrics.density) /
                    (20 * Math.ceil((double) (currentlyPlayingSongDuration / 60000)))));
            seekbar.setOnProgressListener(new OnProgressListener() {
                @Override
                public void onProgressChanged(float progress, boolean fromUser) {
                    if (fromUser) {
                        resumePosition = (int) (progress / 100 * currentlyPlayingSongDuration);
                        if (gemMediaPlayerIsPrepared) {
                            gemMediaPlayer.seekTo(
                                    (int) (progress / 100 * currentlyPlayingSongDuration));
                        }
                        else {
                            gemMediaPlayer.playMedia();
                        }
                    }
                }

                @Override
                public void onStartTracking(float progress) {
                }
                @Override
                public void onStopTracking(float progress) {
                }
            });
            float whereToSeek;
            if (isCheckPointSystemEnabled) {
                whereToSeek = resumePosition == CHECKPOINT_PRIORITY ?
                        100 * currentlyPlayingSong.getCheckpointPosition() /
                                currentlyPlayingSongDuration :
                        100 * resumePosition /
                                currentlyPlayingSongDuration;
            }
            else {
                whereToSeek = resumePosition == CHECKPOINT_PRIORITY ?
                        0 : 100 * resumePosition / currentlyPlayingSongDuration;
            }
            seekbar.setProgress(whereToSeek);
            seekbar.setTouchable(true);
        }
        else {
            seekbar.setTouchable(false);
            seekbar.setChunkWidth(2 * (int) displayMetrics.density);
            seekbar.setOnProgressListener(null);
            seekbar.setProgress(0);
        }
    }

    // Start procedures
    @Override
    protected void onStart() {
        Log.i("State", "onStart started");
        super.onStart();
        Log.i("State", "onStart completed");
    }
    // Restart procedures
    @Override
    protected void onRestart() {
        Log.i("State", "onRestart started");
        super.onRestart();
        Log.i("State", "onRestart completed");
    }
    // Resume procedures
    @Override
    protected void onResume() {
        Log.i("State", "onResume started");
        super.onResume();
        /*if (gemMediaPlayerIsPrepared) {
            refreshSeekbarState();
            refreshTimerScore();
        }tbd*/
        Log.i("State", "onResume completed");
    }
    // Pause procedures
    @Override
    protected void onPause() {
        Log.i("State", "onPause started");
        super.onPause();
        Log.i("State", "onPause completed");
    }
    // Stop procedures
    @Override
    protected void onStop() {
        Log.i("State", "onStop started");
        if (gemMediaPlayerIsPrepared) {
            resumePosition = gemMediaPlayer.getCurrentPosition();
        }/*tbd*/
        savePrefs();
        super.onStop();
        Log.i("State", "onStop completed");
    }
    // System destroys the app
    @Override
    protected void onDestroy() {
        Log.i("State", "onDestroy started");
        // Is here comes some CODE??? tbd
        unregisterReceiver(notificationReceiver);/*tbd*/
        gemMediaPlayer.release();
        notificationManager.cancelAll();
        super.onDestroy();
        Log.i("State", "onDestroy completed");
    }
    // Saving the instance for system-caused Activity destruction case
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (gemMediaPlayerIsPrepared) {
            resumePosition = gemMediaPlayer.getCurrentPosition();
        }/*tbd*/
        savePrefs();
        // Is here comes some CODE??? tbd
        super.onSaveInstanceState(outState);
        Log.i("State", "onSaveInstanceState completed");
    }
    // Restoring the instance after system-caused Activity destruction
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Is here comes some CODE??? tbd
        super.onRestoreInstanceState(savedInstanceState);
        Log.i("State", "onRestoreInstanceState completed");
    }
    // Method to save user's preferences
    public void savePrefs() {
        userPrefs.edit().putBoolean("isFirstTimeBoot", isFirstTimeBoot).apply();
        userPrefs.edit().putBoolean("isShuffled", isShuffled).apply();
        userPrefs.edit().putString("repeatState", gson.toJson(repeatState)).apply();

        userPrefs.edit().putInt(
                "current_additional_info_lineOption", current_additional_info_lineOption)
                .apply();
        userPrefs.edit().putString("currentThemeName", currentThemeName).apply();
        userPrefs.edit().putString("currentEqualizerPresetName", currentEqualizerPresetName)
                .apply();

        userPrefs.edit().putBoolean(
                "needToShowRescanRequestAtStart", needToShowRescanRequestAtStart)
                .apply();
        userPrefs.edit().putBoolean(
                "needToShowSongDetailsOnMainMenu", needToShowSongDetailsOnMainMenu)
                .apply();
        userPrefs.edit().putBoolean(
                "textTickerEffectManuallyDisabling", textTickerEffectManuallyDisabling)
                .apply();
        userPrefs.edit().putBoolean(
                "needToShowPlaybackControlsOnMainMenu", needToShowPlaybackControlsOnMainMenu)
                .apply();
        userPrefs.edit().putBoolean(
                "isCheckPointSystemEnabled", isCheckPointSystemEnabled)
                .apply();
        userPrefs.edit().putInt(
                "amountOfGuaranteedNonRepetitiveSongsWhileShuffling",
                amountOfGuaranteedNonRepetitiveSongsWhileShuffling).apply();

        userPrefs.edit().putString("currentPlaylistName", currentPlaylistName).apply();

        userPrefs.edit().putString("currentlyPlayingSongName", currentlyPlayingSongName).apply();

        for (int i = 0; i < allSongs.size(); i++) {
            userPrefs.edit().putString("songAllSongs " + i, gson.toJson(allSongs.get(i))).apply();
        }
        userPrefs.edit().putInt("allSongsSize", allSongs.size()).apply();

        for (int i = 0; i < currentPlaylist.size(); i++) {
            userPrefs.edit().putInt("currentPlaylistSong " + i, currentPlaylist.get(i)).apply();
        }
        userPrefs.edit().putInt("currentPlaylistSize", currentPlaylist.size()).apply();

        userPrefs.edit().putString("complexAlbumInfoForRecovery", complexAlbumInfoForRecovery)
                .apply();

        userPrefs.edit().putInt("resumePosition", resumePosition).apply();

        userPrefs.edit().putInt("allUserPlaylistsNamesSize", allUserPlaylistsNames.size()).apply();
        for (int i = 0; i < allUserPlaylistsNames.size(); i++) {
            userPrefs.edit().putString("allUserPlaylistsNames " + i, allUserPlaylistsNames.get(i))
                    .apply();
        }

        userPrefs.edit().putString(
                "firstVisiblePositions", gson.toJson(firstVisiblePositions)).apply();
        userPrefs.edit().putString(
                "firstVisiblePositionsUser", gson.toJson(firstVisiblePositionsUser)).apply();

        Log.i("State", "State saved");
    }
    // Method to recover user's preferences
    public void restorePrefs() {
        isFirstTimeBoot = userPrefs.getBoolean("isFirstTimeBoot", true);

        if (!isFirstTimeBoot) {
            isShuffled                                         = userPrefs
                    .getBoolean("isShuffled", false);
            repeatState                                        = gson.fromJson
                    (userPrefs.getString("repeatState", null), RepeatState.class);

            current_additional_info_lineOption                 = userPrefs
                    .getInt("current_additional_info_lineOption", 1);
            currentThemeName                                   = userPrefs
                    .getString("currentThemeName", themeNames[1]);
            currentEqualizerPresetName                         = userPrefs
                    .getString("currentEqualizerPresetName", equalizerPresetNames[0]);

            needToShowRescanRequestAtStart                     = userPrefs
                    .getBoolean("needToShowRescanRequestAtStart", false);
            needToShowSongDetailsOnMainMenu                    = userPrefs
                    .getBoolean("needToShowSongDetailsOnMainMenu", true);
            textTickerEffectManuallyDisabling                  = userPrefs
                    .getBoolean("textTickerEffectManuallyDisabling", false);
            needToShowPlaybackControlsOnMainMenu               = userPrefs
                    .getBoolean("needToShowPlaybackControlsOnMainMenu", true);
            isCheckPointSystemEnabled                   = userPrefs
                    .getBoolean("isCheckPointSystemEnabled", true);
            amountOfGuaranteedNonRepetitiveSongsWhileShuffling = userPrefs
                    .getInt("amountOfGuaranteedNonRepetitiveSongsWhileShuffling", 0);

            currentPlaylistName                                = userPrefs
                    .getString("currentPlaylistName", "").split("\n")[0] + "\n";
            currentlyPlayingSongName                           = userPrefs
                    .getString("currentlyPlayingSongName", "");

            int allSongsSize = userPrefs.getInt("allSongsSize", 0);

            if (currentPlaylistName.equals(playlistsOptionsNames[1] + "\n")) {
                if (allSongsSize != 0) {
                    for (int i = 0; i < allSongsSize; i++) {
                        String key = "songAllSongs " + i;
                        allSongs.add(gson.fromJson(userPrefs
                                .getString(key, null), Song.class));
                        if (allSongs.get(i).getData().equals(currentlyPlayingSongName)) {
                            currentlyPlayingSongOrder    = i;
                            currentlyPlayingSong         = allSongs.get(currentlyPlayingSongOrder);
                            currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                        }
                    }
                }

                currentPlaylist = new ArrayList<>();
                for (int i = 0; i < allSongs.size(); i++) {
                    currentPlaylist.add(i);
                }
            }
            else {
                if (allSongsSize != 0) {
                    for (int i = 0; i < allSongsSize; i++) {
                        String key = "songAllSongs " + i;
                        allSongs.add(gson.fromJson(userPrefs
                                .getString(key, null), Song.class));
                    }
                }

                int currentPlaylistSize = userPrefs.getInt("currentPlaylistSize", 0);
                if (currentPlaylistSize != 0) {
                    for (int i = 0; i < currentPlaylistSize; i++) {
                        String key = "currentPlaylistSong " + i;
                        currentPlaylist.add(userPrefs.getInt(key, 0));
                        if (allSongs.get(currentPlaylist.get(i)).getData()
                                .equals(currentlyPlayingSongName)) {
                            currentlyPlayingSongOrder    = i;
                            currentlyPlayingSong         = allSongs.get(
                                    currentPlaylist.get(currentlyPlayingSongOrder));
                            currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                        }
                    }
                }
            }
            complexAlbumInfoForRecovery = userPrefs
                    .getString("complexAlbumInfoForRecovery", "");
            resumePosition              = userPrefs
                    .getInt("resumePosition", 0);

            int allUserPlaylistsNamesSize = userPrefs
                    .getInt("allUserPlaylistsNamesSize", 1);
            for (int i = 0; i < allUserPlaylistsNamesSize; i++) {
                allUserPlaylistsNames.add(userPrefs
                        .getString("allUserPlaylistsNames " + i, ""));
            }

            firstVisiblePositions     = gson.fromJson(userPrefs.getString(
                    "firstVisiblePositions", String.valueOf(new SparseIntArray())),
                    SparseIntArray.class);
            firstVisiblePositionsUser = gson.fromJson(userPrefs.getString(
                    "firstVisiblePositionsUser", String.valueOf(new SparseIntArray())),
                    SparseIntArray.class);
        }

        Log.i("State", "State recovered");
    }

    // UI methods

    // Shows Toast with given message
    void showCustomToast(String message) {
        custom_toast = findViewById(R.id.custom_toast);

        custom_toast_shadow = custom_toast.findViewById(R.id.custom_toast_shadow);
        custom_toast_shadow.setBackground(new ColorDrawable(themeColorSeekbar));

        custom_toast_frame_outer = custom_toast.findViewById(R.id.custom_toast_frame_outer);
        custom_toast_frame_outer.setBackground(new ColorDrawable(themeColor2));

        custom_toast_frame_inner = custom_toast.findViewById(R.id.custom_toast_frame_inner);
        custom_toast_frame_inner.setBackground(new ColorDrawable(themeColorSeekbar));

        custom_toast_message = custom_toast_frame_inner.findViewById(R.id.custom_toast_message);
        custom_toast_message.setTextColor(themeColor2);
        custom_toast_message.setText(message);

        custom_toast_close_button =
                custom_toast_frame_inner.findViewById(R.id.custom_toast_close_button);
        custom_toast_close_button.setTextColor(themeColor2);
        custom_toast_close_button.setBackground(getImageFromAssets(imageNames[24]));

        custom_toast_close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                custom_toast.animate().alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                custom_toast.setVisibility(View.GONE);

                                custom_toast              = null;
                                custom_toast_shadow       = null;
                                custom_toast_frame_outer  = null;
                                custom_toast_frame_inner  = null;
                                custom_toast_message      = null;
                                custom_toast_close_button = null;

                                isCustomToastShown        = false;

                                if (!arePermissionsGranted) {
                                    GemPlayer.this.finish();
                                }
                            }
                        }).start();
            }
        });
        custom_toast_shadow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                custom_toast_close_button.callOnClick();
            }
        });

        isCustomToastShown = true;

        custom_toast.setVisibility(View.VISIBLE);
        custom_toast.animate().alpha(1).start();
    }

    // Shows Dialog with given message and actions
    void showCustomDialog(String message,
                                 final View.OnClickListener confirmButtonListener,
                                 final View.OnClickListener cancelButtonListener,
                                 final boolean isInSongsRescanRequestAtStart,
                                 final boolean inUserPlaylistAddition) {
        custom_dialog = findViewById(R.id.custom_dialog);

        custom_dialog_shadow = custom_dialog.findViewById(R.id.custom_dialog_shadow);
        custom_dialog_shadow.setBackground(new ColorDrawable(themeColorSeekbar));

        custom_dialog_frame_outer = custom_dialog.findViewById(R.id.custom_dialog_frame_outer);
        custom_dialog_frame_outer.setBackground(new ColorDrawable(themeColor2));

        custom_dialog_frame_inner = custom_dialog.findViewById(R.id.custom_dialog_frame_inner);
        custom_dialog_frame_inner.setBackground(new ColorDrawable(themeColorSeekbar));

        custom_dialog_message = custom_dialog_frame_inner.findViewById(R.id.custom_dialog_message);
        custom_dialog_message.setTextColor(themeColor2);
        if (isInSongsRescanRequestAtStart) {
            message += res.getString(R.string.can_be_disabled_in_preferences);
        }
        custom_dialog_message.setText(message);

        custom_dialog_cancel_button = custom_dialog_frame_inner.findViewById(
                R.id.custom_dialog_cancel_button);
        custom_dialog_cancel_button.setTextColor(themeColor2);
        custom_dialog_cancel_button.setBackground(getImageFromAssets(imageNames[24]));

        custom_dialog_confirm_button = custom_dialog_frame_inner.findViewById(
                R.id.custom_dialog_confirm_button);
        custom_dialog_confirm_button.setTextColor(themeColor2);
        custom_dialog_confirm_button.setBackground(getImageFromAssets(imageNames[24]));

        if (isInSongsRescanRequestAtStart) {
            custom_dialog_songs_rescan_request_on_start_check = custom_dialog_frame_inner
                    .findViewById(R.id.custom_dialog_songs_rescan_request_on_start_check);

            custom_dialog_songs_rescan_request_on_start_check.setTextColor(themeColor2);
            custom_dialog_songs_rescan_request_on_start_check.setHintTextColor(themeColor2);
            custom_dialog_songs_rescan_request_on_start_check
                    .setBackgroundTintList(ColorStateList.valueOf(themeColor1));

            custom_dialog_songs_rescan_request_on_start_check
                    .setChecked(needToShowRescanRequestAtStart);

            custom_dialog_songs_rescan_request_on_start_check.setVisibility(View.VISIBLE);
        }

        if (inUserPlaylistAddition) {
            custom_dialog_user_input = custom_dialog_frame_inner.findViewById(
                    R.id.custom_dialog_new_user_playlist_name);
            custom_dialog_user_input.setTextColor(themeColor2);
            custom_dialog_user_input.setHintTextColor(themeColor2);
            custom_dialog_user_input.setBackgroundTintList(ColorStateList.valueOf(themeColor1));

            custom_dialog_user_input.setOnEditorActionListener(
                    new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_GO) {
                                custom_dialog_confirm_button.callOnClick();
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
            });

            custom_dialog_user_input.setVisibility(View.VISIBLE);
        }

        custom_dialog_confirm_button.setOnClickListener(confirmButtonListener);

        custom_dialog_cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                custom_dialog.animate().alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                custom_dialog.setVisibility(View.GONE);

                                if (isInSongsRescanRequestAtStart) {
                                    if (currentlyPlayingSong != null) {
                                        currentlyPlayingSongDuration =
                                                currentlyPlayingSong.getDuration();
                                    }
                                    else {
                                        currentlyPlayingSongDuration = 0;
                                    }
                                    setSongDetails(new File(currentlyPlayingSongName).exists());

                                    songs_rescan_request_on_start_check.setChecked(
                                    custom_dialog_songs_rescan_request_on_start_check.isChecked());

                                    player_screen.animate().alpha(1).setDuration(500).start();
                                }

                                if (cancelButtonListener != null) {
                                    cancelButtonListener.onClick(null);
                                }

                                nullifyAllCustomDialogObjects();
                            }
                        }).start();
            }
        });

        custom_dialog_shadow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                custom_dialog_cancel_button.callOnClick();
            }
        });

        isCustomDialogShown = true;

        custom_dialog.setVisibility(View.VISIBLE);
        custom_dialog.animate().alpha(1).start();
    }
    void nullifyAllCustomDialogObjects() {
        if (custom_dialog_songs_rescan_request_on_start_check != null) {
            custom_dialog_songs_rescan_request_on_start_check.setVisibility(View.GONE);
        }
        if (custom_dialog_user_input != null) {
            custom_dialog_user_input.setText("");
            custom_dialog_user_input.setVisibility(View.GONE);
        }

        custom_dialog                                     = null;
        custom_dialog_shadow                              = null;
        custom_dialog_frame_outer                         = null;
        custom_dialog_songs_rescan_request_on_start_check = null;
        custom_dialog_user_input                          = null;
        custom_dialog_frame_inner                         = null;
        custom_dialog_message                             = null;
        custom_dialog_cancel_button                       = null;
        custom_dialog_confirm_button                      = null;

        isCustomDialogShown                               = false;
    }
    void hideCustomDialog() {
        custom_dialog.animate().alpha(0)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        custom_dialog.setVisibility(View.GONE);

                        nullifyAllCustomDialogObjects();
                    }
                }).start();
    }

    // Enable text ticker effect on TextView click
    public void addTickerEffectToText(View view) {
        TextView tappedTextView = findViewById(view.getId());
        if (tappedTextView.getEllipsize().equals(TextUtils.TruncateAt.END)) {
            tappedTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            tappedTextView.setHorizontallyScrolling(true);
            tappedTextView.setSelected(true);
        }
        else {
            tappedTextView.setEllipsize(TextUtils.TruncateAt.END);
            tappedTextView.setHorizontallyScrolling(false);
            tappedTextView.setSelected(false);
        }
    }
    // Auto disable text ticker effect
    void disableTextTickerEffect() {
        if (!textTickerEffectManuallyDisabling) {
            if (current_song_artist_player.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_artist_player.setEllipsize(TextUtils.TruncateAt.END);
                current_song_artist_player.setHorizontallyScrolling(false);
                current_song_artist_player.setSelected(false);
            }
            if (current_song_order_player.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_order_player.setEllipsize(TextUtils.TruncateAt.END);
                current_song_order_player.setHorizontallyScrolling(false);
                current_song_order_player.setSelected(false);
            }
            if (current_song_album_player.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_album_player.setEllipsize(TextUtils.TruncateAt.END);
                current_song_album_player.setHorizontallyScrolling(false);
                current_song_album_player.setSelected(false);
            }
            if (timer.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                timer.setEllipsize(TextUtils.TruncateAt.END);
                timer.setHorizontallyScrolling(false);
                timer.setSelected(false);
            }
            if (currently_playing_song_duration_view.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                currently_playing_song_duration_view.setEllipsize(TextUtils.TruncateAt.END);
                currently_playing_song_duration_view.setHorizontallyScrolling(false);
                currently_playing_song_duration_view.setSelected(false);
            }
            if (currently_playing_song_checkpoint_time.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                currently_playing_song_checkpoint_time.setEllipsize(TextUtils.TruncateAt.END);
                currently_playing_song_checkpoint_time.setHorizontallyScrolling(false);
                currently_playing_song_checkpoint_time.setSelected(false);
            }
            if (current_song_title_player.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_title_player.setEllipsize(TextUtils.TruncateAt.END);
                current_song_title_player.setHorizontallyScrolling(false);
                current_song_title_player.setSelected(false);
            }
            if (equalizer_menu_current_preset_name.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                equalizer_menu_current_preset_name.setEllipsize(TextUtils.TruncateAt.END);
                equalizer_menu_current_preset_name.setHorizontallyScrolling(false);
                equalizer_menu_current_preset_name.setSelected(false);
            }
            if (playlists_menu_additional_info_line.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                playlists_menu_additional_info_line.setEllipsize(TextUtils.TruncateAt.END);
                playlists_menu_additional_info_line.setHorizontallyScrolling(false);
                playlists_menu_additional_info_line.setSelected(false);
            }
            if (preferences_menu_header.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                preferences_menu_header.setEllipsize(TextUtils.TruncateAt.END);
                preferences_menu_header.setHorizontallyScrolling(false);
                preferences_menu_header.setSelected(false);
            }
            if (current_song_artist_main_menu.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_artist_main_menu.setEllipsize(TextUtils.TruncateAt.END);
                current_song_artist_main_menu.setHorizontallyScrolling(false);
                current_song_artist_main_menu.setSelected(false);
            }
            if (current_song_album_main_menu.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_album_main_menu.setEllipsize(TextUtils.TruncateAt.END);
                current_song_album_main_menu.setHorizontallyScrolling(false);
                current_song_album_main_menu.setSelected(false);
            }
            if (current_song_title_main_menu.getEllipsize()
                    .equals(TextUtils.TruncateAt.MARQUEE)) {
                current_song_title_main_menu.setEllipsize(TextUtils.TruncateAt.END);
                current_song_title_main_menu.setHorizontallyScrolling(false);
                current_song_title_main_menu.setSelected(false);
            }
        }
    }

    // Set song details TextViews' text under currentlyPlayingSong attributes
    void setSongDetails(boolean isSongExists) {
        if (allSongs.isEmpty()) {
            configureSeekbar(false);

            clearSongDetails();

            showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);

            player_screen_additional_info_line.setText(CommonStrings.NO_SONGS_ON_DEVICE);
            playlists_menu_additional_info_line.setText(CommonStrings.NO_SONGS_ON_DEVICE);
        }
        else {
            configureSeekbar(isSongExists);

            set_additional_info_lineText(isSongExists);

            if (isSongExists) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(currentlyPlayingSongName);
                byte[] artBytes = retriever.getEmbeddedPicture();
                if (artBytes != null && artBytes.length > 0) {
                    Bitmap songImage = BitmapFactory
                            .decodeByteArray(artBytes, 0, artBytes.length);
                    cover_button_small.setImageBitmap(songImage);
                    cover_button_large.setImageBitmap(songImage);
                }
                else {
                    cover_button_small.setImageBitmap(null);
                    cover_button_large.setImageBitmap(null);
                }
            }
            else {
                cover_button_small.setImageBitmap(null);
                cover_button_large.setImageBitmap(null);

                requestSongsRescan(false);
                showCustomToast(CommonStrings.SONG_REMOVED);
            }

            String currentlyPlayingSongArtist = currentlyPlayingSong.getArtist();
            if (!currentlyPlayingSongArtist.equals(CommonStrings.UNKNOWN_ARTIST)) {
                current_song_artist_player.setText(currentlyPlayingSongArtist);
                current_song_artist_main_menu.setText(currentlyPlayingSongArtist);
                current_song_artist_main_menu.setVisibility(View.VISIBLE);
                if (!isMainMenuShown) {
                    current_song_artist_player.setVisibility(View.VISIBLE);
                }
            }
            else {
                current_song_artist_player.setText("");
                current_song_artist_player.setVisibility(View.GONE);
                current_song_artist_main_menu.setText("");
                current_song_artist_main_menu.setVisibility(View.GONE);
            }

            String currentlyPlayingSongOrder = String.valueOf(
                    this.currentlyPlayingSongOrder + 1) + " / " + currentPlaylist.size();
            current_song_order_player.setText(currentlyPlayingSongOrder);

            String currentlyPlayingSongAlbum = currentlyPlayingSong.getAlbum();
            if (!currentlyPlayingSongAlbum.equals(CommonStrings.UNKNOWN_ALBUM)) {
                currentlyPlayingSongAlbum += currentlyPlayingSong.checkYear();

                current_song_album_player.setText(currentlyPlayingSongAlbum);
                current_song_album_main_menu.setText(currentlyPlayingSongAlbum);
                current_song_album_main_menu.setVisibility(View.VISIBLE);
                if (!isMainMenuShown) {
                    current_song_album_player.setVisibility(View.VISIBLE);
                }
            }
            else {
                current_song_album_player.setText("");
                current_song_album_player.setVisibility(View.GONE);
                current_song_album_main_menu.setText("");
                current_song_album_main_menu.setVisibility(View.GONE);
            }

            String currentlyPlayingSongTitle = currentlyPlayingSong.getTitle();
            currentlyPlayingSongTitle += currentlyPlayingSong.checkTrack();
            current_song_title_player.setText(currentlyPlayingSongTitle);
            current_song_title_main_menu.setText(currentlyPlayingSongTitle);

            currently_playing_song_duration_view.setText(returnTimeIn_HH_MM_SS(0));

            timer.setText(returnTimeIn_HH_MM_SS(3));

            currently_playing_song_checkpoint_time.setText(returnTimeIn_HH_MM_SS(2));
        }
    }
    // Clears all song details, if song were removed during #GEM session
    void clearSongDetails() {
        cover_button_small.setImageBitmap(null);
        cover_button_large.setImageBitmap(null);

        current_song_artist_player.setText("");
        current_song_album_player.setText("");
        current_song_order_player.setText("");
        current_song_title_player.setText("");

        current_song_artist_main_menu.setText("");
        current_song_title_main_menu.setText("");
        current_song_album_main_menu.setText("");

        timer.setText(R.string.null_time);
        currently_playing_song_duration_view.setText(R.string.null_time);
        currently_playing_song_checkpoint_time.setText(R.string.null_time);
    }

    // Play audio from playlist
    void playAudioFromPlaylist(ArrayList<Integer> playlistToMakeCurrent, String it_sName,
                                      int songOrder, String newComplexAlbumInfoForRecovery,
                                      boolean fromSearch) {
        if (playlistToMakeCurrent != null) {
            if (playlistToMakeCurrent.isEmpty()) {
                currentlyPlayingSong = allSongs.get(songOrder);
            }
            else {
                currentlyPlayingSong = allSongs.get(playlistToMakeCurrent.get(songOrder));
            }
        }
        else {
            currentlyPlayingSong = allSongs.get(currentPlaylist.get(songOrder));
        }

        currentlyPlayingSongOrder = songOrder;
        currentlyPlayingSongName  = currentlyPlayingSong.getData();
        if (playlistToMakeCurrent != null) {
            if (playlistToMakeCurrent.isEmpty()) {
                currentPlaylist = new ArrayList<>();
                for (int i = 0; i < allSongs.size(); i++) {
                    currentPlaylist.add(i);
                }
            }
            else {
                currentPlaylist = playlistToMakeCurrent;
            }
        }
        if (!it_sName.equals("")) {
            currentPlaylistName = it_sName + "\n";
        }
        complexAlbumInfoForRecovery  = newComplexAlbumInfoForRecovery;
        currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();

        resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

        checkFileExistance(false);

        if (!fromSearch) {
            clear_search_menu_found_matches_Highlighting();
        }
    }
    // Play/pauses song
    public void onPlayPauseClick(View view) {
        if (allSongs.isEmpty()) {
            showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);
        }
        else {
            if (currentlyPlayingSong == null) {
                showCustomToast(CommonStrings.SONG_REMOVED);
            }
            else {
                if (!isPlaying) {
                    if (new File(currentlyPlayingSongName).exists()) {
                        gemMediaPlayer.playMedia();
                    }
                    else {
                        set_additional_info_lineText(false);

                        requestSongsRescan(false);
                        showCustomToast(CommonStrings.SONG_REMOVED);
                    }
                }
                else {
                    gemMediaPlayer.pauseMedia();
                }
            }
        }
    }
    // Auxiliary method to check, if song exists or not and perform according actions
    void checkFileExistance(boolean isRepeatOneEnabled) {
        if (new File(currentlyPlayingSongName).exists()) {
            if (!isRepeatOneEnabled) {
                setSongDetails(true);
            }

            gemMediaPlayer.playMedia();
        }
        else {
            setSongDetails(false);

            gemMediaPlayer.stopMedia();
        }
    }
    // Previous song button click
    public void playPreviousSong(View view) {
        disableTextTickerEffect();
        // Add here a seek to start of song logic on first click
        if (allSongs.isEmpty()) {
            showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);
        }
        else {
            if (playFirstSongAfterLastOnce) {
                playFirstSongAfterLastOnce = false;
            }

            if (repeatState == RepeatState.REPEAT_ONE) {
                resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                checkFileExistance(true);
            }
            else {
                if (checkIfShuffled()) {
                    currentlyPlayingSong         = allSongs.get(currentPlaylist
                            .get(currentlyPlayingSongOrder));
                    currentlyPlayingSongName     = currentlyPlayingSong.getData();
                    currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                    resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                    checkFileExistance(false);
                }
                else {
                    if (currentlyPlayingSongOrder != 0) {
                        currentlyPlayingSong         = allSongs.get(currentPlaylist
                                .get(--currentlyPlayingSongOrder));
                        currentlyPlayingSongName     = currentlyPlayingSong.getData();
                        currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                        resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                        checkFileExistance(false);
                    }
                    else {
                        if (repeatState == RepeatState.REPEAT_ALL) {
                            currentlyPlayingSongOrder    = currentPlaylist.size() - 1;
                            currentlyPlayingSong         = allSongs.get(currentPlaylist
                                    .get(currentlyPlayingSongOrder));
                            currentlyPlayingSongName     = currentlyPlayingSong.getData();
                            currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                            resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                            checkFileExistance(false);
                        }
                        if (repeatState == RepeatState.NO_REPEAT) {
                            if (!playLastSongAfterFirstOnce) {
                                playLastSongAfterFirstOnce = true;

                                showCustomToast(CommonStrings.AT_FIRST_SONG);
                            }
                            else {
                                currentlyPlayingSongOrder    = currentPlaylist.size() - 1;
                                currentlyPlayingSong         = allSongs.get(currentPlaylist
                                        .get(currentlyPlayingSongOrder));
                                currentlyPlayingSongName     = currentlyPlayingSong.getData();
                                currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                                resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                                playLastSongAfterFirstOnce = false;
                                checkFileExistance(false);
                            }
                        }
                    }
                }
                if (playlists_menu_objectsAdapter != null) {
                    playlists_menu_objects.setItemChecked(
                            playlists_menu_objects.getCheckedItemPosition(), false);
                    playlists_menu_objectsAdapter.notifyDataSetChanged();
                }
                clear_search_menu_found_matches_Highlighting();
            }
        }
    }
    // Next song button click
    public void playNextSong(View view) {
        disableTextTickerEffect();
        if (allSongs.isEmpty()) {
            showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);
        }
        else {
            if (playLastSongAfterFirstOnce) {
                playLastSongAfterFirstOnce = false;
            }

            if (repeatState == RepeatState.REPEAT_ONE) {
                resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                checkFileExistance(true);
            }
            else {
                if (checkIfShuffled()) {
                    currentlyPlayingSong         = allSongs.get(
                            currentPlaylist.get(currentlyPlayingSongOrder));
                    currentlyPlayingSongName     = currentlyPlayingSong.getData();
                    currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                    resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                    checkFileExistance(false);
                }
                else {
                    if (currentlyPlayingSongOrder < currentPlaylist.size() - 1) {
                        currentlyPlayingSong         = allSongs.get(currentPlaylist
                                .get(++currentlyPlayingSongOrder));
                        currentlyPlayingSongName     = currentlyPlayingSong.getData();
                        currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                        resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                        checkFileExistance(false);
                    }
                    else {
                        if (repeatState == RepeatState.REPEAT_ALL) {
                            currentlyPlayingSongOrder    = 0;
                            currentlyPlayingSong         = allSongs.get(currentPlaylist
                                    .get(currentlyPlayingSongOrder));
                            currentlyPlayingSongName     = currentlyPlayingSong.getData();
                            currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                            resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                            checkFileExistance(false);
                        }
                        if (repeatState == RepeatState.NO_REPEAT) {
                            if (!playFirstSongAfterLastOnce) {
                                playFirstSongAfterLastOnce = true;

                                showCustomToast(CommonStrings.PLAYLIST_END);
                            }
                            else {
                                currentlyPlayingSongOrder    = 0;
                                currentlyPlayingSong         = allSongs.get(currentPlaylist
                                        .get(currentlyPlayingSongOrder));
                                currentlyPlayingSongName     = currentlyPlayingSong.getData();
                                currentlyPlayingSongDuration = currentlyPlayingSong.getDuration();
                                resumePosition = isCheckPointSystemEnabled ? CHECKPOINT_PRIORITY : 0;

                                playFirstSongAfterLastOnce = false;
                                checkFileExistance(false);
                            }
                        }
                    }
                }
                if (playlists_menu_objectsAdapter != null) {
                    playlists_menu_objects.setItemChecked(
                            playlists_menu_objects.getCheckedItemPosition(), false);
                    playlists_menu_objectsAdapter.notifyDataSetChanged();
                }
                clear_search_menu_found_matches_Highlighting();
            }
        }
    }
    // Switches Play/pause button's images on Player screen and on Main menu
    void switchPlayPauseButtonImage() {
        if (isPlaying) {
            player_screen_play_pause_song_button.setImageDrawable(pause_button_image);
            main_menu_play_pause_song_button.setImageDrawable(pause_button_image);
        }
        else {
            player_screen_play_pause_song_button.setImageDrawable(play_button_image);
            main_menu_play_pause_song_button.setImageDrawable(play_button_image);
        }
    }
    // Method to reset the highlighting in search_menu
    void clear_search_menu_found_matches_Highlighting() {
        if (search_menu_found_matchesAdapter != null) {
            checkedSearchMenuGroupPosition         = -1;
            checkedSearchMenuItemPosition          = -1;
            checkedSearchMenuGroupName             = "";
            checkedSearchMenuGroupPositionInner    = -1;
            checkedSearchMenuItemPositionInner     = -1;
            search_menu_found_matchesAdapter.notifyDataSetChanged();
        }
    }

    // Seekbar and Timer related methods
    // Calculates duration in format 00:00:00
    String returnTimeIn_HH_MM_SS(int option) {
        String calculatedDuration = "";
        long   timeInMilliseconds;
        if (option == 2) {
            timeInMilliseconds = isCheckPointSystemEnabled ?
                    (int) currentlyPlayingSong.getCheckpointPosition() : 0;
        }
        else {
            if (option == 3) {
                timeInMilliseconds = resumePosition == CHECKPOINT_PRIORITY ?
                        (isCheckPointSystemEnabled ?
                                (int) currentlyPlayingSong.getCheckpointPosition() : 0) :
                        resumePosition;
            }
            else {
                timeInMilliseconds = option == 0 ?
                        currentlyPlayingSongDuration : gemMediaPlayer.getCurrentPosition();
            }
        }

        int hours = 0, minutes = 0, seconds = 0;
        if (timeInMilliseconds != 0) {
            hours   = (int) timeInMilliseconds / 3_600_000;
            minutes = (int) (timeInMilliseconds - (hours * 3_600_000)) / 60_000;
            seconds = (int) ((timeInMilliseconds - (hours * 3_600_000) - (minutes * 60_000)) / 1000);
        }

        if (option == 0) {
            if (hours >= 1) {
                currentlyPlayingSongDurationContainsHours = true;
            }
            if (minutes >= 1) {
                currentlyPlayingSongDurationContainsMinutes = true;
            }
        }

        if (currentlyPlayingSongDurationContainsHours) {
            calculatedDuration += (hours >= 1) ? String.valueOf(hours) : "0:";
        }
        if (currentlyPlayingSongDurationContainsMinutes) {
            calculatedDuration += (minutes / 10 >= 1) ?
                    String.valueOf(minutes) :
                    "0" + String.valueOf(minutes) + ":";
        }
        calculatedDuration += (seconds / 10 >= 1) ?
                String.valueOf(seconds) :
                "0" + String.valueOf(seconds);
        return calculatedDuration;
    }
    // Method to refresh seekbarState
    void refreshSeekbarState() {
        if (isPlaying) {
            seekbar.setProgress(
                    100 * gemMediaPlayer.getCurrentPosition() / currentlyPlayingSongDuration);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshSeekbarState();
                }
            }, (long) (1000 * 4 * seekbar.getChunksCount() /
                            Math.ceil((double) (currentlyPlayingSongDuration / 1000))));
        }
    }
    // Method to refresh timer score
    void refreshTimerScore() {
        if (isPlaying) {
            timer.setText(returnTimeIn_HH_MM_SS(1));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshTimerScore();
                }
            }, 1000);
        }
    }

    // Shuffle button click
    public void onShuffleButtonClick(View view) {
        isShuffled = !isShuffled;
        switchShuffleMode();
    }
    // Switches shuffle mode and it's button's image on Player screen and on Main Menu
    void switchShuffleMode() {
        if (isShuffled) {
            songsAlreadyPlayedInShuffleMode = new ArrayList<>();

            player_screen_shuffle_button.setImageDrawable(shuffle_button_image);
            main_menu_shuffle_button.setImageDrawable(shuffle_button_image);
        }
        else {
            songsAlreadyPlayedInShuffleMode = new ArrayList<>();

            player_screen_shuffle_button.setImageDrawable(straight_button_image);
            main_menu_shuffle_button.setImageDrawable(straight_button_image);
        }
    }
    // Check if shuffle mode is on
    boolean checkIfShuffled() {
        if (isShuffled) {
            int comparision;
            if (amountOfGuaranteedNonRepetitiveSongsWhileShuffling == 0) {
                songsAlreadyPlayedInShuffleMode = null;
                currentlyPlayingSongOrder       = (int) (currentPlaylist.size() * Math.random());
            }
            else {
                comparision =
                    amountOfGuaranteedNonRepetitiveSongsWhileShuffling < currentPlaylist.size() ?
                        amountOfGuaranteedNonRepetitiveSongsWhileShuffling : currentPlaylist.size();
                if (songsAlreadyPlayedInShuffleMode.size() == comparision) {
                    songsAlreadyPlayedInShuffleMode = new ArrayList<>();
                }
                int orderToPlay;
                while (true) {
                    orderToPlay = (int) (comparision * Math.random());
                    if (!songsAlreadyPlayedInShuffleMode.contains(orderToPlay)) break;
                }
                currentlyPlayingSongOrder = orderToPlay;
                songsAlreadyPlayedInShuffleMode.add(orderToPlay);
            }
            return true;
        }
        else {
            return false;
        }
    }

    // Repeat button click
    public void onRepeatButtonClick(View view) {
        switch (repeatState) {
            case NO_REPEAT:
                repeatState = RepeatState.REPEAT_ONE;
                break;
            case REPEAT_ONE:
                repeatState = RepeatState.REPEAT_ALL;
                break;
            case REPEAT_ALL:
                repeatState = RepeatState.NO_REPEAT;
                break;
        }
        switchRepeatMode();
    }
    // Switches repeat mode and it's button's image on Player screen and on Main menu
    void switchRepeatMode() {
        switch (repeatState) {
            case NO_REPEAT:
                player_screen_repeat_button.setImageDrawable(no_repeat_button_image);
                main_menu_repeat_button.setImageDrawable(no_repeat_button_image);
                break;
            case REPEAT_ONE:
                player_screen_repeat_button.setImageDrawable(repeat_one_button_image);
                main_menu_repeat_button.setImageDrawable(repeat_one_button_image);
                break;
            case REPEAT_ALL:
                player_screen_repeat_button.setImageDrawable(repeat_all_button_image);
                main_menu_repeat_button.setImageDrawable(repeat_all_button_image);
                break;
        }

        if (playLastSongAfterFirstOnce) {
            playLastSongAfterFirstOnce = false;
        }
        if (playFirstSongAfterLastOnce) {
            playFirstSongAfterLastOnce = false;
        }
    }

    // Shows Equalizer menu
    public void toggleEqualizerMenu(View view) {
        disableTextTickerEffect();
        hideKeyboard();
        if (!isMainMenuShown) showMainMenu();
        if (!isEqualizerMenuToggled) {
            main_menu_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[1]));
            main_menu_help_button.setImageDrawable(getImageFromAssets(imageNames[2]));
            main_menu_playlists_button.setImageDrawable(getImageFromAssets(imageNames[4]));
            main_menu_preferences_button.animate().scaleY(1).start();
            main_menu_search_button.setImageDrawable(getImageFromAssets(imageNames[12]));

            equalizer_menu.setVisibility(View.VISIBLE);
            help_menu.setVisibility(View.GONE);
            playlists_menu.setVisibility(View.GONE);
            preferences_menu.setVisibility(View.GONE);
            search_menu.setVisibility(View.GONE);

            isEqualizerMenuToggled   = true;
            isHelpMenuToggled        = false;
            isPlaylistsMenuToggled   = false;
            isPreferencesMenuToggled = false;
            isSearchMenuToggled      = false;
        }
        if (isHelpMenuLargeElementDescriptionShown) {
            hide_help_menu_element_description_large_layout();
        }
        if (isEqualizerMenuEnlarged) {
            enlargeMainMenu_sSubmenu();
        }
        else if (isHelpMenuEnlarged || isPlaylistsMenuEnlarged ||
                isPreferencesMenuEnlarged || isSearchMenuEnlarged) {
            decreaseMainMenu_sSubmenu();
        }
        if (isEnlargedAdditionalInfoLineShown) {
            hide_enlarged_additional_info_line_layout();
        }
        if (backInsteadOfExit || deepInPreferences) {
            switchBackButtonToExitButton();
        }
        if (isBackButtonHasLongClick) {
            main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[14]));
        }
    }

    // Shows Help menu
    public void toggleHelpMenu(View view) {
        disableTextTickerEffect();
        hideKeyboard();
        if (!isMainMenuShown) showMainMenu();
        if (!isHelpMenuToggled) {
            main_menu_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[0]));
            main_menu_help_button.setImageDrawable(getImageFromAssets(imageNames[3]));
            main_menu_playlists_button.setImageDrawable(getImageFromAssets(imageNames[4]));
            main_menu_preferences_button.animate().scaleY(1).start();
            main_menu_search_button.setImageDrawable(getImageFromAssets(imageNames[12]));

            equalizer_menu.setVisibility(View.GONE);
            help_menu.setVisibility(View.VISIBLE);
            playlists_menu.setVisibility(View.GONE);
            preferences_menu.setVisibility(View.GONE);
            search_menu.setVisibility(View.GONE);

            isEqualizerMenuToggled   = false;
            isHelpMenuToggled        = true;
            isPlaylistsMenuToggled   = false;
            isPreferencesMenuToggled = false;
            isSearchMenuToggled      = false;
        }
        if (isHelpMenuLargeElementDescriptionShown) {
            help_menu_element_description_large_layout.setVisibility(View.VISIBLE);
            help_menu_element_description_large_layout
                    .animate().alpha(1).setDuration(500).start();
        }
        if (isHelpMenuEnlarged) {
            enlargeMainMenu_sSubmenu();
        }
        else if (isEqualizerMenuEnlarged || isPlaylistsMenuEnlarged ||
                isPreferencesMenuEnlarged || isSearchMenuEnlarged) {
            decreaseMainMenu_sSubmenu();
        }
        if (isEnlargedAdditionalInfoLineShown) {
            hide_enlarged_additional_info_line_layout();
        }
        if (backInsteadOfExit || deepInPreferences) {
            switchBackButtonToExitButton();
        }
        if (isBackButtonHasLongClick) {
            main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[14]));
        }
    }

    // Shows Playlists menu
    public void togglePlaylistsMenu(View view) {
        disableTextTickerEffect();
        hideKeyboard();
        if (allSongs.isEmpty()) {
            showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);
        }
        else {
            if (!isMainMenuShown) showMainMenu();
            if (!isPlaylistsMenuToggled) {
                main_menu_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[0]));
                main_menu_help_button.setImageDrawable(getImageFromAssets(imageNames[2]));
                main_menu_playlists_button.setImageDrawable(getImageFromAssets(imageNames[5]));
                main_menu_preferences_button.animate().scaleY(1).start();
                main_menu_search_button.setImageDrawable(getImageFromAssets(imageNames[12]));

                equalizer_menu.setVisibility(View.GONE);
                help_menu.setVisibility(View.GONE);
                playlists_menu.setVisibility(View.VISIBLE);
                preferences_menu.setVisibility(View.GONE);
                search_menu.setVisibility(View.GONE);

                isEqualizerMenuToggled   = false;
                isHelpMenuToggled        = false;
                isPlaylistsMenuToggled   = true;
                isPreferencesMenuToggled = false;
                isSearchMenuToggled      = false;
            }
            if (isHelpMenuLargeElementDescriptionShown) {
                hide_help_menu_element_description_large_layout();
            }
            if (isPlaylistsMenuEnlarged) {
                enlargeMainMenu_sSubmenu();
            }
            else if (isEqualizerMenuEnlarged || isHelpMenuEnlarged ||
                    isPreferencesMenuEnlarged || isSearchMenuEnlarged) {
                decreaseMainMenu_sSubmenu();
            }
            if (isEnlargedAdditionalInfoLineShown) {
                hide_enlarged_additional_info_line_layout();
            }
            if (deepInPreferences) {
                main_menu_exit_and_back_button.setOnClickListener(hideMainMenuOrCover);
                main_menu_exit_and_back_button.animate().rotation(0).start();
            }
            if (backInsteadOfExit) {
                switchExitButtonToBackButton();
            }
            if (isBackButtonHasLongClick) {
                main_menu_exit_and_back_button.setOnLongClickListener(backButtonLongClickListener);
                main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[15]));
            }
        }
    }

    // Shows equalizer menu
    public void togglePreferencesMenu(View view) {
        disableTextTickerEffect();
        hideKeyboard();
        if (!isMainMenuShown) showMainMenu();
        if (!isPreferencesMenuToggled) {
            main_menu_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[0]));
            main_menu_help_button.setImageDrawable(getImageFromAssets(imageNames[2]));
            main_menu_playlists_button.setImageDrawable(getImageFromAssets(imageNames[4]));
            main_menu_preferences_button.animate().scaleY(-1).start();
            main_menu_search_button.setImageDrawable(getImageFromAssets(imageNames[12]));

            equalizer_menu.setVisibility(View.GONE);
            help_menu.setVisibility(View.GONE);
            playlists_menu.setVisibility(View.GONE);
            preferences_menu.setVisibility(View.VISIBLE);
            search_menu.setVisibility(View.GONE);

            isEqualizerMenuToggled        = false;
            isHelpMenuToggled             = false;
            isPlaylistsMenuToggled        = false;
            isPreferencesMenuToggled      = true;
            isSearchMenuToggled           = false;
        }
        if (isHelpMenuLargeElementDescriptionShown) {
            hide_help_menu_element_description_large_layout();
        }
        if (isPreferencesMenuEnlarged) {
            enlargeMainMenu_sSubmenu();
        }
        else if (isEqualizerMenuEnlarged || isHelpMenuEnlarged ||
                isPlaylistsMenuEnlarged || isSearchMenuEnlarged) {
            decreaseMainMenu_sSubmenu();
        }
        if (isEnlargedAdditionalInfoLineShown) {
            hide_enlarged_additional_info_line_layout();
        }
        if (backInsteadOfExit) {
            switchBackButtonToExitButton();
        }
        if (isBackButtonHasLongClick) {
            main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[14]));
        }
        if (deepInPreferences) {
            main_menu_exit_and_back_button.setOnClickListener(backToPreferences);
            main_menu_exit_and_back_button.animate().rotation(90).start();
        }
    }

    // Shows search menu and initializes it's elements
    public void toggleSearchMenu(View view) {
        disableTextTickerEffect();
        hideKeyboard();
        if (allSongs.isEmpty()) {
            showCustomToast(CommonStrings.NO_SONGS_ON_DEVICE);
        }
        else {
            if (!isMainMenuShown) showMainMenu();
            if (!isSearchMenuToggled) {
                main_menu_equalizer_button.setImageDrawable(getImageFromAssets(imageNames[0]));
                main_menu_help_button.setImageDrawable(getImageFromAssets(imageNames[2]));
                main_menu_playlists_button.setImageDrawable(getImageFromAssets(imageNames[4]));
                main_menu_preferences_button.animate().scaleY(1).start();
                main_menu_search_button.setImageDrawable(getImageFromAssets(imageNames[13]));

                equalizer_menu.setVisibility(View.GONE);
                help_menu.setVisibility(View.GONE);
                playlists_menu.setVisibility(View.GONE);
                preferences_menu.setVisibility(View.GONE);
                search_menu.setVisibility(View.VISIBLE);

                isEqualizerMenuToggled   = false;
                isHelpMenuToggled        = false;
                isPlaylistsMenuToggled   = false;
                isPreferencesMenuToggled = false;
                isSearchMenuToggled      = true;
            }
            if (isHelpMenuLargeElementDescriptionShown) {
                hide_help_menu_element_description_large_layout();
            }
            if (isSearchMenuEnlarged) {
                enlargeMainMenu_sSubmenu();
            }
            else if (isEqualizerMenuEnlarged || isHelpMenuEnlarged ||
                    isPlaylistsMenuEnlarged || isPreferencesMenuEnlarged) {
                decreaseMainMenu_sSubmenu();
            }
            if (isEnlargedAdditionalInfoLineShown) {
                hide_enlarged_additional_info_line_layout();
            }
            if (backInsteadOfExit || deepInPreferences) {
                switchBackButtonToExitButton();
            }
            if (isBackButtonHasLongClick) {
                main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[14]));
            }
        }
    }

    // Search engine - Search menu Confirm button click
    public void confirm(View view) {
        uiHandler.sendEmptyMessage(FILL_SEARCH_RESULTS);
    }

    // Preferences menu methods
    public void showAllThemes(View view) {
        preferences_menu_header.setVisibility(View.VISIBLE);
        preferences_menu_header_divider.setVisibility(View.VISIBLE);
        preferences_menu_header.setText(
                R.string.preferences_menu_header_text_themes);
        preferences_menu_elements.setVisibility(View.GONE);
        gem_themes.setVisibility(View.VISIBLE);
        whereAreWePreferences = SHOW_ALL_GEM_THEMES;
        main_menu_exit_and_back_button.setOnClickListener(backToPreferences);
        main_menu_exit_and_back_button.animate().rotation(90).start();
        deepInPreferences = true;
    }
    public void showAdditionalInfoLineOptions(View view) {
        preferences_menu_header.setVisibility(View.VISIBLE);
        preferences_menu_header_divider.setVisibility(View.VISIBLE);
        preferences_menu_header.setText(
                R.string.preferences_menu_header_text_additional_info_line_options);
        preferences_menu_elements.setVisibility(View.GONE);
        additional_info_line_options.setVisibility(View.VISIBLE);
        whereAreWePreferences = SHOW_ADDITIONAL_INFO_LINE_OPTIONS;
        main_menu_exit_and_back_button.setOnClickListener(backToPreferences);
        main_menu_exit_and_back_button.animate().rotation(90).start();
        deepInPreferences = true;
    }
    // Back button onClickListener for Preferences menu
    private View.OnClickListener backToPreferences = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (whereAreWePreferences == SHOW_ALL_GEM_THEMES) {
                gem_themes.setVisibility(View.GONE);
            }
            if (whereAreWePreferences == SHOW_ADDITIONAL_INFO_LINE_OPTIONS) {
                additional_info_line_options.setVisibility(View.GONE);
            }
            preferences_menu_header.setVisibility(View.GONE);
            preferences_menu_header_divider.setVisibility(View.GONE);
            preferences_menu_elements.setVisibility(View.VISIBLE);
            main_menu_exit_and_back_button.setOnClickListener(hideMainMenuOrCover);
            main_menu_exit_and_back_button.animate().rotation(0).start();
            deepInPreferences = false;
        }
    };

    // This is what happened with Main menu Exit button, when user clicks on any of the playlists
    // from All playlists menu:
    // It starts to act as Back button and in some cases get long click action listener
    // to back straight to All playlists menu (according to whereWeAre value)
    void switchExitButtonToBackButton() {
        main_menu_exit_and_back_button.setOnClickListener(backButtonClick);
        main_menu_exit_and_back_button.animate().rotation(90).start();
        backInsteadOfExit = true;
    }
    // Custom Handler to hide confirm_button, being in USER PLAYLISTS
    private Handler confirmButtonHideoutHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            playlists_menu_confirm_button.setOnClickListener(null);
            playlists_menu_confirm_button.animate().alpha(0)
                    .withStartAction(new Runnable() {
                        @Override
                        public void run() {
                            playlists_menu_additional_info_line.setVisibility(View.INVISIBLE);
                        }
                    })
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            playlists_menu_confirm_button.setVisibility(View.GONE);
                            playlists_menu_additional_info_line.setVisibility(View.VISIBLE);
                            playlists_menu_objects.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                        }
                    }).start();
            return true;
        }
    });
    // New Exit button, transformed into Back button OnClickListener
    private View.OnClickListener backButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (inSongsAdditionProcedure) {
                switch (whereAreWeAddition) {
                    case SHORT_WAY:
                    case STANDART_WAY_STEP_1:
                    case ADDITION_COMPLETED:
                        confirmButtonHideoutHandler.sendEmptyMessage(0);
                        playlists_menu_objects
                                .setOnItemLongClickListener(requestUserPlaylistsAddition);
                        playlists_menu_objects.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                        isShortWayAvailable      = false;
                        inSongsAdditionProcedure = false;
                        whereAreWeAddition       = 0;
                        if (whereAreWe == FILL_CURRENT_PLAYLIST_SONGS) {
                            playlists_menu_additional_info_line.setText(
                                    res.getString(R.string.current_playlist,
                                            currentPlaylistName
                                                    .replace("\n", "")
                                                    .replace("\t", "")));
                        }
                        if (whereAreWe == FILL_CURRENT_PLAYLIST_SONGS ||
                                whereAreWe == FILL_ALL_SONGS ||
                                whereAreWe == FILL_ALL_ARTIST_NAMES ||
                                whereAreWe == FILL_ALL_ALBUM_TITLES ||
                                whereAreWe == FILL_ALL_GENRE_NAMES) {
                            removeOnLongClickListenerFromBackButton();
                        }
                        if (needToGoToUserPlaylists) {
                            uiHandler.sendEmptyMessage(FILL_ALL_USER_PLAYLISTS_NAMES);
                        }
                        else {
                            uiHandler.sendEmptyMessage(whereAreWe);
                        }
                        break;
                    case STANDART_WAY_STEP_2:
                        playlists_menu_objectsAdapter = playlists_menu_objectsCopy;
                        playlists_menu_objects.setAdapter(playlists_menu_objectsAdapter);
                        playlists_menu_objects.onRestoreInstanceState(playlists_menu_objectsState);
                        playlists_menu_objectsAdapter.notifyDataSetChanged();
                        transform_playlists_menu_objects_inSongsAdditionProcedure();
                        break;
                }
            }
            else {
                if (whereAreWe == CHOSEN_USER_PLAYLIST_IS_EMPTY ||
                        whereAreWe == DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST ||
                        whereAreWe == ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST ||
                        whereAreWe == MERGE_USER_PLAYLISTS) {
                    confirmButtonHideoutHandler.sendEmptyMessage(0);
                }
                if (backOption == FILL_ALL_ARTIST_NAMES ||
                        backOption == FILL_ALL_ALBUM_TITLES ||
                        backOption == FILL_ALL_GENRE_NAMES ||
                        backOption == FILL_ALL_USER_PLAYLISTS_NAMES) {
                    removeOnLongClickListenerFromBackButton();
                }

                if (whereAreWe == FILL_CHOSEN_USER_PLAYLIST_SONGS) {
                    firstVisiblePositionsUser.put(
                            allUserPlaylistsNames.indexOf(userPlaylistToShow),
                            playlists_menu_objects.getFirstVisiblePosition());
                }
                else {
                    if (firstVisiblePositions.indexOfKey(whereAreWe) > 0) {
                        firstVisiblePositions.put(
                                whereAreWe, playlists_menu_objects.getFirstVisiblePosition());
                    }
                }

                if (needToBackToSpecificAlbumSongs) {
                    if (!albumToShowCopy.equals(albumToShow)) {
                        albumToShow                    = albumToShowCopy;
                        needToBackToSpecificAlbumSongs = false;
                        albumToShowCopy                = "";
                        firstVisiblePositions.put(FILL_SPECIFIC_ALBUM_SONGS,
                                firstVisiblePositionBackup);
                        firstVisiblePositionBackup     = -1;
                        uiHandler.sendEmptyMessage(FILL_SPECIFIC_ALBUM_SONGS);
                    }
                    else {
                        needToBackToSpecificAlbumSongs = false;
                        albumToShowCopy                = "";
                        firstVisiblePositionBackup     = -1;
                        uiHandler.sendEmptyMessage(FILL_ALL_ALBUM_TITLES);
                    }
                }
                else {
                    uiHandler.sendEmptyMessage(backOption);
                }
            }
        }
    };
    // Methods to add OnLongClickListener to Back button and change it's image...
    void addOnLongClickListenerToBackButton() {
        main_menu_exit_and_back_button.setOnLongClickListener(backButtonLongClickListener);
        isBackButtonHasLongClick = true;
        main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[15]));
    }
    // and to perform reverse action
    void removeOnLongClickListenerFromBackButton() {
        main_menu_exit_and_back_button.setOnLongClickListener(null);
        isBackButtonHasLongClick = false;
        main_menu_exit_and_back_button.setImageDrawable(getImageFromAssets(imageNames[14]));
    }
    // New Exit button, transformed into Back button OnLongClickListener
    View.OnLongClickListener backButtonLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (inSongsAdditionProcedure) {
                confirmButtonHideoutHandler.sendEmptyMessage(0);
                playlists_menu_objects
                        .setOnItemLongClickListener(requestUserPlaylistsAddition);
                playlists_menu_objects.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                isShortWayAvailable      = false;
                inSongsAdditionProcedure = false;
                whereAreWeAddition       = 0;
            }

            if (needToMerge) {
                needToMerge = false;
            }

            if (whereAreWe == FILL_CURRENT_PLAYLIST_SONGS) {
                playlists_menu_additional_info_line.setText(
                        res.getString(R.string.current_playlist,
                                currentPlaylistName
                                        .replace("\n", "")
                                        .replace("\t", "")));
            }
            if (whereAreWe == CHOSEN_USER_PLAYLIST_IS_EMPTY ||
                    whereAreWe == DELETE_MULTIPLE_SONGS_FROM_USER_PLAYLIST ||
                    whereAreWe == ADD_MULTIPLE_SONGS_TO_USER_PLAYLIST ||
                    whereAreWe == MERGE_USER_PLAYLISTS) {
                confirmButtonHideoutHandler.sendEmptyMessage(0);
            }

            removeOnLongClickListenerFromBackButton();

            if (whereAreWe == FILL_CHOSEN_USER_PLAYLIST_SONGS) {
                firstVisiblePositionsUser.put(
                        allUserPlaylistsNames.indexOf(userPlaylistToShow),
                        playlists_menu_objects.getFirstVisiblePosition());
            }
            else {
                if (firstVisiblePositions.indexOfKey(whereAreWe) > 0) {
                    firstVisiblePositions.put(
                            whereAreWe, playlists_menu_objects.getFirstVisiblePosition());
                }
            }

            uiHandler.sendEmptyMessage(FILL_PLAYLISTS_OPTIONS);
            return true;
        }
    };
    // Switch Back button to Exit button
    void switchBackButtonToExitButton() {
        main_menu_exit_and_back_button.setOnClickListener(hideMainMenuOrCover);
        main_menu_exit_and_back_button.setOnLongClickListener(null);
        main_menu_exit_and_back_button.animate().rotation(0).start();
    }
    // Finally: setting Android system "Back" button functionality
    @Override
    public void onBackPressed()
    {
        if (isCustomToastShown) {
            custom_toast_close_button.callOnClick();
        }
        else if (isCustomDialogShown) {
            custom_dialog_cancel_button.callOnClick();
        }
        else {
            main_menu_exit_and_back_button.callOnClick();
        }
    }
    /*tbd*/
    /*// And it's longClick
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            main_menu_exit_and_back_button.performLongClick();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }*/

    // Method to hide soft keyboard from screen (just for code clarity)
    void hideKeyboard() {
        try {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // OnClickListener, given to Shadow and at Main menu Exit button
    View.OnClickListener hideMainMenuOrCover = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isCoverEnlarged) {
                hideEnlargedCover(view);
            }
            else {
                disableTextTickerEffect();
                hideKeyboard();
                if (isEnlargedAdditionalInfoLineShown) {
                    enlarged_additional_info_line_decrease_button.performClick();
                }
                if (isMainMenuShown) {
                    hideMainMenu();
                    main_menu_exit_and_back_button.setOnClickListener(hideMainMenuOrCover);
                }
            }
        }
    };

    // Shows enlarged cover
    public void showEnlargedCover(View view) {
        cover_button_small.animate().alpha(0).setDuration(500).start();
        shadow.setVisibility(View.VISIBLE);
        shadow.animate().alpha(1).setDuration(500).start();
        cover_button_large.setVisibility(View.VISIBLE);
        cover_button_large.animate().alpha(1).setDuration(500).start();

        isCoverEnlarged = true;
    }
    // Hides enlarged cover
    public void hideEnlargedCover(View view) {
        cover_button_large.animate().alpha(0).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                cover_button_large.setVisibility(View.GONE);
            }
        }).start();

        shadow.animate().alpha(0).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                shadow.setVisibility(View.GONE);
            }
        }).start();

        cover_button_small.animate().alpha(1).setDuration(500).start();

        isCoverEnlarged = false;
    }

    // Shows Main menu and initializes it's elements
    void showMainMenu() {
        player_screen.animate().alpha(0).setDuration(500).start();

        shadow.setVisibility(View.VISIBLE);
        shadow.animate().alpha(1).setDuration(500).start();

        main_menu.setVisibility(View.VISIBLE);
        main_menu.animate().alpha(1).setDuration(500).start();

        isMainMenuShown = true;
    }
    // Hides Main menu and nullifies it's elements
    void hideMainMenu() {
        if (isHelpMenuLargeElementDescriptionShown) {
            hide_help_menu_element_description_large_layout();
        }

        main_menu.animate().alpha(0).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                main_menu.setVisibility(View.GONE);
                if (isHelpMenuToggled) {
                    isHelpMenuToggled = false;
                }
                if (isEqualizerMenuToggled) {
                    isEqualizerMenuToggled = false;
                }
                if (isPlaylistsMenuToggled) {
                    isPlaylistsMenuToggled = false;
                }
                if (isPreferencesMenuToggled) {
                    isPreferencesMenuToggled = false;
                }
                if (isSearchMenuToggled) {
                    isSearchMenuToggled = false;
                }
            }
        }).start();

        shadow.animate().alpha(0).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                shadow.setVisibility(View.GONE);
            }
        }).start();

        player_screen.animate().alpha(1).setDuration(500).start();

        isMainMenuShown = false;
    }
    // Show large Main menu submenu
    void enlargeMainMenu_sSubmenu() {
        main_menu_menu_s_buttons.setVisibility(View.GONE);
        if (needToShowSongDetailsOnMainMenu) {
            main_menu_song_details.setVisibility(View.GONE);
        }
        if (needToShowPlaybackControlsOnMainMenu) {
            main_menu_playback_controls_1.setVisibility(View.GONE);
            main_menu_shuffle_button.setVisibility(View.GONE);
            main_menu_repeat_button.setVisibility(View.GONE);
        }
        main_menu_playback_controls_2_space_1.setVisibility(View.GONE);
        main_menu_playback_controls_2_space_2.setVisibility(View.GONE);
        main_menu_playback_controls_2_space_3.setVisibility(View.GONE);
        main_menu_playback_controls_2_space_4.setVisibility(View.GONE);
        main_menu_playback_controls_2.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.125f));
        main_menu_s_enlarged_submenu_decrease_button.setVisibility(View.VISIBLE);
    }
    // Hide, but not close previously enlarged Main menu submenu
    void decreaseMainMenu_sSubmenu() {
        main_menu_menu_s_buttons.setVisibility(View.VISIBLE);
        if (needToShowSongDetailsOnMainMenu) {
            main_menu_song_details.setVisibility(View.VISIBLE);
        }
        else {
            main_menu_song_details.setVisibility(View.GONE);
        }
        if (needToShowPlaybackControlsOnMainMenu) {
            main_menu_playback_controls_1.setVisibility(View.VISIBLE);
            main_menu_exit_and_back_button.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            main_menu_shuffle_button.setVisibility(View.VISIBLE);
            main_menu_repeat_button.setVisibility(View.VISIBLE);
        }
        else {
            main_menu_playback_controls_1.setVisibility(View.GONE);
            main_menu_exit_and_back_button.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));
            main_menu_shuffle_button.setVisibility(View.GONE);
            main_menu_repeat_button.setVisibility(View.GONE);
        }
        main_menu_playback_controls_2_space_1.setVisibility(View.VISIBLE);
        main_menu_playback_controls_2_space_2.setVisibility(View.VISIBLE);
        main_menu_playback_controls_2_space_3.setVisibility(View.VISIBLE);
        main_menu_playback_controls_2_space_4.setVisibility(View.VISIBLE);
        main_menu_playback_controls_2.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.25f));
        main_menu_s_enlarged_submenu_decrease_button.setVisibility(View.GONE);
    }

    // Method to hide enlarged_additional_info_line_layout (just for code clarity)
    void hide_enlarged_additional_info_line_layout() {
        enlarged_additional_info_line_layout.animate().alpha(0).setDuration(500)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        enlarged_additional_info_line_layout.setVisibility(View.GONE);
                        isEnlargedAdditionalInfoLineShown = false;
                    }
                }).start();
        player_screen.animate().alpha(1).setDuration(500).start();
    }
    // Same, but for help_menu_element_description_large_layout
    void hide_help_menu_element_description_large_layout() {
        help_menu_element_description_large_layout.animate().alpha(0).setDuration(500)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        help_menu_element_description_large_layout.setVisibility(View.GONE);
                    }
                }).start();
    }

    // Method, used to change scrollbar color of given view, also sets it's size
    public void changeScrollbarColor(View view) {
        try
        {
            Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
            mScrollCacheField.setAccessible(true);
            Object mScrollCache = mScrollCacheField.get(view);

            Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
            scrollBarField.setAccessible(true);
            Object scrollBar = scrollBarField.get(mScrollCache);

            Method method = scrollBar.getClass()
                    .getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
            method.setAccessible(true);

            method.invoke(scrollBar, new ColorDrawable(themeColor2));

            view.setScrollBarSize(4 * (int) displayMetrics.density);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    // Method to return currentlyPlayingSong file as byte[] for seekbar usage
    byte[] analyzeSong(boolean isSongExists) {
        if (isSongExists) {
            File file = new File(currentlyPlayingSongName);
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(bytes);
                return bytes;
            }
            catch (Exception e) {
                e.printStackTrace();
                return new byte[0];
            }
        }
        else {
            return new byte[0];
        }
    }

    // OnClickListener, given to checkpoint_system_button - sets checkpoint if song is paused
    public void setCheckPoint(View view) {
        if (currentlyPlayingSong != null) {
            if (gemMediaPlayerIsPrepared) {
                currentlyPlayingSong.setCheckpointPosition(gemMediaPlayer.getCurrentPosition());
            }
            else {
                currentlyPlayingSong.setCheckpointPosition(0);
            }
            currently_playing_song_checkpoint_time.setText(returnTimeIn_HH_MM_SS(2));
            showCustomToast(CommonStrings.CHECKPOINT_WAS_SET);
        }
    }

    // Empty onClickListener, to avoid some unnecessary clicks
    public void screenLock(View view) {
    }
}