/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.airspy;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController.Gain;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController.GainMode;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.List;

public class AirspyTunerEditor extends TunerConfigurationEditor
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(AirspyTunerEditor.class);

    private JTextField mConfigurationName;
    private JButton mTunerInfo;

    private JComboBox<AirspySampleRate> mSampleRateCombo;
    private JSpinner mFrequencyCorrection;

    private JComboBox<GainMode> mGainModeCombo;

    private JSlider mMasterGain;
    private JLabel mMasterGainValueLabel;

    private JSlider mIFGain;
    private JLabel mIFGainValueLabel;

    private JSlider mLNAGain;
    private JLabel mLNAGainValueLabel;

    private JSlider mMixerGain;
    private JLabel mMixerGainValueLabel;

    private JCheckBox mLNAAGC;
    private JCheckBox mMixerAGC;

    private FrequencyCorrectionChangeListener mFrequencyCorrectionChangeListener = new FrequencyCorrectionChangeListener();
    private AirspyTunerController mController;
    private boolean mLoading;

    public AirspyTunerEditor(TunerConfigurationModel tunerConfigurationModel,
                             AirspyTuner tuner)
    {
        super(tunerConfigurationModel);
        mController = tuner.getController();

        init();
    }

    private AirspyTunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (AirspyTunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][grow]"));

        add(new JLabel("Airspy Tuner Configuration"), "span,align center");

        mConfigurationName = new JTextField();
        mConfigurationName.setEnabled(false);
        mConfigurationName.addFocusListener(new FocusListener()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                save();
            }

            @Override
            public void focusGained(FocusEvent e)
            {
            }
        });

        add(new JLabel("Name:"));
        add(mConfigurationName, "span 2");

        mTunerInfo = new JButton("Tuner Info");
        mTunerInfo.setEnabled(false);
        mTunerInfo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JOptionPane.showMessageDialog(AirspyTunerEditor.this, getTunerInfo(),
                    "Tuner Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(mTunerInfo);

        /**
         * Sample Rate
         */
        add(new JLabel("Sample Rate:"));

        List<AirspySampleRate> rates = mController.getSampleRates();

        mSampleRateCombo = new JComboBox<AirspySampleRate>(
            new DefaultComboBoxModel<AirspySampleRate>(rates.toArray(
                    new AirspySampleRate[0])));
        mSampleRateCombo.setEnabled(false);
        mSampleRateCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                AirspySampleRate rate = (AirspySampleRate)mSampleRateCombo.getSelectedItem();

                try
                {
                    mController.setSampleRate(rate);
                    save();
                }
                catch(LibUsbException | UsbException | SourceException e1)
                {
                    JOptionPane.showMessageDialog(AirspyTunerEditor.this,
                        "Couldn't set sample rate to " + rate.getLabel());

                    mLog.error("Error setting airspy sample rate", e1);
                }
            }
        });

        add(mSampleRateCombo);

        /**
         * Frequency Correction
         */
        SpinnerModel model =
            new SpinnerNumberModel(0.0,   //initial value
                -1000.0,   //min
                1000.0,   //max
                0.1); //step

        mFrequencyCorrection = new JSpinner(model);
        mFrequencyCorrection.setEnabled(false);

        JSpinner.NumberEditor editor =
            (JSpinner.NumberEditor)mFrequencyCorrection.getEditor();

        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(1);
        editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);

        mFrequencyCorrection.addChangeListener(mFrequencyCorrectionChangeListener);

        add(new JLabel("PPM:"));
        add(mFrequencyCorrection);

        add(new JSeparator(), "span,growx,push");

        /**
         * Gain Mode
         */
        add(new JLabel("Gain Mode:"));
        mGainModeCombo = new JComboBox<AirspyTunerController.GainMode>(GainMode.values());
        mGainModeCombo.setEnabled(false);
        mGainModeCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();

                updateGainComponents(mode);

                if(hasItem())
                {
                    AirspyTunerConfiguration airspy = getConfiguration();

                    Gain gain = airspy.getGain();

                    if(mode == GainMode.CUSTOM)
                    {
                        mIFGain.setValue(airspy.getIFGain());
                        mMixerGain.setValue(airspy.getMixerGain());
                        mMixerAGC.setSelected(airspy.isMixerAGC());
                        mLNAGain.setValue(airspy.getLNAGain());
                        mLNAAGC.setSelected(airspy.isLNAAGC());
                    }
                    else
                    {
                        mMixerAGC.setSelected(false);
                        mLNAAGC.setSelected(false);
                        mMasterGain.setValue(gain.getValue());
                        mIFGain.setValue(gain.getIF());
                        mMixerGain.setValue(gain.getMixer());
                        mLNAGain.setValue(gain.getLNA());
                    }
                }

                save();
            }
        });

        add(mGainModeCombo, "wrap");

        /**
         * Gain
         */
        add(new JLabel("Master:"));

        mMasterGain = new JSlider(JSlider.HORIZONTAL,
            AirspyTunerController.GAIN_MIN,
            AirspyTunerController.GAIN_MAX,
            AirspyTunerController.GAIN_MIN);
        mMasterGain.setMajorTickSpacing(1);
        mMasterGain.setPaintTicks(true);

        mMasterGain.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
                int value = mMasterGain.getValue();
                Gain gain = Gain.getGain(mode, value);

                try
                {
                    mController.setGain(gain);
                    save();
                    mMasterGainValueLabel.setText(String.valueOf(gain.getValue()));
                }
                catch(Exception e)
                {
                    mLog.error("Couldn't set airspy gain to:" + gain.name(), e);
                    JOptionPane.showMessageDialog(mMasterGain, "Couldn't set gain value to " +
                        gain.getValue());
                }
            }
        });

        add(mMasterGain, "span 2");

        mMasterGainValueLabel = new JLabel("0");
        add(mMasterGainValueLabel);

        add(new JLabel("IF:"));

        mIFGain = new JSlider(JSlider.HORIZONTAL,
            AirspyTunerController.IF_GAIN_MIN,
            AirspyTunerController.IF_GAIN_MAX,
            AirspyTunerController.IF_GAIN_MIN);

        mIFGain.setMajorTickSpacing(1);
        mIFGain.setPaintTicks(true);

        mIFGain.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                int gain = mIFGain.getValue();

                try
                {
                    mController.setIFGain(gain);
                    save();
                    mIFGainValueLabel.setText(String.valueOf(gain));
                }
                catch(Exception e)
                {
                    mLog.error("Couldn't set airspy IF gain to:" + gain, e);

                    JOptionPane.showMessageDialog(mIFGain, "Couldn't set IF gain value to " + gain);
                }
            }
        });

        add(mIFGain, "span 2");

        mIFGainValueLabel = new JLabel("0");
        add(mIFGainValueLabel);

        /**
         *  Mixer Gain
         */
        add(new JLabel("Mixer:"));

        mMixerGain = new JSlider(JSlider.HORIZONTAL,
            AirspyTunerController.MIXER_GAIN_MIN,
            AirspyTunerController.MIXER_GAIN_MAX,
            AirspyTunerController.MIXER_GAIN_MIN);

        mMixerGain.setMajorTickSpacing(1);
        mMixerGain.setPaintTicks(true);

        mMixerGain.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                int gain = mMixerGain.getValue();

                try
                {
                    mController.setMixerGain(gain);
                    save();
                    mMixerGainValueLabel.setText(String.valueOf(gain));
                }
                catch(Exception e)
                {
                    mLog.error("Couldn't set airspy Mixer gain to:" + gain, e);
                    JOptionPane.showMessageDialog(mIFGain, "Couldn't set Mixer gain value to " + gain);
                }
            }
        });

        add(mMixerGain, "span 2");

        mMixerGainValueLabel = new JLabel("0");
        add(mMixerGainValueLabel);

        add(new JLabel("LNA:"));

        mLNAGain = new JSlider(JSlider.HORIZONTAL,
            AirspyTunerController.LNA_GAIN_MIN,
            AirspyTunerController.LNA_GAIN_MAX,
            AirspyTunerController.LNA_GAIN_MIN);

        mLNAGain.setMajorTickSpacing(1);
        mLNAGain.setPaintTicks(true);

        mLNAGain.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                int gain = mLNAGain.getValue();

                try
                {
                    mController.setLNAGain(gain);
                    save();
                    mLNAGainValueLabel.setText(String.valueOf(gain));
                }
                catch(Exception e)
                {
                    mLog.error("Couldn't set airspy LNA gain to:" + gain, e);
                    JOptionPane.showMessageDialog(mIFGain, "Couldn't set LNA gain value to " + gain);
                }
            }
        });

        add(mLNAGain, "span 2");

        mLNAGainValueLabel = new JLabel("0");
        add(mLNAGainValueLabel);

        mMixerAGC = new JCheckBox("Mixer AGC");
        mMixerAGC.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    mController.setMixerAGC(mMixerAGC.isSelected());
                    mMixerGain.setEnabled(!mMixerAGC.isSelected());

                    save();
                }
                catch(Exception e1)
                {
                    mLog.error("Error setting Mixer AGC Enabled");
                }
            }
        });

        add(mMixerAGC, "span 2,center");

        mLNAAGC = new JCheckBox("LNA AGC");
        mLNAAGC.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    mController.setLNAAGC(mLNAAGC.isSelected());
                    mLNAGain.setEnabled(!mLNAAGC.isSelected());

                    save();
                }
                catch(Exception e1)
                {
                    mLog.error("Error setting LNA AGC Enabled");
                }

            }
        });

        add(mLNAAGC, "span 2,center");
    }

    /**
     * Updates the enabled state of each of the gain controls according to the
     * specified gain mode.  The master gain control is enabled for linearity
     * and sensitivity and the individual gain controls are disabled, and
     * vice-versa for custom mode.
     */
    private void updateGainComponents(GainMode mode)
    {
        switch(mode)
        {
            case LINEARITY:
                if(!mMasterGain.isEnabled())
                {
                    mMasterGain.setEnabled(true);
                    mMasterGainValueLabel.setEnabled(true);
                }
                if(mIFGain.isEnabled())
                {
                    mIFGain.setEnabled(false);
                    mIFGainValueLabel.setEnabled(false);
                }
                if(mLNAGain.isEnabled())
                {
                    mLNAAGC.setEnabled(false);
                    mLNAGainValueLabel.setEnabled(false);
                    mLNAGain.setEnabled(false);
                }
                if(mMixerGain.isEnabled())
                {
                    mMixerGainValueLabel.setEnabled(false);
                    mMixerAGC.setEnabled(false);
                    mMixerGain.setEnabled(false);
                }
                break;
            case SENSITIVITY:
                if(!mMasterGain.isEnabled())
                {
                    mMasterGain.setEnabled(true);
                    mMasterGainValueLabel.setEnabled(true);
                }
                if(mIFGain.isEnabled())
                {
                    mIFGain.setEnabled(false);
                    mIFGainValueLabel.setEnabled(false);
                }
                if(mLNAGain.isEnabled())
                {
                    mLNAAGC.setEnabled(false);
                    mLNAGainValueLabel.setEnabled(false);
                    mLNAGain.setEnabled(false);
                }
                if(mMixerGain.isEnabled())
                {
                    mMixerGainValueLabel.setEnabled(false);
                    mMixerAGC.setEnabled(false);
                    mMixerGain.setEnabled(false);
                }
                break;
            case CUSTOM:
                if(mMasterGain.isEnabled())
                {
                    mMasterGain.setEnabled(false);
                    mMasterGainValueLabel.setEnabled(false);
                }
                if(!mIFGain.isEnabled())
                {
                    mIFGain.setEnabled(true);
                    mIFGainValueLabel.setEnabled(true);
                }
                if(!mLNAGain.isEnabled())
                {
                    mLNAAGC.setEnabled(true);
                    mLNAGainValueLabel.setEnabled(true);
                    mLNAGain.setEnabled(true);
                }
                if(!mMixerGain.isEnabled())
                {
                    mMixerGainValueLabel.setEnabled(true);
                    mMixerAGC.setEnabled(true);
                    mMixerGain.setEnabled(true);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void save()
    {
        if(hasItem() && !mLoading)
        {
            AirspyTunerConfiguration config = getConfiguration();
            config.setName(mConfigurationName.getText());
            config.setSampleRate(((AirspySampleRate)mSampleRateCombo.getSelectedItem()).getRate());

            double value = ((SpinnerNumberModel)mFrequencyCorrection.getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);

            Gain gain = Gain.getGain((GainMode)mGainModeCombo.getSelectedItem(), mMasterGain.getValue());

            config.setGain(gain);
            config.setIFGain(mIFGain.getValue());
            config.setMixerGain(mMixerGain.getValue());
            config.setLNAGain(mLNAGain.getValue());
            config.setMixerAGC(mMixerAGC.isSelected());
            config.setLNAAGC(mLNAAGC.isSelected());

            getTunerConfigurationModel().broadcast(new TunerConfigurationEvent(config, TunerConfigurationEvent.Event.CHANGE));
        }
    }

    private AirspySampleRate fromValue(int value)
    {
        List<AirspySampleRate> rates = mController.getSampleRates();

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

        return null;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(mController.isLocked())
        {
            mSampleRateCombo.setToolTipText("Sample Rate is locked.  Disable decoding channels to unlock.");
        }
        else
        {
            mSampleRateCombo.setToolTipText("Select a sample rate for the tuner");
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        mSampleRateCombo.setEnabled(!locked);
    }

    /**
     * Sets all controls to the argument enabled state
     */
    private void setControlsEnabled(boolean enabled)
    {
        if(mConfigurationName.isEnabled() != enabled)
        {
            mConfigurationName.setEnabled(enabled);
        }

        if(mTunerInfo.isEnabled() != enabled)
        {
            mTunerInfo.setEnabled(enabled);
        }

        updateSampleRateToolTip();

        if(mController.isLocked())
        {
            mSampleRateCombo.setEnabled(false);
        }
        else if(mSampleRateCombo.isEnabled() != enabled)
        {
            mSampleRateCombo.setEnabled(enabled);
        }

        if(mFrequencyCorrection.isEnabled() != enabled)
        {
            mFrequencyCorrection.setEnabled(enabled);
        }

        if(mGainModeCombo.isEnabled() != enabled)
        {
            mGainModeCombo.setEnabled(enabled);
        }

        if(mMasterGain.isEnabled() != enabled)
        {
            mMasterGain.setEnabled(enabled);
        }

        if(mIFGain.isEnabled() != enabled)
        {
            mIFGain.setEnabled(enabled);
        }

        if(mMixerGain.isEnabled() != enabled)
        {
            mMixerGain.setEnabled(enabled);
        }

        if(mMixerAGC.isEnabled() != enabled)
        {
            mMixerAGC.setEnabled(enabled);
        }

        if(mLNAGain.isEnabled() != enabled)
        {
            mLNAGain.setEnabled(enabled);
        }

        if(mLNAAGC.isEnabled() != enabled)
        {
            mLNAAGC.setEnabled(enabled);
        }
    }

    /**
     * Applies the tuner configuration to the editor.  If the configuration is assigned to the
     * currently selected tuner, then the settings are applied.
     */
    @Override
    public void setItem(TunerConfiguration config)
    {
        super.setItem(config);

        mLoading = true;

        if(hasItem() && getItem().isAssigned())
        {
            AirspyTunerConfiguration airspy = getConfiguration();

            setControlsEnabled(true);

            mConfigurationName.setText(airspy.getName());

            AirspySampleRate rate = fromValue(airspy.getSampleRate());

            if(rate != null)
            {
                mSampleRateCombo.setSelectedItem(rate);
            }

            //Update enabled state to reflect when frequency and sample rate controls are locked
            mSampleRateCombo.setEnabled(!mController.isLocked());

            mFrequencyCorrection.removeChangeListener(mFrequencyCorrectionChangeListener);
            mFrequencyCorrection.setValue(airspy.getFrequencyCorrection());
            mFrequencyCorrection.addChangeListener(mFrequencyCorrectionChangeListener);

            GainMode mode = Gain.getGainMode(airspy.getGain());

            //This will set the master, IF, Mixer, LNA and AGC controls
            mGainModeCombo.setSelectedItem(mode);

            if(mode == GainMode.CUSTOM)
            {
                mMasterGain.setValue(airspy.getGain().getValue());
                mIFGain.setValue(airspy.getIFGain());

                if(airspy.isMixerAGC())
                {
                    mMixerAGC.setSelected(airspy.isMixerAGC());
                    mMixerGain.setEnabled(false);
                }
                else
                {
                    mMixerGain.setValue(airspy.getMixerGain());
                }

                if(airspy.isLNAAGC())
                {
                    mLNAAGC.setSelected(airspy.isLNAAGC());
                    mLNAGain.setEnabled(false);
                }
                else
                {
                    mLNAGain.setValue(airspy.getLNAGain());
                }
            }
            else
            {
                mMixerAGC.setSelected(false);
                mLNAAGC.setSelected(false);

                Gain gain = airspy.getGain();

                mMasterGain.setValue(gain.getValue());
                mIFGain.setValue(gain.getIF());
                mMixerGain.setValue(gain.getMixer());
                mLNAGain.setValue(gain.getLNA());
            }
        }
        else
        {
            setControlsEnabled(false);
            mConfigurationName.setText(hasItem() ? getItem().getName() : "");
        }

        mLoading = false;
    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        AirspyDeviceInformation info = mController.getDeviceInfo();

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

    private class FrequencyCorrectionChangeListener implements ChangeListener
    {

        @Override
        public void stateChanged(ChangeEvent e)
        {
            final double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();

            try
            {
                mController.setFrequencyCorrection(value);
            }
            catch(SourceException e1)
            {
                mLog.error("Error setting frequency correction value", e1);
            }

            save();
        }
    }
}