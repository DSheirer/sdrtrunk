/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias;

import com.google.common.collect.Ordering;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.playlist.PlaylistManager;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.controlsfx.control.ToggleSwitch;

/**
 * Editor for multiple selected aliases providing limited options for changing attributes of multiple aliases
 */
public class AliasBulkEditor extends Editor<List<Alias>>
{
    private PlaylistManager mPlaylistManager;
    private Label mEditingLabel;
    private ColorPicker mColorPicker;
    private Button mApplyColorButton;
    private Button mResetColorButton;
    private ComboBox<Icon> mIconNodeComboBox;
    private Button mApplyIconButton;
    private ToggleSwitch mMonitorAudioToggleSwitch;
    private ComboBox<Integer> mMonitorPriorityComboBox;
    private Button mApplyMonitorButton;
    private ToggleSwitch mRecordToggleSwitch;
    private Button mApplyRecordButton;

    private BooleanProperty mChangeInProgressProperty;
    private ReadOnlyBooleanProperty mChangeInProgressROProperty;

    /**
     * Constructs an instance
     *
     * @param playlistManager for accessing icon manager
     */
    public AliasBulkEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        mChangeInProgressProperty = new SimpleBooleanProperty();

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int row = 0;

        Label editorLabel = new Label("Multiple Alias Editor");
        GridPane.setConstraints(editorLabel, 0, row, 3, 1);
        gridPane.getChildren().add(editorLabel);

        GridPane.setConstraints(getEditingLabel(), 3, row, 3, 1);
        GridPane.setHalignment(getEditingLabel(), HPos.RIGHT);
        gridPane.getChildren().add(getEditingLabel());

        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);
        GridPane.setConstraints(separator, 0, ++row, 6, 1);
        gridPane.getChildren().add(separator);

        Label colorLabel = new Label("Color");
        GridPane.setHalignment(colorLabel, HPos.RIGHT);
        GridPane.setConstraints(colorLabel, 0, ++row);
        gridPane.getChildren().add(colorLabel);

        GridPane.setConstraints(getColorPicker(), 1, row, 3, 1);
        gridPane.getChildren().add(getColorPicker());

        GridPane.setConstraints(getApplyColorButton(), 4, row);
        gridPane.getChildren().add(getApplyColorButton());

        GridPane.setConstraints(getResetColorButton(), 5, row);
        gridPane.getChildren().add(getResetColorButton());

        Label iconLabel = new Label("Icon");
        GridPane.setHalignment(iconLabel, HPos.RIGHT);
        GridPane.setConstraints(iconLabel, 0, ++row);
        gridPane.getChildren().add(iconLabel);

        GridPane.setConstraints(getIconNodeComboBox(), 1, row, 3, 1);
        gridPane.getChildren().add(getIconNodeComboBox());

        GridPane.setConstraints(getApplyIconButton(), 4, row);
        gridPane.getChildren().add(getApplyIconButton());

        Label listenLabel = new Label("Listen");
        GridPane.setHalignment(listenLabel, HPos.RIGHT);
        GridPane.setConstraints(listenLabel, 0, ++row);
        gridPane.getChildren().add(listenLabel);

        GridPane.setConstraints(getMonitorAudioToggleSwitch(), 1, row);
        gridPane.getChildren().add(getMonitorAudioToggleSwitch());

        Label priorityLabel = new Label("Priority");
        GridPane.setHalignment(priorityLabel, HPos.RIGHT);
        GridPane.setConstraints(priorityLabel, 2, row);
        gridPane.getChildren().add(priorityLabel);

        GridPane.setConstraints(getMonitorPriorityComboBox(), 3, row);
        gridPane.getChildren().add(getMonitorPriorityComboBox());

        GridPane.setConstraints(getApplyMonitorButton(), 4, row);
        gridPane.getChildren().add(getApplyMonitorButton());

        Label recordLabel = new Label("Record");
        GridPane.setHalignment(recordLabel, HPos.RIGHT);
        GridPane.setConstraints(recordLabel, 0, ++row);
        gridPane.getChildren().add(recordLabel);

        GridPane.setConstraints(getRecordToggleSwitch(), 1, row);
        gridPane.getChildren().add(getRecordToggleSwitch());

        GridPane.setConstraints(getApplyRecordButton(), 4, row);
        gridPane.getChildren().add(getApplyRecordButton());

        getChildren().add(gridPane);
    }

    @Override
    public List<Alias> getItem()
    {
        return super.getItem();
    }

    @Override
    public void setItem(List<Alias> item)
    {
        super.setItem(item);
        getEditingLabel().setText("Editing " + item.size() + " Aliases");
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    /**
     * Property indicating whether a bulk change is in progress.  Bulk changes may cause drastic
     * slow downs in some UI components.
     *
     * @return changeInProgressProperty
     */
    public ReadOnlyBooleanProperty changeInProgressProperty()
    {
        if(mChangeInProgressROProperty == null)
        {
            mChangeInProgressROProperty = BooleanProperty.readOnlyBooleanProperty(mChangeInProgressProperty);
        }

        return mChangeInProgressROProperty;
    }

    private void startChange()
    {
        mChangeInProgressProperty.set(true);
    }

    private void endChange()
    {
        mChangeInProgressProperty.set(false);
    }

    private Label getEditingLabel()
    {
        if(mEditingLabel == null)
        {
            mEditingLabel = new Label("Editing 0 Aliases");
        }

        return mEditingLabel;
    }

    private ColorPicker getColorPicker()
    {
        if(mColorPicker == null)
        {
            mColorPicker = new ColorPicker(Color.BLACK);
            mColorPicker.setEditable(true);
            mColorPicker.setStyle("-fx-color-rect-width: 60px; -fx-color-label-visible: false;");
        }

        return mColorPicker;
    }

    private Button getApplyColorButton()
    {
        if(mApplyColorButton == null)
        {
            mApplyColorButton = new Button("Apply");
            mApplyColorButton.setOnAction(event ->
            {
                startChange();

                int colorValue = ColorUtil.toInteger(getColorPicker().getValue());

                for(Alias alias : getItem())
                {
                    alias.setColor(colorValue);
                }

                endChange();
            });
        }

        return mApplyColorButton;
    }

    private Button getResetColorButton()
    {
        if(mResetColorButton == null)
        {
            mResetColorButton = new Button("Reset Color");
            mResetColorButton.setOnAction(event ->
            {
                startChange();

                for(Alias alias : getItem())
                {
                    alias.setColor(0);
                }

                endChange();
            });
        }

        return mResetColorButton;
    }

    private ComboBox<Icon> getIconNodeComboBox()
    {
        if(mIconNodeComboBox == null)
        {
            mIconNodeComboBox = new ComboBox<>();
            mIconNodeComboBox.setMaxWidth(Double.MAX_VALUE);
            mIconNodeComboBox.setItems(new SortedList(mPlaylistManager.getIconModel().iconsProperty(), Ordering.natural()));
            mIconNodeComboBox.setCellFactory(new IconCellFactory());
            mIconNodeComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) ->
                    {
                        getApplyIconButton().setDisable(newValue == null);
                    });
        }

        return mIconNodeComboBox;
    }

    private Button getApplyIconButton()
    {
        if(mApplyIconButton == null)
        {
            mApplyIconButton = new Button("Apply");
            mApplyIconButton.setDisable(true);
            mApplyIconButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    startChange();

                    Icon icon = getIconNodeComboBox().getSelectionModel().getSelectedItem();

                    if(icon != null)
                    {
                        for(Alias alias : getItem())
                        {
                            alias.setIconName(icon.getName());
                        }
                    }

                    endChange();
                }
            });
        }

        return mApplyIconButton;
    }

    private ToggleSwitch getMonitorAudioToggleSwitch()
    {
        if(mMonitorAudioToggleSwitch == null)
        {
            mMonitorAudioToggleSwitch = new ToggleSwitch();
            mMonitorAudioToggleSwitch.setSelected(true);
            mMonitorAudioToggleSwitch.selectedProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mMonitorAudioToggleSwitch;
    }

    private ToggleSwitch getRecordToggleSwitch()
    {
        if(mRecordToggleSwitch == null)
        {
            mRecordToggleSwitch = new ToggleSwitch();
            mRecordToggleSwitch.setSelected(false);
            mRecordToggleSwitch.selectedProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordToggleSwitch;
    }

    private ComboBox<Integer> getMonitorPriorityComboBox()
    {
        if(mMonitorPriorityComboBox == null)
        {
            mMonitorPriorityComboBox = new ComboBox<>();
            mMonitorPriorityComboBox.getItems().add(null);
            for(int x = io.github.dsheirer.alias.id.priority.Priority.MIN_PRIORITY;
                x < io.github.dsheirer.alias.id.priority.Priority.MAX_PRIORITY; x++)
            {
                mMonitorPriorityComboBox.getItems().add(x);
            }

            mMonitorPriorityComboBox.disableProperty().bind(getMonitorAudioToggleSwitch().selectedProperty().not());
            mMonitorPriorityComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mMonitorPriorityComboBox;
    }

    private Button getApplyMonitorButton()
    {
        if(mApplyMonitorButton == null)
        {
            mApplyMonitorButton = new Button("Apply");
            mApplyMonitorButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    startChange();

                    boolean canMonitor = getMonitorAudioToggleSwitch().isSelected();
                    Integer priority = getMonitorPriorityComboBox().getSelectionModel().getSelectedItem();
                    if(canMonitor)
                    {
                        if(priority == null)
                        {
                            priority = io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY;
                        }
                    }
                    else
                    {
                        priority = io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR;
                    }

                    final Integer pri = priority;
                    for(Alias alias : getItem())
                    {
                        alias.setCallPriority(pri);
                    }

                    endChange();
                }
            });
        }

        return mApplyMonitorButton;
    }

    private Button getApplyRecordButton()
    {
        if(mApplyRecordButton == null)
        {
            mApplyRecordButton = new Button("Apply");
            mApplyRecordButton.setOnAction(event -> {
                startChange();
                boolean record = getRecordToggleSwitch().isSelected();
                for(Alias alias : getItem())
                {
                    alias.setRecordable(record);
                }
                endChange();
            });
        }

        return mApplyRecordButton;
    }

    /**
     * Cell factory for combo box for dislaying icon name and graphic
     */
    public class IconCellFactory implements Callback<ListView<Icon>, ListCell<Icon>>
    {
        @Override
        public ListCell<Icon> call(ListView<Icon> param)
        {
            Label iconLabel = new Label();
            Label textLabel = new Label();
            GridPane gridPane = new GridPane();
            gridPane.setHgap(5);
            GridPane.setHalignment(iconLabel, HPos.RIGHT);
            gridPane.getColumnConstraints().add(new ColumnConstraints(50));
            gridPane.add(iconLabel, 0, 0);
            gridPane.add(textLabel, 1, 0);

            ListCell<Icon> cell = new ListCell<>()
            {
                @Override
                protected void updateItem(Icon item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(empty)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else
                    {
                        textLabel.setText(item.getName());
                        iconLabel.setGraphic(new ImageView(item.getFxImage()));
                        setGraphic(gridPane);
                    }
                }
            };

            return cell;
        }
    }
}
