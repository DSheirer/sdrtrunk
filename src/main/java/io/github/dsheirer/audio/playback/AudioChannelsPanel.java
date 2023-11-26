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
package io.github.dsheirer.audio.playback;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JSeparator;

public class AudioChannelsPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    public AudioChannelsPanel(IAudioController controller, UserPreferences userPreferences, SettingsManager settingsManager,
                              AliasModel aliasModel, IconModel iconModel)
    {
        setLayout(new MigLayout("insets 0 0 0 0",
            "[][sizegroup abc,grow,fill][][sizegroup abc,grow,fill]", "[grow,fill]"));

        setBackground(Color.BLACK);

        List<AudioOutput> outputs = controller.getAudioOutputs();

        addSeparator();

        for(int x = 0; x < outputs.size(); x++)
        {
            add(new AudioChannelPanel(userPreferences, settingsManager, aliasModel, iconModel, outputs.get(x)));

            if(x < outputs.size() - 1)
            {
                addSeparator();
            }
        }

		/* Add an empty channel panel so that the panel is sized appropriately
         * for either a single channel or two channels */
        if(outputs.size() == 1)
        {
            addSeparator();
            add(new AudioChannelPanel(userPreferences, settingsManager, aliasModel, iconModel, null), "growx");
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

    private void addSeparator()
    {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setBackground(Color.DARK_GRAY);
        add(separator);
    }
}
