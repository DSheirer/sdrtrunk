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
package io.github.dsheirer.source.tuner.rtl.fc0013;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

/**
 * FC0013 Tuner Editor
 */
public class FC0013TunerEditor extends TunerEditor<RTL2832Tuner, FC0013TunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(FC0013TunerEditor.class);
    private static final long serialVersionUID = 1L;
    private static final FC0013EmbeddedTuner.LNAGain DEFAULT_LNA_GAIN = FC0013EmbeddedTuner.LNAGain.G14;
    private JButton mTunerInfoButton;
    private JToggleButton mBiasTButton;
    private JComboBox<SampleRate> mSampleRateCombo;

    private JToggleButton mAgcToggleButton;
    private JComboBox<FC0013EmbeddedTuner.LNAGain> mLNAGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public FC0013TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return FC0013EmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return FC0013EmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Access the FC0013 embedded tuner
     * @return tuner if there is a tuner, or null otherwise
     */
    private FC0013EmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (FC0013EmbeddedTuner) getTuner().getController().getEmbeddedTuner();
        }

        return null;
    }

    private String getLogPrefix()
    {
        return getEmbeddedTuner().getTunerType().getLabel() + " Tuner Controller - ";
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel());
        add(getBiasTButton(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JSeparator(), "span,growx,push");

        JPanel gainPanel = new JPanel();
        gainPanel.add(new JLabel("Gain"));
        gainPanel.add(getAgcToggleButton());
        gainPanel.add(new JLabel("LNA:"));
        add(gainPanel);
        add(getLNAGainCombo(), "wrap");
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        if(hasTuner())
        {
            getTunerIdLabel().setText(getTuner().getPreferredName());
        }
        else
        {
            getTunerIdLabel().setText(getDiscoveredTuner().getId());
        }

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
            getBiasTButton().setEnabled(true);
            getBiasTButton().setSelected(getConfiguration().isBiasT());
            getTunerInfoButton().setEnabled(true);
            getSampleRateCombo().setEnabled(true);
            getSampleRateCombo().setSelectedItem(getConfiguration().getSampleRate());
            getAgcToggleButton().setEnabled(true);
            getAgcToggleButton().setSelected(getConfiguration().getAGC());
            getLNAGainCombo().setEnabled(!getConfiguration().getAGC());
            getLNAGainCombo().setSelectedItem(getConfiguration().getLnaGain());
        }
        else
        {
            getBiasTButton().setEnabled(false);
            getBiasTButton().setSelected(false);
            getTunerInfoButton().setEnabled(false);
            getSampleRateCombo().setEnabled(false);
            getAgcToggleButton().setEnabled(false);
            getLNAGainCombo().setEnabled(false);
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    /**
     * Bias-T toggle button
     * @return bias-t button
     */
    private JToggleButton getBiasTButton()
    {
        if(mBiasTButton == null)
        {
            mBiasTButton = new JToggleButton("Bias-T");
            mBiasTButton.setOpaque(true);
            mBiasTButton.setContentAreaFilled(true);
            if(mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mBiasTButton.setBackground(new java.awt.Color(43, 43, 43));
                mBiasTButton.setForeground(new java.awt.Color(187, 187, 187));
            }
            mBiasTButton.setEnabled(false);
            mBiasTButton.addActionListener(e -> {
                if(!isLoading())
                {
                    getTuner().getController().setBiasT(mBiasTButton.isSelected());
                    save();
               }
            });
        }

        return mBiasTButton;
    }

    /**
     * Hyperlink button that provides tuner information
     */
    private JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Info");
            mTunerInfoButton.setOpaque(true);
            mTunerInfoButton.setContentAreaFilled(true);
            if(mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mTunerInfoButton.setBackground(new java.awt.Color(43, 43, 43));
                mTunerInfoButton.setForeground(new java.awt.Color(187, 187, 187));
            }
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(FC0013TunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private JComboBox getLNAGainCombo()
    {
        if(mLNAGainCombo == null)
        {
            mLNAGainCombo = new JComboBox<>(FC0013EmbeddedTuner.LNAGain.values());
            if(mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mLNAGainCombo.setBackground(new java.awt.Color(43, 43, 43));
                mLNAGainCombo.setForeground(new java.awt.Color(187, 187, 187));
                // Set a darker gray for disabled state that's more visible in dark mode
                javax.swing.UIManager.put("ComboBox.disabledForeground", new java.awt.Color(120, 120, 120));
            }
            mLNAGainCombo.setEnabled(false);
            mLNAGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        FC0013EmbeddedTuner.LNAGain lnaGain = (FC0013EmbeddedTuner.LNAGain) mLNAGainCombo.getSelectedItem();

                        if(lnaGain == null)
                        {
                            lnaGain = DEFAULT_LNA_GAIN;
                        }

                        if(mLNAGainCombo.isEnabled())
                        {
                            getEmbeddedTuner().setGain(getAgcToggleButton().isSelected(), lnaGain);
                        }

                        save();
                    }
                    catch(Exception e)
                    {
                        JOptionPane.showMessageDialog(FC0013TunerEditor.this, getLogPrefix() +
                                "couldn't apply the LNA gain setting - " + e.getLocalizedMessage());
                        mLog.error(getLogPrefix() + "couldn't apply LNA gain setting - ", e);
                    }
                }
            });
            mLNAGainCombo.setToolTipText("<html>LNA Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>");
        }

        return mLNAGainCombo;
    }

    private JComboBox getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>(SampleRate.values());
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(e ->
            {
                if(!isLoading())
                {
                    SampleRate sampleRate = (SampleRate) mSampleRateCombo.getSelectedItem();

                    try
                    {
                        getTuner().getController().setSampleRate(sampleRate);
                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(sampleRate.getRate());
                        save();
                    }
                    catch(SourceException | LibUsbException eSampleRate)
                    {
                        JOptionPane.showMessageDialog(FC0013TunerEditor.this,
                                getLogPrefix() + "couldn't apply the sample rate setting [" +
                                        sampleRate.getLabel() + "] " + eSampleRate.getLocalizedMessage());

                        mLog.error(getLogPrefix() + "couldn't apply sample rate setting [" + sampleRate.getLabel() +
                                "]", eSampleRate);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    private JToggleButton getAgcToggleButton()
    {
        if(mAgcToggleButton == null)
        {
            mAgcToggleButton = new JToggleButton("AGC");
            mAgcToggleButton.setEnabled(false);
            mAgcToggleButton.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        boolean agc = getAgcToggleButton().isSelected();
                        FC0013EmbeddedTuner.LNAGain lnaGain = (FC0013EmbeddedTuner.LNAGain)getLNAGainCombo().getSelectedItem();
                        getEmbeddedTuner().setGain(agc, lnaGain);
                        getLNAGainCombo().setEnabled(!agc);
                        save();
                    }
                    catch(Exception e)
                    {
                        JOptionPane.showMessageDialog(FC0013TunerEditor.this, getLogPrefix() +
                                "couldn't set AGC" + e.getLocalizedMessage());
                        mLog.error(getLogPrefix() + "couldn't set AGC", e);
                    }
                }
            });
            mAgcToggleButton.setToolTipText("<html>Automatic Gain Control (AGC). </html>");
        }

        return mAgcToggleButton;
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

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        RTL2832TunerController.Descriptor descriptor = getTuner().getController().getDescriptor();
        sb.append("<html><h3>RTL-2832 with " + getEmbeddedTuner().getTunerType().getLabel() + " Tuner</h3>");

        if(descriptor == null)
        {
            sb.append("No EEPROM Descriptor Available");
        }
        else
        {
            sb.append("<b>USB ID: </b>");
            sb.append(descriptor.getVendorID());
            sb.append(":");
            sb.append(descriptor.getProductID());
            sb.append("<br>");

            sb.append("<b>Vendor: </b>");
            sb.append(descriptor.getVendorLabel());
            sb.append("<br>");

            sb.append("<b>Product: </b>");
            sb.append(descriptor.getProductLabel());
            sb.append("<br>");

            sb.append("<b>Serial: </b>");
            sb.append(descriptor.getSerial());
            sb.append("<br>");

            sb.append("<b>IR Enabled: </b>");
            sb.append(descriptor.irEnabled());
            sb.append("<br>");

            sb.append("<b>Remote Wake: </b>");
            sb.append(descriptor.remoteWakeupEnabled());
            sb.append("<br>");
        }

        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            FC0013TunerConfiguration config = getConfiguration();
            config.setBiasT(getTuner().getController().isBiasT());
            config.setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            config.setSampleRate((SampleRate)getSampleRateCombo().getSelectedItem());
            config.setAGC(getAgcToggleButton().isSelected());
            FC0013EmbeddedTuner.LNAGain lnaGain = (FC0013EmbeddedTuner.LNAGain)getLNAGainCombo().getSelectedItem();
            config.setLnaGain(lnaGain);
            saveConfiguration();
        }
    }
}