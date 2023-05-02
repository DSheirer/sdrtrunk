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

package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.GainReduction;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.util.ThreadPool;
import java.awt.Color;
import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;

/**
 * Abstract RSP tuner editor
 */
public abstract class RspTunerEditor<C extends RspTunerConfiguration> extends TunerEditor<RspTuner,C> implements IGainOverloadListener
{
    private Logger mLog = LoggerFactory.getLogger(RspTunerEditor.class);
    private JSlider mGainSlider;
    private JLabel mGainValueLabel;
    private JComboBox<AgcMode> mAgcModeCombo;
    private JButton mGainOverloadButton;
    private AtomicBoolean mGainOverloadAlert = new AtomicBoolean();

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

    /**
     * IF AGC mode combobox control
     */
    protected JComboBox<AgcMode> getAgcModeCombo()
    {
        if(mAgcModeCombo == null)
        {
            mAgcModeCombo = new JComboBox<>(AgcMode.values());
            mAgcModeCombo.setEnabled(false);
            mAgcModeCombo.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    AgcMode selected = (AgcMode)mAgcModeCombo.getSelectedItem();
                    try
                    {
                        getTunerController().getControlRsp().setAgcMode(selected);
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting AGC mode on RSP device");
                    }
                    save();
                }
            });
        }

        return mAgcModeCombo;
    }

    /**
     * Gain overload button.  Used in a disabled state to indicate (e.g. flashing color) that gain overload is detected.
     */
    protected JButton getGainOverloadButton()
    {
        if(mGainOverloadButton == null)
        {
            mGainOverloadButton = new JButton("Gain Overload");
            mGainOverloadButton.setToolTipText("Notification that manual gain is set too high and causing power overload.  Reduce manual gain when this flashes.");
            mGainOverloadButton.setEnabled(false);
        }

        return mGainOverloadButton;
    }

    @Override
    public void notifyGainOverload(TunerSelect tunerSelect)
    {
        if(hasTuner() && getTuner().getRspTunerController().getTunerSelect() == tunerSelect)
        {
            //Set overload alert
            EventQueue.invokeLater(() -> setGainOverloadAlert(true));

            //Schedule a reset to happen 1 second later
            ThreadPool.SCHEDULED.schedule((Runnable) () -> setGainOverloadAlert(false), 600, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Toggles the alert styling of the disabled gain overload button to indicate an alert condition or a normal
     * operating condition.
     * @param alert true to apply alert styling or false to reset.
     */
    private void setGainOverloadAlert(boolean alert)
    {
        if(alert && mGainOverloadAlert.compareAndSet(false, true))
        {
            getGainOverloadButton().setEnabled(true);
            getGainOverloadButton().setForeground(Color.YELLOW);
            getGainOverloadButton().setBackground(Color.RED);
        }
        else if(!alert && mGainOverloadAlert.compareAndSet(true, false))
        {
            getGainOverloadButton().setEnabled(false);
            getGainOverloadButton().setForeground(getForeground());
            getGainOverloadButton().setBackground(getBackground());
        }
        else
        {
            mLog.info("Ignoring duplicate gain alerting - alert[" + alert + "] atomic [" + mGainOverloadAlert.get() + "]");
        }
    }
}
