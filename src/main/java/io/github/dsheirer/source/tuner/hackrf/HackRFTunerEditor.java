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
package io.github.dsheirer.source.tuner.hackrf;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
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
import java.util.Collections;

public class HackRFTunerEditor extends TunerConfigurationEditor
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(HackRFTunerEditor.class);

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JComboBox<HackRFSampleRate> mComboSampleRate;
    private JSpinner mFrequencyCorrection;
    private JToggleButton mAmplifier;
    private JComboBox<HackRFLNAGain> mComboLNAGain;
    private JComboBox<HackRFVGAGain> mComboVGAGain;
    private boolean mLoading;

    private HackRFTunerController mController;

    public HackRFTunerEditor(TunerConfigurationModel tunerConfigurationModel, HackRFTuner tuner)
    {
        super(tunerConfigurationModel);

        mController = tuner.getController();

        init();
    }

    private HackRFTunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (HackRFTunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][][grow]"));

        add(new JLabel("HackRF Tuner Configuration"), "span,align center");

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
                JOptionPane.showMessageDialog(HackRFTunerEditor.this, getTunerInfo(),
                    "Tuner Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(mTunerInfo);


        HackRFSampleRate[] validRates = HackRFSampleRate.VALID_SAMPLE_RATES
            .toArray(new HackRFSampleRate[0]);
        mComboSampleRate = new JComboBox<>(validRates);
        mComboSampleRate.setEnabled(false);
        mComboSampleRate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                HackRFSampleRate sampleRate = (HackRFSampleRate)mComboSampleRate.getSelectedItem();

                try
                {

                    mController.setSampleRate(sampleRate);
                    save();
                }
                catch(SourceException | UsbException e2)
                {
                    JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                        + " - couldn't apply the sample rate setting [" + sampleRate.getLabel() +
                        "] " + e2.getLocalizedMessage());

                    mLog.error("HackRF Tuner Controller - couldn't apply sample rate setting [" +
                        sampleRate.getLabel() + "]", e);
                }
            }
        });
        add(new JLabel("Sample Rate:"));
        add(mComboSampleRate);

        SpinnerModel model = new SpinnerNumberModel(0.0,   //initial value
            -1000.0,   //min
            1000.0,   //max
            0.1); //step

        mFrequencyCorrection = new JSpinner(model);
        mFrequencyCorrection.setEnabled(false);
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)mFrequencyCorrection.getEditor();

        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(1);
        editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);

        mFrequencyCorrection.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                final double value = ((SpinnerNumberModel)mFrequencyCorrection
                    .getModel()).getNumber().doubleValue();

                try
                {
                    mController.setFrequencyCorrection(value);
                    save();
                }
                catch(SourceException e1)
                {
                    JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                        + " - couldn't apply frequency correction value: " + value +
                        e1.getLocalizedMessage());

                    mLog.error("HackRF Tuner Controller - couldn't apply frequency correction "
                        + "value: " + value, e1);
                }
            }
        });

        add(new JLabel("PPM:"));
        add(mFrequencyCorrection);

        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");
        add(new JLabel("Gain"));
        add(new JLabel(""), "span 2"); //filler

        mAmplifier = new JToggleButton("Amplifier");
        mAmplifier.setEnabled(false);
        mAmplifier.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    mController.setAmplifierEnabled(mAmplifier.isSelected());
                    save();
                }
                catch(UsbException e)
                {
                    mLog.error("couldn't enable/disable amplifier", e);

                    JOptionPane.showMessageDialog(HackRFTunerEditor.this, "Couldn't change amplifier setting",
                        "Error changing amplifier setting", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        add(mAmplifier);

        mComboLNAGain = new JComboBox<HackRFLNAGain>(HackRFLNAGain.values());
        mComboLNAGain.setEnabled(false);
        mComboLNAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    HackRFLNAGain lnaGain = (HackRFLNAGain)mComboLNAGain.getSelectedItem();

                    if(lnaGain == null)
                    {
                        lnaGain = HackRFLNAGain.GAIN_16;
                    }

                    mController.setLNAGain(lnaGain);
                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                        + " - couldn't apply the LNA gain setting - " + e.getLocalizedMessage());

                    mLog.error("HackRF Tuner Controller - couldn't apply LNA gain setting - ", e);
                }
            }
        });
        mComboLNAGain.setToolTipText("<html>LNA Gain.  Adjust to set the IF gain</html>");
        add(new JLabel("LNA:"));
        add(mComboLNAGain);

        mComboVGAGain = new JComboBox<HackRFVGAGain>(HackRFVGAGain.values());
        mComboVGAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    HackRFVGAGain vgaGain = (HackRFVGAGain)mComboVGAGain.getSelectedItem();

                    if(vgaGain == null)
                    {
                        vgaGain = HackRFVGAGain.GAIN_16;
                    }

                    mController.setVGAGain(vgaGain);
                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                        + " - couldn't apply the VGA gain setting - " + e.getLocalizedMessage());

                    mLog.error("HackRF Tuner Controller - couldn't apply VGA gain setting", e);
                }
            }
        });
        mComboVGAGain.setToolTipText("<html>VGA Gain.  Adjust to set the baseband gain</html>");
        add(new JLabel("VGA:"));
        add(mComboVGAGain);
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(mController.isLocked())
        {
            mComboSampleRate.setToolTipText("Sample Rate is locked.  Disable decoding channels to unlock.");
        }
        else
        {
            mComboSampleRate.setToolTipText("Select a sample rate for the tuner");
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        mComboSampleRate.setEnabled(!locked);
    }

    /**
     * Sets each of the tuner configuration controls to the enabled argument state
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

        if(mFrequencyCorrection.isEnabled() != enabled)
        {
            mFrequencyCorrection.setEnabled(enabled);
        }

        updateSampleRateToolTip();

        if(mController.isLocked())
        {
            mComboSampleRate.setEnabled(false);
        }
        else if(mComboSampleRate.isEnabled() != enabled)
        {
            mComboSampleRate.setEnabled(enabled);
        }

        if(mAmplifier.isEnabled() != enabled)
        {
            mAmplifier.setEnabled(enabled);
        }

        if(mComboLNAGain.isEnabled() != enabled)
        {
            mComboLNAGain.setEnabled(enabled);
        }

        if(mComboVGAGain.isEnabled() != enabled)
        {
            mComboVGAGain.setEnabled(enabled);
        }
    }

    private String getTunerInfo()
    {
        HackRFTunerController.BoardID board = HackRFTunerController.BoardID.INVALID;

        try
        {
            board = mController.getBoardID();
        }
        catch(UsbException e)
        {
            mLog.error("couldn't read HackRF board identifier", e);
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>HackRF Tuner</h3>");

        sb.append("<b>Board: </b>");
        sb.append(board.getLabel());
        sb.append("<br>");

        HackRFTunerController.Serial serial = null;

        try
        {
            serial = mController.getSerial();
        }
        catch(Exception e)
        {
            mLog.error("couldn't read HackRF serial number", e);
        }

        if(serial != null)
        {
            sb.append("<b>Serial: </b>");
            sb.append(serial.getSerialNumber());
            sb.append("<br>");

            sb.append("<b>Part: </b>");
            sb.append(serial.getPartID());
            sb.append("<br>");
        }
        else
        {
            sb.append("<b>Serial: Unknown</b><br>");
            sb.append("<b>Part: Unknown</b><br>");
        }

        String firmware = null;

        try
        {
            firmware = mController.getFirmwareVersion();
        }
        catch(Exception e)
        {
            mLog.error("couldn't read HackRF firmware version", e);
        }

        if(firmware != null)
        {
            sb.append("<b>Firmware: </b>");
            sb.append(firmware);
            sb.append("<br>");
        }
        else
        {
            sb.append("<b>Firmware: Unknown</b><br>");
        }

        return sb.toString();
    }

    @Override
    public void setItem(TunerConfiguration tunerConfiguration)
    {
        super.setItem(tunerConfiguration);

        //Toggle loading so that we don't fire a change event and schedule a settings file save
        mLoading = true;

        if(hasItem())
        {
            HackRFTunerConfiguration config = getConfiguration();

            if(tunerConfiguration.isAssigned())
            {
                setControlsEnabled(true);

                mConfigurationName.setText(config.getName());
                mComboSampleRate.setSelectedItem(config.getSampleRate());
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                mAmplifier.setSelected(config.getAmplifierEnabled());
                mComboLNAGain.setSelectedItem(config.getLNAGain());
                mComboVGAGain.setSelectedItem(config.getVGAGain());

                //Update enabled state to reflect when frequency and sample rate controls are locked
                mComboSampleRate.setEnabled(!mController.isLocked());
            }
            else
            {
                setControlsEnabled(false);
                mConfigurationName.setText(config.getName());
            }
        }
        else
        {
            setControlsEnabled(false);
            mConfigurationName.setText("");
        }

        mLoading = false;
    }

    @Override
    public void save()
    {
        if(hasItem() && !mLoading)
        {
            HackRFTunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setSampleRate((HackRFSampleRate)mComboSampleRate.getSelectedItem());
            config.setAmplifierEnabled(mAmplifier.isSelected());
            config.setLNAGain((HackRFLNAGain)mComboLNAGain.getSelectedItem());
            config.setVGAGain((HackRFVGAGain)mComboVGAGain.getSelectedItem());

            getTunerConfigurationModel().broadcast(
                new TunerConfigurationEvent(getConfiguration(), TunerConfigurationEvent.Event.CHANGE));
        }
    }
}