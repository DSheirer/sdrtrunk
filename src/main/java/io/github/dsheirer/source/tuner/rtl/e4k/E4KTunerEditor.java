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
package io.github.dsheirer.source.tuner.rtl.e4k;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KLNAGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KMixerGain;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

/**
 * E4000 tuner editor
 */
public class E4KTunerEditor extends TunerEditor<RTL2832Tuner, E4KTunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(E4KTunerEditor.class);
    private static final long serialVersionUID = 1L;
    private JButton mTunerInfoButton;
    private JComboBox<SampleRate> mSampleRateCombo;
    private JComboBox<E4KGain> mMasterGainCombo;
    private JComboBox<E4KMixerGain> mMixerGainCombo;
    private JComboBox<E4KLNAGain> mLNAGainCombo;
    private JComboBox<E4KEmbeddedTuner.IFGain> mIfGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public E4KTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    /**
     * Access to the E4000 embedded tuner
     * @return E4000 tuner if there is a tuner, or null otherwise
     */
    public E4KEmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (E4KEmbeddedTuner)getTuner().getController().getEmbeddedTuner();
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
        add(getFrequencyPanel(), "span 2");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Mixer/LNA Gain Control"), "wrap");

        add(new JLabel("Master:"));
        add(getMasterGainCombo(), "wrap");

        add(new JLabel("Mixer:"));
        add(getMixerGainCombo(), "wrap");

        add(new JLabel("LNA:"));
        add(getLNAGainCombo(), "wrap");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("IF Gain:"));
        add(getIfGainCombo(), "wrap");
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
        getTunerStatusLabel().setText(getDiscoveredTuner().getTunerStatus().toString());
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        if(hasTuner())
        {
            getTunerInfoButton().setEnabled(true);
            getSampleRateCombo().setEnabled(true);
            getSampleRateCombo().setSelectedItem(getConfiguration().getSampleRate());
            getMasterGainCombo().setEnabled(true);
            getIfGainCombo().setEnabled(true);
            getIfGainCombo().setSelectedItem(getConfiguration().getIFGain());

            E4KGain gain = getConfiguration().getMasterGain();
            getMasterGainCombo().setEnabled(true);
            getMasterGainCombo().setSelectedItem(gain);

            if(gain == E4KGain.MANUAL)
            {
                getMixerGainCombo().setSelectedItem(getConfiguration().getMixerGain());
                getMixerGainCombo().setEnabled(true);

                getLNAGainCombo().setSelectedItem(getConfiguration().getLNAGain());
                getLNAGainCombo().setEnabled(true);
            }
            else
            {
                getMixerGainCombo().setEnabled(false);
                getMixerGainCombo().setSelectedItem(gain.getMixerGain());

                getLNAGainCombo().setEnabled(false);
                getLNAGainCombo().setSelectedItem(gain.getLNAGain());
            }
        }
        else
        {
            getTunerInfoButton().setEnabled(false);
            getSampleRateCombo().setEnabled(false);
            getMasterGainCombo().setEnabled(false);
            getMixerGainCombo().setEnabled(false);
            getLNAGainCombo().setEnabled(false);
            getIfGainCombo().setEnabled(false);
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    private JComboBox getIfGainCombo()
    {
        if(mIfGainCombo == null)
        {
            mIfGainCombo = new JComboBox<>(E4KEmbeddedTuner.IFGain.values());
            mIfGainCombo.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if(!isLoading())
                    {
                        try
                        {
                            E4KEmbeddedTuner.IFGain selected = (E4KEmbeddedTuner.IFGain) getIfGainCombo().getSelectedItem();
                            getEmbeddedTuner().setIFGain(selected, true);
                            save();
                        }
                        catch(LibUsbException lue)
                        {
                            JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4000 Tuner Controller - "
                                    + "couldn't apply the IF setting - " + lue.getLocalizedMessage());
                            mLog.error("E4000 Tuner Controller - couldn't apply IF gain setting", e);
                        }
                    }
                }
            });
            mIfGainCombo.setToolTipText("Linear IF Gain");
            mIfGainCombo.setEnabled(true);
        }

        return mIfGainCombo;
    }

    private JComboBox getLNAGainCombo()
    {
        if(mLNAGainCombo == null)
        {
            mLNAGainCombo = new JComboBox<>(E4KLNAGain.values());
            mLNAGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        E4KLNAGain lnaGain = (E4KLNAGain) mLNAGainCombo.getSelectedItem();
                        getEmbeddedTuner().setLNAGain(lnaGain, true);
                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4000 Tuner Controller - "
                                + "couldn't apply the LNA gain setting - " + e.getLocalizedMessage());
                        mLog.error("E4000 Tuner Controller - couldn't apply LNA gain setting - ", e);
                    }
                }
            });
            mLNAGainCombo.setToolTipText("<html>LNA Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>");
            mLNAGainCombo.setEnabled(false);
        }

        return mLNAGainCombo;
    }

    private JComboBox getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new JComboBox<>(E4KMixerGain.values());
            mMixerGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        E4KMixerGain mixerGain = (E4KMixerGain) mMixerGainCombo.getSelectedItem();
                        getEmbeddedTuner().setMixerGain(mixerGain, true);
                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4000 Tuner Controller - "
                                + "couldn't apply the mixer gain setting - " + e.getLocalizedMessage());
                        mLog.error("E4000 Tuner Controller - couldn't apply mixer gain setting", e);
                    }
                }
            });
            mMixerGainCombo.setToolTipText("<html>Mixer Gain.  Set master gain to <b>MASTER</b> to enable adjustment</html>");
            mMixerGainCombo.setEnabled(false);
        }

        return mMixerGainCombo;
    }

    private JComboBox getMasterGainCombo()
    {
        if(mMasterGainCombo == null)
        {
            mMasterGainCombo = new JComboBox<>(E4KGain.values());
            mMasterGainCombo.setEnabled(false);
            mMasterGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        E4KGain gain = (E4KGain) mMasterGainCombo.getSelectedItem();
                        getEmbeddedTuner().setGain((E4KGain) mMasterGainCombo.getSelectedItem(), true);
                        if(gain == E4KGain.MANUAL)
                        {
                            getMixerGainCombo().setSelectedItem(getEmbeddedTuner().getMixerGain(true));
                            getMixerGainCombo().setEnabled(true);
                            getLNAGainCombo().setSelectedItem(getEmbeddedTuner().getLNAGain(true));
                            getLNAGainCombo().setEnabled(true);
                        }
                        else
                        {
                            getMixerGainCombo().setEnabled(false);
                            getMixerGainCombo().setSelectedItem(gain.getMixerGain());
                            getLNAGainCombo().setEnabled(false);
                            getLNAGainCombo().setSelectedItem(gain.getLNAGain());
                        }

                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4000 Tuner Controller - "
                                + "couldn't apply the gain setting - " + e.getLocalizedMessage());
                        mLog.error("E4000 Tuner Controller - couldn't apply gain setting", e);
                    }
                }
            });

            mMasterGainCombo.setToolTipText("<html>Select <b>AUTOMATIC</b> for auto gain, <b>MANUAL</b> to enable<br> " +
                    "independent control of <i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>settings, or one of the " +
                    "individual gain settings for<br>semi-manual gain control</html>");
        }

        return mMasterGainCombo;
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
                        JOptionPane.showMessageDialog(E4KTunerEditor.this,
                                "E4000 Tuner Controller - couldn't apply the sample rate setting [" +
                                        sampleRate.getLabel() + "] " + eSampleRate.getLocalizedMessage());

                        mLog.error("E4000 Tuner Controller - couldn't apply sample rate setting [" +
                                sampleRate.getLabel() + "]", eSampleRate);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    private JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Tuner Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(E4KTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
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

        sb.append("<html><h3>RTL-2832 with E4000 Tuner</h3>");

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
            E4KTunerConfiguration config = getConfiguration();
            config.setFrequency(getFrequencyControl().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            config.setSampleRate((SampleRate)getSampleRateCombo().getSelectedItem());
            config.setMasterGain((E4KGain)getMasterGainCombo().getSelectedItem());
            config.setMixerGain((E4KMixerGain)getMixerGainCombo().getSelectedItem());
            config.setLNAGain((E4KLNAGain)getLNAGainCombo().getSelectedItem());
            config.setIFGain((E4KEmbeddedTuner.IFGain)getIfGainCombo().getSelectedItem());
            saveConfiguration();
        }
    }
}