/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.tone.TonesID;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.identifier.tone.AmbeTone;
import io.github.dsheirer.identifier.tone.Tone;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TonesEditor extends IdentifierEditor<TonesID>
{
    private final static Logger mLog = LoggerFactory.getLogger(TonesEditor.class);

    private ListView<Tone> mToneListView;
    private ComboBox<AmbeTone> mAmbeToneComboBox;
    private TextField mDurationTextField;
    private IntegerFormatter mDurationIntegerFormatter;
    private Button mAddToneButton;
    private Button mDeleteToneButton;
    private Button mMoveUpButton;
    private Button mMoveDownButton;
    private DurationValueChangeListener mDurationValueChangeListener = new DurationValueChangeListener();
    private AmbeToneComboBoxEventHandler mAmbeToneComboBoxEventHandler = new AmbeToneComboBoxEventHandler();

    public TonesEditor()
    {
        VBox upDownBox = new VBox();
        upDownBox.setAlignment(Pos.CENTER);
        upDownBox.setSpacing(10);
        upDownBox.getChildren().addAll(getMoveUpButton(), getMoveDownButton());

        GridPane editorsPane = new GridPane();
        editorsPane.setAlignment(Pos.CENTER);
        editorsPane.setHgap(5);
        editorsPane.setVgap(10);

        Label toneLabel = new Label("Tone");
        GridPane.setConstraints(toneLabel, 0, 0);
        GridPane.setHalignment(toneLabel, HPos.RIGHT);
        editorsPane.getChildren().add(toneLabel);

        GridPane.setConstraints(getAmbeToneComboBox(), 1, 0);
        editorsPane.getChildren().add(getAmbeToneComboBox());

        Label durationLabel = new Label("Duration 1-50");
        GridPane.setConstraints(durationLabel, 0, 1);
        GridPane.setHalignment(durationLabel, HPos.RIGHT);
        editorsPane.getChildren().add(durationLabel);

        GridPane.setConstraints(getDurationTextField(), 1, 1);
        editorsPane.getChildren().add(getDurationTextField());

        VBox buttonBox = new VBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);
        buttonBox.getChildren().addAll(getAddToneButton(), getDeleteToneButton());

        HBox editorBox = new HBox();
        editorBox.setSpacing(10);
        HBox.setHgrow(getToneListView(), Priority.ALWAYS);
        editorBox.getChildren().addAll(getToneListView(), upDownBox, editorsPane, buttonBox);

        getChildren().add(editorBox);
    }

    @Override
    public void setItem(TonesID item)
    {
        super.setItem(item);

        getToneListView().setDisable(item == null);
        getAddToneButton().setDisable(item == null);

        if(item != null)
        {
            getToneListView().setItems(item.getToneSequence().tonesProperty());
        }
        else
        {
            getToneListView().setItems(FXCollections.emptyObservableList());
        }
    }

    @Override
    public void save()
    {
        //No-op
    }

    @Override
    public void dispose()
    {
        //No-op
    }

    private void setTone(Tone tone)
    {
        getDeleteToneButton().setDisable(tone == null);

        mAmbeToneComboBoxEventHandler.disable();
        mDurationValueChangeListener.disable();

        getAmbeToneComboBox().setDisable(tone == null);
        getDurationTextField().setDisable(tone == null);

        if(tone != null)
        {
            getAmbeToneComboBox().getSelectionModel().select(tone.getAmbeTone());
            getDurationIntegerFormatter().setValue(tone.getDuration());
        }
        else
        {
            getAmbeToneComboBox().getSelectionModel().select(null);
            getDurationIntegerFormatter().setValue(2);
        }

        mAmbeToneComboBoxEventHandler.enable();
        mDurationValueChangeListener.enable();

        updateMoveButtons();
    }

    private Button getAddToneButton()
    {
        if(mAddToneButton == null)
        {
            mAddToneButton = new Button("Add Tone");
            mAddToneButton.setMaxWidth(Double.MAX_VALUE);
            mAddToneButton.setDisable(true);
            mAddToneButton.setOnAction(event -> {
                Tone tone = new Tone(AmbeTone.DTMF_0, 2);
                getItem().getToneSequence().addTone(tone);
                modifiedProperty().set(true);
                getToneListView().getSelectionModel().select(tone);
                getToneListView().scrollTo(tone);
            });
        }

        return mAddToneButton;
    }

    private Button getDeleteToneButton()
    {
        if(mDeleteToneButton == null)
        {
            mDeleteToneButton = new Button("Delete Tone");
            mDeleteToneButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteToneButton.setDisable(true);
            mDeleteToneButton.setOnAction(event -> {
                Tone selected = getToneListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    getItem().getToneSequence().removeTone(selected);
                    modifiedProperty().set(true);
                }
            });
        }

        return mDeleteToneButton;
    }

    private void updateMoveButtons()
    {
        if(getToneListView().getItems().size() >= 2 && getToneListView().getSelectionModel().getSelectedItem() != null)
        {
            Tone selected = getToneListView().getSelectionModel().getSelectedItem();
            getMoveUpButton().setDisable(getToneListView().getItems().indexOf(selected) == 0);
            getMoveDownButton().setDisable(getToneListView().getItems().indexOf(selected) >= getToneListView().getItems().size() - 1);
        }
        else
        {
            getMoveDownButton().setDisable(true);
            getMoveUpButton().setDisable(true);
        }
    }

    private Button getMoveUpButton()
    {
        if(mMoveUpButton == null)
        {
            mMoveUpButton = new Button();
            IconNode iconNode = new IconNode(FontAwesome.ARROW_UP);
            iconNode.setIconSize(12);
            iconNode.setFill(getMoveUpButton().getTextFill());
            mMoveUpButton.setGraphic(iconNode);
            mMoveUpButton.setMaxWidth(Double.MAX_VALUE);
            mMoveUpButton.setDisable(true);
            mMoveUpButton.setOnAction(event -> {
                Tone selected = getToneListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    int index = getItem().getToneSequence().getTones().indexOf(selected);
                    getItem().getToneSequence().getTones().remove(selected);
                    getItem().getToneSequence().getTones().add(index - 1, selected);
                    getToneListView().getSelectionModel().select(selected);
                }
            });
        }

        return mMoveUpButton;
    }

    private Button getMoveDownButton()
    {
        if(mMoveDownButton == null)
        {
            mMoveDownButton = new Button();
            IconNode iconNode = new IconNode(FontAwesome.ARROW_DOWN);
            iconNode.setIconSize(12);
            iconNode.setFill(getMoveDownButton().getTextFill());
            mMoveDownButton.setGraphic(iconNode);
            mMoveDownButton.setMaxWidth(Double.MAX_VALUE);
            mMoveDownButton.setDisable(true);
            mMoveDownButton.setOnAction(event -> {
                Tone selected = getToneListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    int index = getItem().getToneSequence().getTones().indexOf(selected);
                    getItem().getToneSequence().getTones().remove(selected);
                    getItem().getToneSequence().getTones().add(index + 1, selected);
                    getToneListView().getSelectionModel().select(selected);
                }
            });
        }

        return mMoveDownButton;
    }

    private ListView<Tone> getToneListView()
    {
        if(mToneListView == null)
        {
            mToneListView = new ListView<>();
            mToneListView.setDisable(true);
            mToneListView.setPrefHeight(75);
            mToneListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                setTone(newValue);
            });
        }

        return mToneListView;
    }

    private ComboBox<AmbeTone> getAmbeToneComboBox()
    {
        if(mAmbeToneComboBox == null)
        {
            mAmbeToneComboBox = new ComboBox<>();
            mAmbeToneComboBox.setTooltip(new Tooltip("Tone to match"));
            mAmbeToneComboBox.getItems().addAll(AmbeTone.ALL_VALID_TONES);
            mAmbeToneComboBox.setDisable(true);
            mAmbeToneComboBox.setOnAction(mAmbeToneComboBoxEventHandler);
        }

        return mAmbeToneComboBox;
    }

    private TextField getDurationTextField()
    {
        if(mDurationTextField == null)
        {
            mDurationTextField = new TextField();
            mDurationTextField.setTooltip(new Tooltip("Duration in 20 millisecond units"));
            mDurationTextField.setMaxWidth(Double.MAX_VALUE);
            mDurationTextField.setTextFormatter(getDurationIntegerFormatter());
            mDurationTextField.setDisable(true);
        }

        return mDurationTextField;
    }

    private IntegerFormatter getDurationIntegerFormatter()
    {
        if(mDurationIntegerFormatter == null)
        {
            mDurationIntegerFormatter = new IntegerFormatter(1, 50);
            mDurationIntegerFormatter.valueProperty().addListener(mDurationValueChangeListener);
        }

        return mDurationIntegerFormatter;
    }

    private class DurationValueChangeListener implements ChangeListener<Integer>
    {
        private boolean mEnabled = false;

        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(mEnabled && newValue != null)
            {
                Tone selected = getToneListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    selected.setDuration(newValue);
                }
            }
        }

        public void enable()
        {
            mEnabled = true;
        }

        public void disable()
        {
            mEnabled = false;
        }
    }

    private class AmbeToneComboBoxEventHandler implements EventHandler<ActionEvent>
    {
        private boolean mEnabled = false;

        @Override
        public void handle(ActionEvent event)
        {
            if(mEnabled)
            {
                Tone selectedTone = getToneListView().getSelectionModel().getSelectedItem();

                if(selectedTone != null)
                {
                    AmbeTone ambeTone = mAmbeToneComboBox.getSelectionModel().getSelectedItem();
                    selectedTone.setAmbeTone(ambeTone);
                    modifiedProperty().set(true);
                }
            }
        }

        public void enable()
        {
            mEnabled = true;
        }

        public void disable()
        {
            mEnabled = false;
        }
    }
}
