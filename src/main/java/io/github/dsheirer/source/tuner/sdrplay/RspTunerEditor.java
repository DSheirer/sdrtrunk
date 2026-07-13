/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.util.ThreadPool;
import java.awt.Color;
import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

/**
 * Abstract RSP tuner editor
 */
public abstract class RspTunerEditor<C extends RspTunerConfiguration> extends TunerEditor<RspTuner,C> implements ITunerStatusListener
{
    private Logger mLog = LoggerFactory.getLogger(RspTunerEditor.class);
    protected static final String MANUAL = "Manual";
    protected static final String AUTOMATIC = "Automatic";
    private JToggleButton mAgcButton;
    private JLabel mGainValueLabel;
    private LnaSlider mLNASlider;
    private IfGainSlider mIfGainSlider;
    private JButton mGainOverloadButton;
    private JPanel mGainPanel;
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

    @Override
    public long getMinimumTunableFrequency()
    {
        return RspTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return RspTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Gain controls panel
     * @return gain panel
     */
    protected JPanel getGainPanel()
    {
        if(mGainPanel == null)
        {
            mGainPanel = new JPanel();
            mGainPanel.setLayout(new MigLayout("insets 0","[][][grow,fill][][]",""));
            mGainPanel.add(getGainValueLabel());
            mGainPanel.add(getGainOverloadButton());
            mGainPanel.add(new JLabel()); //empty label to grow to fill space
            mGainPanel.add(new JLabel("IF Gain Mode:"));
            mGainPanel.add(getAgcButton());
        }

        return mGainPanel;
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
     * IF AGC mode (enable/disable) toggle button
     */
    protected JToggleButton getAgcButton()
    {
        if(mAgcButton == null)
        {
            mAgcButton = new JToggleButton(MANUAL);
            mAgcButton.setEnabled(false);
            mAgcButton.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setAgcMode(mAgcButton.isSelected() ? AgcMode.ENABLE : AgcMode.DISABLE);
                        updateGainLabel();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting AGC mode on RSP device");
                    }
                    save();
                }

                getIfGainSlider().setEnabled(!getAgcButton().isSelected());

                if(mAgcButton.isSelected())
                {
                    getAgcButton().setText(AUTOMATIC);
                }
                else
                {
                    getAgcButton().setText(MANUAL);
                }
            });
        }

        return mAgcButton;
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

    @Override
    public void frequencyUpdated()
    {
        updateLnaSlider();
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
    }

    /**
     * Updates the LNA slider with the number of LNA states available for the current frequency.
     */
    protected void updateLnaSlider()
    {
        if(hasTuner())
        {
            int max = getTunerController().getControlRsp().getMaximumLNASetting();
            if(max != getLNASlider().getMaximum())
            {
                EventQueue.invokeLater(() -> {
                    setLoading(true);
                    //Adjust the value if it's less than max
                    getLNASlider().setValue(Math.min(getLNASlider().getValue(), max));
                    getLNASlider().setMaximum(max);
                    setLoading(false);
                });
            }
        }
    }

    /**
     * LNA Gain Slider
     */
    protected LnaSlider getLNASlider()
    {
        if(mLNASlider == null)
        {
            mLNASlider = new LnaSlider();
            mLNASlider.setOpaque(true);
            if(mUserPreferences != null && mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mLNASlider.setBackground(new java.awt.Color(43, 43, 43));
                mLNASlider.setForeground(new java.awt.Color(187, 187, 187));
            }
            mLNASlider.setEnabled(true);
            mLNASlider.setMajorTickSpacing(1);
            mLNASlider.setPaintTicks(true);
            mLNASlider.addChangeListener(event ->
            {
                JSlider source = (JSlider)event.getSource();

                if(!source.getValueIsAdjusting())
                {
                    updateGain();
                }
            });
        }

        return mLNASlider;
    }

    /**
     * Updates the gain settings when the LNA or Baseband gain value is changed by the user.
     */
    private void updateGain()
    {
        int lna = getLNASlider().getLNA();
        int gr = getIfGainSlider().getGR();

        if(hasTuner() && !isLoading())
        {
            try
            {
                getTunerController().getControlRsp().setGain(lna, gr);
                save();
                updateGainLabel();
            }
            catch(Exception e)
            {
                mLog.error("Couldn't set RSP gain to LNA:" + lna + " Gain Reduction:" + gr, e);
                JOptionPane.showMessageDialog(mIfGainSlider, "Couldn't set RSP gain value to LNA:" + lna + " Gain Reduction:" + gr);
            }
        }
    }

    protected void updateGainLabel()
    {
        try
        {
            float currentGain = getTunerController().getControlRsp().getCurrentGain();
            getGainValueLabel().setText((int)currentGain + " dB");
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error accessing current gain value from RSP tuner", se);
        }
    }

    /**
     * IF Gain Slider
     */
    protected IfGainSlider getIfGainSlider()
    {
        if(mIfGainSlider == null)
        {
            mIfGainSlider = new IfGainSlider();
            mIfGainSlider.setOpaque(true);
            if(mUserPreferences != null && mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mIfGainSlider.setBackground(new java.awt.Color(43, 43, 43));
                mIfGainSlider.setForeground(new java.awt.Color(187, 187, 187));
            }
            mIfGainSlider.setEnabled(true);
            mIfGainSlider.setMajorTickSpacing(1);
            mIfGainSlider.setPaintTicks(true);
            mIfGainSlider.addChangeListener(event ->
            {
                JSlider source = (JSlider)event.getSource();

                if(!source.getValueIsAdjusting())
                {
                    updateGain();
                }
            });
        }

        return mIfGainSlider;
    }

    /**
     * JSlider implementation that inverts the scale to support LNA values.
     */
    public class LnaSlider extends JSlider
    {
        /**
         * Constructs an instance
         */
        public LnaSlider()
        {
            super(JSlider.HORIZONTAL, 0,9,9);
        }

        /**
         * Slider value converted to the LNA scale.
         * @return lna value.
         */
        public int getLNA()
        {
             return getMaximum() - getValue();
        }

        /**
         * Sets the slider with the value converted from the provided LNA value.
         * @param lna value to apply
         */
        public void setLNA(int lna)
        {
            setValue(getMaximum() - lna);
        }
    }

    /**
     * JSlider implementation that inverts the scale to support IF Gain (aka Gain Reduction) values.
     */
    public class IfGainSlider extends JSlider
    {
        /**
         * Constructs an instance
         */
        public IfGainSlider()
        {
            super(JSlider.HORIZONTAL, 0, 39, 30);
        }

        /**
         * Slider value converted to the gain reduction scale.
         * @return gain reduction value.
         */
        public int getGR()
        {
            return getIfGainSlider().getMaximum() - getIfGainSlider().getValue() + 20;
        }

        /**
         * Sets the slider with the value converted from the provided gain reduction value.
         * @param gainReduction value to apply
         */
        public void setGR(int gainReduction)
        {
            setValue(getMaximum() - (gainReduction - 20));
        }
    }
}
