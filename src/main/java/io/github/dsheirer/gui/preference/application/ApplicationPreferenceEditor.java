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

package io.github.dsheirer.gui.preference.application;

import io.github.dsheirer.gui.preference.PreferenceEditor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.preference.application.ApplicationPreference;
import jakarta.annotation.PostConstruct;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;


/**
 * Preference settings for application
 */
public class ApplicationPreferenceEditor extends PreferenceEditor
{
    private ApplicationPreference mApplicationPreference;
    private GridPane mEditorPane;
    private Label mAutoStartTimeoutLabel;
    private Spinner<Integer> mTimeoutSpinner;

    /**
     * Constructs an instance
     */
    public ApplicationPreferenceEditor()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        mApplicationPreference = getUserPreferences().getApplicationPreference();
        getChildren().add(getEditorPane());
    }

    @Override
    public PreferenceEditorType getPreferenceEditorType()
    {
        return PreferenceEditorType.APPLICATION;
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
            GridPane.setHalignment(getAutoStartTimeoutLabel(), HPos.RIGHT);
            mEditorPane.add(getAutoStartTimeoutLabel(), 0, row);
            mEditorPane.add(getTimeoutSpinner(), 1, row);
            mEditorPane.add(new Label("seconds"), 2, row);
        }

        return mEditorPane;
    }

    private Label getAutoStartTimeoutLabel()
    {
        if(mAutoStartTimeoutLabel == null)
        {
            mAutoStartTimeoutLabel = new Label("Channel Auto-Start Timeout");
        }

        return mAutoStartTimeoutLabel;
    }

    /**
     * Spinner to select channel auto-start timeout value in range 0-30 seconds.
     * @return spinner
     */
    private Spinner<Integer> getTimeoutSpinner()
    {
        if(mTimeoutSpinner == null)
        {
            mTimeoutSpinner = new Spinner<>(0, 30, mApplicationPreference.getChannelAutoStartTimeout(), 1);
            mTimeoutSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mApplicationPreference.setChannelAutoStartTimeout(newValue));
        }

        return mTimeoutSpinner;
    }
}
