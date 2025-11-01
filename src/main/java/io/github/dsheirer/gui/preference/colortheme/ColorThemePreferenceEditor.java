/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.colortheme;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.colortheme.ColorThemePreference;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * Preference settings for color theme
 */
public class ColorThemePreferenceEditor extends HBox
{
    private ColorThemePreference mColorThemePreference;
    private GridPane mEditorPane;
    private ToggleSwitch mDarkModeToggle;

    /**
     * Constructs an instance
     * @param userPreferences for obtaining reference to preference.
     */
    public ColorThemePreferenceEditor(UserPreferences userPreferences)
    {
        mColorThemePreference = userPreferences.getColorThemePreference();
        setMaxWidth(Double.MAX_VALUE);

        VBox vbox = new VBox();
        vbox.setMaxHeight(Double.MAX_VALUE);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.getChildren().add(getEditorPane());
        HBox.setHgrow(vbox, Priority.ALWAYS);
        getChildren().add(vbox);
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            int row = 0;
            mEditorPane = new GridPane();
            mEditorPane.setMaxWidth(Double.MAX_VALUE);
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(3);
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            Label themeLabel = new Label("Application Color Theme");
            mEditorPane.add(themeLabel, 0, row, 2, 1);
            
            GridPane.setHalignment(getDarkModeToggle(), HPos.RIGHT);
            mEditorPane.add(getDarkModeToggle(), 0, ++row);
            mEditorPane.add(new Label("Enable Dark Mode"), 1, row, 2, 1);

            Label restartLabel = new Label("Note: Application restart is required for theme changes to take full effect.");
            restartLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
            mEditorPane.add(restartLabel, 0, ++row, 3, 1);

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(30);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setHgrow(Priority.ALWAYS);
            mEditorPane.getColumnConstraints().addAll(c1, c2);
        }

        return mEditorPane;
    }

    /**
     * Toggle switch to enable/disable dark mode.
     */
    private ToggleSwitch getDarkModeToggle()
    {
        if(mDarkModeToggle == null)
        {
            mDarkModeToggle = new ToggleSwitch();
            mDarkModeToggle.setSelected(mColorThemePreference.isDarkModeEnabled());
            mDarkModeToggle.selectedProperty().addListener((observable, oldValue, enabled) -> {
                mColorThemePreference.setDarkModeEnabled(enabled);
                showRestartAlert();
            });
        }

        return mDarkModeToggle;
    }

    /**
     * Shows an alert informing the user that a restart is recommended.
     */
    private void showRestartAlert()
    {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Restart Required");
        alert.setHeaderText("Theme Change");
        alert.setContentText("Please restart the application for the theme change to take full effect.");
        alert.showAndWait();
    }
}

