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
package io.github.dsheirer.source.tuner.airspy;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController.Gain;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController.GainMode;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Airspy tuner editor/controller
 */
public class AirspyTunerEditor extends TunerEditor<AirspyTuner, AirspyTunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(AirspyTunerEditor.class);
    private JButton mTunerInfoButton;
    private JComboBox<AirspySampleRate> mSampleRateCombo;
    private JComboBox<GainMode> mGainModeCombo;
    private JSlider mMasterGainSlider;
    private JLabel mMasterGainLabel;
    private JLabel mMasterGainValueLabel;
    private JSlider mIFGainSlider;
    private JLabel mIFGainLabel;
    private JLabel mIFGainValueLabel;
    private JSlider mLNAGainSlider;
    private JLabel mLNAGainValueLabel;
    private JSlider mMixerGainSlider;
    private JLabel mMixerGainValueLabel;
    private JCheckBox mLNAAGCCheckBox;
    private JCheckBox mMixerAGCCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable Airspy tuner and controller
     */
    public AirspyTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
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
        getSampleRateCombo().setEnabled(hasTuner() && !getTuner().getTunerController().isLockedSampleRate());
        getTunerInfoButton().setEnabled(hasTuner());
        updateGainComponents((hasTuner() && hasConfiguration()) ? getConfiguration().getGain() : null);

        if(hasTuner())
        {
            List<AirspySampleRate> rates = getTuner().getController().getSampleRates();
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>(rates.toArray(new AirspySampleRate[rates.size()])));

            if(hasConfiguration())
            {
                AirspySampleRate sampleRate = getSampleRate(getConfiguration().getSampleRate());
                getSampleRateCombo().setSelectedItem(sampleRate);
            }
        }
        else
        {
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>());
        }

        setLoading(false);
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

        add(new JLabel("Mode:"));
        add(getGainModeCombo(), "wrap");

        add(getMasterGainLabel());
        add(getMasterGainSlider());
        add(getMasterGainValueLabel());

        add(getIFGainLabel());
        add(getIFGainSlider());
        add(getIFGainValueLabel());

        add(getMixerAGCCheckBox());
        add(getMixerGainSlider());
        add(getMixerGainValueLabel());

        add(getLNAAGCCheckBox());
        add(getLNAGainSlider());
        add(getLNAGainValueLabel());
    }

    private JCheckBox getLNAAGCCheckBox()
    {
        if(mLNAAGCCheckBox == null)
        {
            mLNAAGCCheckBox = new JCheckBox("AGC LNA:");
            mLNAAGCCheckBox.setEnabled(false);
            mLNAAGCCheckBox.addActionListener(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setLNAAGC(getLNAAGCCheckBox().isSelected());
                        getLNAGainSlider().setEnabled(!getLNAAGCCheckBox().isSelected());
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting LNA AGC Enabled");
                    }
                }
            });
        }

        return mLNAAGCCheckBox;
    }

    private JCheckBox getMixerAGCCheckBox()
    {
        if(mMixerAGCCheckBox == null)
        {
            mMixerAGCCheckBox = new JCheckBox("AGC Mixer:");
            mMixerAGCCheckBox.setEnabled(false);
            mMixerAGCCheckBox.addActionListener(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setMixerAGC(getMixerAGCCheckBox().isSelected());
                        getMixerGainSlider().setEnabled(!getMixerAGCCheckBox().isSelected());
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting Mixer AGC Enabled");
                    }
                }
            });
        }

        return mMixerAGCCheckBox;
    }

    private JLabel getLNAGainValueLabel()
    {
        if(mLNAGainValueLabel == null)
        {
            mLNAGainValueLabel = new JLabel("0");
            mLNAGainValueLabel.setEnabled(false);
        }

        return mLNAGainValueLabel;
    }

    private JSlider getLNAGainSlider()
    {
        if(mLNAGainSlider == null)
        {
            mLNAGainSlider = new JSlider(JSlider.HORIZONTAL, AirspyTunerController.LNA_GAIN_MIN,
                    AirspyTunerController.LNA_GAIN_MAX, AirspyTunerController.LNA_GAIN_MIN);
            mLNAGainSlider.setEnabled(false);
            mLNAGainSlider.setMajorTickSpacing(1);
            mLNAGainSlider.setPaintTicks(true);
            mLNAGainSlider.addChangeListener(event ->
            {
                int gain = mLNAGainSlider.getValue();

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setLNAGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set airspy LNA gain to:" + gain, e);
                        JOptionPane.showMessageDialog(mLNAGainSlider, "Couldn't set LNA gain value to " + gain);
                    }
                }

                getLNAGainValueLabel().setText(String.valueOf(gain));
            });
        }

        return mLNAGainSlider;
    }

    private JLabel getMixerGainValueLabel()
    {
        if(mMixerGainValueLabel == null)
        {
            mMixerGainValueLabel = new JLabel("0");
            mMixerGainValueLabel.setEnabled(false);
        }

        return mMixerGainValueLabel;
    }

    private JSlider getMixerGainSlider()
    {
        if(mMixerGainSlider == null)
        {
            mMixerGainSlider = new JSlider(JSlider.HORIZONTAL, AirspyTunerController.MIXER_GAIN_MIN,
                    AirspyTunerController.MIXER_GAIN_MAX, AirspyTunerController.MIXER_GAIN_MIN);
            mMixerGainSlider.setEnabled(false);
            mMixerGainSlider.setMajorTickSpacing(1);
            mMixerGainSlider.setPaintTicks(true);
            mMixerGainSlider.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent event)
                {
                    int gain = mMixerGainSlider.getValue();

                    if(hasTuner() && !isLoading())
                    {
                        try
                        {
                            getTuner().getController().setMixerGain(gain);
                            save();
                        }
                        catch(Exception e)
                        {
                            mLog.error("Couldn't set airspy Mixer gain to:" + gain, e);
                            JOptionPane.showMessageDialog(mMixerGainSlider, "Couldn't set Mixer gain value to " + gain);
                        }
                    }

                    getMixerGainValueLabel().setText(String.valueOf(gain));
                }
            });
        }

        return mMixerGainSlider;
    }

    private JLabel getIFGainLabel()
    {
        if(mIFGainLabel == null)
        {
            mIFGainLabel = new JLabel("IF:");
        }

        return mIFGainLabel;
    }

    private JLabel getIFGainValueLabel()
    {
        if(mIFGainValueLabel == null)
        {
            mIFGainValueLabel = new JLabel("0");
            mIFGainValueLabel.setEnabled(false);
        }

        return mIFGainValueLabel;
    }

    private JSlider getIFGainSlider()
    {
        if(mIFGainSlider == null)
        {
            mIFGainSlider = new JSlider(JSlider.HORIZONTAL, AirspyTunerController.IF_GAIN_MIN,
                    AirspyTunerController.IF_GAIN_MAX, AirspyTunerController.IF_GAIN_MIN);
            mIFGainSlider.setEnabled(false);
            mIFGainSlider.setMajorTickSpacing(1);
            mIFGainSlider.setPaintTicks(true);
            mIFGainSlider.addChangeListener(event ->
            {
                int gain = mIFGainSlider.getValue();

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setIFGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set airspy IF gain to:" + gain, e);
                        JOptionPane.showMessageDialog(mIFGainSlider, "Couldn't set IF gain value to " + gain);
                    }
                }

                getIFGainValueLabel().setText(String.valueOf(gain));
            });
        }

        return mIFGainSlider;
    }

    private JLabel getMasterGainLabel()
    {
        if(mMasterGainLabel == null)
        {
            mMasterGainLabel = new JLabel("Master:");
        }

        return mMasterGainLabel;
    }

    private JLabel getMasterGainValueLabel()
    {
        if(mMasterGainValueLabel == null)
        {
            mMasterGainValueLabel = new JLabel("0");
            mMasterGainValueLabel.setEnabled(false);
        }

        return mMasterGainValueLabel;
    }

    private JSlider getMasterGainSlider()
    {
        if(mMasterGainSlider == null)
        {
            mMasterGainSlider = new JSlider(JSlider.HORIZONTAL, AirspyTunerController.GAIN_MIN,
                    AirspyTunerController.GAIN_MAX, AirspyTunerController.GAIN_MIN);
            mMasterGainSlider.setEnabled(false);
            mMasterGainSlider.setMajorTickSpacing(1);
            mMasterGainSlider.setPaintTicks(true);

            mMasterGainSlider.addChangeListener(event ->
            {
                GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
                int value = mMasterGainSlider.getValue();
                Gain gain = Gain.getGain(mode, value);

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set airspy gain to:" + gain.name(), e);
                        JOptionPane.showMessageDialog(mMasterGainSlider, "Couldn't set gain value to " +
                                gain.getValue());
                    }
                }

                getMasterGainValueLabel().setText(String.valueOf(value));
            });
        }

        return mMasterGainSlider;
    }

    private JComboBox<GainMode> getGainModeCombo()
    {
        if(mGainModeCombo == null)
        {
            mGainModeCombo = new JComboBox<>(GainMode.values());
            mGainModeCombo.setEnabled(false);
            mGainModeCombo.addActionListener(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
                    int value = getMasterGainSlider().getValue();
                    Gain gain = Gain.getGain(mode, value);
                    updateGainComponents(gain);
                    save();
                }
            });
        }

        return mGainModeCombo;
    }

    private JComboBox<AirspySampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if(hasTuner() && !isLoading())
                    {
                        AirspySampleRate rate = (AirspySampleRate)mSampleRateCombo.getSelectedItem();

                        try
                        {
                            getTuner().getController().setSampleRate(rate);
                            save();
                        }
                        catch(Exception e1)
                        {
                            JOptionPane.showMessageDialog(AirspyTunerEditor.this,
                                    "Couldn't set sample rate to " + rate.getLabel());
                            mLog.error("Error setting airspy sample rate", e1);
                        }
                    }
                }
            });
        }

        return mSampleRateCombo;
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
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(AirspyTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    /**
     * Updates the enabled state of each of the gain controls according to the
     * specified gain mode.  The master gain control is enabled for linearity
     * and sensitivity and the individual gain controls are disabled, and
     * vice-versa for custom mode.
     */
    private void updateGainComponents(Gain gain)
    {
        if(hasTuner() && gain != null)
        {
            boolean isCustom = gain.equals(Gain.CUSTOM);

            getGainModeCombo().setEnabled(true);
            getGainModeCombo().setSelectedItem(gain.getGainMode());
            getMasterGainLabel().setEnabled(!isCustom);
            getMasterGainSlider().setEnabled(!isCustom);
            getMasterGainSlider().setValue(gain.getValue());
            getMasterGainValueLabel().setEnabled(!isCustom);
            getIFGainLabel().setEnabled(isCustom);
            getIFGainSlider().setEnabled(isCustom);
            getIFGainValueLabel().setEnabled(isCustom);
            getLNAAGCCheckBox().setEnabled(isCustom);
            getLNAGainSlider().setEnabled(isCustom && !getConfiguration().isLNAAGC());
            getLNAGainValueLabel().setEnabled(isCustom);
            getMixerAGCCheckBox().setEnabled(isCustom);
            getMixerGainSlider().setEnabled(isCustom && !getConfiguration().isMixerAGC());
            getMixerGainValueLabel().setEnabled(isCustom);
            if(isCustom)
            {
                getIFGainSlider().setValue(getConfiguration().getIFGain());
                getLNAGainSlider().setValue(getConfiguration().getLNAGain());
                getMixerGainSlider().setValue(getConfiguration().getMixerGain());
                getMixerAGCCheckBox().setSelected(getConfiguration().isMixerAGC());
                getLNAAGCCheckBox().setSelected(getConfiguration().isLNAAGC());
            }
            else
            {
                getIFGainSlider().setValue(0);
                getLNAGainSlider().setValue(0);
                getMixerGainSlider().setValue(0);
                getMixerAGCCheckBox().setSelected(false);
                getLNAAGCCheckBox().setSelected(false);
            }
        }
        else
        {
            getGainModeCombo().setEnabled(false);
            getMasterGainLabel().setEnabled(false);
            getMasterGainSlider().setEnabled(false);
            getMasterGainSlider().setValue(0);
            getMasterGainValueLabel().setEnabled(false);
            getIFGainLabel().setEnabled(false);
            getIFGainSlider().setEnabled(false);
            getIFGainSlider().setValue(0);
            getIFGainValueLabel().setEnabled(false);
            getLNAAGCCheckBox().setEnabled(false);
            getLNAAGCCheckBox().setSelected(false);
            getLNAGainSlider().setEnabled(false);
            getLNAGainSlider().setValue(0);
            getLNAGainValueLabel().setEnabled(false);
            getMixerAGCCheckBox().setEnabled(false);
            getMixerAGCCheckBox().setSelected(false);
            getMixerGainSlider().setEnabled(false);
            getMixerGainSlider().setValue(0);
            getMixerGainValueLabel().setEnabled(false);
        }
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            double value = ((SpinnerNumberModel) getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            getConfiguration().setSampleRate(((AirspySampleRate)getSampleRateCombo().getSelectedItem()).getRate());
            Gain gain = Gain.getGain((GainMode)mGainModeCombo.getSelectedItem(), getMasterGainSlider().getValue());
            getConfiguration().setGain(gain);
            getConfiguration().setIFGain(getIFGainSlider().getValue());
            getConfiguration().setMixerGain(getMixerGainSlider().getValue());
            getConfiguration().setLNAGain(getLNAGainSlider().getValue());
            getConfiguration().setMixerAGC(getMixerAGCCheckBox().isSelected());
            getConfiguration().setLNAAGC(getLNAAGCCheckBox().isSelected());
            saveConfiguration();
        }
    }

    /**
     * Finds the airspy sample rate entry that matches the value.
     * @param value in Hertz
     * @return the matching rate entry or null.
     */
    private AirspySampleRate getSampleRate(int value)
    {
        if(hasTuner())
        {
            List<AirspySampleRate> rates = getTuner().getController().getSampleRates();

            for(AirspySampleRate rate : rates)
            {
                if(rate.getRate() == value)
                {
                    return rate;
                }
            }

            if(rates.size() > 0)
            {
                return rates.get(0);
            }
        }

        return null;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getController().isLockedSampleRate())
        {
            getSampleRateCombo().setToolTipText("Sample Rate is locked.  Disable decoding channels to unlock.");
        }
        else
        {
            getSampleRateCombo().setToolTipText("Select a sample rate for the tuner");
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
        if(getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
        {
            return getDiscoveredTuner().getErrorMessage();
        }

        if(hasTuner())
        {
            StringBuilder sb = new StringBuilder();

            AirspyDeviceInformation info = getTuner().getController().getDeviceInfo();

            sb.append("<html><h3>Airspy Tuner</h3>");
            sb.append("<b>Serial: </b>");
            sb.append(info.getSerialNumber());
            sb.append("<br>");

            sb.append("<b>Firmware: </b>");
            String[] firmware = info.getVersion().split(" ");
            sb.append(firmware.length > 1 ? firmware[0] : info.getVersion());
            sb.append("<br>");

            sb.append("<b>Part: </b>");
            sb.append(info.getPartNumber());
            sb.append("<br>");

            sb.append("<b>Board ID: </b>");
            sb.append(info.getBoardID().getLabel());
            sb.append("<br>");

            return sb.toString();
        }

        return null;
    }
}