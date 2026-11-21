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
package io.github.dsheirer.gui.events;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.playback.AudioChannel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.audio.playback.TalkgroupHold;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.event.EventCategory;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.EventSearchCriteria;
import io.github.dsheirer.event.EventManager;
import io.github.dsheirer.gui.calls.AliasSearchField;
import io.github.dsheirer.gui.calls.CallAliasResolver;
import io.github.dsheirer.gui.calls.CallsView;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.gui.table.TableRowCopy;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Events tab: searchable view of the decode events from all channels, stored in the events table of the database,
 * with live updates.
 *
 * The table shows one page of search results, newest first.  On the first page, new events that match the search
 * criteria are added at the top as they arrive, and events that change (e.g. the duration of a call) are updated.
 *
 * Rows can be selected (Ctrl+A selects the page) and copied to the clipboard as tab separated text (Ctrl+C or the
 * context menu), which pastes into a spreadsheet.
 */
public class EventsView extends VBox
{
    private static final String TABLE_KEY = "events.view.table";
    private static final int MAX_EVENT_KEYS = 5000;
    private final ComboBox<Integer> mPageSizeComboBox = new ComboBox<>();
    private int mPageSize = CallsView.DEFAULT_PAGE_SIZE;

    private final DatabaseManager mDatabaseManager;
    private final EventManager mEventManager;
    private final AudioPlaybackManager mAudioPlaybackManager;
    private final AliasModel mAliasModel;
    private final CallAliasResolver mAliasResolver;
    private final ObservableList<EventRecord> mEvents = FXCollections.observableArrayList();
    private final TableView<EventRecord> mTableView = new TableView<>(mEvents);
    private final TableColumnMonitor<EventRecord> mTableColumnMonitor;

    /**
     * Latest snapshot of recent live events by event key, so that events reloaded from the database continue to
     * update.  Oldest entries are dropped.
     */
    private final Map<Long,EventRecord> mLiveEvents = new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long,EventRecord> eldest)
        {
            return size() > MAX_EVENT_KEYS;
        }
    };

    /**
     * Event key of recent live events by database row id.  Oldest entries are dropped.
     */
    private final Map<Long,Long> mEventKeyById = new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long,Long> eldest)
        {
            return size() > MAX_EVENT_KEYS;
        }
    };

    private final Listener<EventRecord> mEventListener = event -> Platform.runLater(() -> receiveLiveEvent(event));
    private final Listener<EventRecord> mSavedListener = event -> Platform.runLater(() -> receiveSavedEvent(event));
    private final Listener<DatabaseEvent> mDatabaseListener = event -> Platform.runLater(() -> receive(event));
    private final ListChangeListener<Alias> mAliasChangeListener = change -> scheduleAliasRefresh();
    private boolean mAliasRefreshPending;

    private final AliasSearchField mTalkgroupField;
    private final AliasSearchField mRadioField;
    private final ComboBox<EventCategory> mCategoryComboBox = new ComboBox<>();
    private final ComboBox<DecodeEventType> mEventTypeComboBox = new ComboBox<>();
    private final ComboBox<Protocol> mProtocolComboBox = new ComboBox<>();
    private final TextField mSystemField = new TextField();
    private final TextField mDetailsField = new TextField();
    private final DatePicker mFromDatePicker = new DatePicker();
    private final DatePicker mToDatePicker = new DatePicker();
    private final Label mStatusLabel = new Label();
    private final Button mPreviousButton = new Button("Newer");
    private final Button mNextButton = new Button("Older");
    private EventSearchCriteria mCriteria = EventSearchCriteria.all();
    private int mOffset = 0;
    private long mTotal = 0;
    private long mSearchSequence = 0;

    /**
     * Constructs an instance
     * @param databaseManager for searching the events
     * @param eventManager for live event updates
     * @param audioPlaybackManager for holding a talkgroup on an audio channel
     * @param aliasModel for the current TO and FROM aliases of the events, and the alias search lists
     */
    public EventsView(DatabaseManager databaseManager, EventManager eventManager,
                      AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel)
    {
        mDatabaseManager = databaseManager;
        mEventManager = eventManager;
        mAudioPlaybackManager = audioPlaybackManager;
        mAliasModel = aliasModel;
        mAliasResolver = new CallAliasResolver(aliasModel);
        mTalkgroupField = new AliasSearchField(AliasSearchField.Kind.TALKGROUP, aliasModel, "Talkgroup",
            this::applySearch);
        mRadioField = new AliasSearchField(AliasSearchField.Kind.RADIO, aliasModel, "Radio", this::applySearch);

        setSpacing(6);
        setPadding(new Insets(6));
        getStyleClass().addAll("calls-view", "events-view");

        createColumns();
        mTableColumnMonitor = new TableColumnMonitor<>(mTableView, TABLE_KEY);
        mTableView.setPlaceholder(new Label("No events"));
        mTableView.setRowFactory(table -> createRow());
        mTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mTableView.setOnKeyPressed(event -> {
            if(TableRowCopy.isCopy(event))
            {
                copySelected();
                event.consume();
            }
        });
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        getChildren().addAll(createSearchBar(), mTableView, createPagingBar());

        if(mEventManager != null)
        {
            mEventManager.addEventListener(mEventListener);
        }

        if(mDatabaseManager != null)
        {
            mDatabaseManager.addEventSavedListener(mSavedListener);
            mDatabaseManager.addListener(mDatabaseListener);
        }

        if(mAliasModel != null)
        {
            mAliasModel.aliasList().addListener(mAliasChangeListener);
        }

        search();
    }

    /**
     * Deregisters listeners
     */
    public void dispose()
    {
        if(mEventManager != null)
        {
            mEventManager.removeEventListener(mEventListener);
        }

        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeEventSavedListener(mSavedListener);
            mDatabaseManager.removeListener(mDatabaseListener);
        }

        if(mAliasModel != null)
        {
            mAliasModel.aliasList().removeListener(mAliasChangeListener);
        }
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

    private static final DecimalFormat DURATION_FORMAT = new DecimalFormat("0.0");

    /**
     * Formats an event duration in seconds with one decimal, like the Now Playing events table.
     */
    static String formatDuration(long milliseconds)
    {
        return milliseconds > 0 ? DURATION_FORMAT.format(milliseconds / 1e3d) : "";
    }

    /**
     * Event types for the event type filter: all types, or the types of the category, sorted by label.
     */
    static List<DecodeEventType> getEventTypes(EventCategory category)
    {
        return Arrays.stream(DecodeEventType.values())
            .filter(type -> category == null || category.contains(type))
            .sorted(Comparator.comparing(DecodeEventType::getLabel, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private HBox createSearchBar()
    {
        mTalkgroupField.setPrefColumnCount(11);
        mRadioField.setPrefColumnCount(11);

        mCategoryComboBox.getItems().add(null);
        mCategoryComboBox.getItems().addAll(EventCategory.values());
        mCategoryComboBox.setButtonCell(createCell("Any Category"));
        mCategoryComboBox.setCellFactory(list -> createCell("Any Category"));
        mCategoryComboBox.setPromptText("Any Category");
        mCategoryComboBox.setTooltip(new Tooltip("Event category: the same groupings as the Now Playing event filters"));
        mCategoryComboBox.setOnAction(event -> {
            //Limit the event type choices to the category
            DecodeEventType selected = mEventTypeComboBox.getValue();
            List<DecodeEventType> types = getEventTypes(mCategoryComboBox.getValue());
            mEventTypeComboBox.getItems().setAll(types);
            mEventTypeComboBox.getItems().addFirst(null);
            mEventTypeComboBox.setValue(types.contains(selected) ? selected : null);
            applySearch();
        });

        mEventTypeComboBox.getItems().add(null);
        mEventTypeComboBox.getItems().addAll(getEventTypes(null));
        mEventTypeComboBox.setButtonCell(createCell("Any Event"));
        mEventTypeComboBox.setCellFactory(list -> createCell("Any Event"));
        mEventTypeComboBox.setPromptText("Any Event");
        mEventTypeComboBox.setVisibleRowCount(20);
        mEventTypeComboBox.setOnAction(event -> applySearch());

        mProtocolComboBox.getItems().add(null);
        mProtocolComboBox.getItems().addAll(Arrays.stream(Protocol.values())
            .filter(protocol -> protocol != Protocol.UNKNOWN)
            .sorted(Comparator.comparing(Protocol::toString, String.CASE_INSENSITIVE_ORDER)).toList());
        mProtocolComboBox.setButtonCell(createCell("Any Protocol"));
        mProtocolComboBox.setCellFactory(list -> createCell("Any Protocol"));
        mProtocolComboBox.setPromptText("Any Protocol");
        mProtocolComboBox.setVisibleRowCount(20);
        mProtocolComboBox.setOnAction(event -> applySearch());

        mSystemField.setPromptText("System");
        mSystemField.setPrefColumnCount(8);
        mDetailsField.setPromptText("Details");
        mDetailsField.setPrefColumnCount(10);
        mDetailsField.setTooltip(new Tooltip("Matches part of the event details (e.g. a text message or GPS)"));

        mFromDatePicker.setPromptText("From date");
        mFromDatePicker.setPrefWidth(130);
        mToDatePicker.setPromptText("To date");
        mToDatePicker.setPrefWidth(130);

        Button searchButton = new Button("Search");
        IconNode searchIcon = new IconNode(FontAwesome.SEARCH);
        searchIcon.setIconSize(12);
        searchIcon.fillProperty().bind(searchButton.textFillProperty());
        searchButton.setGraphic(searchIcon);
        searchButton.setOnAction(event -> applySearch());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> {
            mTalkgroupField.reset();
            mRadioField.reset();
            mCategoryComboBox.setValue(null);
            mEventTypeComboBox.getItems().setAll(getEventTypes(null));
            mEventTypeComboBox.getItems().addFirst(null);
            mEventTypeComboBox.setValue(null);
            mProtocolComboBox.setValue(null);
            mSystemField.clear();
            mDetailsField.clear();
            mFromDatePicker.setValue(null);
            mToDatePicker.setValue(null);
            applySearch();
        });

        for(TextField field: new TextField[]{mSystemField, mDetailsField})
        {
            field.setOnAction(event -> applySearch());
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(6, mTalkgroupField, mRadioField, mCategoryComboBox, mEventTypeComboBox,
            mProtocolComboBox, mSystemField, mDetailsField, mFromDatePicker, mToDatePicker, searchButton, clearButton,
            spacer, new ColumnConfigurationButton(mTableColumnMonitor, "Events Columns"));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static <T> ListCell<T> createCell(String nullLabel)
    {
        return new ListCell<>()
        {
            @Override
            protected void updateItem(T item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? nullLabel : item.toString()));
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

        mPageSize = CallsView.getStoredPageSize(Setting.EVENTS_VIEW_PAGE_SIZE.getInt(CallsView.DEFAULT_PAGE_SIZE));
        mPageSizeComboBox.getItems().addAll(CallsView.PAGE_SIZES);
        mPageSizeComboBox.setValue(mPageSize);
        mPageSizeComboBox.setTooltip(new Tooltip("Number of events per page"));
        mPageSizeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null && newValue != mPageSize)
            {
                //Keep the first displayed event on the new page
                mOffset = (mOffset / newValue) * newValue;
                mPageSize = newValue;
                Setting.EVENTS_VIEW_PAGE_SIZE.putInt(newValue);
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
        mTableView.getColumns().add(column("start", "Time", 140, event -> CallsView.formatTime(event.getStartTime())));
        mTableView.getColumns().add(column("duration", "Duration", 65, event -> formatDuration(event.getDuration())));
        mTableView.getColumns().add(column("event", "Event", 150, event -> event.getEventTypeValue().getLabel()));
        TableColumn<EventRecord,String> category = column("category", "Category", 100,
            event -> event.getCategory().toString());
        category.setVisible(false);
        mTableView.getColumns().add(category);
        mTableView.getColumns().add(column("to", "To", 90, EventRecord::getToId));
        mTableView.getColumns().add(column("toAlias", "To Alias", 150, mAliasResolver::getToAliasName));
        mTableView.getColumns().add(column("patched", "Patched", 140, mAliasResolver::getPatchedTalkgroups));
        mTableView.getColumns().add(column("from", "From", 90, EventRecord::getFromId));
        mTableView.getColumns().add(column("fromAlias", "From Alias", 150, mAliasResolver::getFromAliasName));
        mTableView.getColumns().add(column("talkerAlias", "Talker Alias", 120, EventRecord::getTalkerAlias));
        mTableView.getColumns().add(column("channelNumber", "Channel Number", 110, EventsView::getChannelNumber));
        mTableView.getColumns().add(column("frequency", "Frequency", 90, event -> event.getFrequency() > 0 ?
            String.format("%.5f", event.getFrequency() / 1E6) : ""));
        mTableView.getColumns().add(column("details", "Details", 300, EventRecord::getDetails));

        //Protocol, system, site and channel at the end by default, like the Calls tab
        mTableView.getColumns().add(column("protocol", "Protocol", 80, event -> event.getProtocolValue() ==
            Protocol.UNKNOWN ? "" : event.getProtocolValue().toString()));
        mTableView.getColumns().add(column("system", "System", 110, EventRecord::getSystem));
        mTableView.getColumns().add(column("site", "Site", 90, EventRecord::getSite));
        mTableView.getColumns().add(column("channel", "Channel", 110, EventRecord::getChannel));

        for(TableColumn<EventRecord,?> column: mTableView.getColumns())
        {
            column.setSortable(false);
        }
    }

    /**
     * Channel descriptor and timeslot of the event, like the Now Playing events table.
     */
    static String getChannelNumber(EventRecord event)
    {
        String descriptor = event.getChannelDescriptor();
        boolean timeslot = event.getTimeslot() >= 0;

        if(descriptor != null)
        {
            return timeslot && !descriptor.contains("TS") ? descriptor + " TS" + event.getTimeslot() : descriptor;
        }

        return timeslot ? "TS" + event.getTimeslot() : null;
    }

    private static TableColumn<EventRecord,String> column(String id, String title, double width,
                                                          Function<EventRecord,String> value)
    {
        TableColumn<EventRecord,String> column = new TableColumn<>(title);
        column.setId(id);
        column.setPrefWidth(width);
        column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue() != null ?
            value.apply(cdf.getValue()) : null));
        return column;
    }

    private TableRow<EventRecord> createRow()
    {
        TableRow<EventRecord> row = new TableRow<>();
        row.setOnContextMenuRequested(event -> {
            if(!row.isEmpty())
            {
                //Keep a multiple selection when the click is on one of the selected rows
                if(!mTableView.getSelectionModel().isSelected(row.getIndex()))
                {
                    mTableView.getSelectionModel().clearAndSelect(row.getIndex());
                }

                createContextMenu(row.getItem()).show(row, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        return row;
    }

    /**
     * Context menu for an event: hold the TO talkgroup on an audio channel, copy the details.
     */
    private ContextMenu createContextMenu(EventRecord event)
    {
        ContextMenu menu = new ContextMenu();
        TalkgroupHold hold = TalkgroupHold.from(event, mAliasResolver.getToAliasName(event));

        if(mAudioPlaybackManager != null)
        {
            List<AudioChannel> channels = mAudioPlaybackManager.getAudioChannels();

            for(int x = 0; x < channels.size(); x++)
            {
                final int channelIndex = x;
                String channelName = CallsView.getChannelLabel(channels.get(x), channels.size());
                TalkgroupHold current = mAudioPlaybackManager.getHold(x);

                MenuItem holdItem = new MenuItem(hold != null ? "Hold Talkgroup " + event.getToId() + channelName :
                    "Hold Talkgroup" + channelName);
                holdItem.setDisable(hold == null || hold.isSameTalkgroup(current));
                holdItem.setOnAction(e -> mAudioPlaybackManager.setHold(channelIndex, hold));
                menu.getItems().add(holdItem);

                if(current != null)
                {
                    MenuItem release = new MenuItem("Release Hold " + current + channelName);
                    release.setOnAction(e -> mAudioPlaybackManager.setHold(channelIndex, null));
                    menu.getItems().add(release);
                }
            }

            if(!channels.isEmpty())
            {
                menu.getItems().add(new SeparatorMenuItem());
            }
        }

        MenuItem copy = new MenuItem("Copy Details");
        copy.setDisable(event.getDetails() == null);
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(event.getDetails());
            Clipboard.getSystemClipboard().setContent(content);
        });
        menu.getItems().add(copy);

        int selected = mTableView.getSelectionModel().getSelectedIndices().size();
        MenuItem copyRows = new MenuItem(selected > 1 ? "Copy " + selected + " Selected Rows" : "Copy Selected Row");
        copyRows.setOnAction(e -> copySelected());
        MenuItem selectAll = new MenuItem("Select All Rows on This Page");
        selectAll.setOnAction(e -> mTableView.getSelectionModel().selectAll());
        menu.getItems().addAll(new SeparatorMenuItem(), copyRows, selectAll);
        return menu;
    }

    /**
     * Copies the selected rows to the clipboard as tab separated text, with a header line of the column names.  The
     * visible columns are copied in their displayed order, and the rows in their displayed order.
     */
    void copySelected()
    {
        int count = TableRowCopy.copySelected(mTableView);

        if(count > 0)
        {
            mStatusLabel.setText("Copied " + count + (count == 1 ? " event" : " events") + " to the clipboard");
        }
    }

    /**
     * Applies a change to the rows and keeps the selected events selected.
     */
    private void preserveSelection(Runnable change)
    {
        TableRowCopy.preserveSelection(mTableView, EventsView::getKeys, change);
    }

    /**
     * Identity keys of an event row: the live event key and the database row id
     */
    private static List<Object> getKeys(EventRecord event)
    {
        List<Object> keys = new ArrayList<>(2);

        if(event.getEventKey() != 0)
        {
            keys.add("K" + event.getEventKey());
        }

        if(event.getId() != 0)
        {
            keys.add("I" + event.getId());
        }

        return keys;
    }

    private void applySearch()
    {
        long start = toMillis(mFromDatePicker.getValue(), false);
        long end = toMillis(mToDatePicker.getValue(), true);
        mCriteria = new EventSearchCriteria(mTalkgroupField.getFilter(), mRadioField.getFilter(),
            mCategoryComboBox.getValue(), mEventTypeComboBox.getValue(), mProtocolComboBox.getValue(),
            mSystemField.getText(), mDetailsField.getText(), start, end);
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
     * Search criteria of the view (for testing)
     */
    EventSearchCriteria getCriteria()
    {
        return mCriteria;
    }

    /**
     * Rows of the view (for testing)
     */
    List<EventRecord> getEvents()
    {
        return mEvents;
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
        EventSearchCriteria criteria = mCriteria;
        int offset = mOffset;
        mStatusLabel.setText("Searching ...");

        mDatabaseManager.searchEvents(criteria, offset, mPageSize).whenComplete((result, error) ->
            Platform.runLater(() -> {
            if(sequence != mSearchSequence)
            {
                return;
            }

            if(error != null)
            {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                mStatusLabel.setText("Search failed: " + cause.getMessage());
                mEvents.clear();
                mTotal = 0;
                updatePaging();
                return;
            }

            List<EventRecord> events = new ArrayList<>();

            if(offset == 0)
            {
                //Live events that haven't been written to the database yet
                List<EventRecord> unsaved = new ArrayList<>();

                for(EventRecord live: mLiveEvents.values())
                {
                    if(live.getId() == 0 && criteria.matches(live))
                    {
                        unsaved.add(live);
                    }
                }

                unsaved.sort((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()));
                events.addAll(unsaved);
            }

            for(EventRecord stored: result.events())
            {
                //Use the newest live snapshot of a recent event, so that it continues to update
                Long eventKey = mEventKeyById.get(stored.getId());
                EventRecord live = eventKey != null ? mLiveEvents.get(eventKey) : null;
                events.add(live != null ? live : stored);
            }

            preserveSelection(() -> mEvents.setAll(events));
            mTotal = result.total();
            updatePaging();
        }));
    }

    private void updatePaging()
    {
        mPreviousButton.setDisable(mOffset == 0);
        mNextButton.setDisable(mOffset + mPageSize >= mTotal);

        if(mTotal == 0)
        {
            mStatusLabel.setText(mCriteria.isEmpty() ? "No events" : "No matching events");
        }
        else
        {
            mStatusLabel.setText("Events " + (mOffset + 1) + " - " + Math.min(mTotal, mOffset + mPageSize) + " of " +
                mTotal + (mCriteria.isEmpty() ? "" : " matching"));
        }
    }

    private int indexOf(EventRecord event)
    {
        for(int x = 0; x < mEvents.size(); x++)
        {
            if(isSameEvent(mEvents.get(x), event))
            {
                return x;
            }
        }

        return -1;
    }

    private static boolean isSameEvent(EventRecord a, EventRecord b)
    {
        if(a.getEventKey() != 0 && a.getEventKey() == b.getEventKey())
        {
            return true;
        }

        return a.getId() != 0 && a.getId() == b.getId();
    }

    /**
     * Live event snapshot from the event manager.  FX thread.
     */
    void receiveLiveEvent(EventRecord event)
    {
        //Keep the database id that this view already learned for the event
        EventRecord previous = mLiveEvents.get(event.getEventKey());

        if(previous != null && previous.getId() != 0 && event.getId() == 0)
        {
            event = event.copy();
            event.setId(previous.getId());
        }

        mLiveEvents.put(event.getEventKey(), event);
        int index = indexOf(event);
        final EventRecord update = event;

        if(index >= 0)
        {
            preserveSelection(() -> mEvents.set(index, update));
        }
        else if(mOffset == 0 && mCriteria.matches(event))
        {
            //Insert in start time order
            int insert = 0;

            while(insert < mEvents.size() && mEvents.get(insert).getStartTime() > event.getStartTime())
            {
                insert++;
            }

            if(insert < mPageSize)
            {
                final int position = insert;
                preserveSelection(() -> {
                    mEvents.add(position, update);

                    if(mEvents.size() > mPageSize)
                    {
                        mEvents.removeLast();
                    }
                });
                mTotal++;

                updatePaging();
            }
        }
    }

    /**
     * Event written to the database, with its row id.  FX thread.
     */
    void receiveSavedEvent(EventRecord saved)
    {
        if(saved.getEventKey() == 0)
        {
            return;
        }

        mEventKeyById.put(saved.getId(), saved.getEventKey());
        EventRecord live = mLiveEvents.get(saved.getEventKey());

        if(live != null && live.getId() != saved.getId())
        {
            EventRecord updated = live.copy();
            updated.setId(saved.getId());
            mLiveEvents.put(saved.getEventKey(), updated);
            int index = indexOf(updated);

            if(index >= 0)
            {
                preserveSelection(() -> mEvents.set(index, updated));
            }
        }
    }

    private void receive(DatabaseEvent event)
    {
        if(event.type() == DatabaseEvent.Type.CONNECTED || event.type() == DatabaseEvent.Type.EVENTS_CHANGED)
        {
            if(event.type() == DatabaseEvent.Type.CONNECTED)
            {
                mEventKeyById.clear();
            }

            search();
        }
        else if(event.type() == DatabaseEvent.Type.ERROR)
        {
            mStatusLabel.setText("Call database is not available: " + event.errorMessage());
        }
    }
}
