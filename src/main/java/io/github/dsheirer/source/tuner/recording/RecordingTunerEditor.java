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
package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import javafx.scene.control.Label;

/**
 * Recording tuner editor.  Shows the recording file and the frequency of the recording.
 */
public class RecordingTunerEditor extends TunerEditor<RecordingTuner,RecordingTunerConfiguration>
{
    private final Label mRecordingPath = new Label();

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public RecordingTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        mRecordingPath.setWrapText(true);
        mRecordingPath.getStyleClass().add("tuner-value-label");
        getTuningSection().addRow("File", mRecordingPath);
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return 0;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return Long.MAX_VALUE;
    }

    @Override
    protected void tunerStatusUpdated()
    {
        mRecordingPath.setText(hasConfiguration() ? getConfiguration().getPath() : "");
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RecordingTunerConfiguration config = getConfiguration();
            config.setFrequency(getFrequencyControl().getFrequency());
            config.setMinimumFrequency(getMinimumFrequencyField().getFrequency());
            config.setMaximumFrequency(getMaximumFrequencyField().getFrequency());
            saveConfiguration();
        }
    }
}
