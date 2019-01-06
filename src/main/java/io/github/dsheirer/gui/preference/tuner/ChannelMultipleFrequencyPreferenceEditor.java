/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.gui.preference.tuner;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelMultiFrequencyPreference;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;


/**
 * Preference settings for channel source configurations containing multiple frequencies
 */
public class ChannelMultipleFrequencyPreferenceEditor extends HBox
{
    private static final String HELP_TEXT = "Rotation delay determines how long a decoder will wait before rotating " +
        "to the next frequency in a list for a rotating control channel configuration.";

    private static final Insets MARGINS = new Insets(5,5,5,5);
    private ChannelMultiFrequencyPreference mChannelMultiFrequencyPreference;
    private GridPane mEditorPane;
    private Label mRotationDelayLabel;
    private Spinner<Integer> mRotationDelaySpinner;
    private Button mResetButton;
    private Label mHelpText;

    public ChannelMultipleFrequencyPreferenceEditor(UserPreferences userPreferences)
    {
        mChannelMultiFrequencyPreference = userPreferences.getChannelMultiFrequencyPreference();
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            GridPane.setMargin(getRotationDelaySpinner(), new Insets(5, 5, 5, 5));
            mEditorPane.add(getRotationDelaySpinner(), 0, 0);
            GridPane.setMargin(getRotationDelayLabel(), new Insets(5, 5, 5, 5));
            mEditorPane.add(getRotationDelayLabel(), 1, 0);
            GridPane.setMargin(getResetButton(), MARGINS);
            mEditorPane.add(getResetButton(), 2, 0);
            GridPane.setMargin(getHelpText(), MARGINS);
            mEditorPane.add(getHelpText(), 0, 1, 3, 1);
        }

        return mEditorPane;
    }

    public Spinner<Integer> getRotationDelaySpinner()
    {
        if(mRotationDelaySpinner == null)
        {
            int currentDelay = (int)(mChannelMultiFrequencyPreference.getRotationDelay() / 1000);
            mRotationDelaySpinner = new Spinner<>(1, 60, currentDelay, 1);
            mRotationDelaySpinner.setEditable(true);
            mRotationDelaySpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                mChannelMultiFrequencyPreference.setRotationDelay(newValue * 1000);
            });
        }

        return mRotationDelaySpinner;
    }

    public Label getRotationDelayLabel()
    {
        if(mRotationDelayLabel == null)
        {
            mRotationDelayLabel = new Label("Rotation Delay (seconds)");
        }

        return mRotationDelayLabel;
    }

    public Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button("Reset");
            mResetButton.setOnAction(event -> {
                    mChannelMultiFrequencyPreference.resetRotationDelay();
                    int currentDelay = (int)(mChannelMultiFrequencyPreference.getRotationDelay() / 1000);
                    getRotationDelaySpinner().getValueFactory().setValue(currentDelay);
            });
        }

        return mResetButton;
    }

    public Label getHelpText()
    {
        if(mHelpText == null)
        {
            mHelpText = new Label(HELP_TEXT);
            mHelpText.setWrapText(true);
        }

        return mHelpText;
    }
}
