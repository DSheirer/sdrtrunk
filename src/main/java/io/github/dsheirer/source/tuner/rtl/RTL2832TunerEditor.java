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
package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xTunerConfiguration;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTL-2832 tuner editor for all supported embedded tuner types (E4000, FC0012, FC0013, FC2580, R820T and R828D).
 *
 * Gain options are the gains that librtlsdr reports for the embedded tuner, plus the tuner's automatic gain control.
 * The E4000 additionally has a total IF gain that is distributed across its IF gain stages.
 */
public class RTL2832TunerEditor extends TunerEditor<RTL2832Tuner, RTL2832TunerConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(RTL2832TunerEditor.class);
    private static final long serialVersionUID = 1L;
    private JButton mTunerInfoButton;
    private JToggleButton mBiasTButton;
    private JComboBox<SampleRate> mSampleRateCombo;
    private JToggleButton mAgcToggleButton;
    private JComboBox<Gain> mGainCombo;
    private JLabel mIFGainLabel;
    private JComboBox<Integer> mIFGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public RTL2832TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        if(hasTuner())
        {
            return getTuner().getController().getMinimumFrequency();
        }

        return R8xTunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        if(hasTuner())
        {
            return getTuner().getController().getMaximumFrequency();
        }

        return R8xTunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private String getLogPrefix()
    {
        return (hasTuner() ? getTuner().getController().getTunerType().getLabel() : "RTL-2832") + " Tuner Controller - ";
    }

    /**
     * Indicates if the tuner has an E4000 embedded tuner, which supports IF gain control.
     */
    private boolean isE4K()
    {
        return hasTuner() && getTuner().getController().getTunerType() == TunerType.ELONICS_E4000;
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

        add(new JLabel("Gain:"));
        add(getGainCombo());
        add(getAgcToggleButton(), "wrap");

        add(getIFGainLabel());
        add(getIFGainCombo(), "wrap");
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

        DefaultComboBoxModel<Gain> gainModel = new DefaultComboBoxModel<>();

        if(hasTuner())
        {
            RTL2832TunerController controller = getTuner().getController();

            if(controller.hasGainControl())
            {
                for(int gain : controller.getTunerGains())
                {
                    gainModel.addElement(new Gain(gain));
                }
            }
        }

        getGainCombo().setModel(gainModel);

        boolean e4k = isE4K();
        getIFGainLabel().setVisible(e4k);
        getIFGainCombo().setVisible(e4k);

        if(hasTuner() && hasConfiguration())
        {
            RTL2832TunerConfiguration config = getConfiguration();
            getBiasTButton().setEnabled(true);
            getBiasTButton().setSelected(config.isBiasT());
            getTunerInfoButton().setEnabled(true);
            getSampleRateCombo().setEnabled(true);
            getSampleRateCombo().setSelectedItem(config.getSampleRate());
            RTL2832TunerController controller = getTuner().getController();
            //The FC2580 always uses its internal AGC and the FC0012 doesn't support AGC
            boolean agc = controller.hasGainControl() ? controller.hasTunerAGC() && config.isAGC() : true;
            getAgcToggleButton().setEnabled(controller.hasTunerAGC());
            getAgcToggleButton().setSelected(agc);
            getAgcToggleButton().setToolTipText(getAgcToolTip(controller));
            getGainCombo().setEnabled(!agc && gainModel.getSize() > 0);
            getGainCombo().setSelectedItem(new Gain(getTuner().getController().getClosestTunerGain(config.getGain())));

            if(e4k && config instanceof E4KTunerConfiguration e4kConfig)
            {
                getIFGainCombo().setEnabled(true);
                getIFGainCombo().setSelectedItem(e4kConfig.getIFGainDb());
            }
        }
        else
        {
            getBiasTButton().setEnabled(false);
            getBiasTButton().setSelected(false);
            getTunerInfoButton().setEnabled(false);
            getSampleRateCombo().setEnabled(false);
            getAgcToggleButton().setEnabled(false);
            getGainCombo().setEnabled(false);
            getIFGainCombo().setEnabled(false);
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    /**
     * Bias-T toggle button
     */
    private JToggleButton getBiasTButton()
    {
        if(mBiasTButton == null)
        {
            mBiasTButton = new JToggleButton("Bias-T");
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
     * Button that provides tuner information
     */
    private JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(RTL2832TunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    /**
     * Applies the current AGC and gain settings to the tuner and saves the configuration.
     */
    private void applyGain()
    {
        if(isLoading() || !hasTuner())
        {
            return;
        }

        boolean agc = getAgcToggleButton().isSelected();
        Gain gain = (Gain)getGainCombo().getSelectedItem();
        getGainCombo().setEnabled(!agc && getGainCombo().getItemCount() > 0);

        try
        {
            getTuner().getController().setGain(agc, gain != null ? gain.value() : getConfiguration().getGain());
            save();
        }
        catch(SourceException se)
        {
            JOptionPane.showMessageDialog(RTL2832TunerEditor.this, getLogPrefix() +
                    "couldn't apply the gain setting - " + se.getLocalizedMessage());
            mLog.error(getLogPrefix() + "couldn't apply gain setting", se);
        }
    }

    /**
     * Tool tip for the AGC button that explains when the tuner doesn't support switching AGC.
     */
    private static String getAgcToolTip(RTL2832TunerController controller)
    {
        if(!controller.hasGainControl())
        {
            return "The " + controller.getTunerType().getLabel() + " tuner always uses its automatic gain control (AGC)";
        }
        else if(!controller.hasTunerAGC())
        {
            return "The " + controller.getTunerType().getLabel() + " tuner doesn't support automatic gain control (AGC)";
        }

        return "Turn on the tuner's automatic gain control (AGC)";
    }

    private JToggleButton getAgcToggleButton()
    {
        if(mAgcToggleButton == null)
        {
            mAgcToggleButton = new JToggleButton("AGC");
            mAgcToggleButton.setEnabled(false);
            mAgcToggleButton.setToolTipText("Turn on the tuner's automatic gain control (AGC)");
            mAgcToggleButton.addActionListener(e -> applyGain());
        }

        return mAgcToggleButton;
    }

    private JComboBox<Gain> getGainCombo()
    {
        if(mGainCombo == null)
        {
            mGainCombo = new JComboBox<>();
            mGainCombo.setEnabled(false);
            mGainCombo.setToolTipText("Tuner gain.  Turn off AGC to enable adjustment.");
            mGainCombo.addActionListener(e -> applyGain());
        }

        return mGainCombo;
    }

    private JLabel getIFGainLabel()
    {
        if(mIFGainLabel == null)
        {
            mIFGainLabel = new JLabel("IF Gain (dB):");
        }

        return mIFGainLabel;
    }

    private JComboBox<Integer> getIFGainCombo()
    {
        if(mIFGainCombo == null)
        {
            mIFGainCombo = new JComboBox<>();

            for(int gain = RTL2832TunerController.E4K_IF_GAIN_MINIMUM; gain <= RTL2832TunerController.E4K_IF_GAIN_MAXIMUM; gain++)
            {
                mIFGainCombo.addItem(gain);
            }

            mIFGainCombo.setEnabled(false);
            mIFGainCombo.setToolTipText("E4000 IF gain, distributed across the six IF gain stages");
            mIFGainCombo.addActionListener(e ->
            {
                Integer gain = (Integer)mIFGainCombo.getSelectedItem();

                if(!isLoading() && hasTuner() && gain != null)
                {
                    try
                    {
                        getTuner().getController().setE4KIFGain(gain);
                        save();
                    }
                    catch(SourceException se)
                    {
                        JOptionPane.showMessageDialog(RTL2832TunerEditor.this, getLogPrefix() +
                                "couldn't apply the IF gain setting - " + se.getLocalizedMessage());
                        mLog.error(getLogPrefix() + "couldn't apply IF gain setting", se);
                    }
                }
            });
        }

        return mIFGainCombo;
    }

    private JComboBox<SampleRate> getSampleRateCombo()
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
                    catch(SourceException se)
                    {
                        JOptionPane.showMessageDialog(RTL2832TunerEditor.this,
                                getLogPrefix() + "couldn't apply the sample rate setting [" +
                                        sampleRate.getLabel() + "] " + se.getLocalizedMessage());

                        mLog.error(getLogPrefix() + "couldn't apply sample rate setting [" + sampleRate.getLabel() +
                                "]", se);
                    }
                }
            });
        }

        return mSampleRateCombo;
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
        sb.append("<html><h3>RTL-2832 with ").append(getTuner().getController().getTunerType().getLabel())
                .append(" Tuner</h3>");

        if(descriptor == null)
        {
            sb.append("No EEPROM Descriptor Available");
        }
        else
        {
            sb.append("<b>USB ID: </b>").append(descriptor.getVendorID()).append(":")
                    .append(descriptor.getProductID()).append("<br>");
            sb.append("<b>Vendor: </b>").append(descriptor.getVendorLabel()).append("<br>");
            sb.append("<b>Product: </b>").append(descriptor.getProductLabel()).append("<br>");
            sb.append("<b>Serial: </b>").append(descriptor.getSerial()).append("<br>");
            sb.append("<b>IR Enabled: </b>").append(descriptor.irEnabled()).append("<br>");
            sb.append("<b>Remote Wake: </b>").append(descriptor.remoteWakeupEnabled()).append("<br>");
        }

        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RTL2832TunerConfiguration config = getConfiguration();
            config.setBiasT(getTuner().getController().isBiasT());
            config.setFrequency(getFrequencyControl().getFrequency());
            config.setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            config.setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            config.setSampleRate((SampleRate)getSampleRateCombo().getSelectedItem());
            config.setAGC(getAgcToggleButton().isSelected());

            if(getGainCombo().getSelectedItem() instanceof Gain gain)
            {
                config.setGain(gain.value());
            }

            if(config instanceof E4KTunerConfiguration e4k && getIFGainCombo().getSelectedItem() instanceof Integer ifGain)
            {
                e4k.setIFGainDb(ifGain);
            }

            saveConfiguration();
        }
    }

    /**
     * Tuner gain combo box entry.
     * @param value in tenths of a dB
     */
    private record Gain(int value)
    {
        @Override
        public String toString()
        {
            return String.format("%.1f dB", value / 10.0);
        }
    }
}
