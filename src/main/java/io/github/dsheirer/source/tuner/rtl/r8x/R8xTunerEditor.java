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
package io.github.dsheirer.source.tuner.rtl.r8x;

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
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.usb.UsbException;

/**
 * R8xxx Tuner Editor
 */
public class R8xTunerEditor extends TunerEditor<RTL2832Tuner, R8xTunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(R8xTunerEditor.class);
    private static final long serialVersionUID = 1L;
    private static final R8xEmbeddedTuner.MasterGain DEFAULT_GAIN = R8xEmbeddedTuner.MasterGain.GAIN_279;
    private JButton mTunerInfoButton;
    private JToggleButton mBiasTButton;
    private JComboBox<SampleRate> mSampleRateCombo;
    private JComboBox<R8xEmbeddedTuner.MasterGain> mMasterGainCombo;
    private JComboBox<R8xEmbeddedTuner.MixerGain> mMixerGainCombo;
    private JComboBox<R8xEmbeddedTuner.LNAGain> mLNAGainCombo;
    private JComboBox<R8xEmbeddedTuner.VGAGain> mVGAGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public R8xTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return R8xEmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return R8xEmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Access the R8xxx embedded tuner
     * @return R8xxx tuner if there is a tuner, or null otherwise
     */
    private R8xEmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (R8xEmbeddedTuner) getTuner().getController().getEmbeddedTuner();
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
        add(new JLabel("Gain Control"), "wrap");

        add(new JLabel("Master:"));
        add(getMasterGainCombo(), "wrap");

        add(new JLabel("Mixer:"));
        add(getMixerGainCombo(), "wrap");

        add(new JLabel("LNA:"));
        add(getLNAGainCombo(), "wrap");

        add(new JLabel("VGA:"));
        add(getVGAGainCombo(), "wrap");
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
            getMasterGainCombo().setEnabled(true);
            R8xEmbeddedTuner.MasterGain gain = getConfiguration().getMasterGain();
            getMasterGainCombo().setEnabled(true);
            getMasterGainCombo().setSelectedItem(gain);

            if(gain == R8xEmbeddedTuner.MasterGain.MANUAL)
            {
                getMixerGainCombo().setSelectedItem(getConfiguration().getMixerGain());
                getMixerGainCombo().setEnabled(true);

                getLNAGainCombo().setSelectedItem(getConfiguration().getLNAGain());
                getLNAGainCombo().setEnabled(true);

                getVGAGainCombo().setSelectedItem(getConfiguration().getVGAGain());
                getVGAGainCombo().setEnabled(true);
            }
            else
            {
                getMixerGainCombo().setEnabled(false);
                getMixerGainCombo().setSelectedItem(gain.getMixerGain());

                getLNAGainCombo().setEnabled(false);
                getLNAGainCombo().setSelectedItem(gain.getLNAGain());

                getVGAGainCombo().setEnabled(false);
                getVGAGainCombo().setSelectedItem(gain.getVGAGain());
            }
        }
        else
        {
            getBiasTButton().setEnabled(false);
            getBiasTButton().setSelected(false);
            getTunerInfoButton().setEnabled(false);
            getSampleRateCombo().setEnabled(false);
            getMasterGainCombo().setEnabled(false);
            getMixerGainCombo().setEnabled(false);
            getLNAGainCombo().setEnabled(false);
            getVGAGainCombo().setEnabled(false);
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    /**
     * Bias-T toggle button
     * @return
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
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(R8xTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private JComboBox getVGAGainCombo()
    {
        if(mVGAGainCombo == null)
        {
            mVGAGainCombo = new JComboBox<>(R8xEmbeddedTuner.VGAGain.values());
            if(mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mVGAGainCombo.setBackground(new java.awt.Color(43, 43, 43));
                mVGAGainCombo.setForeground(new java.awt.Color(187, 187, 187));
                // Set a darker gray for disabled state that's more visible in dark mode
                javax.swing.UIManager.put("ComboBox.disabledForeground", new java.awt.Color(120, 120, 120));
            }
            mVGAGainCombo.setEnabled(false);
            mVGAGainCombo.addActionListener(arg0 ->
            {
                try
                {
                    R8xEmbeddedTuner.VGAGain vgaGain = (R8xEmbeddedTuner.VGAGain) mVGAGainCombo.getSelectedItem();

                    if(vgaGain == null)
                    {
                        vgaGain = DEFAULT_GAIN.getVGAGain();
                    }

                    if(mVGAGainCombo.isEnabled())
                    {
                        getEmbeddedTuner().setVGAGain(vgaGain, true);
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(R8xTunerEditor.this, getLogPrefix() +
                            "couldn't apply the VGA gain setting - " + e.getLocalizedMessage());
                    mLog.error(getLogPrefix() + "couldn't apply VGA gain setting", e);
                }
            });
            mVGAGainCombo.setToolTipText("<html>VGA Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>");
        }

        return mVGAGainCombo;
    }

    private JComboBox getLNAGainCombo()
    {
        if(mLNAGainCombo == null)
        {
            mLNAGainCombo = new JComboBox<>(R8xEmbeddedTuner.LNAGain.values());
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
                try
                {
                    R8xEmbeddedTuner.LNAGain lnaGain = (R8xEmbeddedTuner.LNAGain) mLNAGainCombo.getSelectedItem();

                    if(lnaGain == null)
                    {
                        lnaGain = DEFAULT_GAIN.getLNAGain();
                    }

                    if(mLNAGainCombo.isEnabled())
                    {
                        getEmbeddedTuner().setLNAGain(lnaGain, true);
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(R8xTunerEditor.this, getLogPrefix() +
                            "couldn't apply the LNA gain setting - " + e.getLocalizedMessage());
                    mLog.error(getLogPrefix() + "couldn't apply LNA " + "gain setting - ", e);
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
                        JOptionPane.showMessageDialog(R8xTunerEditor.this,
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

    private JComboBox getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new JComboBox<>(R8xEmbeddedTuner.MixerGain.values());
            if(mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mMixerGainCombo.setBackground(new java.awt.Color(43, 43, 43));
                mMixerGainCombo.setForeground(new java.awt.Color(187, 187, 187));
                // Set a darker gray for disabled state that's more visible in dark mode
                javax.swing.UIManager.put("ComboBox.disabledForeground", new java.awt.Color(120, 120, 120));
            }
            mMixerGainCombo.setEnabled(false);
            mMixerGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        R8xEmbeddedTuner.MixerGain mixerGain = (R8xEmbeddedTuner.MixerGain) mMixerGainCombo.getSelectedItem();

                        if(mixerGain == null)
                        {
                            mixerGain = DEFAULT_GAIN.getMixerGain();
                        }

                        if(mMixerGainCombo.isEnabled())
                        {
                            getEmbeddedTuner().setMixerGain(mixerGain, true);
                        }

                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(R8xTunerEditor.this, getLogPrefix() +
                                "couldn't apply the mixer gain setting - " + e.getLocalizedMessage());

                        mLog.error(getLogPrefix() + "couldn't apply mixer gain setting - ", e);
                    }
                }
            });
            mMixerGainCombo.setToolTipText("<html>Mixer Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>");
        }

        return mMixerGainCombo;
    }

    private JComboBox getMasterGainCombo()
    {
        if(mMasterGainCombo == null)
        {
            mMasterGainCombo = new JComboBox<>(R8xEmbeddedTuner.MasterGain.values());
            mMasterGainCombo.setEnabled(false);
            mMasterGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        R8xEmbeddedTuner.MasterGain gain = (R8xEmbeddedTuner.MasterGain)getMasterGainCombo().getSelectedItem();
                        getEmbeddedTuner().setGain((R8xEmbeddedTuner.MasterGain)getMasterGainCombo().getSelectedItem(), true);

                        if(gain == R8xEmbeddedTuner.MasterGain.MANUAL)
                        {
                            getMixerGainCombo().setSelectedItem(gain.getMixerGain());
                            getMixerGainCombo().setEnabled(true);

                            getLNAGainCombo().setSelectedItem(gain.getLNAGain());
                            getLNAGainCombo().setEnabled(true);

                            getVGAGainCombo().setSelectedItem(gain.getVGAGain());
                            getVGAGainCombo().setEnabled(true);
                        }
                        else
                        {
                            getMixerGainCombo().setEnabled(false);
                            getMixerGainCombo().setSelectedItem(gain.getMixerGain());

                            getLNAGainCombo().setEnabled(false);
                            getLNAGainCombo().setSelectedItem(gain.getLNAGain());

                            getVGAGainCombo().setEnabled(false);
                            getVGAGainCombo().setSelectedItem(gain.getVGAGain());
                        }

                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(R8xTunerEditor.this, getLogPrefix() +
                                "couldn't apply the gain setting - " + e.getLocalizedMessage());
                        mLog.error(getLogPrefix() + "couldn't apply gain setting - ", e);
                    }
                }
            });
            mMasterGainCombo.setToolTipText("<html>Select <b>AUTOMATIC</b> for auto gain, <b>MANUAL</b> to enable<br> " +
                    "independent control of <i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>settings, or one of the " +
                    "individual gain settings for<br>semi-manual gain control</html>");
        }

        return mMasterGainCombo;
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
            R8xTunerConfiguration config = getConfiguration();
            config.setBiasT(getTuner().getController().isBiasT());
            config.setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            config.setSampleRate((SampleRate)getSampleRateCombo().getSelectedItem());
            R8xEmbeddedTuner.MasterGain gain = (R8xEmbeddedTuner.MasterGain)getMasterGainCombo().getSelectedItem();
            config.setMasterGain(gain);
            R8xEmbeddedTuner.MixerGain mixerGain = (R8xEmbeddedTuner.MixerGain)getMixerGainCombo().getSelectedItem();
            config.setMixerGain(mixerGain);
            R8xEmbeddedTuner.LNAGain lnaGain = (R8xEmbeddedTuner.LNAGain)getLNAGainCombo().getSelectedItem();
            config.setLNAGain(lnaGain);
            R8xEmbeddedTuner.VGAGain vgaGain = (R8xEmbeddedTuner.VGAGain)getVGAGainCombo().getSelectedItem();
            config.setVGAGain(vgaGain);
            saveConfiguration();
        }
    }
}