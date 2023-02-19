/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.tuner;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.preference.source.TunerPreference;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;


/**
 * Preference settings for channel event view
 */
public class TunerPreferenceEditor extends HBox
{
    private static final String HELP_TEXT_POLYPHASE = "Processes all channels from tuner.  This " +
        "channelizer is more efficient when decoding 3 or more channels.";
    private static final String HELP_TEXT_HETERODYNE = "Processes each channel on-demand.  This " +
        "channelizer may work better for computers with constrained resources when processing a small number of channels.";

    private TunerPreference mTunerPreference;
    private GridPane mEditorPane;
    private ChoiceBox<ChannelizerType> mChannelizerTypeChoiceBox;
    private Label mChannelizerLabel;
    private Label mPolyphaseLabel;
    private Label mHelpTextPolyphaseLabel;
    private Label mHeterodyneLabel;
    private Label mHelpTextHeterodyneLabel;
    private ChoiceBox<RspDuoSelectionMode> mRspDuoTunerModeChoiceBox;
    private Label mRspDuoModeLabel;

    public TunerPreferenceEditor(UserPreferences userPreferences)
    {
        mTunerPreference = userPreferences.getTunerPreference();
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            int row = 0;
            mEditorPane = new GridPane();
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(10);
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));
            GridPane.setHalignment(getChannelizerLabel(), HPos.RIGHT);
            mEditorPane.add(getChannelizerLabel(), 0, row);
            mEditorPane.add(getChannelizerTypeChoiceBox(), 1, row);
            mEditorPane.add(getPolyphaseLabel(), 0, ++row, 2, 1);
            mEditorPane.add(getHelpTextPolyphaseLabel(), 0, ++row, 2, 3);
            row += 3;
            mEditorPane.add(new Label(" "), 0, row);
            mEditorPane.add(getHeterodyneLabel(), 0, ++row, 2, 1);
            mEditorPane.add(getHelpTextHeterodyneLabel(), 0, ++row, 2, 3);
            row += 3;
            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, row, 2, 1);
            mEditorPane.add(getRspDuoModeLabel(), 0, ++row);
            mEditorPane.add(getRspDuoTunerModeChoiceBox(), 1, row);
        }

        return mEditorPane;
    }

    private Label getChannelizerLabel()
    {
        if(mChannelizerLabel == null)
        {
            mChannelizerLabel = new Label("Channelizer Type");
        }

        return mChannelizerLabel;
    }

    private ChoiceBox<ChannelizerType> getChannelizerTypeChoiceBox()
    {
        if(mChannelizerTypeChoiceBox == null)
        {
            mChannelizerTypeChoiceBox = new ChoiceBox<>();
            mChannelizerTypeChoiceBox.getItems().addAll(ChannelizerType.values());

            ChannelizerType current = mTunerPreference.getChannelizerType();
            mChannelizerTypeChoiceBox.getSelectionModel().select(current);

            mChannelizerTypeChoiceBox.setOnAction(event -> {
                ChannelizerType selected = mChannelizerTypeChoiceBox.getSelectionModel().getSelectedItem();
                mTunerPreference.setChannelizerType(selected);

                Label label = new Label("Please restart the application for this change to take effect");
                label.setWrapText(true);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.getDialogPane().setContent(label);
                alert.initOwner(((Node)getChannelizerTypeChoiceBox()).getScene().getWindow());
                alert.show();
            });
        }

        return mChannelizerTypeChoiceBox;
    }

    private Label getPolyphaseLabel()
    {
        if(mPolyphaseLabel == null)
        {
            mPolyphaseLabel = new Label("Polyphase (default)");
        }

        return mPolyphaseLabel;
    }

    private Label getHelpTextPolyphaseLabel()
    {
        if(mHelpTextPolyphaseLabel == null)
        {
            mHelpTextPolyphaseLabel = new Label(HELP_TEXT_POLYPHASE);
            mHelpTextPolyphaseLabel.setWrapText(true);
        }

        return mHelpTextPolyphaseLabel;
    }

    private Label getHeterodyneLabel()
    {
        if(mHeterodyneLabel == null)
        {
            mHeterodyneLabel = new Label("Heterodyne");
        }

        return mHeterodyneLabel;
    }

    private Label getHelpTextHeterodyneLabel()
    {
        if(mHelpTextHeterodyneLabel == null)
        {
            mHelpTextHeterodyneLabel = new Label(HELP_TEXT_HETERODYNE);
            mHelpTextHeterodyneLabel.setWrapText(true);
        }

        return mHelpTextHeterodyneLabel;
    }

    private ChoiceBox<RspDuoSelectionMode> getRspDuoTunerModeChoiceBox()
    {
        if(mRspDuoTunerModeChoiceBox == null)
        {
            mRspDuoTunerModeChoiceBox = new ChoiceBox<>();
            mRspDuoTunerModeChoiceBox.getItems().addAll(RspDuoSelectionMode.values());

            RspDuoSelectionMode current = mTunerPreference.getRspDuoTunerMode();
            mRspDuoTunerModeChoiceBox.getSelectionModel().select(current);

            mRspDuoTunerModeChoiceBox.setOnAction(event -> {
                RspDuoSelectionMode selected = mRspDuoTunerModeChoiceBox.getSelectionModel().getSelectedItem();
                mTunerPreference.setRspDuoTunerMode(selected);

                Label label = new Label("Please restart the application for this change to take effect");
                label.setWrapText(true);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.getDialogPane().setContent(label);
                alert.initOwner(((Node)getRspDuoTunerModeChoiceBox()).getScene().getWindow());
                alert.show();
            });
        }

        return mRspDuoTunerModeChoiceBox;
    }

    private Label getRspDuoModeLabel()
    {
        if(mRspDuoModeLabel == null)
        {
            mRspDuoModeLabel = new Label("SDRPlay RSPduo Selection Mode");
        }

        return mRspDuoModeLabel;
    }
}
