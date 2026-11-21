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

package io.github.dsheirer.gui.broadcast;

import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.streaming.StreamActivityRecorder;
import io.github.dsheirer.database.streaming.StreamActivitySearchCriteria;
import io.github.dsheirer.gui.calls.CallsView;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.gui.table.TableRowCopy;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.swing.SwingSettings;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Streaming tab: the configured streams with their connection status and counts, and below them each call that was
 * selected for a stream - one row per call and stream - with the aliases that selected it and what the stream did
 * with it: pending, streaming/uploading, sent, or the reason that it wasn't sent.
 *
 * Selecting a stream in the streams table shows the calls of that stream.  The rows are stored in the database
 * (see the streaming activity age-off rule) and shown a page at a time, newest first.  On the first page, the rows of
 * this session are added and updated live.  Without a database, the rows of this session are shown.
 */
public class StreamingView extends StackPane
{
    static final String TABLE_KEY = "streaming.activity.table";
    private static final double DEFAULT_DIVIDER = 0.3;
    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private static final ThreadLocal<SimpleDateFormat> STATUS_TIME_FORMAT =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    private final BroadcastModel mBroadcastModel;
    private final StreamActivityModel mActivityModel;
    private final BroadcastStatusView mBroadcastStatusView;
    private final SplitPane mSplitPane = new SplitPane();
    private final ObservableList<StreamActivity> mItems = FXCollections.observableArrayList();
    private final TableView<StreamActivity> mTableView = new TableView<>(mItems);
    private final ComboBox<StreamChoice> mStreamComboBox = new ComboBox<>();
    private final ComboBox<StatusFilter> mStatusComboBox = new ComboBox<>();
    private final TextField mSearchField = new TextField();
    private final Label mSummaryLabel = new Label();
    private final Label mStatusLabel = new Label();
    private final Button mPreviousButton = new Button("Newer");
    private final Button mNextButton = new Button("Older");
    private final ComboBox<Integer> mPageSizeComboBox = new ComboBox<>();
    private final PauseTransition mSearchDelay = new PauseTransition(Duration.millis(400));
    private final DatabaseManager mDatabaseManager;
    private final Listener<DatabaseEvent> mDatabaseListener = event -> Platform.runLater(() -> receive(event));
    private StreamActivitySearchCriteria mCriteria = StreamActivitySearchCriteria.all();
    /**
     * Rows of the current page from the database, newest first
     */
    private List<StreamActivity> mStoredRows = List.of();
    private List<String> mStoredStreamNames = List.of();
    private String mDatabaseError;
    private int mPageSize = CallsView.DEFAULT_PAGE_SIZE;
    private int mOffset = 0;
    private long mTotal = 0;
    private long mSearchSequence = 0;
    private final AtomicBoolean mUpdateScheduled = new AtomicBoolean();
    private final Listener<StreamActivity> mActivityListener = activity -> scheduleUpdate();
    private final javafx.collections.ListChangeListener<ConfiguredBroadcast> mBroadcastListListener =
        change -> scheduleUpdate();
    private boolean mUpdatingStreams;

    /**
     * Stream filter choice: a stream name, or null for all streams.
     */
    record StreamChoice(String name)
    {
        @Override
        public String toString()
        {
            return name != null ? name : "All Streams";
        }
    }

    /**
     * Status filter choices
     */
    enum StatusFilter
    {
        ALL("All Statuses", status -> true),
        PROBLEMS("Not Sent & Errors", StreamStatus::isProblem),
        IN_PROGRESS("Pending & In Progress", status -> status.getCategory() == StreamStatus.Category.IN_PROGRESS),
        SENT("Sent", status -> status == StreamStatus.SENT),
        SKIPPED("Duplicate & Suppressed", status -> status.getCategory() == StreamStatus.Category.SKIPPED);

        private final String mLabel;
        private final Set<StreamStatus> mStatuses;

        StatusFilter(String label, Predicate<StreamStatus> predicate)
        {
            mLabel = label;
            Set<StreamStatus> statuses = EnumSet.noneOf(StreamStatus.class);

            for(StreamStatus status: StreamStatus.values())
            {
                if(predicate.test(status))
                {
                    statuses.add(status);
                }
            }

            //All statuses is no status filter
            mStatuses = statuses.size() == StreamStatus.values().length ? Set.of() : Set.copyOf(statuses);
        }

        /**
         * Statuses that the filter shows, or empty for all
         */
        Set<StreamStatus> getStatuses()
        {
            return mStatuses;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param broadcastModel with the streams and the stream activity
     * @param swingSettings with the column widths of the Swing version of the streams table, migrated once
     * @param databaseManager with the stored rows, or null to show the rows of this session only
     */
    public StreamingView(BroadcastModel broadcastModel, SwingSettings swingSettings, DatabaseManager databaseManager)
    {
        mBroadcastModel = broadcastModel;
        mDatabaseManager = databaseManager;
        mActivityModel = broadcastModel.getStreamActivityModel();
        mBroadcastStatusView = new BroadcastStatusView(broadcastModel, swingSettings);
        mBroadcastStatusView.setMinHeight(90);
        getStyleClass().add("streaming-view");

        URL stylesheet = StreamingView.class.getResource("/broadcast_status.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        VBox activityPane = createActivityPane();
        activityPane.setMinHeight(150);

        mSplitPane.setOrientation(Orientation.VERTICAL);
        mSplitPane.getItems().addAll(mBroadcastStatusView, activityPane);
        SplitPane.setResizableWithParent(mBroadcastStatusView, false);
        mSplitPane.setDividerPositions(clamp(Setting.STREAMING_VIEW_DIVIDER.getDouble(DEFAULT_DIVIDER)));
        //The split pane moves its divider while it lays out, so only store the position after the user drags it
        mSplitPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> storeDivider());
        getChildren().add(mSplitPane);

        //Selecting a stream shows its calls
        mBroadcastStatusView.getTableView().getSelectionModel().selectedItemProperty()
            .addListener((observable, oldValue, selected) -> {
                if(selected != null && selected.getBroadcastConfiguration() != null)
                {
                    selectStream(selected.getBroadcastConfiguration().getName());
                }
            });

        mActivityModel.addListener(mActivityListener);
        mBroadcastModel.getConfiguredBroadcasts().addListener(mBroadcastListListener);

        if(mDatabaseManager != null)
        {
            mDatabaseManager.addListener(mDatabaseListener);
        }

        search();
    }

    /**
     * Deregisters from the models.
     */
    public void dispose()
    {
        mActivityModel.removeListener(mActivityListener);
        mBroadcastModel.getConfiguredBroadcasts().removeListener(mBroadcastListListener);
        mSearchDelay.stop();

        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeListener(mDatabaseListener);
        }
        mBroadcastStatusView.dispose();
    }

    /**
     * Streams table
     */
    public BroadcastStatusView getBroadcastStatusView()
    {
        return mBroadcastStatusView;
    }

    /**
     * Stream calls table
     */
    public TableView<StreamActivity> getTableView()
    {
        return mTableView;
    }

    private VBox createActivityPane()
    {
        createColumns();
        TableColumnMonitor<StreamActivity> monitor = new TableColumnMonitor<>(mTableView, TABLE_KEY);
        mTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        mTableView.setFixedCellSize(26);
        mTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Label placeholder = new Label("No calls have been selected for streaming.  Calls appear here when an " +
            "alias of the call has a stream.");
        placeholder.getStyleClass().add("broadcast-placeholder");
        placeholder.setWrapText(true);
        mTableView.setPlaceholder(placeholder);
        mTableView.setOnKeyPressed(event -> {
            if(TableRowCopy.isCopy(event))
            {
                TableRowCopy.copySelected(mTableView);
                event.consume();
            }
        });
        mTableView.setRowFactory(table -> {
            TableRow<StreamActivity> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && !row.isEmpty() && row.getItem() != null)
                {
                    selectStream(row.getItem().getStreamName());
                }
            });
            return row;
        });
        mTableView.setContextMenu(createContextMenu());
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        IconNode titleIcon = new IconNode(FontAwesome.LIST);
        titleIcon.setIconSize(14);
        titleIcon.getStyleClass().add("broadcast-title-icon");
        Label title = new Label("Streamed Calls", titleIcon);
        title.getStyleClass().add("broadcast-title");
        title.setTooltip(new Tooltip("Each call that an alias selected for a stream, one row per call and stream"));
        mSummaryLabel.getStyleClass().add("broadcast-summary");

        mStreamComboBox.setTooltip(new Tooltip("Show the calls of one stream"));
        mStreamComboBox.getItems().add(new StreamChoice(null));
        mStreamComboBox.getSelectionModel().selectFirst();
        mStreamComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(!mUpdatingStreams)
            {
                applyCriteria();
            }
        });

        mStatusComboBox.getItems().addAll(StatusFilter.values());
        mStatusComboBox.getSelectionModel().select(StatusFilter.ALL);
        mStatusComboBox.setTooltip(new Tooltip("Show the calls with a status"));
        mStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyCriteria());

        mSearchField.setPromptText("Search to, from, alias or detail");
        mSearchField.setPrefColumnCount(18);
        //Search once the user stops typing
        mSearchDelay.setOnFinished(event -> applyCriteria());
        mSearchField.textProperty().addListener((observable, oldValue, newValue) -> mSearchDelay.playFromStart());
        mSearchField.setOnAction(event -> {
            mSearchDelay.stop();
            applyCriteria();
        });

        ColumnConfigurationButton columnsButton = new ColumnConfigurationButton(monitor, "Streamed Calls Columns");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, title, mSummaryLabel, spacer, mStreamComboBox, mStatusComboBox, mSearchField,
            columnsButton);
        toolbar.getStyleClass().add("broadcast-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox pane = new VBox(toolbar, mTableView, createPagingBar());
        pane.getStyleClass().addAll("broadcast-status-view", "stream-activity-view");
        return pane;
    }

    private ContextMenu createContextMenu()
    {
        MenuItem copy = new MenuItem("Copy");
        copy.setAccelerator(TableRowCopy.COPY_KEY);
        copy.setOnAction(event -> TableRowCopy.copySelected(mTableView));
        MenuItem showStream = new MenuItem("Show Only This Stream");
        showStream.setOnAction(event -> {
            StreamActivity selected = mTableView.getSelectionModel().getSelectedItem();

            if(selected != null)
            {
                selectStream(selected.getStreamName());
            }
        });
        MenuItem showAll = new MenuItem("Show All Streams");
        showAll.setOnAction(event -> selectStream(null));
        ContextMenu menu = new ContextMenu(copy, new javafx.scene.control.SeparatorMenuItem(), showStream, showAll);
        menu.setOnShowing(event -> showStream.setDisable(mTableView.getSelectionModel().getSelectedItem() == null));
        return menu;
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

        mPageSize = CallsView.getStoredPageSize(Setting.STREAMING_VIEW_PAGE_SIZE.getInt(CallsView.DEFAULT_PAGE_SIZE));
        mPageSizeComboBox.getItems().addAll(CallsView.PAGE_SIZES);
        mPageSizeComboBox.setValue(mPageSize);
        mPageSizeComboBox.setTooltip(new Tooltip("Number of rows per page"));
        mPageSizeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null && newValue != mPageSize)
            {
                //Keep the first displayed row on the new page
                mOffset = (mOffset / newValue) * newValue;
                mPageSize = newValue;
                Setting.STREAMING_VIEW_PAGE_SIZE.putInt(newValue);
                search();
            }
        });
        Label pageSizeLabel = new Label("Page size:");
        pageSizeLabel.setLabelFor(mPageSizeComboBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(6, mStatusLabel, spacer, pageSizeLabel, mPageSizeComboBox, refreshButton,
            mPreviousButton, mNextButton);
        bar.getStyleClass().add("broadcast-paging-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    /**
     * Builds the search criteria from the filters and searches from the first page.
     */
    private void applyCriteria()
    {
        StreamChoice stream = mStreamComboBox.getValue();
        StatusFilter status = mStatusComboBox.getValue() != null ? mStatusComboBox.getValue() : StatusFilter.ALL;
        StreamActivitySearchCriteria criteria = new StreamActivitySearchCriteria(stream != null ? stream.name() : null,
            status.getStatuses(), mSearchField.getText());

        if(!criteria.equals(mCriteria))
        {
            mCriteria = criteria;
            mOffset = 0;
            search();
        }
    }

    /**
     * Searches the database for the current page.  The rows of this session are shown when there's no database.
     */
    private void search()
    {
        if(mDatabaseManager == null || !mDatabaseManager.isConnected())
        {
            mDatabaseError = mDatabaseManager == null ? null : "The call database is not available" +
                (mDatabaseManager.getErrorMessage() != null ? ": " + mDatabaseManager.getErrorMessage() : "");
            mStoredRows = List.of();
            mTotal = 0;
            mOffset = 0;
            update();
            return;
        }

        long sequence = ++mSearchSequence;
        StreamActivitySearchCriteria criteria = mCriteria;
        int offset = mOffset;
        int pageSize = mPageSize;
        mStatusLabel.setText("Searching ...");

        mDatabaseManager.searchStreamActivity(criteria, offset, pageSize).whenComplete((result, error) ->
            Platform.runLater(() -> {
                if(sequence != mSearchSequence)
                {
                    return;
                }

                if(error != null)
                {
                    Throwable cause = error.getCause() != null ? error.getCause() : error;
                    mDatabaseError = "Search failed: " + cause.getMessage();
                    mStoredRows = List.of();
                    mTotal = 0;
                }
                else
                {
                    mDatabaseError = null;
                    mStoredRows = result.records().stream().map(StreamActivityRecorder::toActivity).toList();
                    mStoredStreamNames = result.streamNames();
                    mTotal = result.total();
                }

                update();
            }));
    }

    private void receive(DatabaseEvent event)
    {
        switch(event.type())
        {
            case CONNECTED, STREAM_ACTIVITY_CHANGED -> search();
            case ERROR -> {
                mDatabaseError = "The call database is not available: " + event.errorMessage();
                update();
            }
            default -> {}
        }
    }

    /**
     * Shows the calls of the stream, or of all streams when the name is null.
     */
    void selectStream(String streamName)
    {
        StreamChoice choice = new StreamChoice(streamName);

        if(!mStreamComboBox.getItems().contains(choice))
        {
            mStreamComboBox.getItems().add(choice);
        }

        mStreamComboBox.getSelectionModel().select(choice);
    }

    private void createColumns()
    {
        mTableView.getColumns().add(column("start", "Call Start", 140, a -> formatTime(a.getCallStart())));
        mTableView.getColumns().add(column("stream", "Stream", 160, StreamActivity::getStreamName));
        TableColumn<StreamActivity,String> type = column("type", "Stream Type", 120,
            a -> a.getServerType() != null ? a.getServerType().toString() : "");
        type.setVisible(false);
        mTableView.getColumns().add(type);
        mTableView.getColumns().add(createStatusColumn());
        mTableView.getColumns().add(tooltipColumn("aliases", "Selected By Alias", 240,
            StreamActivity::getSelectingAliasText,
            a -> a.getSelectingAliases().isEmpty() ? null : "Aliases of the call that stream to " +
                a.getStreamName() + ":\n" + a.getSelectingAliases().stream().map(Object::toString)
                .collect(Collectors.joining("\n"))));
        mTableView.getColumns().add(column("to", "To", 90, StreamActivity::getTo));
        mTableView.getColumns().add(column("from", "From", 90, StreamActivity::getFrom));
        mTableView.getColumns().add(column("duration", "Duration", 70, a -> formatDuration(a.getDuration())));
        mTableView.getColumns().add(tooltipColumn("detail", "Detail", 360, StreamActivity::getDetail,
            StreamActivity::getDetail));
        TableColumn<StreamActivity,String> updated = column("updated", "Updated", 70,
            a -> STATUS_TIME_FORMAT.get().format(new Date(a.getStatusTime())));
        updated.setVisible(false);
        mTableView.getColumns().add(updated);
        TableColumn<StreamActivity,String> aliasList = column("aliasList", "Alias List", 120,
            StreamActivity::getAliasList);
        aliasList.setVisible(false);
        mTableView.getColumns().add(aliasList);
        mTableView.getColumns().add(column("protocol", "Protocol", 80, StreamActivity::getProtocol));
        mTableView.getColumns().add(column("system", "System", 110, StreamActivity::getSystem));
        TableColumn<StreamActivity,String> site = column("site", "Site", 90, StreamActivity::getSite);
        site.setVisible(false);
        mTableView.getColumns().add(site);
        mTableView.getColumns().add(column("channel", "Channel", 110, StreamActivity::getChannel));

        for(TableColumn<StreamActivity,?> column: mTableView.getColumns())
        {
            column.setSortable(false);
        }
    }

    private TableColumn<StreamActivity,StreamActivity> createStatusColumn()
    {
        TableColumn<StreamActivity,StreamActivity> column = new TableColumn<>("Status");
        column.setId("status");
        column.setPrefWidth(110);
        column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue()));
        TableRowCopy.setCopyText(column, activity -> activity.getStatus().toString());
        column.setCellFactory(param -> new TableCell<>()
        {
            private final Label mChip = new Label();

            {
                mChip.getStyleClass().add("broadcast-status-chip");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(StreamActivity item, boolean empty)
            {
                super.updateItem(item, empty);

                if(empty || item == null)
                {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }

                StreamStatus status = item.getStatus();
                mChip.setText(status.toString());
                mChip.getStyleClass().removeIf(styleClass -> styleClass.startsWith("broadcast-status-severity-"));
                mChip.getStyleClass().add("broadcast-status-severity-" + getSeverity(status));
                setGraphic(mChip);
                setTooltip(new Tooltip(status + " at " + STATUS_TIME_FORMAT.get().format(new Date(
                    item.getStatusTime())) + (item.getDetail() != null ? "\n" + item.getDetail() : "")));
            }
        });
        return column;
    }

    /**
     * Status chip style suffix for the status
     */
    static String getSeverity(StreamStatus status)
    {
        return switch(status.getCategory())
        {
            case IN_PROGRESS -> "active";
            case SENT -> "ok";
            case SKIPPED -> "neutral";
            case NOT_SENT -> "warning";
            case ERROR -> "error";
        };
    }

    private static TableColumn<StreamActivity,String> column(String id, String title, double width,
                                                             Function<StreamActivity,String> value)
    {
        TableColumn<StreamActivity,String> column = new TableColumn<>(title);
        column.setId(id);
        column.setPrefWidth(width);
        column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue() != null ?
            value.apply(cdf.getValue()) : null));
        return column;
    }

    private static TableColumn<StreamActivity,String> tooltipColumn(String id, String title, double width,
                                                                    Function<StreamActivity,String> value,
                                                                    Function<StreamActivity,String> tooltip)
    {
        TableColumn<StreamActivity,String> column = column(id, title, width, value);
        column.setCellFactory(param -> new TableCell<>()
        {
            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                TableRow<StreamActivity> row = getTableRow();
                StreamActivity activity = empty || row == null ? null : row.getItem();
                String text = activity != null ? tooltip.apply(activity) : null;
                setTooltip(text != null && !text.isEmpty() ? new Tooltip(text) : null);
            }
        });
        return column;
    }

    /**
     * Schedules an update on the FX thread.  Rows are added and updated by the streaming threads, often several at a
     * time, so the updates are coalesced.
     */
    private void scheduleUpdate()
    {
        if(mUpdateScheduled.compareAndSet(false, true))
        {
            Platform.runLater(() -> {
                mUpdateScheduled.set(false);
                update();
            });
        }
    }

    /**
     * Updates the rows, stream choices and summary from the model.  FX thread only.
     */
    private void update()
    {
        List<StreamActivity> activities = mActivityModel.getActivities();
        updateStreamChoices(activities);
        List<StreamActivity> rows = getRows(activities, mStoredRows, mCriteria, mOffset, mPageSize);

        if(!rows.equals(mItems))
        {
            TableRowCopy.preserveSelection(mTableView, activity -> List.of(activity.getKey()),
                () -> mItems.setAll(rows));
        }
        else
        {
            mTableView.refresh();
        }

        mSummaryLabel.setText(getSummary(activities));
        updatePaging(rows.size());
    }

    /**
     * Rows of the page: the stored rows, with the live row in place of the stored row of the same call and stream so
     * that it continues to update.  The first page also has the live rows that match the criteria, newest first, up to
     * the page size.
     * @param live rows of this session, oldest first
     * @param stored rows of the page from the database, newest first
     * @param criteria of the search
     * @param offset of the page
     * @param pageSize maximum rows
     * @return rows to show, newest first
     */
    static List<StreamActivity> getRows(List<StreamActivity> live, List<StreamActivity> stored,
                                        StreamActivitySearchCriteria criteria, int offset, int pageSize)
    {
        Map<String,StreamActivity> liveByKey = new HashMap<>();

        for(StreamActivity activity: live)
        {
            liveByKey.put(activity.getKey(), activity);
        }

        Map<String,StreamActivity> rows = new LinkedHashMap<>();

        for(StreamActivity activity: stored)
        {
            rows.put(activity.getKey(), liveByKey.getOrDefault(activity.getKey(), activity));
        }

        if(offset > 0)
        {
            return new ArrayList<>(rows.values());
        }

        for(StreamActivity activity: live)
        {
            if(criteria.matches(activity))
            {
                rows.put(activity.getKey(), activity);
            }
        }

        List<StreamActivity> sorted = new ArrayList<>(rows.values());
        sorted.sort(Comparator.comparingLong(StreamActivity::getCallStart).reversed());
        return sorted.size() > pageSize ? new ArrayList<>(sorted.subList(0, pageSize)) : sorted;
    }

    private void updatePaging(int shown)
    {
        long total = Math.max(mTotal, mOffset + shown);
        mPreviousButton.setDisable(mOffset == 0);
        mNextButton.setDisable(mOffset + mPageSize >= mTotal);

        String text;

        if(shown == 0)
        {
            text = mCriteria.isEmpty() ? "No streamed calls" : "No matching streamed calls";
        }
        else
        {
            text = "Rows " + (mOffset + 1) + " - " + (mOffset + shown) + " of " + total +
                (mCriteria.isEmpty() ? "" : " matching");
        }

        if(mDatabaseError != null)
        {
            text += "  -  " + mDatabaseError + (mOffset == 0 ? " - showing the calls since startup" : "");
        }
        else if(mDatabaseManager == null)
        {
            text += "  -  calls since startup";
        }

        mStatusLabel.setText(text);
    }

    /**
     * Adds the configured streams and the streams of the rows (e.g. a stream name that no stream has) to the stream
     * choices, keeping the selected choice.
     */
    private void updateStreamChoices(List<StreamActivity> activities)
    {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for(ConfiguredBroadcast broadcast: mBroadcastModel.getConfiguredBroadcasts())
        {
            if(broadcast.getBroadcastConfiguration().getName() != null)
            {
                names.add(broadcast.getBroadcastConfiguration().getName());
            }
        }

        for(StreamActivity activity: activities)
        {
            if(activity.getStreamName() != null)
            {
                names.add(activity.getStreamName());
            }
        }

        names.addAll(mStoredStreamNames);
        StreamChoice selected = mStreamComboBox.getValue();

        if(selected != null && selected.name() != null)
        {
            names.add(selected.name());
        }

        List<StreamChoice> choices = new ArrayList<>();
        choices.add(new StreamChoice(null));
        names.forEach(name -> choices.add(new StreamChoice(name)));

        if(!choices.equals(mStreamComboBox.getItems()))
        {
            mUpdatingStreams = true;
            mStreamComboBox.getItems().setAll(choices);
            mStreamComboBox.getSelectionModel().select(selected != null ? selected : choices.getFirst());
            mUpdatingStreams = false;
        }
    }

    /**
     * Summary of the row status counts, for example: 120 calls · 2 pending · 3 not sent · 1 error
     */
    static String getSummary(List<StreamActivity> activities)
    {
        if(activities.isEmpty())
        {
            return "";
        }

        int inProgress = 0;
        int notSent = 0;
        int errors = 0;

        for(StreamActivity activity: activities)
        {
            switch(activity.getStatus().getCategory())
            {
                case IN_PROGRESS -> inProgress++;
                case NOT_SENT -> notSent++;
                case ERROR -> errors++;
                default -> {}
            }
        }

        List<String> parts = new ArrayList<>();
        parts.add("Since startup: " + String.format("%,d", activities.size()) +
            (activities.size() == 1 ? " call" : " calls"));

        if(inProgress > 0)
        {
            parts.add(inProgress + " pending");
        }

        if(notSent > 0)
        {
            parts.add(notSent + " not sent");
        }

        if(errors > 0)
        {
            parts.add(errors + (errors == 1 ? " error" : " errors"));
        }

        return String.join("  ·  ", parts);
    }

    private void storeDivider()
    {
        if(!mSplitPane.getDividers().isEmpty() && getScene() != null)
        {
            Setting.STREAMING_VIEW_DIVIDER.putDouble(clamp(mSplitPane.getDividers().getFirst().getPosition()));
        }
    }

    private static double clamp(double value)
    {
        return Double.isNaN(value) ? DEFAULT_DIVIDER : Math.max(0.1, Math.min(0.8, value));
    }

    private static String formatTime(long time)
    {
        return time > 0 ? TIME_FORMAT.get().format(new Date(time)) : "";
    }

    private static String formatDuration(long milliseconds)
    {
        long seconds = Math.round(milliseconds / 1000.0);
        return seconds >= 60 ? String.format("%d:%02d", seconds / 60, seconds % 60) : seconds + " s";
    }
}
