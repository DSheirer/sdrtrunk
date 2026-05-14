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
package io.github.dsheirer.gui.preference.network;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.network.HeartbeatEntry;
import io.github.dsheirer.preference.network.HeartbeatPreference;
import java.util.Optional;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.controlsfx.control.ToggleSwitch;

/**
 * Preference editor for the P25 Control Channel Heartbeat Monitor feature.
 */
public class HeartbeatPreferenceEditor extends HBox
{
    private final HeartbeatPreference mPreference;
    private ObservableList<HeartbeatEntry> mItems;
    private TableView<HeartbeatEntry> mTable;
    private Button mEditButton;
    private Button mRemoveButton;

    public HeartbeatPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getHeartbeatPreference();
        mItems = FXCollections.observableArrayList(mPreference.getEntries());
        setMaxWidth(Double.MAX_VALUE);

        VBox outer = new VBox();
        outer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(buildTable(), Priority.ALWAYS);

        ScrollPane topScroll = new ScrollPane(buildExplanation());
        topScroll.setFitToWidth(true);
        topScroll.setPrefHeight(320);
        topScroll.setMaxHeight(320);
        topScroll.setStyle("-fx-background-color: transparent;");

        VBox tableSection = buildTableSection();
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        outer.getChildren().addAll(topScroll, tableSection);
        HBox.setHgrow(outer, Priority.ALWAYS);
        getChildren().add(outer);
    }

    private VBox buildExplanation()
    {
        VBox box = new VBox(12);
        box.setPadding(new Insets(18, 20, 10, 20));
        box.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Control Channel Heartbeat Monitor");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label subtitle = new Label(
            "Continuously verify that your P25 trunking control channels are alive and actively " +
            "decoding — not just tuned but actually receiving valid messages from the tower.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #555555;");

        box.getChildren().addAll(title, subtitle, new Separator());
        box.getChildren().add(sectionHeader("HOW IT WORKS"));

        Label howItWorks = new Label(
            "P25 trunking towers transmit RFSS Status Broadcast messages on the control channel " +
            "several times per second — these are normal housekeeping messages that carry the " +
            "system ID and site ID of the site your receiver is locked to.\n\n" +
            "SDRTrunk watches for these messages on each configured (System ID, Site ID) pair.  " +
            "When a match is found, it fires a throttled HTTP GET request to each of your " +
            "configured push URL endpoints.  If the control channel goes silent — antenna " +
            "problem, SDR disconnect, coverage gap — the pings stop, and your monitoring tool " +
            "marks the site as down and alerts you.");
        howItWorks.setWrapText(true);

        box.getChildren().add(howItWorks);
        box.getChildren().add(new Separator());
        box.getChildren().add(sectionHeader("IMPORTANT — WHAT IT DOES AND DOES NOT CHECK"));

        Label scope = new Label(
            "✓  The control channel is tuned, receiving RF, and decoding valid P25 messages\n" +
            "✓  The specific site (System ID + Site ID) is actively broadcasting\n" +
            "✗  It does NOT verify that voice calls are being decoded\n" +
            "✗  It does NOT apply to DMR or other protocols (P25 Phase 1 control channels only)\n" +
            "✗  It does NOT apply to traffic/voice channels — only the control channel");
        scope.setWrapText(true);
        scope.setStyle("-fx-background-color: #f0f4f8; -fx-padding: 8px; " +
                       "-fx-background-radius: 4px; -fx-font-size: 11.5px;");

        box.getChildren().add(scope);
        box.getChildren().add(new Separator());
        box.getChildren().add(sectionHeader("FINDING YOUR SYSTEM ID AND SITE ID"));

        Label findIds = new Label(
            "1.  Start SDRTrunk and let the control channel lock on your P25 system.\n" +
            "2.  Click the channel in the main window and open the Details tab.\n" +
            "3.  Look for lines like:  SYSTEM: 1674   and   SITE: 1\n" +
            "4.  Use those decimal numbers in the System ID and Site ID fields below.\n\n" +
            "Note: SDRTrunk sometimes shows both decimal and hex (e.g. SYSTEM: 1674 / 0x68A).\n" +
            "Always use the decimal number — the one before the slash.");
        findIds.setWrapText(true);

        Label restartNote = new Label("⚠  Changes take effect after restarting SDRTrunk.");
        restartNote.setStyle("-fx-text-fill: #996600; -fx-font-size: 11px;");

        box.getChildren().addAll(findIds, restartNote);
        return box;
    }

    private VBox buildTableSection()
    {
        VBox box = new VBox(8);
        box.setPadding(new Insets(6, 20, 18, 20));
        box.setMaxWidth(Double.MAX_VALUE);

        Label header = sectionHeader("HEARTBEAT ENTRIES");
        Label hint = new Label("Each entry monitors one P25 site. Entries can be disabled without deleting them.");
        hint.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");

        VBox.setVgrow(buildTable(), Priority.ALWAYS);

        HBox buttons = buildButtons();

        box.getChildren().addAll(header, hint, buildTable(), buttons);
        return box;
    }

    @SuppressWarnings("unchecked")
    private TableView<HeartbeatEntry> buildTable()
    {
        if(mTable != null) return mTable;

        mTable = new TableView<>(mItems);
        mTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mTable.setPlaceholder(new Label("No heartbeat entries — click Add to create one."));
        mTable.setPrefHeight(200);
        VBox.setVgrow(mTable, Priority.ALWAYS);

        TableColumn<HeartbeatEntry, Boolean> enabledCol = new TableColumn<>("On");
        enabledCol.setMaxWidth(46);
        enabledCol.setMinWidth(46);
        enabledCol.setResizable(false);
        enabledCol.setCellValueFactory(data ->
                new SimpleBooleanProperty(data.getValue().isEnabled()));
        enabledCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setDisable(true);
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) { setGraphic(null); }
                else { cb.setSelected(item); setGraphic(cb); }
            }
        });

        TableColumn<HeartbeatEntry, String> nameCol = new TableColumn<>("Channel Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getChannelName()));

        TableColumn<HeartbeatEntry, Number> sysCol = new TableColumn<>("System ID");
        sysCol.setMaxWidth(90); sysCol.setMinWidth(80);
        sysCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSystemId()));

        TableColumn<HeartbeatEntry, Number> siteCol = new TableColumn<>("Site ID");
        siteCol.setMaxWidth(76); siteCol.setMinWidth(68);
        siteCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSiteId()));

        TableColumn<HeartbeatEntry, Number> intervalCol = new TableColumn<>("Interval (s)");
        intervalCol.setMaxWidth(90); intervalCol.setMinWidth(80);
        intervalCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getIntervalSeconds()));

        TableColumn<HeartbeatEntry, String> kumaCol = new TableColumn<>("Uptime Kuma URL");
        kumaCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getKumaUrl()));

        TableColumn<HeartbeatEntry, String> rdioCol = new TableColumn<>("Push URL 2");
        rdioCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPushUrl2()));

        mTable.getColumns().addAll(enabledCol, nameCol, sysCol, siteCol, intervalCol, kumaCol, rdioCol);

        mTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = selected != null;
            mEditButton.setDisable(!hasSelection);
            mRemoveButton.setDisable(!hasSelection);
        });

        return mTable;
    }

    private HBox buildButtons()
    {
        Button addButton = new Button("Add");
        addButton.setOnAction(e -> onAdd());

        mEditButton = new Button("Edit");
        mEditButton.setDisable(true);
        mEditButton.setOnAction(e -> onEdit());

        mRemoveButton = new Button("Remove");
        mRemoveButton.setDisable(true);
        mRemoveButton.setStyle("-fx-text-fill: #cc0000;");
        mRemoveButton.setOnAction(e -> onRemove());

        HBox row = new HBox(8, addButton, mEditButton, mRemoveButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void onAdd()
    {
        showEditDialog(null).ifPresent(entry -> {
            mPreference.addEntry(entry);
            mItems.add(entry);
        });
    }

    private void onEdit()
    {
        int selectedIndex = mTable.getSelectionModel().getSelectedIndex();
        if(selectedIndex < 0) return;

        HeartbeatEntry original = mItems.get(selectedIndex);
        showEditDialog(original).ifPresent(updated -> {
            mPreference.updateEntry(selectedIndex, updated);
            mItems.set(selectedIndex, updated);
            mTable.refresh();
        });
    }

    private void onRemove()
    {
        int selectedIndex = mTable.getSelectionModel().getSelectedIndex();
        if(selectedIndex < 0) return;

        mPreference.removeEntry(selectedIndex);
        mItems.remove(selectedIndex);
    }

    private Optional<HeartbeatEntry> showEditDialog(HeartbeatEntry existing)
    {
        boolean isNew = (existing == null);
        HeartbeatEntry working = isNew ? new HeartbeatEntry() : new HeartbeatEntry(existing);

        Dialog<HeartbeatEntry> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Heartbeat Entry" : "Edit Heartbeat Entry");
        dialog.setHeaderText(null);

        if(!getScene().getWindow().equals(null))
        {
            dialog.initOwner(getScene().getWindow());
        }

        ButtonType saveType   = new ButtonType("Save",   ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints labelCol = new ColumnConstraints(130);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        ToggleSwitch enabledToggle = new ToggleSwitch();
        enabledToggle.setSelected(working.isEnabled());
        enabledToggle.selectedProperty().addListener((obs, o, v) -> working.setEnabled(v));
        int row = 0;
        grid.add(rightLabel("Enabled:"), 0, row);
        grid.add(enabledToggle, 1, row++);

        TextField channelNameField = new TextField(working.getChannelName());
        channelNameField.setPromptText("e.g. ALPHA_SITE");
        channelNameField.textProperty().addListener((obs, o, v) -> working.setChannelName(v));
        GridPane.setFillWidth(channelNameField, true);
        grid.add(rightLabel("Channel Name:"), 0, row);
        grid.add(channelNameField, 1, row++);

        Spinner<Integer> sysIdSpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65535, working.getSystemId()));
        sysIdSpinner.setEditable(true);
        sysIdSpinner.setPrefWidth(120);
        sysIdSpinner.valueProperty().addListener((obs, o, v) -> { if(v != null) working.setSystemId(v); });
        Label sysIdHint = smallHint("Decimal — shown in the Details tab as SYSTEM: 291");
        grid.add(rightLabel("System ID:"), 0, row);
        HBox sysRow = new HBox(8, sysIdSpinner, sysIdHint);
        sysRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(sysRow, 1, row++);

        Spinner<Integer> siteIdSpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65535, working.getSiteId()));
        siteIdSpinner.setEditable(true);
        siteIdSpinner.setPrefWidth(120);
        siteIdSpinner.valueProperty().addListener((obs, o, v) -> { if(v != null) working.setSiteId(v); });
        Label siteIdHint = smallHint("Decimal — shown in the Details tab as SITE: 1");
        grid.add(rightLabel("Site ID:"), 0, row);
        HBox siteRow = new HBox(8, siteIdSpinner, siteIdHint);
        siteRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(siteRow, 1, row++);

        TextField kumaField = new TextField(working.getKumaUrl());
        kumaField.setPromptText("http://kuma-host:3001/api/push/TOKEN?status=up&msg=OK&ping=");
        kumaField.textProperty().addListener((obs, o, v) -> working.setKumaUrl(v));
        GridPane.setFillWidth(kumaField, true);
        grid.add(rightLabel("Uptime Kuma URL:"), 0, row);
        grid.add(kumaField, 1, row);
        grid.add(smallHint("Leave blank to skip"), 1, ++row);
        row++;

        TextField rdioField = new TextField(working.getPushUrl2());
        rdioField.setPromptText("https://your-monitor.example.com/api/beat/TOKEN  (optional)");
        rdioField.textProperty().addListener((obs, o, v) -> working.setPushUrl2(v));
        GridPane.setFillWidth(rdioField, true);
        grid.add(rightLabel("Push URL 2:"), 0, row);
        grid.add(rdioField, 1, row);
        grid.add(smallHint("Secondary HTTP push endpoint — leave blank to skip"), 1, ++row);
        row++;

        Spinner<Integer> intervalSpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 3600, working.getIntervalSeconds(), 10));
        intervalSpinner.setEditable(true);
        intervalSpinner.setPrefWidth(100);
        intervalSpinner.valueProperty().addListener((obs, o, v) -> { if(v != null) working.setIntervalSeconds(v); });
        Label intervalHint = smallHint("seconds between pings  (30 recommended; >= Kuma's heartbeat interval)");
        grid.add(rightLabel("Ping Interval:"), 0, row);
        HBox intRow = new HBox(8, intervalSpinner, intervalHint);
        intRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(intRow, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(620);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        Runnable validate = () -> {
            boolean ok = (working.getSystemId() > 0 || working.getSiteId() > 0) &&
                         (!working.getKumaUrl().isBlank() || !working.getPushUrl2().isBlank());
            saveButton.setDisable(!ok);
        };
        validate.run();
        sysIdSpinner.valueProperty().addListener((obs, o, v) -> validate.run());
        siteIdSpinner.valueProperty().addListener((obs, o, v) -> validate.run());
        kumaField.textProperty().addListener((obs, o, v) -> validate.run());
        rdioField.textProperty().addListener((obs, o, v) -> validate.run());

        dialog.setResultConverter(buttonType ->
                buttonType == saveType ? working : null);

        return dialog.showAndWait();
    }

    private Label sectionHeader(String text)
    {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a5276;");
        return l;
    }

    private Label rightLabel(String text)
    {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        GridPane.setHalignment(l, HPos.RIGHT);
        return l;
    }

    private Label smallHint(String text)
    {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #777777; -fx-font-size: 10.5px;");
        l.setWrapText(true);
        return l;
    }
}
