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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.gui.control.FrequencyEntry;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredNetworkTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredRecordingTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.network.AddNetworkTunerDialog;
import io.github.dsheirer.source.tuner.recording.AddRecordingTunerDialog;
import io.github.dsheirer.util.ThreadPool;
import java.net.URL;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuners tab: the list of discovered tuners on the left and the editor for the selected tuner on the right.
 */
public class TunerView extends SplitPane implements DiscoveredTunerModel.ModelListener
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerView.class);
    private static final String TABLE_KEY = "tuner.view";
    private static final String SWING_TABLE_KEY = "tuner.view.panel";
    private static final PseudoClass STATUS_ENABLED = PseudoClass.getPseudoClass("enabled");
    private static final PseudoClass STATUS_DISABLED = PseudoClass.getPseudoClass("disabled-tuner");
    private static final PseudoClass STATUS_ERROR = PseudoClass.getPseudoClass("error");
    private static final PseudoClass STATUS_REMOVED = PseudoClass.getPseudoClass("removed");

    private final UserPreferences mUserPreferences;
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final TunerConfigurationManager mTunerConfigurationManager;
    private final DiscoveredTunerEditor mDiscoveredTunerEditor;
    private final ObservableList<DiscoveredTuner> mTuners = FXCollections.observableArrayList();
    private final TableView<DiscoveredTuner> mTableView = new TableView<>();
    private final TableColumnMonitor<DiscoveredTuner> mTableColumnMonitor;
    private final Button mRemoveButton = new Button("Remove");

    /**
     * Constructs an instance.  Must be invoked on the JavaFX application thread.
     * @param tunerManager for tuners
     * @param userPreferences for tuner editors and column settings
     */
    public TunerView(TunerManager tunerManager, UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mDiscoveredTunerModel = tunerManager.getDiscoveredTunerModel();
        mTunerConfigurationManager = tunerManager.getTunerConfigurationManager();
        mDiscoveredTunerEditor = new DiscoveredTunerEditor(userPreferences, tunerManager);

        getStyleClass().add("tuner-view");
        URL stylesheet = TunerView.class.getResource("/tuner_editor.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        createColumns();
        mTableColumnMonitor = new TableColumnMonitor<>(mTableView, TABLE_KEY, userPreferences.getSwingPreference(),
                SWING_TABLE_KEY);

        mTableView.setItems(mTuners);
        mTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        mTableView.setPlaceholder(new Label("No tuners discovered"));
        mTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldTuner, tuner) -> {
            if(tuner != mDiscoveredTunerEditor.getItem())
            {
                mDiscoveredTunerEditor.setItem(tuner);
            }

            updateRemoveButton();
        });
        mTableView.setRowFactory(table -> {
            TableRow<DiscoveredTuner> row = new TableRow<>();
            row.contextMenuProperty().bind(row.itemProperty().map(this::createContextMenu));
            return row;
        });
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        VBox listPane = new VBox(createToolbar(), mTableView);
        listPane.getStyleClass().add("tuner-list-pane");
        listPane.setMinWidth(260);

        getItems().addAll(listPane, mDiscoveredTunerEditor);
        setOrientation(Orientation.HORIZONTAL);
        setDividerPositions(0.42);
        SplitPane.setResizableWithParent(listPane, false);

        mDiscoveredTunerModel.addModelListener(this);
        mTuners.setAll(mDiscoveredTunerModel.getDiscoveredTuners());
        mTableView.sort();
    }

    private HBox createToolbar()
    {
        MenuItem addRecording = new MenuItem("Recording Tuner ...");
        addRecording.setOnAction(e -> new AddRecordingTunerDialog(getScene().getWindow(), mUserPreferences,
                mDiscoveredTunerModel, mTunerConfigurationManager).show());
        MenuItem addNetwork = new MenuItem("Network Tuner (rtl_tcp) ...");
        addNetwork.setOnAction(e -> new AddNetworkTunerDialog(getScene().getWindow(), mUserPreferences,
                mDiscoveredTunerModel, mTunerConfigurationManager).show());
        MenuButton addButton = new MenuButton("Add Tuner", null, addRecording, addNetwork);
        addButton.setTooltip(new Tooltip("Add a recording tuner that replays a baseband recording, or an rtl_tcp " +
                "network tuner"));

        mRemoveButton.setTooltip(new Tooltip("Remove the selected recording or network tuner"));
        mRemoveButton.setDisable(true);
        mRemoveButton.setOnAction(e -> removeTuner(mTableView.getSelectionModel().getSelectedItem()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(addButton, mRemoveButton, spacer,
                new ColumnConfigurationButton(mTableColumnMonitor, "Tuner Columns"));
        toolbar.getStyleClass().add("tuner-list-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private void createColumns()
    {
        TableColumn<DiscoveredTuner,TunerStatus> status = new TableColumn<>("Status");
        status.setId("status");
        status.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTunerStatus()));
        status.setCellFactory(column -> new StatusCell());
        status.setPrefWidth(90);
        TableColumnMonitor.setLegacyIndex(status, 0);

        TableColumn<DiscoveredTuner,String> tunerClass = textColumn("tunerClass", "Class",
                tuner -> tuner.getTunerClass().toString(), 1);
        tunerClass.setPrefWidth(120);
        TableColumn<DiscoveredTuner,String> type = textColumn("type", "Type",
                tuner -> tuner.hasTuner() ? tuner.getTuner().getTunerType().getLabel() : "", 2);
        type.setPrefWidth(120);

        TableColumn<DiscoveredTuner,Long> frequency = new TableColumn<>("Frequency");
        frequency.setId("frequency");
        frequency.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().hasTuner() ?
                cell.getValue().getTuner().getTunerController().getFrequency() : null));
        frequency.setCellFactory(column -> new TableCell<>()
        {
            @Override
            protected void updateItem(Long item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : FrequencyEntry.format(item) + " MHz");
            }
        });
        frequency.getStyleClass().add("tuner-frequency-column");
        frequency.setPrefWidth(130);
        TableColumnMonitor.setLegacyIndex(frequency, 3);

        TableColumn<DiscoveredTuner,String> channels = textColumn("channels", "Channels", tuner -> {
            if(!tuner.hasTuner())
            {
                return "";
            }

            int count = tuner.getTuner().getChannelSourceManager().getTunerChannelCount();
            boolean locked = tuner.getTuner().getTunerController().isLockedSampleRate();
            return count + (locked ? " (locked)" : "");
        }, 4);

        mTableView.getColumns().addAll(status, tunerClass, type, frequency, channels);
        mTableView.getSortOrder().add(type);
    }

    private static TableColumn<DiscoveredTuner,String> textColumn(String id, String title,
                                                                  Function<DiscoveredTuner,String> value, int legacyIndex)
    {
        TableColumn<DiscoveredTuner,String> column = new TableColumn<>(title);
        column.setId(id);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        TableColumnMonitor.setLegacyIndex(column, legacyIndex);
        return column;
    }

    private ContextMenu createContextMenu(DiscoveredTuner tuner)
    {
        if(tuner == null)
        {
            return null;
        }

        MenuItem logState = new MenuItem("Log Tuner State");
        logState.setOnAction(e -> tuner.logState());

        MenuItem remove = new MenuItem("Remove Tuner");
        remove.setDisable(!isRemovable(tuner));
        remove.setOnAction(e -> removeTuner(tuner));

        return new ContextMenu(logState, remove);
    }

    private static boolean isRemovable(DiscoveredTuner tuner)
    {
        return tuner instanceof DiscoveredRecordingTuner || tuner instanceof DiscoveredNetworkTuner;
    }

    private void updateRemoveButton()
    {
        DiscoveredTuner selected = mTableView.getSelectionModel().getSelectedItem();
        mRemoveButton.setDisable(selected == null || !isRemovable(selected));
    }

    private void removeTuner(DiscoveredTuner selected)
    {
        if(selected != null && isRemovable(selected))
        {
            mLog.info("Removing Tuner: " + selected);

            if(selected == mDiscoveredTunerEditor.getItem())
            {
                mDiscoveredTunerEditor.setItem(null);
            }

            //Stopping a tuner can block, so do it off of the JavaFX thread
            ThreadPool.CACHED.submit(() -> {
                selected.stop();
                mTunerConfigurationManager.removeTunerConfiguration(selected.getTunerConfiguration());
                mDiscoveredTunerModel.removeDiscoveredTuner(selected);
            });
        }
    }

    @Override
    public void tunerAdded(DiscoveredTuner discoveredTuner)
    {
        Platform.runLater(() -> {
            if(!mTuners.contains(discoveredTuner))
            {
                mTuners.add(discoveredTuner);
                mTableView.sort();
            }
        });
    }

    @Override
    public void tunerRemoved(DiscoveredTuner discoveredTuner)
    {
        Platform.runLater(() -> {
            mTuners.remove(discoveredTuner);

            if(mDiscoveredTunerEditor.getItem() == discoveredTuner)
            {
                mDiscoveredTunerEditor.setItem(null);
            }
        });
    }

    @Override
    public void tunerUpdated(DiscoveredTuner discoveredTuner)
    {
        Platform.runLater(() -> {
            mTableView.refresh();
            updateRemoveButton();
        });
    }

    /**
     * Selects the tuner in the list, e.g. for a request to show a tuner's settings.
     */
    public void select(DiscoveredTuner discoveredTuner)
    {
        mTableView.getSelectionModel().select(discoveredTuner);
    }

    /**
     * Discovered tuner editor, for testing.
     */
    DiscoveredTunerEditor getDiscoveredTunerEditor()
    {
        return mDiscoveredTunerEditor;
    }

    /**
     * Table view, for testing.
     */
    TableView<DiscoveredTuner> getTableView()
    {
        return mTableView;
    }

    /**
     * Status column cell that shows the status as a colored pill.
     */
    private static class StatusCell extends TableCell<DiscoveredTuner,TunerStatus>
    {
        private final Label mPill = new Label();

        StatusCell()
        {
            mPill.getStyleClass().add("tuner-status-chip");
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(TunerStatus status, boolean empty)
        {
            super.updateItem(status, empty);
            setText(null);

            if(empty || status == null)
            {
                setGraphic(null);
                setTooltip(null);
                return;
            }

            mPill.setText(status.toString());
            mPill.pseudoClassStateChanged(STATUS_ENABLED, status == TunerStatus.ENABLED);
            mPill.pseudoClassStateChanged(STATUS_DISABLED, status == TunerStatus.DISABLED);
            mPill.pseudoClassStateChanged(STATUS_ERROR, status == TunerStatus.ERROR);
            mPill.pseudoClassStateChanged(STATUS_REMOVED, status == TunerStatus.REMOVED);
            setGraphic(mPill);

            DiscoveredTuner tuner = getTableRow() != null ? getTableRow().getItem() : null;
            setTooltip(tuner != null && tuner.hasErrorMessage() ? new Tooltip(tuner.getErrorMessage()) : null);
        }
    }
}
