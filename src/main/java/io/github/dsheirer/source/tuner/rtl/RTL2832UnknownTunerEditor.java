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

package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JSeparator;

/**
 * Tuner editor for RTL2832 tuner that has not been started, or for an unknown tuner type
 */
public class RTL2832UnknownTunerEditor extends TunerEditor<RTL2832Tuner, RTL2832TunerConfiguration>
{
    /**
     * Constructs an instance
     *
     * @param userPreferences for starting wide-band recorders
     * @param tunerManager for requesting configuration saves.
     * @param discoveredTuner that is not started, or that doesn't have a recognized tuner type
     */
    public RTL2832UnknownTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
                                     DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel(), "wrap");

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");

//        add(new JLabel("Frequency (MHz):"));
//        add(getFrequencyPanel(), "wrap");
//
//        add(new JLabel("Sample Rate:"));
////        add(getSampleRateCombo(), "wrap");
//
//        add(new JSeparator(), "span,growx,push");
    }

    @Override
    protected void save()
    {
        //No-op
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);
        getTunerIdLabel().setText(getDiscoveredTuner().getId() + (hasTuner() ? " ID:" + getTuner().getUniqueID() : ""));

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        setLoading(false);
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }
}
