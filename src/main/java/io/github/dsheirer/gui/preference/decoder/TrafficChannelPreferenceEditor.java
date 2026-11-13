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
package io.github.dsheirer.gui.preference.decoder;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.decoder.TrafficChannelPreference;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Preference settings for traffic channel allocation on trunked systems
 */
public class TrafficChannelPreferenceEditor extends VBox
{
    private final TrafficChannelPreference mTrafficChannelPreference;
    private CheckBox mIgnoreUnaliasedTalkgroupsCheckBox;

    public TrafficChannelPreferenceEditor(UserPreferences userPreferences)
    {
        mTrafficChannelPreference = userPreferences.getTrafficChannelPreference();

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(10);

        Label description = new Label("Talkgroup calls on a talkgroup with no alias in the control channel's alias " +
            "list are left untuned, which keeps tuners free for the talkgroups you have aliased.  Ignored calls " +
            "still appear in the channel events.");
        description.setWrapText(true);

        getChildren().addAll(getIgnoreUnaliasedTalkgroupsCheckBox(), description);
    }

    private CheckBox getIgnoreUnaliasedTalkgroupsCheckBox()
    {
        if(mIgnoreUnaliasedTalkgroupsCheckBox == null)
        {
            mIgnoreUnaliasedTalkgroupsCheckBox = new CheckBox("Don't tune unaliased talkgroups");
            mIgnoreUnaliasedTalkgroupsCheckBox.setSelected(mTrafficChannelPreference.isIgnoreUnaliasedTalkgroups());
            mIgnoreUnaliasedTalkgroupsCheckBox.setOnAction(event -> mTrafficChannelPreference
                .setIgnoreUnaliasedTalkgroups(mIgnoreUnaliasedTalkgroupsCheckBox.isSelected()));
        }

        return mIgnoreUnaliasedTalkgroupsCheckBox;
    }
}
