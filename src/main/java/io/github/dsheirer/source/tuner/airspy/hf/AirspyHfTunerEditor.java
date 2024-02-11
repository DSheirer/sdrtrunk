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

package io.github.dsheirer.source.tuner.airspy.hf;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.io.IOException;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

/**
 * Tuner editor for Airspy HF+/Discovery tuners
 */
public class AirspyHfTunerEditor extends TunerEditor<AirspyHfTuner,AirspyHfTunerConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspyHfTunerEditor.class);
    private JComboBox<AirspyHfSampleRate> mSampleRateCombo;
    private JComboBox<Attenuation> mAttenuationCombo;
    private JToggleButton mAgcToggleButton;
    private JToggleButton mLnaToggleButton;

    /**
     * Constructs an instance
     *
     * @param userPreferences
     * @param tunerManager for requesting configuration saves.
     * @param discoveredTuner
     */
    public AirspyHfTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return AirspyHfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return AirspyHfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel(), "wrap");

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JSeparator(), "span");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new MigLayout("insets 0", "[left]", ""));
        buttonPanel.add(getAgcToggleButton());
        buttonPanel.add(getLnaToggleButton());
        add(new JLabel(" "));
        add(buttonPanel, "wrap");

        add(new JLabel("Attenuation:"));
        add(getAttenuationCombo(), "wrap");
    }

    @Override
    protected void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = ((SpinnerNumberModel) getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            getConfiguration().setSampleRate((int)getTuner().getController().getSampleRate());

            Attenuation attenuation = (Attenuation)getAttenuationCombo().getSelectedItem();
            getConfiguration().setAttenuation(attenuation);
            getConfiguration().setAgc(getAgcToggleButton().isSelected());
            getConfiguration().setLna(getLnaToggleButton().isSelected());

            saveConfiguration();
        }
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        if(hasTuner())
        {
            getTunerIdLabel().setText("Airspy " + getTuner().getController().getBoardId() + " SER# " +
                    getTuner().getController().getSerialNumber());

            //Permanently disable the sample rates combo and force 912kHz usage - during dev testing, attempts to use
            //the other sample rates produced inconsistent results.
            getSampleRateCombo().setEnabled(false);
            getSampleRateCombo().removeAllItems();
            for(AirspyHfSampleRate sampleRate: getTuner().getController().getAvailableSampleRates())
            {
                getSampleRateCombo().addItem(sampleRate);
            }
            getAgcToggleButton().setEnabled(true);
            getAgcToggleButton().setSelected(getTuner().getController().getAgc());
            getLnaToggleButton().setEnabled(true);
            getLnaToggleButton().setSelected(getTuner().getController().getLna());
            getSampleRateCombo().setSelectedItem(getTuner().getController().getCurrentAirspySampleRate());
            getAttenuationCombo().setEnabled(true);
            getAttenuationCombo().setSelectedItem(getTuner().getController().getAttenuation());
        }
        else
        {
            getTunerIdLabel().setText("Airspy HF+");
            getAgcToggleButton().setEnabled(false);
            getAgcToggleButton().setSelected(false);
            getLnaToggleButton().setEnabled(false);
            getLnaToggleButton().setSelected(false);
            getSampleRateCombo().setEnabled(false);
            getAttenuationCombo().setEnabled(false);
            getAttenuationCombo().setSelectedItem(Attenuation.A0);
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getTunerController().isLockedSampleRate())
        {
            getSampleRateCombo().setToolTipText("Sample Rate is locked.  Disable decoding channels to unlock.");
        }
        else if(hasTuner())
        {
            getSampleRateCombo().setToolTipText("Select a sample rate for the tuner");
        }
        else
        {
            getSampleRateCombo().setToolTipText("No tuner available");
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setEnabled(!locked);
        updateSampleRateToolTip();
    }

    /**
     * Sample rate combo for selecting among available sample rates.
     * @return sample rate combo
     */
    private JComboBox<AirspyHfSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(e ->
            {
                if(!isLoading())
                {
                    AirspyHfSampleRate sampleRate = (AirspyHfSampleRate)mSampleRateCombo.getSelectedItem();

                    if(sampleRate != null)
                    {
                        try
                        {
                            getTuner().getController().setSampleRate(sampleRate);

                            //Adjust the min/max values for the sample rate.
                            adjustForSampleRate(sampleRate.getSampleRate());

                            save();
                        }
                        catch(SourceException se)
                        {
                            mLog.error("Error setting Airspy Hf Sample Rate [" + sampleRate + "]", se);
                            JOptionPane.showMessageDialog(AirspyHfTunerEditor.this,
                                    "Airspy Tuner Controller - couldn't apply the sample rate setting [" +
                                            sampleRate + "] " + se.getLocalizedMessage());
                        }
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Combobox with available attenuation items
     * @return combo box
     */
    private JComboBox<Attenuation> getAttenuationCombo()
    {
        if(mAttenuationCombo == null)
        {
            mAttenuationCombo = new JComboBox<>(Attenuation.values());
            mAttenuationCombo.setEnabled(false);
            mAttenuationCombo.addActionListener(e -> {
                if(!isLoading())
                {
                    Attenuation selected = (Attenuation)mAttenuationCombo.getSelectedItem();
                    try
                    {
                        getTuner().getController().setAttenuation(selected);
                        save();
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error setting Airspy Hf attenuation [" + selected + "]", ioe);
                        JOptionPane.showMessageDialog(AirspyHfTunerEditor.this,
                                "Airspy Tuner Controller - couldn't apply attenuation setting [" +
                                        selected + "] " + ioe.getLocalizedMessage());
                    }
                }
            });
        }

        return mAttenuationCombo;
    }

    /**
     * Automatic Gain Control (AGC) toggle button
     */
    private JToggleButton getAgcToggleButton()
    {
        if(mAgcToggleButton == null)
        {
            mAgcToggleButton = new JToggleButton("AGC");
            mAgcToggleButton.setToolTipText("Automatic Gain Control");
            mAgcToggleButton.setEnabled(false);
            mAgcToggleButton.addActionListener(e -> {
                if(!isLoading())
                {
                    try
                    {
                        getTuner().getController().setAgc(mAgcToggleButton.isSelected());
                        save();
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error setting Airspy HF AGC", ioe);
                        JOptionPane.showMessageDialog(AirspyHfTunerEditor.this,
                        "Airspy HF Tuner Controller - couldn't change AGC setting" + ioe.getLocalizedMessage());
                    }
                }
            });
        }

        return mAgcToggleButton;
    }

    /**
     * Low Noise Amplifier (LNA) toggle button
     */
    private JToggleButton getLnaToggleButton()
    {
        if(mLnaToggleButton == null)
        {
            mLnaToggleButton = new JToggleButton("LNA");
            mLnaToggleButton.setToolTipText("Low Noise Amplifier");
            mLnaToggleButton.setEnabled(false);
            mLnaToggleButton.addActionListener(e -> {
                if(!isLoading())
                {
                    try
                    {
                        getTuner().getController().setLna(mLnaToggleButton.isSelected());
                        save();
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error setting Airspy HF LNA", ioe);
                        JOptionPane.showMessageDialog(AirspyHfTunerEditor.this,
                                "Airspy HF Tuner Controller - couldn't change LNA setting" + ioe.getLocalizedMessage());
                    }
                }
            });
        }

        return mLnaToggleButton;
    }
}
