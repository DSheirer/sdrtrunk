/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio;

import audio.output.AudioOutput;
import icon.IconManager;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AudioChannelsPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    public AudioChannelsPanel(IconManager iconManager, SettingsManager settingsManager, IAudioController controller)
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[][sg abc,grow,fill][][sg abc,grow,fill]", "[grow,fill]"));

        setBackground(Color.BLACK);

        List<AudioOutput> outputs = controller.getAudioOutputs();

        addSeparator();

        for(int x = 0; x < outputs.size(); x++)
        {
            add(new AudioChannelPanel(iconManager, settingsManager, outputs.get(x)));

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
            add(new AudioChannelPanel(iconManager, settingsManager, null), "growx");
        }
    }

    private void addSeparator()
    {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setBackground(Color.DARK_GRAY);
        add(separator);
    }
}
