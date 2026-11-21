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

package io.github.dsheirer.gui.preference.radioreference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.radioreference.AliasListUpdateInterval;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Preference settings for alias lists that track a Radio Reference system.
 */
public class RadioReferencePreferenceEditor extends VBox
{
    private RadioReferencePreference mRadioReferencePreference;
    private ComboBox<AliasListUpdateInterval> mUpdateIntervalComboBox;

    public RadioReferencePreferenceEditor(UserPreferences userPreferences)
    {
        mRadioReferencePreference = userPreferences.getRadioReferencePreference();

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(10);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        Label intervalLabel = new Label("Check Tracked Systems For Talkgroup Changes:");
        intervalLabel.setLabelFor(getUpdateIntervalComboBox());
        GridPane.setHalignment(intervalLabel, HPos.RIGHT);
        gridPane.add(intervalLabel, 0, 0);
        gridPane.add(getUpdateIntervalComboBox(), 1, 0);

        Label help = new Label("Alias lists that track a Radio Reference system check the system on this schedule " +
            "while sdrtrunk is running and a Radio Reference login is stored.  Alias lists with Auto-Update enabled " +
            "add or update talkgroup aliases when the system changed.  Use Refresh Now in the playlist editor's " +
            "Aliases tab to update an alias list immediately.");
        help.setWrapText(true);
        help.setMaxWidth(500);

        getChildren().addAll(gridPane, help);
    }

    private ComboBox<AliasListUpdateInterval> getUpdateIntervalComboBox()
    {
        if(mUpdateIntervalComboBox == null)
        {
            mUpdateIntervalComboBox = new ComboBox<>();
            mUpdateIntervalComboBox.getItems().addAll(AliasListUpdateInterval.values());
            mUpdateIntervalComboBox.getSelectionModel().select(mRadioReferencePreference.getAliasListUpdateInterval());
            mUpdateIntervalComboBox.setOnAction(event -> mRadioReferencePreference
                .setAliasListUpdateInterval(mUpdateIntervalComboBox.getSelectionModel().getSelectedItem()));
        }

        return mUpdateIntervalComboBox;
    }
}
