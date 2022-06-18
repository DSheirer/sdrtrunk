/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay;

import com.github.dsheirer.sdrplay.parameter.tuner.GainReduction;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;

/**
 * Abstract RSP tuner editor
 */
public abstract class RspTunerEditor<C extends RspTunerConfiguration> extends TunerEditor<RspTuner,C>
{
    private Logger mLog = LoggerFactory.getLogger(RspTunerEditor.class);
    private JSlider mGainSlider;
    private JLabel mGainValueLabel;

    /**
     * Constructs an instance
     * @param userPreferences for preference settings
     * @param tunerManager to notify for state changes
     * @param discoveredTuner to be edited.
     */
    public RspTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
    }

    private RspTunerController getTunerController()
    {
        return (RspTunerController) getTuner().getTunerController();
    }

    /**
     * Gain reduction slider control
     */
    protected JSlider getGainSlider()
    {
        if(mGainSlider == null)
        {
            mGainSlider = new JSlider(JSlider.HORIZONTAL, GainReduction.MIN_GAIN_INDEX,
                    GainReduction.MAX_GAIN_INDEX, GainReduction.MIN_GAIN_INDEX);
            mGainSlider.setEnabled(false);
            mGainSlider.setMajorTickSpacing(1);
            mGainSlider.setPaintTicks(true);
            mGainSlider.addChangeListener(event ->
            {
                int gain = mGainSlider.getValue();

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set RSP gain to:" + gain, e);
                        JOptionPane.showMessageDialog(mGainSlider, "Couldn't set RSP gain value to " + gain);
                    }
                }

                getGainValueLabel().setText(String.valueOf(gain));
            });
        }

        return mGainSlider;
    }

    /**
     * Label for displaying current gain index value
     */
    protected JLabel getGainValueLabel()
    {
        if(mGainValueLabel == null)
        {
            mGainValueLabel = new JLabel("0");
            mGainValueLabel.setEnabled(false);
        }

        return mGainValueLabel;
    }
}
