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
package io.github.dsheirer.audio.playback;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import java.awt.Color;
import java.awt.Component;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * Displays one or more audio channel panels.
 */
public class AudioChannelsPanel extends JPanel
{
    /**
     * Constructs an instance
     * @param iconModel for icon access
     * @param userPreferences to monitor for audio playback changes
     * @param settingsManager for tone insertion settings
     * @param controller for the audio channels
     * @param aliasModel for accessing aliases
     */
    public AudioChannelsPanel(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                              IAudioController controller, AliasModel aliasModel)
    {
        setLayout(new MigLayout("insets 0 0 0 0",
            "[][sizegroup abc,grow,fill][][sizegroup abc,grow,fill]", "[grow,fill]"));

        setBackground(Color.BLACK);

        addSeparator();

        for(int x = 0; x < controller.getAudioChannels().size(); x++)
        {
            add(new AudioChannelPanel(controller.getAudioChannels().get(x), aliasModel, iconModel, settingsManager, userPreferences));

            if(x < controller.getAudioChannels().size() - 1)
            {
                addSeparator();
            }
        }

		/* Add an empty channel panel so that the panel is sized appropriately
         * for either a single channel or two channels */
        if(controller.getAudioChannels().size() == 1)
        {
            addSeparator();
            add(new AudioChannelPanel(null, aliasModel, iconModel, settingsManager, userPreferences), "growx");
        }
    }

    /**
     * Prepares for dispose to allow deregistering from services
     */
    public void dispose()
    {
        for(Component component: getComponents())
        {
            if(component instanceof AudioChannelPanel)
            {
                ((AudioChannelPanel)component).dispose();
            }
        }
    }

    /**
     * Adds a separator to this panel.
     */
    private void addSeparator()
    {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setBackground(Color.DARK_GRAY);
        add(separator);
    }
}
