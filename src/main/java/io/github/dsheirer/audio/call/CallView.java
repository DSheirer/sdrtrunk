/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.audio.call;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.audio.AudioManager;
import io.github.dsheirer.audio.playbackfx.AudioPlaybackChannelController;
import io.github.dsheirer.audio.playbackfx.AudioPlaybackController;
import io.github.dsheirer.audio.playbackfx.PlaybackMode;
import io.github.dsheirer.gui.control.TimestampTableCellFactory;
import io.github.dsheirer.gui.javafx.table.TableColumnEditor;
import io.github.dsheirer.gui.javafx.table.TableViewColumnController;
import io.github.dsheirer.source.mixer.MixerChannel;
import io.github.dsheirer.util.ThreadPool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import jiconfont.icons.elusive.Elusive;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Audio call events playback and audio event database control/view.
 */
public class CallView extends VBox implements IPageRequestListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CallView.class);
    private static final Sort DEFAULT_SORT = Sort.by("eventTime").descending();

    @Resource
    private AudioManager mAudioManager;
    @Resource
    private CallRepository mCallRepository;
    @Resource
    private AudioPlaybackController mAudioPlaybackController;

    private CallEventListener mCallEventListener = new CallEventListener();
    private SortOrderListener mSortOrderListener = new SortOrderListener();
    private ObservableList<Call> mCalls = FXCollections.observableArrayList();
    private TableView<Call> mCallTableView;
    private TableViewColumnController mCallTableViewMonitor;
    private ProgressIndicator mPlaceholderProgressIndicator;
    private HBox mSearchBox;
    private HBox mPagingBox;
    private TextField mSearchField;
    private PagingController mPagingController;
    private ComboBox<Integer> mPageSizeComboBox;
    private Label mCallsCountLabel;
    private LongProperty mCallCount = new SimpleLongProperty();
    private Sort mSort = DEFAULT_SORT;
    private ComboBox<SearchColumn> mSearchColumnComboBox;
    private CheckBox mSearchExactCheckBox;
    private AudioPlaybackControlView mAudioPlaybackControlView;
    private Button mAutoModeLeftButton;
    private Button mAutoModeRightButton;
    private Button mLockModeLeftButton;
    private Button mLockModeRightButton;
    private TableColumnEditor mTableColumnEditor;
    private SplitPane mTableSplitPane;
    private Button mConfigureColumnsButton;

    /**
     * Constructs an instance
     */
    public CallView()
    {
        mCallCount.addListener((observable, oldValue, newValue) -> {
            getCallsCountLabel().setText("Calls: " + newValue);
            int pageCount = (int)(mCallCount.get() / getPageSize());
            if(mCallCount.get() % getPageSize() != 0)
            {
                pageCount++;
            }
            getPagingController().update(getPagingController().getCurrentPage(), pageCount);
        });
    }

    /**
     * Post instantiation/startup steps.
     */
    @PostConstruct
    public void postConstruct()
    {
        //Register the audio playback control view as a playback status listener with the primary audio channel controller
        mAudioPlaybackController.getPrimaryController().add(getAudioPlaybackControlView());

        HBox controlBox = new HBox();
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setSpacing(2);
        controlBox.setPadding(new Insets(5, 5, 5, 5));
        getAudioPlaybackControlView().setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(getSearchBox(), Priority.ALWAYS);
        getSearchBox().setAlignment(Pos.CENTER_RIGHT);

        Separator separator1 = new Separator(Orientation.VERTICAL);
        separator1.setPadding(new Insets(0, 10, 0, 10));
        Separator separator2 = new Separator(Orientation.VERTICAL);
        separator2.setPadding(new Insets(0, 10, 0, 10));
        Label autoLabel = new Label("Auto:");
        Label lockLabel = new Label("Lock:");
        getSearchExactCheckBox().setPadding(new Insets(0, 10, 0, 0));
        controlBox.getChildren().addAll(getAudioPlaybackControlView(), separator1, autoLabel, getAutoModeLeftButton(),
                getAutoModeRightButton(), separator2, lockLabel, getLockModeLeftButton(), getLockModeRightButton(),
                getSearchBox(), getConfigureColumnsButton());

        getTableColumnEditor().closedProperty().addListener((observable, previouslyClosed, closed) -> {
            if(closed && !previouslyClosed)
            {
                getTableSplitPane().getItems().remove(getTableColumnEditor());
            }
        });

        VBox.setVgrow(getTableSplitPane(), Priority.ALWAYS);
        getChildren().addAll(controlBox, getTableSplitPane(), getPagingBox());

        getPagingController().setPadding(new Insets(5, 5, 5, 5));
        getPagingController().update(1, 20);

        //Register for call event notifications
        mAudioManager.add(mCallEventListener);

        getPlaceholderProgressIndicator().setVisible(true);
    }

    /**
     * Pre-destruction or shutdown steps
     */
    @PreDestroy
    public void preDestroy()
    {
        //De-register from call event notifications
        mAudioManager.remove(mCallEventListener);
    }

    /**
     * Shows first page of results.
     */
    private void showFirstPage()
    {
        showPage(1);
    }

    /**
     * Sets the page to view.  This implements the IPageRequestListener interface.
     * @param page number to display.
     */
    @Override
    public void showPage(int page)
    {
        if(page >= 1)
        {
            Platform.runLater(() -> getPlaceholderProgressIndicator().setVisible(true));

            final String searchTerm = getSearchField().getText();
            final SearchColumn searchColumn = getSearchColumnComboBox().getSelectionModel().getSelectedItem();
            final boolean wildcard = !getSearchExactCheckBox().isSelected();

            //Spin the query off onto a cached thread and then place the results back onto the FX thread.
            ThreadPool.CACHED.submit(() ->
            {
                final PageRequest pageRequest = PageRequest.of(page - 1, getPageSize(), mSort);

                try
                {
                    Page<Call> callPage = null;

                    if(searchTerm != null && !searchTerm.isEmpty() && searchColumn != null)
                    {
                        final String wildcardSearch = (wildcard ? "%" : "") + searchTerm + (wildcard ? "%" : "");

                        switch(searchColumn)
                        {
                            case ANY_ID ->  callPage = mCallRepository.findByAnyIdWithPagination(wildcardSearch, pageRequest);
                            case CALL_TYPE -> callPage = mCallRepository.findByCallTypeWithPagination(wildcardSearch, pageRequest);
                            case CHANNEL -> callPage = mCallRepository.findByChannelWithPagination(wildcardSearch, pageRequest);
                            case FROM -> callPage = mCallRepository.findByFromWithPagination(wildcardSearch, pageRequest);
                            case PROTOCOL -> callPage = mCallRepository.findByProtocolWithPagination(wildcardSearch, pageRequest);
                            case SITE -> callPage = mCallRepository.findBySiteWithPagination(wildcardSearch, pageRequest);
                            case SYSTEM -> callPage = mCallRepository.findBySystemWithPagination(wildcardSearch, pageRequest);
                            case TO -> callPage = mCallRepository.findByToWithPagination(wildcardSearch, pageRequest);
                        }
                    }
                    else
                    {
                        callPage = mCallRepository.findAll(pageRequest);
                    }

                    if(callPage != null)
                    {
                        final Page<Call> finalCallPage = callPage;

                        Platform.runLater(() -> {
                            mCalls.clear();
                            mCalls.addAll(finalCallPage.stream().toList());
                            getPlaceholderProgressIndicator().setVisible(false);
                            getPagingController().update(finalCallPage.getNumber() + 1, finalCallPage.getTotalPages());
                            mCallCount.set(finalCallPage.getTotalElements());
                        });
                    }
                }
                catch(Throwable t)
                {
                    LOGGER.error("Error submitting paging [" + page + "] query to database", t);

                    Platform.runLater(() -> {
                        getPlaceholderProgressIndicator().setVisible(false);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        if(getParent() != null)
                        {
                            alert.initOwner(getParent().getScene().getWindow());
                        }
                        alert.setHeaderText("Database Error!");
                        alert.setContentText("Error retrieving page " + page + " from the database - reason: " + t.getMessage());
                        alert.showAndWait();
                    });
                }
            });
        }
        else
        {
            LOGGER.warn("Invalid page [" + page + "] number request");
        }
    }

    /**
     * Updates the sort order for the table and resets the page to page 1.
     */
    private void updateSortOrder()
    {
        mSort = null;

        for(TableColumn<Call,?> tableColumn: getCallTableView().getSortOrder())
        {
            Sort columnSort = Sort.by(tableColumn.getId());
            if(tableColumn.getSortType() == TableColumn.SortType.DESCENDING)
            {
                columnSort = columnSort.descending();
            }

            if(mSort == null)
            {
                mSort = columnSort;
            }
            else
            {
                mSort = mSort.and(columnSort);
            }
        }

        if(mSort == null)
        {
            mSort = DEFAULT_SORT;
        }

        showFirstPage();
    }

    /**
     * Current page size
     */
    private int getPageSize()
    {
        return getPageSizeComboBox().getValue();
    }

    /**
     * Configure column order and visibility button
     */
    private Button getConfigureColumnsButton()
    {
        if(mConfigureColumnsButton == null)
        {
            mConfigureColumnsButton = new Button();
            mConfigureColumnsButton.setTooltip(new Tooltip("Configure call table column ordering and visibility"));
            IconNode icon = new IconNode(FontAwesome.COG);
            mConfigureColumnsButton.setGraphic(icon);
            mConfigureColumnsButton.setOnAction(event -> {
                //Set the split pane to expose the column configuration panel.
                if(!getTableSplitPane().getItems().contains(getTableColumnEditor()))
                {
                    getTableSplitPane().getItems().add(getTableColumnEditor());
                }
                getTableColumnEditor().closedProperty().set(false);
                double splitWidth = getTableSplitPane().getWidth();
                double scale = 0.5;
                if(splitWidth > 260)
                {
                    scale = (splitWidth - 260) / splitWidth;
                }
                getTableSplitPane().setDividerPosition(0, scale);
            });
        }

        return mConfigureColumnsButton;
    }

    /**
     * Split pane for call table and table column editor
     */
    private SplitPane getTableSplitPane()
    {
        if(mTableSplitPane == null)
        {
            mTableSplitPane = new SplitPane(getCallTableView());
        }

        return mTableSplitPane;
    }

    /**
     * Table column order/visibility editor
     */
    private TableColumnEditor getTableColumnEditor()
    {
        if(mTableColumnEditor == null)
        {
            mTableColumnEditor = new TableColumnEditor(getCallTableView(), mCallTableViewMonitor);
        }

        return mTableColumnEditor;
    }

    /**
     * Sets playback mode to auto
     */
    private Button getAutoModeLeftButton()
    {
        if(mAutoModeLeftButton == null)
        {
            mAutoModeLeftButton = new Button("Left");
            mAutoModeLeftButton.setTooltip(new Tooltip("Set left audio channel to auto playback mode"));
            AudioPlaybackChannelController left = mAudioPlaybackController.getChannelController(MixerChannel.LEFT);
            mAutoModeLeftButton.disableProperty().bind(left.playbackModeProperty().isEqualTo(PlaybackMode.AUTO));
            mAutoModeLeftButton.setOnAction(event -> {
                final AudioPlaybackChannelController controller = mAudioPlaybackController.getChannelController(MixerChannel.LEFT);
                if(controller != null)
                {
                    getCallTableView().getSelectionModel().clearSelection();
                    controller.auto();
                }
            });
        }

        return mAutoModeLeftButton;
    }

    /**
     * Sets playback mode to auto
     */
    private Button getAutoModeRightButton()
    {
        if(mAutoModeRightButton == null)
        {
            mAutoModeRightButton = new Button("Right");
            mAutoModeRightButton.setTooltip(new Tooltip("Set right audio channel to auto playback mode"));
            AudioPlaybackChannelController right = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
            mAutoModeRightButton.disableProperty().bind(right.playbackModeProperty().isEqualTo(PlaybackMode.AUTO));
            mAutoModeRightButton.setOnAction(event -> {
                final AudioPlaybackChannelController controller = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
                if(controller != null)
                {
                    getCallTableView().getSelectionModel().clearSelection();
                    controller.auto();
                }
            });
        }

        return mAutoModeRightButton;
    }

    /**
     * Sets playback mode to auto - locked to the currently selected TO identifier to the left channel
     */
    private Button getLockModeLeftButton()
    {
        if(mLockModeLeftButton == null)
        {
            mLockModeLeftButton = new Button("Left");
            mLockModeLeftButton.setTooltip(new Tooltip("Lock the left audio channel for auto playback of selected call talkgroup"));
            mLockModeLeftButton.setDisable(true);
            mLockModeLeftButton.setOnAction(event -> {
                final AudioPlaybackChannelController left = mAudioPlaybackController.getChannelController(MixerChannel.LEFT);
                if(left != null)
                {
                    final Call selected = getCallTableView().getSelectionModel().getSelectedItem();
                    getCallTableView().getSelectionModel().clearSelection();
                    getLockModeLeftButton().setDisable(true);

                    if(selected != null)
                    {
                        //If the right controller is locked for this same call, reset it to auto so that only 1 is locked
                        final AudioPlaybackChannelController right = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
                        if(right.isLockedFor(selected))
                        {
                            right.auto();
                        }

                        left.lockAutoTo(selected.getToId(), selected.getSystem());
                    }
                    else
                    {
                        left.auto();
                    }
                }
            });
        }

        return mLockModeLeftButton;
    }

    /**
     * Sets playback mode to auto - locked to the currently selected TO identifier to the left channel
     */
    private Button getLockModeRightButton()
    {
        if(mLockModeRightButton == null)
        {
            mLockModeRightButton = new Button("Right");
            mLockModeRightButton.setTooltip(new Tooltip("Lock the left audio channel for auto playback of selected call talkgroup"));
            mLockModeRightButton.setDisable(true);
            mLockModeRightButton.setOnAction(event -> {
                final AudioPlaybackChannelController right = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
                if(right != null)
                {
                    final Call selected = getCallTableView().getSelectionModel().getSelectedItem();
                    getCallTableView().getSelectionModel().clearSelection();
                    getLockModeRightButton().setDisable(true);

                    if(selected != null)
                    {
                        //If the left controller is locked for this same call, reset it to auto so that only 1 is locked
                        final AudioPlaybackChannelController left = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
                        if(left.isLockedFor(selected))
                        {
                            left.auto();
                        }

                        right.lockAutoTo(selected.getToId(), selected.getSystem());
                    }
                    else
                    {
                        right.auto();
                    }
                }
            });
        }

        return mLockModeRightButton;
    }

    /**
     * Audio playback control view.
     */
    private AudioPlaybackControlView getAudioPlaybackControlView()
    {
        if(mAudioPlaybackControlView == null)
        {
            mAudioPlaybackControlView = new AudioPlaybackControlView();
        }

        return mAudioPlaybackControlView;
    }

    /**
     * Calls label to display the number of calls in the database.
     */
    private Label getCallsCountLabel()
    {
        if(mCallsCountLabel == null)
        {
            mCallsCountLabel = new Label("Calls: 0");
        }

        return mCallsCountLabel;
    }

    /**
     * Toggles wildcard searching
     * @return true for exact match or false for wildcard/fuzzy matching.
     */
    private CheckBox getSearchExactCheckBox()
    {
        if(mSearchExactCheckBox == null)
        {
            mSearchExactCheckBox = new CheckBox("Exact Match");
            mSearchExactCheckBox.setTooltip(new Tooltip("Toggle exact matching (checked) or wildcard/fuzzy " +
                    "matching (unchecked)"));
            mSearchExactCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> showFirstPage());
        }

        return mSearchExactCheckBox;
    }

    /**
     * Page size selection combo box.
     *
     * Note: when the user changes the page size, we always request page 1 again.
     */
    private ComboBox<Integer> getPageSizeComboBox()
    {
        if(mPageSizeComboBox == null)
        {
            ObservableList<Integer> pageSizes = FXCollections.observableArrayList();
            pageSizes.addAll(25, 50, 100, 200);
            mPageSizeComboBox = new ComboBox<>(pageSizes);
            mPageSizeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showFirstPage());
            mPageSizeComboBox.setTooltip(new Tooltip("Select the page size"));
            mPageSizeComboBox.getSelectionModel().select(0);
        }

        return mPageSizeComboBox;
    }

    /**
     * Page size selection combo box.
     *
     * Note: when the user changes the page size, we always request page 1 again.
     */
    private ComboBox<SearchColumn> getSearchColumnComboBox()
    {
        if(mSearchColumnComboBox == null)
        {
            mSearchColumnComboBox = new ComboBox<>(FXCollections.observableArrayList(SearchColumn.values()));
            mSearchColumnComboBox.getSelectionModel().select(SearchColumn.ANY_ID);
            mSearchColumnComboBox.setTooltip(new Tooltip("Select the field(s) to search against"));
        }

        return mSearchColumnComboBox;
    }

    /**
     * Paging controls panel
     */
    private HBox getPagingBox()
    {
        if(mPagingBox == null)
        {
            mPagingBox = new HBox();
            mPagingBox.setAlignment(Pos.CENTER_LEFT);
            mPagingBox.setSpacing(3);
            Label pageSizeLabel = new Label("Page Size:");
            pageSizeLabel.setPadding(new Insets(0, 2, 0, 10));
            getCallsCountLabel().setPadding(new Insets(0, 0, 0, 10));
            mPagingBox.getChildren().addAll(getPagingController(), pageSizeLabel, getPageSizeComboBox(), getCallsCountLabel());
        }

        return mPagingBox;
    }

    /**
     * Call table view
     * @return
     */
    private TableView<Call> getCallTableView()
    {
        if(mCallTableView == null)
        {
            mCallTableView = new TableView<>(mCalls);
            mCallTableView.setMaxWidth(Double.MAX_VALUE);

            TableColumn playbackColumn = new TableColumn();
            playbackColumn.setSortable(false);
            playbackColumn.setId("playback");
            playbackColumn.sortTypeProperty().addListener(mSortOrderListener);
            IconNode iconNode = new IconNode(Elusive.VOLUME_UP);
            iconNode.setIconSize(16);
            iconNode.setFill(Color.BLACK);
            playbackColumn.setGraphic(iconNode);
            playbackColumn.setCellValueFactory(new PropertyValueFactory<>("playbackChannel"));
            playbackColumn.setPrefWidth(40);

            TableColumn eventTimeColumn = new TableColumn();
            eventTimeColumn.setId("eventTime");
            eventTimeColumn.sortTypeProperty().addListener(mSortOrderListener);
            eventTimeColumn.setText("Date/Time");
            eventTimeColumn.setCellValueFactory(new PropertyValueFactory<Call, Long>(Call.COLUMN_EVENT_TIME));
            eventTimeColumn.setCellFactory(new TimestampTableCellFactory<Call>());
            eventTimeColumn.setPrefWidth(160);

            TableColumn durationColumn = new TableColumn();
            durationColumn.setId("duration");
            durationColumn.sortTypeProperty().addListener(mSortOrderListener);
            durationColumn.setText("Duration");
            durationColumn.setCellValueFactory(new PropertyValueFactory<Call, Double>(Call.COLUMN_DURATION));
            durationColumn.setPrefWidth(70);

            TableColumn callTypeColumn = new TableColumn();
            callTypeColumn.setId("callType");
            callTypeColumn.sortTypeProperty().addListener(mSortOrderListener);
            callTypeColumn.setText("Type");
            callTypeColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_CALL_TYPE));
            callTypeColumn.setPrefWidth(85);

            TableColumn toIdColumn = new TableColumn();
            toIdColumn.setId("toId");
            toIdColumn.sortTypeProperty().addListener(mSortOrderListener);
            toIdColumn.setText("To");
            toIdColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_TO_ID));
            toIdColumn.setPrefWidth(80);

            TableColumn toAliasColumn = new TableColumn();
            toAliasColumn.setId("toAlias");
            toAliasColumn.sortTypeProperty().addListener(mSortOrderListener);
            toAliasColumn.setText("To Alias");
            toAliasColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_TO_ALIAS));
            toAliasColumn.setPrefWidth(100);

            TableColumn fromIdColumn = new TableColumn();
            fromIdColumn.setId("fromId");
            fromIdColumn.sortTypeProperty().addListener(mSortOrderListener);
            fromIdColumn.setText("From");
            fromIdColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_FROM_ID));
            fromIdColumn.setPrefWidth(70);

            TableColumn fromAliasColumn = new TableColumn();
            fromAliasColumn.setId("fromAlias");
            fromAliasColumn.sortTypeProperty().addListener(mSortOrderListener);
            fromAliasColumn.setText("From Alias");
            fromAliasColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_FROM_ALIAS));
            fromAliasColumn.setPrefWidth(100);

            TableColumn protocolColumn = new TableColumn();
            protocolColumn.setId("protocol");
            protocolColumn.sortTypeProperty().addListener(mSortOrderListener);
            protocolColumn.setText("Protocol");
            protocolColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_PROTOCOL));
            protocolColumn.setPrefWidth(90);

            TableColumn frequencyColumn = new TableColumn();
            frequencyColumn.setId("frequency");
            frequencyColumn.sortTypeProperty().addListener(mSortOrderListener);
            frequencyColumn.setText("Frequency");
            frequencyColumn.setCellValueFactory(new PropertyValueFactory<Call, Double>(Call.COLUMN_FREQUENCY));
            frequencyColumn.setPrefWidth(80);

            TableColumn systemColumn = new TableColumn();
            systemColumn.setId("system");
            systemColumn.sortTypeProperty().addListener(mSortOrderListener);
            systemColumn.setText("System");
            systemColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_SYSTEM));
            systemColumn.setPrefWidth(80);

            TableColumn siteColumn = new TableColumn();
            siteColumn.setVisible(false);
            siteColumn.setId("mSite");
            siteColumn.sortTypeProperty().addListener(mSortOrderListener);
            siteColumn.setText("Site");
            siteColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_SITE));
            siteColumn.setPrefWidth(80);

            TableColumn channelColumn = new TableColumn();
            channelColumn.setVisible(false);
            channelColumn.setId("channel");
            channelColumn.sortTypeProperty().addListener(mSortOrderListener);
            channelColumn.setText("Channel");
            channelColumn.setCellValueFactory(new PropertyValueFactory<Call, String>(Call.COLUMN_CHANNEL));
            channelColumn.setPrefWidth(80);

            TableColumn duplicateColumn = new TableColumn();
            duplicateColumn.setVisible(false);
            duplicateColumn.setId("duplicate");
            duplicateColumn.sortTypeProperty().addListener(mSortOrderListener);
            duplicateColumn.setText("Duplicate");
            duplicateColumn.setCellValueFactory(new PropertyValueFactory<Call, Boolean>(Call.COLUMN_DUPLICATE));
            duplicateColumn.setPrefWidth(80);

            TableColumn recordColumn = new TableColumn();
            recordColumn.setVisible(false);
            recordColumn.setId("record");
            recordColumn.sortTypeProperty().addListener(mSortOrderListener);
            recordColumn.setText("Record");
            recordColumn.setCellValueFactory(new PropertyValueFactory<Call, Boolean>(Call.COLUMN_RECORD));
            recordColumn.setPrefWidth(80);

            TableColumn streamColumn = new TableColumn();
            streamColumn.setVisible(false);
            streamColumn.setId("stream");
            streamColumn.sortTypeProperty().addListener(mSortOrderListener);
            streamColumn.setText("Stream");
            streamColumn.setCellValueFactory(new PropertyValueFactory<Call, Boolean>(Call.COLUMN_STREAM));
            streamColumn.setPrefWidth(80);

            TableColumn monitorColumn = new TableColumn();
            monitorColumn.setVisible(false);
            monitorColumn.setId("monitor");
            monitorColumn.sortTypeProperty().addListener(mSortOrderListener);
            monitorColumn.setText("Monitor");
            monitorColumn.setCellValueFactory(new PropertyValueFactory<Call, Integer>(Call.COLUMN_MONITOR));
            monitorColumn.setCellFactory(new PriorityCellFactory());
            monitorColumn.setPrefWidth(80);

            mCallTableView.getSortOrder().addListener(mSortOrderListener);
            mCallTableView.getColumns().addAll(playbackColumn, eventTimeColumn, durationColumn, callTypeColumn,
                    toIdColumn, toAliasColumn, fromIdColumn, fromAliasColumn, protocolColumn, frequencyColumn,
                    systemColumn, siteColumn, channelColumn, duplicateColumn, recordColumn, streamColumn, monitorColumn);
            mCallTableView.setPlaceholder(getPlaceholderProgressIndicator());
            mCallTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedCall) -> {
                if(selectedCall != null)
                {
                    mAudioPlaybackController.replay(MixerChannel.LEFT, selectedCall);
                }
                else if(oldValue != null && selectedCall == null)
                {
                    //We were in replay but now we don't have a selected call
                    mAudioPlaybackController.auto(MixerChannel.LEFT, null, null);
                }

                AudioPlaybackChannelController left = mAudioPlaybackController.getChannelController(MixerChannel.LEFT);
                AudioPlaybackChannelController right = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
                getLockModeLeftButton().setDisable(selectedCall == null || left.isLockedFor(selectedCall));
                getLockModeRightButton().setDisable(selectedCall == null || right.isLockedFor(selectedCall));
            });
            getPlaceholderProgressIndicator().setVisible(true);

            //Bind the playback control view to the primary audio channel playback controller
            getAudioPlaybackControlView().mediaPlayerProperty()
                    .bind(mAudioPlaybackController.getPrimaryController().mediaPlayerProperty());

            mCallTableViewMonitor = new TableViewColumnController(mCallTableView, "call_table_view");
        }

        return mCallTableView;
    }

    /**
     * Paging controller
     */
    public PagingController getPagingController()
    {
        if(mPagingController == null)
        {
            mPagingController = new PagingController(this::showPage);
        }

        return mPagingController;
    }

    /**
     * Progress indicator to convey data loading status when the table content is being updated.
     * @return progress indicator
     */
    public ProgressIndicator getPlaceholderProgressIndicator()
    {
        if(mPlaceholderProgressIndicator == null)
        {
            mPlaceholderProgressIndicator = new ProgressIndicator();
            mPlaceholderProgressIndicator.setVisible(false);
            mPlaceholderProgressIndicator.setProgress(-1); //always spinning
        }

        return mPlaceholderProgressIndicator;
    }

    private HBox getSearchBox()
    {
        if(mSearchBox == null)
        {
            mSearchBox = new HBox();
            mSearchBox.setAlignment(Pos.CENTER_LEFT);
            mSearchBox.setSpacing(5);

            Label searchLabel = new Label("Search:");
            searchLabel.setAlignment(Pos.CENTER_RIGHT);
            mSearchBox.getChildren().addAll(searchLabel, getSearchField(), getSearchColumnComboBox(), getSearchExactCheckBox());
        }

        return mSearchBox;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.setTooltip(new Tooltip("Type a value to search against the selected database field(s)"));
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                showFirstPage();
            });
        }

        return mSearchField;
    }

    /**
     * Monitors the calls table view for sort column and sort order changes to refresh the call event records.
     */
    private class SortOrderListener implements ListChangeListener<TableColumn<Call, ?>>, ChangeListener
    {
        @Override
        public void onChanged(Change<? extends TableColumn<Call, ?>> c)
        {
            updateSortOrder();
        }

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
            updateSortOrder();
        }
    }

    /**
     * Handles call events from the AudioManager.
     */
    private class CallEventListener implements ICallEventListener
    {
        @Override
        public void added(Call call)
        {
            Platform.runLater(() -> {
                //Only update the calls table if we're viewing page 1
                if(getPagingController().getCurrentPage() == 1)
                {
                    if(mCalls.size() == getPageSize())
                    {
                        mCalls.remove(mCalls.size() - 1);
                    }

                    mCalls.add(0, call);

                    getCallTableView().sort();

                    //If the user hasn't selected an item, scroll to the new item.
                    if(getCallTableView().getSelectionModel().getSelectedItem() != null)
                    {
                        getCallTableView().scrollTo(call);
                    }
                }

                mCallCount.set(mCallCount.get() + 1);
            });
        }

        @Override
        public void updated(Call call)
        {
            Platform.runLater(() -> {
                if(mCalls.contains(call))
                {
                    mCalls.set(mCalls.indexOf(call), call);
                }
            });
        }

        @Override
        public void completed(Call call, AudioSegment audioSegment)
        {
            Platform.runLater(() -> {
                if(mCalls.contains(call))
                {
                    mCalls.set(mCalls.indexOf(call), call);
                }
            });

            audioSegment.removeLease(getClass().toString());
        }

        @Override
        public void deleted(Call call)
        {
            Platform.runLater(() -> {
                if(mCalls.contains(call))
                {
                    mCalls.remove(call);
                }

                mCallCount.set(mCallCount.get() - 1);
            });
        }
    }

    public class PriorityCellFactory implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            TableCell tableCell = new TableCell<Alias, Integer>()
            {
                @Override
                protected void updateItem(Integer item, boolean empty)
                {
                    if(empty)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else if(item == io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR)
                    {
                        setText("Mute");
                        final IconNode iconNode = new IconNode(FontAwesome.VOLUME_OFF);
                        iconNode.setIconSize(20);
                        iconNode.setFill(Color.RED);
                        setGraphic(iconNode);
                    }
                    else if(item == io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY)
                    {
                        setText("Default");
                        final IconNode iconNode = new IconNode(FontAwesome.VOLUME_UP);
                        iconNode.setIconSize(20);
                        iconNode.setFill(Color.GREEN);
                        setGraphic(iconNode);
                    }
                    else
                    {
                        setText(item.toString());
                        final IconNode iconNode = new IconNode(FontAwesome.VOLUME_UP);
                        iconNode.setIconSize(20);
                        iconNode.setFill(Color.GREEN);
                        setGraphic(iconNode);
                    }
                }
            };

            return tableCell;
        }
    }

}
