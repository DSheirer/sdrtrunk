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
package io.github.dsheirer.gui.calls;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.playback.AudioChannel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.audio.playback.ChannelPlayback;
import io.github.dsheirer.audio.playback.TalkgroupHold;
import io.github.dsheirer.call.CallManager;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.CallType;
import io.github.dsheirer.database.call.RecordingState;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.Preferences;

/**
 * Calls tab: searchable view of the call database with live updates for calls in progress and a built-in player for
 * call recordings.
 *
 * The table shows one page of search results, newest first.  On the first page, new calls that match the search
 * criteria are added at the top as they start, and the duration of calls in progress counts up.
 */
public class CallsView extends VBox
{
    private static final String TABLE_KEY = "calls.view.table";
    private static final String PREFERENCE_PAGE_SIZE = "calls.view.page.size";
    static final List<Integer> PAGE_SIZES = List.of(50, 100, 250, 500);
    static final int DEFAULT_PAGE_SIZE = 500;
    private final Preferences mPreferences = Preferences.userNodeForPackage(CallsView.class);
    private final ComboBox<Integer> mPageSizeComboBox = new ComboBox<>();
    private int mPageSize = DEFAULT_PAGE_SIZE;
    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    private final DatabaseManager mDatabaseManager;
    private final CallManager mCallManager;
    private final AudioPlaybackManager mAudioPlaybackManager;
    private final ObservableList<CallRecord> mCalls = FXCollections.observableArrayList();
    private final TableView<CallRecord> mTableView = new TableView<>(mCalls);
    private final TableColumnMonitor<CallRecord> mTableColumnMonitor;
    private final CallPlayer mCallPlayer;
    private final Map<Long,CallRecord> mLiveCalls = new LinkedHashMap<>();
    private static final int MAX_CALL_KEYS = 5000;
    /**
     * Call key (audio segment id) of recent calls by database row id, so that calls reloaded from the database keep
     * their link to the live call and to the audio channel that plays them.  Oldest entries are dropped.
     */
    private final Map<Long,Long> mCallKeyById = new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long,Long> eldest)
        {
            return size() > MAX_CALL_KEYS;
        }
    };
    private final Listener<CallRecord> mCallListener = call -> Platform.runLater(() -> receiveLiveCall(call));
    private final Listener<CallRecord> mSavedListener = call -> Platform.runLater(() -> receiveSavedCall(call));
    private final Listener<DatabaseEvent> mDatabaseListener = event -> Platform.runLater(() -> receive(event));
    private final Listener<ChannelPlayback> mPlaybackListener = playback -> Platform.runLater(() -> receive(playback));
    private final Map<Integer,ChannelPlayback> mPlaybackByChannel = new HashMap<>();

    private final AliasSearchField mToField;
    private final AliasSearchField mFromField;
    private final TextField mTalkerAliasField = new TextField();
    private final CallAliasResolver mAliasResolver;
    private final ListChangeListener<Alias> mAliasChangeListener = change -> scheduleAliasRefresh();
    private final AliasModel mAliasModel;
    private boolean mAliasRefreshPending;
    private final TextField mSystemField = new TextField();
    private final ComboBox<Protocol> mProtocolComboBox = new ComboBox<>();
    private final ComboBox<CallType> mCallTypeComboBox = new ComboBox<>();
    private final DatePicker mFromDatePicker = new DatePicker();
    private final DatePicker mToDatePicker = new DatePicker();
    private final Label mStatusLabel = new Label();
    private final Button mPreviousButton = new Button("Newer");
    private final Button mNextButton = new Button("Older");
    private final Timeline mDurationTimeline;
    private CallSearchCriteria mCriteria = CallSearchCriteria.all();
    private int mOffset = 0;
    private long mTotal = 0;
    private long mSearchSequence = 0;

    /**
     * Constructs an instance
     * @param databaseManager for searching the calls
     * @param callManager for live call updates
     * @param audioPlaybackManager for holding a talkgroup on an audio channel
     * @param aliasModel for the current TO and FROM aliases of the calls, and the alias search lists
     */
    public CallsView(DatabaseManager databaseManager, CallManager callManager,
                     AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
        mAliasResolver = new CallAliasResolver(aliasModel);
        mToField = new AliasSearchField(AliasSearchField.Kind.TALKGROUP, aliasModel, "To / talkgroup",
            this::applySearch);
        mFromField = new AliasSearchField(AliasSearchField.Kind.RADIO, aliasModel, "From / radio", this::applySearch);
        mCallPlayer = new CallPlayer(audioPlaybackManager);
        mCallPlayer.setAliasResolver(mAliasResolver);
        mDatabaseManager = databaseManager;
        mCallManager = callManager;
        mAudioPlaybackManager = audioPlaybackManager;

        setSpacing(6);
        setPadding(new Insets(6));
        getStyleClass().add("calls-view");

        createColumns();
        mTableColumnMonitor = new TableColumnMonitor<>(mTableView, TABLE_KEY);
        mTableView.setPlaceholder(new Label("No calls"));
        mTableView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
        mTableView.setRowFactory(table -> createRow());
        mTableView.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER)
            {
                play(mTableView.getSelectionModel().getSelectedItem());
            }
        });
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        mCallPlayer.setFinishedListener(this::playNext);

        getChildren().addAll(createSearchBar(), mTableView, createPagingBar(), mCallPlayer);

        mDurationTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshActiveDurations()));
        mDurationTimeline.setCycleCount(Timeline.INDEFINITE);
        mDurationTimeline.play();

        if(mCallManager != null)
        {
            mCallManager.addCallListener(mCallListener);
        }

        if(mDatabaseManager != null)
        {
            mDatabaseManager.addCallSavedListener(mSavedListener);
            mDatabaseManager.addListener(mDatabaseListener);
        }

        if(mAliasModel != null)
        {
            mAliasModel.aliasList().addListener(mAliasChangeListener);
        }

        if(mAudioPlaybackManager != null)
        {
            for(ChannelPlayback playback: mAudioPlaybackManager.getChannelPlayback())
            {
                mPlaybackByChannel.put(playback.channelIndex(), playback);
            }

            mAudioPlaybackManager.addPlaybackListener(mPlaybackListener);
        }

        search();
    }

    /**
     * Deregisters listeners and releases the player
     */
    public void dispose()
    {
        mDurationTimeline.stop();
        mCallPlayer.dispose();

        if(mCallManager != null)
        {
            mCallManager.removeCallListener(mCallListener);
        }

        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeCallSavedListener(mSavedListener);
            mDatabaseManager.removeListener(mDatabaseListener);
        }

        if(mAliasModel != null)
        {
            mAliasModel.aliasList().removeListener(mAliasChangeListener);
        }

        if(mAudioPlaybackManager != null)
        {
            mAudioPlaybackManager.removePlaybackListener(mPlaybackListener);
        }
    }

    /**
     * Audio channel playback change.  FX thread.
     */
    private void receive(ChannelPlayback playback)
    {
        ChannelPlayback previous = mPlaybackByChannel.put(playback.channelIndex(), playback);

        //Drop the state of channels that no longer exist after an audio output change (e.g. stereo to mono)
        mPlaybackByChannel.keySet().removeIf(index -> index >= playback.channelCount());

        if(previous == null || previous.audioSegmentId() != playback.audioSegmentId() ||
            previous.replayCallId() != playback.replayCallId())
        {
            mTableView.refresh();
        }
    }

    /**
     * Audio channel that is playing the call, or null if the call isn't playing.
     */
    ChannelPlayback getPlayback(CallRecord call)
    {
        ChannelPlayback replay = getReplay(call);

        if(replay != null)
        {
            return replay;
        }

        if(call == null || call.getCallKey() == 0)
        {
            return null;
        }

        for(ChannelPlayback playback: mPlaybackByChannel.values())
        {
            if(playback.audioSegmentId() == call.getCallKey())
            {
                return playback;
            }
        }

        return null;
    }

    /**
     * Audio channel that is replaying the recorded call, or null.
     */
    ChannelPlayback getReplay(CallRecord call)
    {
        if(call == null || call.getId() == 0)
        {
            return null;
        }

        for(ChannelPlayback playback: mPlaybackByChannel.values())
        {
            if(playback.replayCallId() == call.getId())
            {
                return playback;
            }
        }

        return null;
    }

    /**
     * Style suffix for an audio channel: left, right or mono
     */
    static String getChannelStyle(ChannelPlayback playback)
    {
        return switch(playback.channelName())
        {
            case "LEFT" -> "left";
            case "RIGHT" -> "right";
            default -> "mono";
        };
    }

    /**
     * Refreshes the alias columns once after a burst of alias changes (e.g. loading or editing the playlist).
     */
    private void scheduleAliasRefresh()
    {
        if(!mAliasRefreshPending)
        {
            mAliasRefreshPending = true;
            Platform.runLater(() -> {
                mAliasRefreshPending = false;
                mTableView.refresh();
            });
        }
    }

    /**
     * Page size from the stored preference, or the default when the stored value isn't one of the choices.
     */
    static int getStoredPageSize(int stored)
    {
        return PAGE_SIZES.contains(stored) ? stored : DEFAULT_PAGE_SIZE;
    }

    /**
     * Formats a timestamp for display
     */
    static String formatTime(long timestamp)
    {
        return timestamp > 0 ? TIME_FORMAT.get().format(new Date(timestamp)) : "";
    }

    private static String formatDuration(long milliseconds)
    {
        long seconds = Math.max(0, milliseconds / 1000);

        if(seconds >= 3600)
        {
            return String.format("%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
        }

        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private HBox createSearchBar()
    {
        mToField.setPrefColumnCount(12);
        mFromField.setPrefColumnCount(12);
        mTalkerAliasField.setPromptText("Talker alias");
        mTalkerAliasField.setPrefColumnCount(9);
        mTalkerAliasField.setTooltip(new Tooltip("Matches part of the talker alias decoded from the radio"));
        mSystemField.setPromptText("System");
        mSystemField.setPrefColumnCount(8);

        mProtocolComboBox.getItems().add(null);
        mProtocolComboBox.getItems().addAll(Protocol.TALKGROUP_PROTOCOLS);

        if(!mProtocolComboBox.getItems().contains(Protocol.APCO25_PHASE2))
        {
            mProtocolComboBox.getItems().add(Protocol.APCO25_PHASE2);
        }

        mProtocolComboBox.setButtonCell(createProtocolCell());
        mProtocolComboBox.setCellFactory(list -> createProtocolCell());
        mProtocolComboBox.setPromptText("Any Protocol");

        mCallTypeComboBox.getItems().add(null);
        mCallTypeComboBox.getItems().addAll(CallType.GROUP, CallType.PATCH, CallType.PRIVATE, CallType.TELEPHONE,
            CallType.UNKNOWN);
        mCallTypeComboBox.setButtonCell(createCallTypeCell());
        mCallTypeComboBox.setCellFactory(list -> createCallTypeCell());
        mCallTypeComboBox.setPromptText("Any Type");
        mCallTypeComboBox.setTooltip(new Tooltip("Call type: group, patch, private (radio to radio), telephone " +
            "interconnect, or other (no talkgroup or radio, e.g. conventional analog)"));
        mCallTypeComboBox.setOnAction(event -> applySearch());

        mFromDatePicker.setPromptText("From date");
        mFromDatePicker.setPrefWidth(130);
        mToDatePicker.setPromptText("To date");
        mToDatePicker.setPrefWidth(130);

        Button searchButton = new Button("Search");
        IconNode searchIcon = new IconNode(FontAwesome.SEARCH);
        searchIcon.setIconSize(12);
        searchIcon.fillProperty().bind(searchButton.textFillProperty());
        searchButton.setGraphic(searchIcon);
        searchButton.setDefaultButton(false);
        searchButton.setOnAction(event -> applySearch());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> {
            mToField.reset();
            mFromField.reset();
            mTalkerAliasField.clear();
            mSystemField.clear();
            mProtocolComboBox.setValue(null);
            mCallTypeComboBox.setValue(null);
            mFromDatePicker.setValue(null);
            mToDatePicker.setValue(null);
            applySearch();
        });

        for(TextField field: new TextField[]{mTalkerAliasField, mSystemField})
        {
            field.setOnAction(event -> applySearch());
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(6, mToField, mFromField, mTalkerAliasField, mCallTypeComboBox, mProtocolComboBox,
            mSystemField,
            mFromDatePicker, mToDatePicker, searchButton, clearButton, spacer,
            new ColumnConfigurationButton(mTableColumnMonitor, "Calls Columns"));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static ListCell<CallType> createCallTypeCell()
    {
        return new ListCell<>()
        {
            @Override
            protected void updateItem(CallType item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Any Type" : item.getFilterLabel()));
            }
        };
    }

    private static ListCell<Protocol> createProtocolCell()
    {
        return new ListCell<>()
        {
            @Override
            protected void updateItem(Protocol item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Any Protocol" : item.toString()));
            }
        };
    }

    private HBox createPagingBar()
    {
        mPreviousButton.setOnAction(event -> {
            mOffset = Math.max(0, mOffset - mPageSize);
            search();
        });
        mNextButton.setOnAction(event -> {
            mOffset += mPageSize;
            search();
        });
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> search());

        mPageSize = getStoredPageSize(mPreferences.getInt(PREFERENCE_PAGE_SIZE, DEFAULT_PAGE_SIZE));
        mPageSizeComboBox.getItems().addAll(PAGE_SIZES);
        mPageSizeComboBox.setValue(mPageSize);
        mPageSizeComboBox.setTooltip(new Tooltip("Number of calls per page"));
        mPageSizeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null && newValue != mPageSize)
            {
                //Keep the first displayed call on the new page
                mOffset = (mOffset / newValue) * newValue;
                mPageSize = newValue;
                mPreferences.putInt(PREFERENCE_PAGE_SIZE, newValue);
                search();
            }
        });
        Label pageSizeLabel = new Label("Page size:");
        pageSizeLabel.setLabelFor(mPageSizeComboBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(6, mStatusLabel, spacer, pageSizeLabel, mPageSizeComboBox, refreshButton,
            mPreviousButton, mNextButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void createColumns()
    {
        TableColumn<CallRecord,String> start = column("start", "Start", 140,
            call -> formatTime(call.getStartTime()));
        TableColumn<CallRecord,String> duration = column("duration", "Duration", 70,
            call -> formatDuration(call.getDuration()));
        TableColumn<CallRecord,CallRecord> status = new TableColumn<>("");
        status.setId("status");
        TableColumnMonitor.setName(status, "Status");
        status.setPrefWidth(34);
        status.setSortable(false);
        status.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue()));
        status.setCellFactory(column -> new StatusCell());

        TableColumn<CallRecord,CallRecord> playing = new TableColumn<>("Playing");
        playing.setId("playing");
        playing.setPrefWidth(70);
        playing.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue()));
        playing.setCellFactory(column -> new PlayingCell());

        mTableView.getColumns().add(start);
        mTableView.getColumns().add(duration);
        mTableView.getColumns().add(status);
        mTableView.getColumns().add(playing);
        mTableView.getColumns().add(column("callType", "Type", 70, call -> call.getCallType().toString()));
        mTableView.getColumns().add(column("to", "To", 90, CallRecord::getToId));
        mTableView.getColumns().add(column("toAlias", "To Alias", 150, mAliasResolver::getToAliasName));
        mTableView.getColumns().add(column("patched", "Patched", 140, mAliasResolver::getPatchedTalkgroups));
        mTableView.getColumns().add(column("from", "From", 90, CallRecord::getFromId));
        mTableView.getColumns().add(column("fromAlias", "From Alias", 150, mAliasResolver::getFromAliasName));
        mTableView.getColumns().add(column("talkerAlias", "Talker Alias", 120, CallRecord::getTalkerAlias));
        TableColumn<CallRecord,String> recording = column("recording", "Recording", 90,
            call -> call.getRecordingState().toString());
        recording.setVisible(false);
        mTableView.getColumns().add(recording);
        TableColumn<CallRecord,String> file = column("file", "File", 260, call -> call.getFilePath() != null ?
            java.nio.file.Path.of(call.getFilePath()).getFileName().toString() : "");
        file.setVisible(false);
        mTableView.getColumns().add(file);

        //Protocol, system, site and channel are at the end by default (#2742)
        mTableView.getColumns().add(column("protocol", "Protocol", 80, call -> call.getProtocolValue() ==
            Protocol.UNKNOWN ? "" : call.getProtocolValue().toString()));
        mTableView.getColumns().add(column("system", "System", 110, CallRecord::getSystem));
        mTableView.getColumns().add(column("site", "Site", 90, CallRecord::getSite));
        mTableView.getColumns().add(column("channel", "Channel", 110, CallRecord::getChannel));
        TableColumn<CallRecord,String> frequency = column("frequency", "Frequency", 90,
            call -> call.getFrequency() > 0 ? String.format("%.5f", call.getFrequency() / 1E6) : "");
        frequency.setVisible(false);
        mTableView.getColumns().add(frequency);
        TableColumn<CallRecord,String> timeslot = column("timeslot", "TS", 34,
            call -> call.getTimeslot() > 0 ? String.valueOf(call.getTimeslot()) : "");
        timeslot.setVisible(false);
        mTableView.getColumns().add(timeslot);

        for(TableColumn<CallRecord,?> column: mTableView.getColumns())
        {
            column.setSortable(false);
        }
    }

    private static TableColumn<CallRecord,String> column(String id, String title, double width,
                                                         Function<CallRecord,String> value)
    {
        TableColumn<CallRecord,String> column = new TableColumn<>(title);
        column.setId(id);
        column.setPrefWidth(width);
        column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue() != null ?
            value.apply(cdf.getValue()) : null));
        return column;
    }

    /**
     * Status icon cell: in progress, playable recording, encrypted, duplicate
     */
    private class StatusCell extends TableCell<CallRecord,CallRecord>
    {
        private final IconNode mIcon = new IconNode();

        StatusCell()
        {
            mIcon.setIconSize(12);
            mIcon.getStyleClass().add("calls-status-icon");
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(CallRecord call, boolean empty)
        {
            super.updateItem(call, empty);

            if(empty || call == null)
            {
                setGraphic(null);
                setTooltip(null);
                return;
            }

            String tip;

            if(call.isActive())
            {
                mIcon.setIconCode(FontAwesome.CIRCLE);
                mIcon.setFill(javafx.scene.paint.Color.web("#3ddc84"));
                tip = "In progress" + (call.getRecordingState() == RecordingState.RECORDING ? " - recording" : "");
            }
            else if(mCallPlayer.currentCallProperty().get() != null && isSameCall(call,
                mCallPlayer.currentCallProperty().get()))
            {
                mIcon.setIconCode(FontAwesome.VOLUME_UP);
                mIcon.setFill(javafx.scene.paint.Color.web("#4a90d9"));
                tip = "Playing";
            }
            else if(call.isPlayable())
            {
                mIcon.setIconCode(FontAwesome.PLAY_CIRCLE_O);
                mIcon.setFill(javafx.scene.paint.Color.GRAY);
                tip = "Recorded - double-click to play";
            }
            else if(call.isEncrypted())
            {
                mIcon.setIconCode(FontAwesome.LOCK);
                mIcon.setFill(javafx.scene.paint.Color.GRAY);
                tip = "Encrypted";
            }
            else
            {
                mIcon.setIconCode(FontAwesome.MINUS);
                mIcon.setFill(javafx.scene.paint.Color.GRAY);
                tip = call.getRecordingState().toString();
            }

            if(call.isDuplicate())
            {
                tip += " (duplicate)";
            }

            setGraphic(mIcon);
            setTooltip(new Tooltip(tip));
        }
    }

    /**
     * Audio channel chip (LEFT, RIGHT or MONO) for a call that is playing on an audio channel
     */
    private class PlayingCell extends TableCell<CallRecord,CallRecord>
    {
        private final Label mChip = new Label();
        private final IconNode mIcon = new IconNode(FontAwesome.VOLUME_UP);

        PlayingCell()
        {
            mIcon.setIconSize(11);
            mIcon.getStyleClass().add("calls-playing-icon");
            mChip.setGraphic(mIcon);
            mChip.getStyleClass().add("calls-playing-chip");
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(CallRecord call, boolean empty)
        {
            super.updateItem(call, empty);
            ChannelPlayback playback = empty ? null : getPlayback(call);

            if(playback == null)
            {
                setGraphic(null);
                setTooltip(null);
                return;
            }

            mChip.getStyleClass().removeAll("calls-playing-left", "calls-playing-right", "calls-playing-mono");
            mChip.getStyleClass().add("calls-playing-" + getChannelStyle(playback));
            mChip.setText(playback.channelName());
            mIcon.setIconCode(playback.isReplay() ? FontAwesome.HISTORY : FontAwesome.VOLUME_UP);
            setGraphic(mChip);
            setTooltip(new Tooltip((playback.isReplay() ? "Replaying" : "Playing") + " on the " +
                (playback.channelCount() > 1 ? playback.channelName().toLowerCase() + " audio channel" :
                    "audio output")));
        }
    }

    private static final PseudoClass PLAYING_LEFT = PseudoClass.getPseudoClass("playing-left");
    private static final PseudoClass PLAYING_RIGHT = PseudoClass.getPseudoClass("playing-right");
    private static final PseudoClass PLAYING_MONO = PseudoClass.getPseudoClass("playing-mono");

    private TableRow<CallRecord> createRow()
    {
        TableRow<CallRecord> row = new TableRow<>()
        {
            @Override
            protected void updateItem(CallRecord call, boolean empty)
            {
                super.updateItem(call, empty);
                ChannelPlayback playback = empty ? null : getPlayback(call);
                String style = playback != null ? getChannelStyle(playback) : "";
                pseudoClassStateChanged(PLAYING_LEFT, style.equals("left"));
                pseudoClassStateChanged(PLAYING_RIGHT, style.equals("right"));
                pseudoClassStateChanged(PLAYING_MONO, style.equals("mono"));
            }
        };

        row.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty())
            {
                play(row.getItem());
            }
        });

        row.setOnContextMenuRequested(event -> {
            if(!row.isEmpty())
            {
                mTableView.getSelectionModel().select(row.getIndex());
                createContextMenu(row.getItem()).show(row, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });

        return row;
    }

    /**
     * Context menu for a call: play, hold talkgroup on an audio channel, copy file path.
     */
    private ContextMenu createContextMenu(CallRecord call)
    {
        ContextMenu menu = new ContextMenu();

        MenuItem play = new MenuItem("Play Recording");
        play.setDisable(!call.isPlayable());
        play.setOnAction(event -> play(call));
        menu.getItems().add(play);

        TalkgroupHold hold = TalkgroupHold.from(call, mAliasResolver.getToAliasName(call));

        if(mAudioPlaybackManager != null)
        {
            List<AudioChannel> channels = mAudioPlaybackManager.getAudioChannels();

            if(!channels.isEmpty())
            {
                menu.getItems().add(new SeparatorMenuItem());
            }

            for(int x = 0; x < channels.size(); x++)
            {
                final int channelIndex = x;
                String channelName = getChannelLabel(channels.get(x), channels.size());
                TalkgroupHold current = mAudioPlaybackManager.getHold(x);

                MenuItem holdItem = new MenuItem(hold != null ? "Hold Talkgroup " + call.getToId() + channelName :
                    "Hold Talkgroup" + channelName);
                holdItem.setDisable(hold == null || hold.isSameTalkgroup(current));
                holdItem.setOnAction(event -> mAudioPlaybackManager.setHold(channelIndex, hold));
                menu.getItems().add(holdItem);

                if(current != null)
                {
                    MenuItem release = new MenuItem("Release Hold " + current + channelName);
                    release.setOnAction(event -> mAudioPlaybackManager.setHold(channelIndex, null));
                    menu.getItems().add(release);
                }
            }
        }

        menu.getItems().add(new SeparatorMenuItem());
        MenuItem copy = new MenuItem("Copy Recording File Path");
        copy.setDisable(call.getFilePath() == null);
        copy.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(call.getFilePath());
            Clipboard.getSystemClipboard().setContent(content);
        });
        menu.getItems().add(copy);

        return menu;
    }

    /**
     * Menu label suffix for the audio channel, empty for a mono output.
     */
    static String getChannelLabel(AudioChannel channel, int channelCount)
    {
        if(channelCount <= 1)
        {
            return "";
        }

        return switch(channel.getChannelName())
        {
            case "LEFT" -> " on Left Channel";
            case "RIGHT" -> " on Right Channel";
            default -> " on " + channel.getChannelName();
        };
    }

    private void play(CallRecord call)
    {
        if(call != null && call.isPlayable())
        {
            mCallPlayer.play(call);
            mTableView.refresh();
        }
    }

    /**
     * Plays the next newer recorded call after the finished call, when auto-play is enabled.
     */
    private void playNext(CallRecord finished)
    {
        mTableView.refresh();

        if(!mCallPlayer.isAutoPlay())
        {
            return;
        }

        int index = indexOf(finished);

        //Newest calls are at the top, so the next newer call is above the finished call
        for(int x = index - 1; x >= 0; x--)
        {
            CallRecord candidate = mCalls.get(x);

            if(candidate.isPlayable())
            {
                mTableView.getSelectionModel().select(x);
                mTableView.scrollTo(Math.max(0, x - 3));
                play(candidate);
                return;
            }
        }
    }

    private int indexOf(CallRecord call)
    {
        for(int x = 0; x < mCalls.size(); x++)
        {
            if(isSameCall(mCalls.get(x), call))
            {
                return x;
            }
        }

        return -1;
    }

    private static boolean isSameCall(CallRecord a, CallRecord b)
    {
        if(a.getCallKey() != 0 && a.getCallKey() == b.getCallKey())
        {
            return true;
        }

        return a.getId() != 0 && a.getId() == b.getId();
    }

    private void applySearch()
    {
        long start = toMillis(mFromDatePicker.getValue(), false);
        long end = toMillis(mToDatePicker.getValue(), true);
        mCriteria = new CallSearchCriteria(mToField.getFilter(), mFromField.getFilter(), mTalkerAliasField.getText(),
            mProtocolComboBox.getValue(), mCallTypeComboBox.getValue(), mSystemField.getText(), start, end);
        mOffset = 0;
        search();
    }

    private static long toMillis(LocalDate date, boolean endOfDay)
    {
        if(date == null)
        {
            return 0;
        }

        LocalDate day = endOfDay ? date.plusDays(1) : date;
        long millis = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return endOfDay ? millis - 1 : millis;
    }

    /**
     * Runs the current search and replaces the table contents with the results.
     */
    private void search()
    {
        if(mDatabaseManager == null)
        {
            mStatusLabel.setText("Call database is not available");
            return;
        }

        long sequence = ++mSearchSequence;
        CallSearchCriteria criteria = mCriteria;
        int offset = mOffset;
        mStatusLabel.setText("Searching ...");

        mDatabaseManager.search(criteria, offset, mPageSize).whenComplete((result, error) -> Platform.runLater(() -> {
            if(sequence != mSearchSequence)
            {
                return;
            }

            if(error != null)
            {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                mStatusLabel.setText("Search failed: " + cause.getMessage());
                mCalls.clear();
                mTotal = 0;
                updatePaging();
                return;
            }

            CallRecord selected = mTableView.getSelectionModel().getSelectedItem();
            List<CallRecord> calls = new ArrayList<>();

            if(offset == 0)
            {
                //Live calls that haven't been written to the database yet
                List<CallRecord> unsaved = new ArrayList<>();

                for(CallRecord live: mLiveCalls.values())
                {
                    if(live.getId() == 0 && criteria.matches(live))
                    {
                        unsaved.add(live);
                    }
                }

                unsaved.sort((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()));
                calls.addAll(unsaved);
            }

            for(CallRecord stored: result.calls())
            {
                //Use the live snapshot for a call in progress, so that it continues to update.  A recent completed
                //call gets its call key back, so that it shows when it's still playing on an audio channel.
                Long callKey = mCallKeyById.get(stored.getId());
                CallRecord live = callKey != null ? mLiveCalls.get(callKey) : null;

                if(live != null)
                {
                    calls.add(live);
                }
                else if(callKey != null)
                {
                    CallRecord keyed = stored.copy();
                    keyed.setCallKey(callKey);
                    calls.add(keyed);
                }
                else
                {
                    calls.add(stored);
                }
            }

            mCalls.setAll(calls);
            mTotal = result.total();
            updatePaging();

            if(selected != null)
            {
                int index = indexOf(selected);

                if(index >= 0)
                {
                    mTableView.getSelectionModel().select(index);
                }
            }
        }));
    }

    private void updatePaging()
    {
        mPreviousButton.setDisable(mOffset == 0);
        mNextButton.setDisable(mOffset + mPageSize >= mTotal);

        if(mTotal == 0)
        {
            mStatusLabel.setText(mCriteria.isEmpty() ? "No calls" : "No matching calls");
        }
        else
        {
            mStatusLabel.setText("Calls " + (mOffset + 1) + " - " + Math.min(mTotal, mOffset + mPageSize) + " of " +
                mTotal + (mCriteria.isEmpty() ? "" : " matching"));
        }
    }

    /**
     * Live call snapshot from the call recorder.  FX thread.
     */
    private void receiveLiveCall(CallRecord call)
    {
        //Keep the database id that this view already learned for the call
        CallRecord previous = mLiveCalls.get(call.getCallKey());

        if(previous != null && previous.getId() != 0 && call.getId() == 0)
        {
            call = call.copy();
            call.setId(previous.getId());
        }

        if(call.isActive())
        {
            mLiveCalls.put(call.getCallKey(), call);
        }
        else
        {
            mLiveCalls.remove(call.getCallKey());
        }

        int index = indexOf(call);

        if(index >= 0)
        {
            mCalls.set(index, call);
        }
        else if(mOffset == 0 && mCriteria.matches(call))
        {
            //New call (or a short call that completed before its first update).  Insert in start time order.
            int insert = 0;

            while(insert < mCalls.size() && mCalls.get(insert).getStartTime() > call.getStartTime())
            {
                insert++;
            }

            mCalls.add(insert, call);
            mTotal++;

            if(mCalls.size() > mPageSize)
            {
                mCalls.removeLast();
            }

            updatePaging();
        }
    }

    /**
     * Call written to the database, with its row id.  FX thread.
     */
    private void receiveSavedCall(CallRecord saved)
    {
        if(saved.getCallKey() == 0)
        {
            return;
        }

        CallRecord live = mLiveCalls.get(saved.getCallKey());
        mCallKeyById.put(saved.getId(), saved.getCallKey());

        if(live != null)
        {
            if(live.getId() != saved.getId())
            {
                CallRecord updated = live.copy();
                updated.setId(saved.getId());
                mLiveCalls.put(saved.getCallKey(), updated);

                int index = indexOf(updated);

                if(index >= 0)
                {
                    mCalls.set(index, updated);
                }
            }
        }
        else
        {
            //Completed call: record the id on the row so it matches the stored call after a search
            for(int x = 0; x < mCalls.size(); x++)
            {
                CallRecord row = mCalls.get(x);

                if(row.getCallKey() == saved.getCallKey() && row.getId() != saved.getId())
                {
                    CallRecord updated = row.copy();
                    updated.setId(saved.getId());
                    mCalls.set(x, updated);
                    break;
                }
            }
        }
    }

    private void receive(DatabaseEvent event)
    {
        if(event.type() == DatabaseEvent.Type.CONNECTED || event.type() == DatabaseEvent.Type.CALLS_CHANGED)
        {
            if(event.type() == DatabaseEvent.Type.CONNECTED)
            {
                mCallKeyById.clear();
            }

            search();
        }
        else if(event.type() == DatabaseEvent.Type.ERROR)
        {
            mStatusLabel.setText("Call database is not available: " + event.errorMessage());
        }
    }

    /**
     * Refreshes the duration of the calls in progress once per second.
     */
    private void refreshActiveDurations()
    {
        if(!mLiveCalls.isEmpty() && isVisible() && getScene() != null)
        {
            for(CallRecord call: mCalls)
            {
                if(call.isActive())
                {
                    mTableView.refresh();
                    return;
                }
            }
        }
    }
}
