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
package io.github.dsheirer.source.tuner.rtl.r820t;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TEmbeddedTuner.R820TGain;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TEmbeddedTuner.R820TLNAGain;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TEmbeddedTuner.R820TMixerGain;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TEmbeddedTuner.R820TVGAGain;
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
import javax.swing.SpinnerNumberModel;
import javax.usb.UsbException;

public class R820TTunerEditor extends TunerEditor<RTL2832Tuner,R820TTunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(R820TTunerEditor.class);
    private static final long serialVersionUID = 1L;
    private static final R820TGain DEFAULT_GAIN = R820TGain.GAIN_279;
    private JButton mTunerInfoButton;
    private JComboBox<SampleRate> mSampleRateCombo;
    private JComboBox<R820TGain> mMasterGainCombo;
    private JComboBox<R820TMixerGain> mMixerGainCombo;
    private JComboBox<R820TLNAGain> mLNAGainCombo;
    private JComboBox<R820TVGAGain> mVGAGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public R820TTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    /**
     * Access the R820T embedded tuner
     * @return R820T tuner if there is a tuner, or null otherwise
     */
    private R820TEmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (R820TEmbeddedTuner)getTuner().getController().getEmbeddedTuner();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

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
            getTunerInfoButton().setEnabled(true);
            getSampleRateCombo().setEnabled(true);
            getSampleRateCombo().setSelectedItem(getConfiguration().getSampleRate());
            getMasterGainCombo().setEnabled(true);
            R820TGain gain = getConfiguration().getMasterGain();
            getMasterGainCombo().setEnabled(true);
            getMasterGainCombo().setSelectedItem(gain);

            if(gain == R820TGain.MANUAL)
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
     * Hyperlink button that provides tuner information
     */
    private JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(R820TTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private JComboBox getVGAGainCombo()
    {
        if(mVGAGainCombo == null)
        {
            mVGAGainCombo = new JComboBox<>(R820TVGAGain.values());
            mVGAGainCombo.setEnabled(false);
            mVGAGainCombo.addActionListener(arg0 ->
            {
                try
                {
                    R820TVGAGain vgaGain = (R820TVGAGain) mVGAGainCombo.getSelectedItem();

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
                    JOptionPane.showMessageDialog(R820TTunerEditor.this, "R820T Tuner Controller - " +
                            "couldn't apply the VGA gain setting - " + e.getLocalizedMessage());
                    mLog.error("R820T Tuner Controller - couldn't apply VGA gain setting", e);
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
            mLNAGainCombo = new JComboBox<>(R820TLNAGain.values());
            mLNAGainCombo.setEnabled(false);
            mLNAGainCombo.addActionListener(arg0 ->
            {
                try
                {
                    R820TLNAGain lnaGain = (R820TLNAGain) mLNAGainCombo.getSelectedItem();

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
                    JOptionPane.showMessageDialog(R820TTunerEditor.this, "R820T Tuner Controller - " +
                            "couldn't apply the LNA gain setting - " + e.getLocalizedMessage());
                    mLog.error("R820T Tuner Controller - couldn't apply LNA " + "gain setting - ", e);
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
                        save();
                    }
                    catch(SourceException | LibUsbException eSampleRate)
                    {
                        JOptionPane.showMessageDialog(R820TTunerEditor.this,
                                "R820T Tuner Controller - couldn't apply the sample rate setting [" +
                                        sampleRate.getLabel() + "] " + eSampleRate.getLocalizedMessage());

                        mLog.error("R820T Tuner Controller - couldn't apply sample rate setting [" + sampleRate.getLabel() +
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
            mMixerGainCombo = new JComboBox<>(R820TMixerGain.values());
            mMixerGainCombo.setEnabled(false);
            mMixerGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        R820TMixerGain mixerGain = (R820TMixerGain) mMixerGainCombo.getSelectedItem();

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
                        JOptionPane.showMessageDialog(R820TTunerEditor.this, "R820T Tuner Controller - " +
                                "couldn't apply the mixer gain setting - " + e.getLocalizedMessage());

                        mLog.error("R820T Tuner Controller - couldn't apply mixer gain setting - ", e);
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
            mMasterGainCombo = new JComboBox<>(R820TGain.values());
            mMasterGainCombo.setEnabled(false);
            mMasterGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        R820TGain gain = (R820TGain)getMasterGainCombo().getSelectedItem();
                        getEmbeddedTuner().setGain((R820TGain)getMasterGainCombo().getSelectedItem(), true);

                        if(gain == R820TGain.MANUAL)
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
                        JOptionPane.showMessageDialog(R820TTunerEditor.this, "R820T Tuner Controller - " +
                                "couldn't apply the gain setting - " + e.getLocalizedMessage());
                        mLog.error("R820T Tuner Controller - couldn't apply gain setting - ", e);
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
        sb.append("<html><h3>RTL-2832 with R820T Tuner</h3>");

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
            R820TTunerConfiguration config = getConfiguration();

            config.setFrequency(getFrequencyControl().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            config.setSampleRate((SampleRate)getSampleRateCombo().getSelectedItem());
            R820TGain gain = (R820TGain)getMasterGainCombo().getSelectedItem();
            config.setMasterGain(gain);
            R820TMixerGain mixerGain = (R820TMixerGain)getMixerGainCombo().getSelectedItem();
            config.setMixerGain(mixerGain);
            R820TLNAGain lnaGain = (R820TLNAGain)getLNAGainCombo().getSelectedItem();
            config.setLNAGain(lnaGain);
            R820TVGAGain vgaGain = (R820TVGAGain)getVGAGainCombo().getSelectedItem();
            config.setVGAGain(vgaGain);
            saveConfiguration();
        }
    }
}