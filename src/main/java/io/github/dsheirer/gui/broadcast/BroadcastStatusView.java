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

import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.streaming.ViewStreamingRequest;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.settings.swing.SwingSettings;
import io.github.dsheirer.sample.Listener;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToIntFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;

/**
 * Status of the configured audio streams: type, name, connection status and the streamed, queued, rejected, aged
 * off and failed audio counts.  The column selection, order and widths are kept across sessions.
 */
public class BroadcastStatusView extends VBox
{
    static final String TABLE_KEY = "broadcast.status";
    static final String SWING_TABLE_KEY = "application.broadcast.status.panel";

    private final BroadcastModel mBroadcastModel;
    private final ObservableList<ConfiguredBroadcast> mItems = FXCollections.observableArrayList();
    private final TableView<ConfiguredBroadcast> mTableView = new TableView<>(mItems);
    private final Label mSummaryLabel = new Label();
    private final AtomicBoolean mUpdateScheduled = new AtomicBoolean();
    private final Map<BroadcastServerType,Image> mServerTypeImages = new EnumMap<>(BroadcastServerType.class);
    private final Listener<BroadcastEvent> mBroadcastEventListener = event -> scheduleUpdate();
    private final ListChangeListener<ConfiguredBroadcast> mListChangeListener = change -> scheduleUpdate();

    static
    {
        IconFontFX.register(FontAwesome.getIconFont());
    }

    /**
     * Constructs an instance
     * @param broadcastModel with the configured streams
     * @param swingSettings with the column widths of the Swing version of this table, migrated once
     */
    public BroadcastStatusView(BroadcastModel broadcastModel, SwingSettings swingSettings)
    {
        mBroadcastModel = broadcastModel;
        getStyleClass().add("broadcast-status-view");
        URL stylesheet = BroadcastStatusView.class.getResource("/broadcast_status.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        mTableView.getColumns().addAll(List.of(
            createServerTypeColumn(),
            createNameColumn(),
            createStatusColumn(),
            createCountColumn("queued", "Queued", AbstractAudioBroadcaster::getAudioQueueSize,
                "Recordings waiting to be streamed or uploaded"),
            createCountColumn("streamed", "Streamed/Uploaded", AbstractAudioBroadcaster::getStreamedAudioCount,
                "Recordings streamed or uploaded"),
            createCountColumn("duplicate", "Duplicate Rejected",
                AbstractAudioBroadcaster::getDuplicateRejectedAudioCount, "Duplicate calls that were not streamed"),
            createCountColumn("agedOff", "Aged Off", AbstractAudioBroadcaster::getAgedOffAudioCount,
                "Recordings discarded because they waited too long to be streamed"),
            createCountColumn("errors", "Upload Error", AbstractAudioBroadcaster::getAudioErrorCount,
                "Recordings that failed to stream or upload")));
        mTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        mTableView.setTableMenuButtonVisible(false);
        mTableView.setFixedCellSize(26);

        Label placeholder = new Label("No streams are configured.  Use Configure Streams to add a stream.");
        placeholder.getStyleClass().add("broadcast-placeholder");
        mTableView.setPlaceholder(placeholder);
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        TableColumnMonitor<ConfiguredBroadcast> monitor = new TableColumnMonitor<>(mTableView, TABLE_KEY,
            swingSettings, SWING_TABLE_KEY);

        IconNode titleIcon = new IconNode(FontAwesome.PODCAST);
        titleIcon.setIconSize(14);
        titleIcon.getStyleClass().add("broadcast-title-icon");
        Label title = new Label("Streaming", titleIcon);
        title.getStyleClass().add("broadcast-title");
        mSummaryLabel.getStyleClass().add("broadcast-summary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        IconNode configureIcon = new IconNode(FontAwesome.COG);
        configureIcon.setIconSize(13);
        Button configureButton = new Button("Configure Streams", configureIcon);
        configureIcon.fillProperty().bind(configureButton.textFillProperty());
        configureButton.setTooltip(new Tooltip("Add and edit streams in the playlist editor"));
        configureButton.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewStreamingRequest()));

        ColumnConfigurationButton columnsButton = new ColumnConfigurationButton(monitor, "Streaming Status Columns");

        HBox toolbar = new HBox(10, title, mSummaryLabel, spacer, configureButton, columnsButton);
        toolbar.getStyleClass().add("broadcast-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(toolbar, mTableView);

        mBroadcastModel.addListener(mBroadcastEventListener);
        mBroadcastModel.getConfiguredBroadcasts().addListener(mListChangeListener);
        update();
    }

    /**
     * Deregisters from the broadcast model.
     */
    public void dispose()
    {
        mBroadcastModel.removeListener(mBroadcastEventListener);
        mBroadcastModel.getConfiguredBroadcasts().removeListener(mListChangeListener);
    }

    /**
     * Table showing the streams.
     */
    public TableView<ConfiguredBroadcast> getTableView()
    {
        return mTableView;
    }

    /**
     * Schedules an update on the FX thread.  Broadcasters report changes on their own threads, often several at a
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
     * Updates the table rows and summary from the model.  FX thread only.
     */
    private void update()
    {
        List<ConfiguredBroadcast> current = new ArrayList<>(mBroadcastModel.getConfiguredBroadcasts());

        if(!current.equals(mItems))
        {
            mItems.setAll(current);
        }
        else
        {
            mTableView.refresh();
        }

        mSummaryLabel.setText(getSummary(current));
    }

    /**
     * Summary of the stream status counts, for example: 3 connected, 1 error.
     */
    static String getSummary(List<ConfiguredBroadcast> broadcasts)
    {
        if(broadcasts.isEmpty())
        {
            return "";
        }

        int connected = 0;
        int warning = 0;
        int error = 0;
        int disabled = 0;

        for(ConfiguredBroadcast broadcast: broadcasts)
        {
            switch(getSeverity(broadcast.getStatus()))
            {
                case OK -> connected++;
                case WARNING -> warning++;
                case ERROR -> error++;
                case DISABLED -> disabled++;
                default -> {}
            }
        }

        List<String> parts = new ArrayList<>();
        parts.add(connected + " of " + broadcasts.size() + " connected");

        if(error > 0)
        {
            parts.add(error + (error == 1 ? " error" : " errors"));
        }

        if(warning > 0)
        {
            parts.add(warning + (warning == 1 ? " warning" : " warnings"));
        }

        if(disabled > 0)
        {
            parts.add(disabled + " disabled");
        }

        return String.join("  ·  ", parts);
    }

    /**
     * Status severity used to color the status.
     */
    public enum Severity {OK, NEUTRAL, WARNING, ERROR, DISABLED}

    public static Severity getSeverity(BroadcastState state)
    {
        if(state == null)
        {
            return Severity.NEUTRAL;
        }

        return switch(state)
        {
            case CONNECTED -> Severity.OK;
            case DISABLED -> Severity.DISABLED;
            case INVALID_SETTINGS, NETWORK_UNAVAILABLE -> Severity.WARNING;
            default -> state.isWarningState() ? Severity.WARNING :
                state.isErrorState() ? Severity.ERROR : Severity.NEUTRAL;
        };
    }

    private TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> createServerTypeColumn()
    {
        TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> column = createColumn("type", "Stream Type", 170);
        column.setCellFactory(param -> new TableCell<>()
        {
            private final ImageView mImageView = new ImageView();

            @Override
            protected void updateItem(ConfiguredBroadcast item, boolean empty)
            {
                super.updateItem(item, empty);
                BroadcastServerType type = empty || item == null ? null : item.getBroadcastServerType();

                if(type == null)
                {
                    setText(null);
                    setGraphic(null);
                }
                else
                {
                    setText(type.toString());
                    mImageView.setImage(getImage(type));
                    mImageView.setFitHeight(14);
                    mImageView.setPreserveRatio(true);
                    setGraphic(mImageView.getImage() != null ? mImageView : null);
                }
            }
        });
        return column;
    }

    private TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> createNameColumn()
    {
        TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> column = createColumn("name", "Name", 220);
        column.setCellFactory(param -> new TableCell<>()
        {
            @Override
            protected void updateItem(ConfiguredBroadcast item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getBroadcastConfiguration().getName());
            }
        });
        return column;
    }

    private TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> createStatusColumn()
    {
        TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> column = createColumn("status", "Status", 190);
        column.setCellFactory(param -> new TableCell<>()
        {
            private final Label mChip = new Label();

            {
                mChip.getStyleClass().add("broadcast-status-chip");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(ConfiguredBroadcast item, boolean empty)
            {
                super.updateItem(item, empty);

                if(empty || item == null)
                {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }

                BroadcastState state = item.getStatus();
                mChip.setText(state != null ? state.toString() : "Unknown");
                mChip.getStyleClass().removeIf(styleClass -> styleClass.startsWith("broadcast-status-severity-"));
                mChip.getStyleClass().add("broadcast-status-severity-" +
                    getSeverity(state).name().toLowerCase());
                setGraphic(mChip);

                BroadcastState lastBad = item.lastBadBroadcastStateProperty().get();
                setTooltip(lastBad != null && lastBad != state ? new Tooltip("Last problem: " + lastBad) : null);
            }
        });
        return column;
    }

    private TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> createCountColumn(String id, String title,
        ToIntFunction<AbstractAudioBroadcaster> count, String description)
    {
        TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> column = createColumn(id, title, 130);
        Label header = new Label(title);
        header.setTooltip(new Tooltip(description));
        column.setText(null);
        column.setGraphic(header);
        TableColumnMonitor.setName(column, title);
        column.setCellFactory(param -> new TableCell<>()
        {
            {
                setAlignment(Pos.CENTER_RIGHT);
                getStyleClass().add("broadcast-count");
            }

            @Override
            protected void updateItem(ConfiguredBroadcast item, boolean empty)
            {
                super.updateItem(item, empty);
                AbstractAudioBroadcaster broadcaster = empty || item == null ? null : item.getAudioBroadcaster();
                setText(broadcaster != null ? String.format("%,d", count.applyAsInt(broadcaster)) : null);
            }
        });
        return column;
    }

    private static TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> createColumn(String id, String title,
                                                                                      double width)
    {
        TableColumn<ConfiguredBroadcast,ConfiguredBroadcast> column = new TableColumn<>(title);
        column.setId(id);
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(param.getValue()));
        return column;
    }

    private Image getImage(BroadcastServerType type)
    {
        return mServerTypeImages.computeIfAbsent(type, serverType -> {
            String path = serverType.getIconPath();
            return path != null ? new Icon(serverType.name(), path).getFxImage() : null;
        });
    }
}
