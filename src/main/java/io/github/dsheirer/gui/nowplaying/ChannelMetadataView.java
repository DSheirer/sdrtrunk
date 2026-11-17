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
package io.github.dsheirer.gui.nowplaying;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.metadata.ChannelAndMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadataField;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.decoder.ChannelStateIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Table of the channel metadata for the currently processing channels.  Selecting a row broadcasts the processing
 * chain for the channel to the registered listeners.
 */
public class ChannelMetadataView extends VBox implements ChannelMetadataModel.Listener
{
    private static final String TABLE_KEY = "now.playing.channels";
    private static final String SWING_TABLE_KEY = "channel.metadata.panel";
    private static final String EMPTY_IDENTIFIER = "-----";
    private static final String EMPTY_STATE = "----";
    private static final Map<State,String> STATE_STYLES = new EnumMap<>(State.class);

    static
    {
        //Colors match the Swing version of the table (java.awt.Color constants)
        STATE_STYLES.put(State.ACTIVE, style("#00FFFF", "#0000FF"));
        STATE_STYLES.put(State.CALL, style("#0000FF", "#FFFF00"));
        STATE_STYLES.put(State.CONTROL, style("#FFC800", "#0000FF"));
        STATE_STYLES.put(State.DATA, style("#00FF00", "#0000FF"));
        STATE_STYLES.put(State.ENCRYPTED, style("#FF00FF", "#FFFFFF"));
        STATE_STYLES.put(State.FADE, style("#C0C0C0", "#404040"));
        STATE_STYLES.put(State.IDLE, style("#FFFFFF", "#404040"));
        STATE_STYLES.put(State.RESET, style("#FFAFAF", "#FFFF00"));
        STATE_STYLES.put(State.TEARDOWN, style("#404040", "#FFFFFF"));
    }

    private final ChannelProcessingManager mChannelProcessingManager;
    private final ChannelMetadataModel mChannelMetadataModel;
    private final IconModel mIconModel;
    private final TalkgroupFormatPreference mTalkgroupFormatPreference;
    private final TableView<Row> mTableView = new TableView<>();
    private final ObservableList<Row> mRows = FXCollections.observableArrayList();
    //Changed on the JavaFX thread and read on the decoder threads that send channel metadata updates
    private final Map<ChannelMetadata,Row> mRowMap = new ConcurrentHashMap<>();
    private final Broadcaster<ProcessingChain> mSelectedProcessingChainBroadcaster = new Broadcaster<>();
    private final TableColumnMonitor<Row> mTableColumnMonitor;
    private Channel mUserSelectedChannel;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param channelProcessingManager for the channel metadata and processing chains
     * @param iconModel for alias icons
     * @param userPreferences for talkgroup formatting and column settings
     */
    public ChannelMetadataView(ChannelProcessingManager channelProcessingManager, IconModel iconModel,
                               UserPreferences userPreferences)
    {
        mChannelProcessingManager = channelProcessingManager;
        mChannelMetadataModel = mChannelProcessingManager.getChannelMetadataModel();
        mIconModel = iconModel;
        mTalkgroupFormatPreference = userPreferences.getTalkgroupFormatPreference();

        mTableView.getColumns().addAll(createColumns());
        mTableView.setItems(mRows);
        mTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        mTableView.setPlaceholder(new javafx.scene.control.Label("No channels are processing"));
        mTableView.setRowFactory(tableView -> createRow());
        mTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldRow, newRow) -> selected(newRow));
        mTableColumnMonitor = new TableColumnMonitor<>(mTableView, TABLE_KEY, userPreferences.getSwingPreference(),
            SWING_TABLE_KEY);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(5, spacer, new ColumnConfigurationButton(mTableColumnMonitor, "Now Playing Columns"));
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setPadding(new Insets(2, 2, 2, 2));

        VBox.setVgrow(mTableView, Priority.ALWAYS);
        getChildren().addAll(toolbar, mTableView);

        for(ChannelMetadata channelMetadata: mChannelMetadataModel.addListener(this))
        {
            addRow(channelMetadata);
        }

        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Adds the listener to receive the processing chain for the channel that the user selects in the table, or null
     * when the selection is cleared.  Notifications arrive on the JavaFX application thread.
     */
    public void addProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mSelectedProcessingChainBroadcaster.addListener(listener);
    }

    /**
     * Prepares this view for disposal.
     */
    public void dispose()
    {
        mChannelMetadataModel.removeListener(this);
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    /**
     * Receives preference update notifications via the event bus
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        //Talkgroup format changes the identifiers and the application preference can change the alias colors
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT || preferenceType == PreferenceType.APPLICATION)
        {
            Platform.runLater(() -> mRows.forEach(Row::refresh));
        }
    }

    @Override
    public void added(ChannelAndMetadata channelAndMetadata)
    {
        List<ChannelMetadata> channelMetadata = new ArrayList<>(channelAndMetadata.getChannelMetadata());

        Platform.runLater(() -> {
            for(ChannelMetadata metadata: channelMetadata)
            {
                addRow(metadata);
            }

            //The DMR Capacity+ rest channel rotation converts the selected channel to a traffic channel and restarts
            //the previous channel.  Re-select the row of the restarted channel so that the user isn't chasing it.
            if(mUserSelectedChannel != null && !channelMetadata.isEmpty() &&
                mUserSelectedChannel.getChannelID() == channelAndMetadata.getChannel().getChannelID())
            {
                Row row = mRowMap.get(channelMetadata.getFirst());

                if(row != null)
                {
                    mTableView.getSelectionModel().select(row);
                }
            }
        });
    }

    @Override
    public void removed(ChannelMetadata channelMetadata)
    {
        Platform.runLater(() -> {
            Row row = mRowMap.remove(channelMetadata);

            if(row != null)
            {
                mRows.remove(row);
            }
        });
    }

    @Override
    public void updated(ChannelMetadata channelMetadata, ChannelMetadataField channelMetadataField)
    {
        Row row = mRowMap.get(channelMetadata);

        if(row != null)
        {
            row.scheduleRefresh();
        }
    }

    /**
     * Adds a row for the channel metadata.  Call on the JavaFX application thread.
     */
    private void addRow(ChannelMetadata channelMetadata)
    {
        if(!mRowMap.containsKey(channelMetadata))
        {
            Row row = new Row(channelMetadata);
            mRowMap.put(channelMetadata, row);
            mRows.add(row);
        }
    }

    private void selected(Row row)
    {
        ProcessingChain processingChain = null;

        if(row != null)
        {
            mUserSelectedChannel = mChannelMetadataModel.getChannelFromMetadata(row.getChannelMetadata());
            processingChain = mChannelProcessingManager.getProcessingChain(mUserSelectedChannel);
        }

        mSelectedProcessingChainBroadcaster.broadcast(processingChain);
    }

    private TableRow<Row> createRow()
    {
        TableRow<Row> tableRow = new TableRow<>();
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setOnShowing(event -> {
            contextMenu.getItems().clear();
            Row row = tableRow.getItem();
            Channel channel = row != null ? mChannelMetadataModel.getChannelFromMetadata(row.getChannelMetadata()) : null;

            if(channel != null)
            {
                MenuItem viewChannel = new MenuItem("View/Edit: " + channel.getShortTitle());
                viewChannel.setOnAction(e -> MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel)));
                contextMenu.getItems().add(viewChannel);
            }
            else
            {
                MenuItem none = new MenuItem("No Actions Available");
                none.setDisable(true);
                contextMenu.getItems().add(none);
            }
        });
        tableRow.setContextMenu(contextMenu);
        return tableRow;
    }

    private List<TableColumn<Row,?>> createColumns()
    {
        List<TableColumn<Row,?>> columns = new ArrayList<>();

        TableColumn<Row,State> status = NowPlayingCells.column("status", "Status", null, Pos.CENTER,
            row -> {
                ChannelStateIdentifier identifier = row.getChannelMetadata().getChannelStateIdentifier();
                return identifier != null ? identifier.getValue() : null;
            }, Row::revisionProperty);
        status.setCellFactory(column -> new NowPlayingCells.AlignedCell<>(column)
        {
            @Override
            protected void updateItem(State state, boolean empty)
            {
                super.updateItem(state, empty);

                if(empty)
                {
                    setText(null);
                    setStyle(null);
                }
                else if(state == null)
                {
                    setText(EMPTY_STATE);
                    setStyle(null);
                }
                else
                {
                    setText(state.getDisplayValue());
                    setStyle(STATE_STYLES.get(state));
                }
            }
        });
        columns.add(status);

        columns.add(NowPlayingCells.column("decoder", "Decoder", null, Pos.CENTER,
            row -> toText(row.getChannelMetadata().getDecoderTypeConfigurationIdentifier()), Row::revisionProperty));

        columns.add(NowPlayingCells.column("from", "From", null, Pos.CENTER,
            row -> {
                String text = mTalkgroupFormatPreference.format(row.getChannelMetadata().getFromIdentifier());
                return text == null || text.isEmpty() ? EMPTY_IDENTIFIER : text;
            }, Row::revisionProperty));

        //Talker alias sent over the air by the radio (e.g. DMR or P25)
        columns.add(NowPlayingCells.column("talker", "Talker", null, Pos.CENTER,
            row -> {
                Identifier talkerAlias = row.getChannelMetadata().getTalkerAliasIdentifier();
                return talkerAlias != null && talkerAlias.getValue() != null ? talkerAlias.getValue().toString() : null;
            }, Row::revisionProperty));

        TableColumn<Row,AliasDisplay> fromAlias = NowPlayingCells.column("from.alias", "Alias", "From Alias",
            Pos.CENTER_LEFT, row -> AliasDisplay.of(row.getChannelMetadata().getFromIdentifierAliases()),
            Row::revisionProperty);
        fromAlias.setCellFactory(NowPlayingCells.alias(mIconModel));
        columns.add(fromAlias);

        columns.add(NowPlayingCells.column("to", "To", null, Pos.CENTER,
            row -> {
                String text = mTalkgroupFormatPreference.format(row.getChannelMetadata().getToIdentifier());
                return text == null || text.isEmpty() ? EMPTY_IDENTIFIER : text;
            }, Row::revisionProperty));

        TableColumn<Row,AliasDisplay> toAlias = NowPlayingCells.column("to.alias", "Alias", "To Alias",
            Pos.CENTER_LEFT, row -> AliasDisplay.of(row.getChannelMetadata().getToIdentifierAliases()),
            Row::revisionProperty);
        toAlias.setCellFactory(NowPlayingCells.alias(mIconModel));
        columns.add(toAlias);

        columns.add(NowPlayingCells.column("channel", "Channel", null, Pos.CENTER,
            row -> getLogicalChannelName(row.getChannelMetadata()), Row::revisionProperty));

        TableColumn<Row,Long> frequency = NowPlayingCells.column("frequency", "Frequency", null, Pos.CENTER,
            row -> row.getChannelMetadata().getFrequencyConfigurationIdentifier() != null ?
                row.getChannelMetadata().getFrequencyConfigurationIdentifier().getValue() : null,
            Row::revisionProperty);
        DecimalFormat frequencyFormat = new DecimalFormat("#.00000");
        frequency.setCellFactory(NowPlayingCells.formatted(value -> frequencyFormat.format(value / 1e6d)));
        columns.add(frequency);

        //Tuner that sources the channel
        columns.add(NowPlayingCells.column("tuner", "Tuner", null, Pos.CENTER,
            row -> getTunerName(row.getChannelMetadata()), Row::revisionProperty));

        //System and site labels from the channel configuration, hidden by default
        TableColumn<Row,String> system = NowPlayingCells.column("system", "System", null, Pos.CENTER,
            row -> toText(row.getChannelMetadata().getSystemConfigurationIdentifier()), Row::revisionProperty);
        system.setVisible(false);
        columns.add(system);

        TableColumn<Row,String> site = NowPlayingCells.column("site", "Site", null, Pos.CENTER,
            row -> toText(row.getChannelMetadata().getSiteConfigurationIdentifier()), Row::revisionProperty);
        site.setVisible(false);
        columns.add(site);

        columns.add(NowPlayingCells.column("channel.name", "Channel Name", null, Pos.CENTER,
            row -> toText(row.getChannelMetadata().getChannelNameConfigurationIdentifier()), Row::revisionProperty));

        //Alias list from the channel configuration
        columns.add(NowPlayingCells.column("alias.list", "Alias List", null, Pos.CENTER,
            row -> toText(row.getChannelMetadata().getAliasListConfigurationIdentifier()), Row::revisionProperty));

        //The channel rows are in channel start order, the same as the Swing version of the table
        for(TableColumn<Row,?> column: columns)
        {
            column.setSortable(false);
        }

        //Column numbers in the Swing version of the table, for its stored column widths.  Talker, Tuner, System, Site and Alias
        //List are new columns.
        String[] swingColumns = {"status", "decoder", "from", "from.alias", "to", "to.alias", "channel", "frequency",
            "channel.name"};

        for(TableColumn<Row,?> column: columns)
        {
            int swingColumn = Arrays.asList(swingColumns).indexOf(column.getId());

            if(swingColumn >= 0)
            {
                TableColumnMonitor.setLegacyIndex(column, swingColumn);
            }
        }

        return columns;
    }

    /**
     * Name of the tuner that sources the channel, or null if the channel isn't sourced by a tuner.
     */
    private String getTunerName(ChannelMetadata channelMetadata)
    {
        Channel channel = mChannelMetadataModel.getChannelFromMetadata(channelMetadata);
        ProcessingChain processingChain = channel != null ? mChannelProcessingManager.getProcessingChain(channel) : null;

        if(processingChain != null && processingChain.getSource() instanceof TunerChannelSource tunerChannelSource)
        {
            return tunerChannelSource.getTunerName();
        }

        return null;
    }

    /**
     * Decoder logical channel name with the timeslot, if any
     */
    private static String getLogicalChannelName(ChannelMetadata channelMetadata)
    {
        DecoderLogicalChannelNameIdentifier id = channelMetadata.getDecoderLogicalChannelNameIdentifier();

        if(channelMetadata.hasTimeslot())
        {
            if(id == null)
            {
                return "TS:" + channelMetadata.getTimeslot();
            }

            return id.getValue().replace(" TS1", "").replace(" TS2", "") + " TS:" + channelMetadata.getTimeslot();
        }

        return id != null ? id.getValue() : null;
    }

    private static String toText(Identifier identifier)
    {
        return identifier != null ? identifier.toString() : null;
    }

    private static String style(String background, String foreground)
    {
        return "-fx-background-color: " + background + "; -fx-text-fill: " + foreground + ";";
    }

    /**
     * Table row item for a channel metadata.  The revision increments each time the channel metadata changes so that
     * the row cells update.
     */
    public static class Row
    {
        private final ChannelMetadata mChannelMetadata;
        private final IntegerProperty mRevision = new SimpleIntegerProperty();
        private final AtomicBoolean mRefreshPending = new AtomicBoolean();

        Row(ChannelMetadata channelMetadata)
        {
            mChannelMetadata = channelMetadata;
        }

        public ChannelMetadata getChannelMetadata()
        {
            return mChannelMetadata;
        }

        public IntegerProperty revisionProperty()
        {
            return mRevision;
        }

        /**
         * Updates the row cells.  Call on the JavaFX application thread.
         */
        void refresh()
        {
            mRevision.set(mRevision.get() + 1);
        }

        /**
         * Schedules a row cell update on the JavaFX application thread.  Can be called from any thread.  Updates
         * that arrive while one is pending are combined.
         */
        void scheduleRefresh()
        {
            if(mRefreshPending.compareAndSet(false, true))
            {
                Platform.runLater(() -> {
                    mRefreshPending.set(false);
                    refresh();
                });
            }
        }
    }
}
