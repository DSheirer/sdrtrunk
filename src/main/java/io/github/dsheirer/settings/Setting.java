/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.settings;

import java.util.prefs.Preferences;

/**
 * Every value that the application stores in the java.util.prefs user preferences store.
 *
 * Each setting names the preferences node that holds it and its key within that node.  The node paths are the paths
 * used before the settings classes moved to this package (they were derived from the package of the class that stored
 * the value), so that values stored by earlier versions are still found.  Don't change a node path or a key without
 * migrating the stored values, or users lose their settings.
 *
 * Keys that contain {@code %s} are templates that are completed with one or more arguments, for example a protocol,
 * a stage name or a table name.  Use {@link #with(Object...)} to access the value of a templated setting, and the
 * value accessors on this enumeration for a fixed key.
 */
public enum Setting
{
    //Application
    APPLICATION_AUTOMATIC_DIAGNOSTIC_MONITORING(Node.APPLICATION, "automatic.diagnostic.monitoring"),
    APPLICATION_CHANNEL_AUTO_START_TIMEOUT(Node.APPLICATION, "channel.auto.start.timeout"),
    APPLICATION_COMPLEMENTARY_ALIAS_COLORS(Node.APPLICATION, "ui.complementary.alias.colors"),
    APPLICATION_DARK_MODE(Node.APPLICATION, "dark.mode"),
    APPLICATION_GUI_SCALE(Node.APPLICATION, "ui.gui.scale"),
    APPLICATION_THEME(Node.APPLICATION, "ui.theme"),

    //Call management and duplicate call detection
    CALL_DETECT_DUPLICATE_RADIO(Node.CALL_MANAGEMENT, "duplicate.call.detect.radio"),
    CALL_DETECT_DUPLICATE_TALKGROUP(Node.CALL_MANAGEMENT, "duplicate.call.detect.talkgroup"),
    CALL_PATCHGROUP_STREAMING(Node.CALL_MANAGEMENT, "patchgroup.streaming"),
    CALL_SUPPRESS_DUPLICATE_PLAYBACK(Node.CALL_MANAGEMENT, "suppress.duplicate.audio.playback"),
    CALL_SUPPRESS_DUPLICATE_RECORDING(Node.CALL_MANAGEMENT, "suppress.duplicate.audio.recording"),
    CALL_SUPPRESS_DUPLICATE_STREAMING(Node.CALL_MANAGEMENT, "suppress.duplicate.audio.streaming"),

    //Decode events
    DECODE_EVENT_TIMESTAMP_FORMAT(Node.DECODE_EVENT, "timestamp.format"),

    //Directories
    DIRECTORY_APPLICATION_LOGS(Node.DIRECTORY, "directory.application.logs"),
    DIRECTORY_APPLICATION_ROOT(Node.DIRECTORY, "directory.application.root"),
    DIRECTORY_CONFIGURATION(Node.DIRECTORY, "directory.configuration"),
    DIRECTORY_EVENT_LOGS(Node.DIRECTORY, "directory.event.logs"),
    DIRECTORY_JMBE(Node.DIRECTORY, "directory.jmbe"),
    DIRECTORY_MAX_USAGE_EVENT_LOGS(Node.DIRECTORY, "directory.max.usage.event.logs"),
    DIRECTORY_MAX_USAGE_RECORDINGS(Node.DIRECTORY, "directory.max.usage.recordings"),
    DIRECTORY_PLAYLIST(Node.DIRECTORY, "directory.playlist"),
    DIRECTORY_RECORDING(Node.DIRECTORY, "directory.recording"),
    DIRECTORY_SCREEN_CAPTURE(Node.DIRECTORY, "directory.screen.capture"),
    DIRECTORY_STREAMING(Node.DIRECTORY, "directory.streaming"),

    //JavaFX stage size and location: argument is the {@link Window} key
    JAVAFX_STAGE_HEIGHT(Node.JAVAFX, "stage.height.%s"),
    JAVAFX_STAGE_MAXIMIZED(Node.JAVAFX, "state.maximized%s"),
    JAVAFX_STAGE_WIDTH(Node.JAVAFX, "stage.width.%s"),
    JAVAFX_STAGE_X(Node.JAVAFX, "stage.x.%s"),
    JAVAFX_STAGE_Y(Node.JAVAFX, "stage.y.%s"),

    //JMBE library
    JMBE_LIBRARY_ALERT_REQUIRED(Node.JMBE_LIBRARY, "alert.jmbe.required"),
    JMBE_LIBRARY_PATH(Node.JMBE_LIBRARY, "path.jmbe.library.1.0.0"),

    //Main window
    MAIN_WINDOW_BROADCAST_DIVIDER(Node.GUI, "sdrtrunk.main.window.streaming.fraction"),
    MAIN_WINDOW_BROADCAST_STATUS_VISIBLE(Node.GUI, "sdrtrunk.broadcast.status.visible"),
    MAIN_WINDOW_NOW_PLAYING_DETAILS_VISIBLE(Node.GUI, "sdrtrunk.now.playing.details.visible"),
    MAIN_WINDOW_RESOURCE_STATUS_VISIBLE(Node.GUI, "sdrtrunk.resource.status.visible"),
    MAIN_WINDOW_SPECTRAL_DISPLAY_VISIBLE(Node.GUI, "sdrtrunk.spectral.display.visible"),
    MAIN_WINDOW_SPECTRAL_DIVIDER(Node.GUI, "sdrtrunk.main.window.spectral.fraction"),

    //Map
    MAP_CENTER(Node.MAP, "center"),
    MAP_SHOW_RADIO_REFERENCE_SITES(Node.MAP, "show.radioreference.sites"),
    MAP_TILE_SERVER(Node.MAP, "tile.server"),
    MAP_TILE_SERVER_KEY(Node.MAP, "tile.server.key.%s"), //Argument: tile server id
    MAP_TILE_STYLE(Node.MAP, "tile.style"),
    MAP_ZOOM(Node.MAP, "zoom"),

    //MP3 encoding
    MP3_NORMALIZE_BEFORE_ENCODE(Node.MP3, "audio.mp3.normalize.before.encode"),
    MP3_SAMPLE_RATE(Node.MP3, "audio.mp3.sample.rate"),
    MP3_SETTING(Node.MP3, "audio.mp3.setting"),

    //Audio playback
    PLAYBACK_AUDIO_CHANNEL_COUNT(Node.PLAYBACK, "audio.playback.channel.count"),
    PLAYBACK_AUDIO_DEVICE_NAME(Node.PLAYBACK, "audio.playback.device.name"),
    PLAYBACK_DROP_TONE_FREQUENCY(Node.PLAYBACK, "audio.playback.segment.drop.frequency"),
    PLAYBACK_DROP_TONE_VOLUME(Node.PLAYBACK, "audio.playback.segment.drop.volume"),
    PLAYBACK_START_TONE_FREQUENCY(Node.PLAYBACK, "audio.playback.segment.start.frequency"),
    PLAYBACK_START_TONE_VOLUME(Node.PLAYBACK, "audio.playback.segment.start.volume"),
    PLAYBACK_USE_DROP_TONE(Node.PLAYBACK, "audio.playback.segment.drop.tone"),
    PLAYBACK_USE_START_TONE(Node.PLAYBACK, "audio.playback.segment.start.tone"),

    //Playlists
    PLAYLIST_LIST(Node.PLAYLIST, "playlist.list"),
    PLAYLIST_PATH(Node.PLAYLIST, "playlist.path"),

    //Radio Reference
    RADIO_REFERENCE_ALIAS_LIST_UPDATE_INTERVAL(Node.RADIO_REFERENCE, "alias.list.update.interval"),
    RADIO_REFERENCE_CREATE_AND_SHOW_CHANNEL_EDITOR(Node.RADIO_REFERENCE, "create.and.show.channel.editor"),
    RADIO_REFERENCE_ENCRYPTED_TALKGROUP_DO_NOT_MONITOR(Node.RADIO_REFERENCE, "encrypted.talkgroup.import.do.not.monitor"),
    RADIO_REFERENCE_LOCATION(Node.RADIO_REFERENCE, "location"),
    RADIO_REFERENCE_LOCATION_LABEL(Node.RADIO_REFERENCE, "location.label"),
    RADIO_REFERENCE_PASSWORD(Node.RADIO_REFERENCE, "user.authorization"),
    RADIO_REFERENCE_PREFERRED_AGENCY_COUNTY(Node.RADIO_REFERENCE, "preferred.agency.county"),
    RADIO_REFERENCE_PREFERRED_AGENCY_NATIONAL(Node.RADIO_REFERENCE, "preferred.agency.national"),
    RADIO_REFERENCE_PREFERRED_AGENCY_STATE(Node.RADIO_REFERENCE, "preferred.agency.state"),
    RADIO_REFERENCE_PREFERRED_COUNTRY(Node.RADIO_REFERENCE, "preferred.country"),
    RADIO_REFERENCE_PREFERRED_COUNTY(Node.RADIO_REFERENCE, "preferred.county"),
    RADIO_REFERENCE_PREFERRED_STATE(Node.RADIO_REFERENCE, "preferred.state"),
    RADIO_REFERENCE_PREFERRED_SYSTEM_COUNTY(Node.RADIO_REFERENCE, "preferred.system.county"),
    RADIO_REFERENCE_PREFERRED_SYSTEM_STATE(Node.RADIO_REFERENCE, "preferred.system.state"),
    RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_COUNTY(Node.RADIO_REFERENCE, "create.and.show.editor.county"),
    RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_NATIONAL(Node.RADIO_REFERENCE, "create.and.show.editor.national"),
    RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_STATE(Node.RADIO_REFERENCE, "create.and.show.editor.state"),
    RADIO_REFERENCE_SHOW_PASSWORD(Node.RADIO_REFERENCE, "show.password"),
    RADIO_REFERENCE_STORE_CREDENTIALS(Node.RADIO_REFERENCE, "store.credentials"),
    RADIO_REFERENCE_USER_NAME(Node.RADIO_REFERENCE, "user.name"),

    //Recent files menus: arguments are the menu identifier and the recent file index
    RECENT_FILE(Node.GUI, "%s.recent.file.%s"),

    //Settings dialog
    SETTINGS_EDITOR_LAST_PAGE(Node.GUI, "settings.editor.last.page"),

    //Recording
    RECORD_AUDIO_FORMAT(Node.RECORD, "audio.record.format"),
    RECORD_BASEBAND_FORMAT(Node.RECORD, "baseband.record.format"),

    //Swing window and table state stored by earlier versions, read only to migrate it: argument is the swing key
    SWING_COLUMN_WIDTH(Node.SWING, "%s.column.%s"), //Arguments: table key and legacy column index
    SWING_HEIGHT(Node.SWING, "%s.height"),
    SWING_MAXIMIZED(Node.SWING, "%s.maximized"),
    SWING_WIDTH(Node.SWING, "%s.width"),
    SWING_X(Node.SWING, "%s.x"),
    SWING_Y(Node.SWING, "%s.y"),

    //JavaFX table columns: first argument is the table key
    TABLE_COLUMN_HIDDEN(Node.TABLE, "%s.column.hidden"),
    TABLE_COLUMN_KNOWN(Node.TABLE, "%s.column.known"),
    TABLE_COLUMN_ORDER(Node.TABLE, "%s.column.order"),
    TABLE_COLUMN_WIDTH(Node.TABLE, "%s.column.width.%s"), //Arguments: table key and column id

    //Talkgroup formats: argument is the protocol name
    TALKGROUP_FIXED_WIDTH(Node.TALKGROUP_FORMAT, "talkgroup.fixed.width.%s"),
    TALKGROUP_FORMAT(Node.TALKGROUP_FORMAT, "talkgroup.format.%s"),

    //Tuners
    TUNER_CHANNELIZER_TYPE(Node.SOURCE, "channelizer.type"),
    TUNER_RSP_DUO_TUNER_MODE(Node.SOURCE, "rsp.duo.tuner.mode"),

    //Vector (SIMD) calibration
    VECTOR_CALIBRATION(Node.VECTOR_CALIBRATE, "%s"), //Argument: calibration type key
    VECTOR_CALIBRATION_ENVIRONMENT(Node.VECTOR_CALIBRATION, "calibration.environment"),
    VECTOR_CALIBRATION_RESULT(Node.VECTOR_CALIBRATE_RESULT, "%s"), //Argument: calibration type key
    VECTOR_ENABLED(Node.VECTOR_CALIBRATION, "vector.enabled"),
    VECTOR_HIDE_CALIBRATION_DIALOG(Node.VECTOR_CALIBRATION, "hide.calibration.dialog"),

    //Message recording viewers
    VIEWER_DMR_LAST_DIRECTORY(Node.VIEWER, "last.selected.directory.dmr"),
    VIEWER_NXDN_LAST_DIRECTORY(Node.VIEWER, "last.selected.directory.nxdn"),
    VIEWER_NXDN_LAST_FILE(Node.VIEWER, "last.selected.file.nxdn"),
    VIEWER_P25P1_LAST_DIRECTORY(Node.VIEWER, "last.selected.directory.p25p1"),
    VIEWER_P25P2_LAST_DIRECTORY(Node.VIEWER, "last.selected.directory.p25p2"),
    VIEWER_P25P2_LAST_NAC(Node.VIEWER, "last.nac.value.p25p2"),
    VIEWER_P25P2_LAST_SYSTEM(Node.VIEWER, "last.system.value.p25p2"),
    VIEWER_P25P2_LAST_WACN(Node.VIEWER, "last.wacn.value.p25p2");

    private static final String TEMPLATE_ARGUMENT = "%s";
    private final String mNodePath;
    private final String mKey;
    private final int mArgumentCount;

    Setting(String nodePath, String key)
    {
        mNodePath = nodePath;
        mKey = key;

        int count = 0;
        int index = key.indexOf(TEMPLATE_ARGUMENT);

        while(index >= 0)
        {
            count++;
            index = key.indexOf(TEMPLATE_ARGUMENT, index + TEMPLATE_ARGUMENT.length());
        }

        mArgumentCount = count;
    }

    /**
     * Path of the preferences node that holds this setting.
     */
    public String getNodePath()
    {
        return mNodePath;
    }

    /**
     * Preferences node that holds this setting.
     */
    public Preferences getNode()
    {
        return Preferences.userRoot().node(mNodePath);
    }

    /**
     * Count of arguments that complete the key: zero for a fixed key.
     */
    public int getArgumentCount()
    {
        return mArgumentCount;
    }

    /**
     * Key for this setting, completed with the arguments when the key is a template.
     * @param arguments that complete a templated key, one per {@code %s} in the key.
     * @return key
     * @throws IllegalArgumentException if the count of arguments doesn't match the key template
     */
    public String getKey(Object... arguments)
    {
        if(arguments.length != mArgumentCount)
        {
            throw new IllegalArgumentException("Setting " + name() + " requires " + mArgumentCount +
                " argument(s) but was given " + arguments.length);
        }

        return mArgumentCount == 0 ? mKey : mKey.formatted(arguments);
    }

    /**
     * Stored value for this setting with the key completed by the arguments.
     * @param arguments that complete a templated key, one per {@code %s} in the key.
     */
    public Value with(Object... arguments)
    {
        return new Value(getNode(), getKey(arguments));
    }

    public String get(String defaultValue)
    {
        return with().get(defaultValue);
    }

    public void put(String value)
    {
        with().put(value);
    }

    public boolean getBoolean(boolean defaultValue)
    {
        return with().getBoolean(defaultValue);
    }

    public void putBoolean(boolean value)
    {
        with().putBoolean(value);
    }

    public int getInt(int defaultValue)
    {
        return with().getInt(defaultValue);
    }

    public void putInt(int value)
    {
        with().putInt(value);
    }

    public long getLong(long defaultValue)
    {
        return with().getLong(defaultValue);
    }

    public void putLong(long value)
    {
        with().putLong(value);
    }

    public double getDouble(double defaultValue)
    {
        return with().getDouble(defaultValue);
    }

    public void putDouble(double value)
    {
        with().putDouble(value);
    }

    public void remove()
    {
        with().remove();
    }

    /**
     * Application windows (JavaFX stages) whose size and location are stored with the JAVAFX_STAGE settings.  The keys
     * are the stage keys used by earlier versions, so don't change them.
     */
    public enum Window
    {
        CHANNEL_MAP_EDITOR("channel.map"),
        ICON_MANAGER("icon.manager"),
        JMBE_EDITOR("jmbe.editor"),
        MAIN_WINDOW("main.window"),
        PLAYLIST_EDITOR("playlist"),
        RECORDING_VIEWER("recording.viewer"),
        SETTINGS_EDITOR("user.preferences");

        private final String mKey;

        Window(String key)
        {
            mKey = key;
        }

        /**
         * Key that completes the JAVAFX_STAGE setting keys for this window.
         */
        public String getKey()
        {
            return mKey;
        }
    }

    /**
     * A stored value: the preferences node and the completed key.
     * @param node that holds the value
     * @param key of the value
     */
    public record Value(Preferences node, String key)
    {
        public String get(String defaultValue)
        {
            return node.get(key, defaultValue);
        }

        public void put(String value)
        {
            node.put(key, value);
        }

        public boolean getBoolean(boolean defaultValue)
        {
            return node.getBoolean(key, defaultValue);
        }

        public void putBoolean(boolean value)
        {
            node.putBoolean(key, value);
        }

        public int getInt(int defaultValue)
        {
            return node.getInt(key, defaultValue);
        }

        public void putInt(int value)
        {
            node.putInt(key, value);
        }

        public long getLong(long defaultValue)
        {
            return node.getLong(key, defaultValue);
        }

        public void putLong(long value)
        {
            node.putLong(key, value);
        }

        public double getDouble(double defaultValue)
        {
            return node.getDouble(key, defaultValue);
        }

        public void putDouble(double value)
        {
            node.putDouble(key, value);
        }

        public void remove()
        {
            node.remove(key);
        }
    }

    /**
     * Preferences node paths.  These are the paths that java.util.prefs derived from the package of each class that
     * stored values before the move to the settings package.  They are fixed text so that stored values stay where
     * they are when classes move.
     */
    private static final class Node
    {
        private static final String ROOT = "/io/github/dsheirer/";
        private static final String PREFERENCE = ROOT + "preference/";

        private static final String APPLICATION = PREFERENCE + "application";
        private static final String CALL_MANAGEMENT = PREFERENCE + "duplicate";
        private static final String DECODE_EVENT = PREFERENCE + "event";
        private static final String DIRECTORY = PREFERENCE + "directory";
        private static final String JAVAFX = PREFERENCE + "javafx";
        private static final String JMBE_LIBRARY = PREFERENCE + "decoder";
        private static final String MAP = PREFERENCE + "map";
        private static final String MP3 = PREFERENCE + "mp3";
        private static final String PLAYBACK = PREFERENCE + "playback";
        private static final String PLAYLIST = PREFERENCE + "playlist";
        private static final String RADIO_REFERENCE = PREFERENCE + "radioreference";
        private static final String RECORD = PREFERENCE + "record";
        private static final String SOURCE = PREFERENCE + "source";
        private static final String SWING = PREFERENCE + "swing";
        private static final String TALKGROUP_FORMAT = PREFERENCE + "identifier";
        private static final String VECTOR_CALIBRATION = PREFERENCE + "calibration";

        private static final String GUI = ROOT + "gui";
        private static final String TABLE = ROOT + "gui/table";
        private static final String VECTOR_CALIBRATE = ROOT + "vector/calibrate";
        private static final String VECTOR_CALIBRATE_RESULT = ROOT + "vector/calibrate/result";
        private static final String VIEWER = ROOT + "gui/viewer";
    }
}
