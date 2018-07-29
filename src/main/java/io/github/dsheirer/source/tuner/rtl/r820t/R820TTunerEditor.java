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
package io.github.dsheirer.source.tuner.rtl.r820t;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerController.R820TGain;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerController.R820TLNAGain;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerController.R820TMixerGain;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerController.R820TVGAGain;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
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

public class R820TTunerEditor extends TunerConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(R820TTunerEditor.class);

    private static final long serialVersionUID = 1L;
    private static final R820TGain DEFAULT_GAIN = R820TGain.GAIN_279;

    private R820TTunerController mController;

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JComboBox<SampleRate> mComboSampleRate;
    private JSpinner mFrequencyCorrection;
    private JCheckBox mAutoPPMEnabled;
    private JComboBox<R820TGain> mComboMasterGain;
    private JComboBox<R820TMixerGain> mComboMixerGain;
    private JComboBox<R820TLNAGain> mComboLNAGain;
    private JComboBox<R820TVGAGain> mComboVGAGain;
    private boolean mLoading;

    public R820TTunerEditor(TunerConfigurationModel tunerConfigurationModel, RTL2832Tuner tuner)
    {
        super(tunerConfigurationModel);
        mController = (R820TTunerController)tuner.getController();

        init();
    }

    private R820TTunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (R820TTunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][][][grow]"));

        add(new JLabel("R820T Tuner Configuration"), "span,align center");

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
                JOptionPane.showMessageDialog(R820TTunerEditor.this, getTunerInfo(),
                    "Tuner Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(mTunerInfo);

        mComboSampleRate = new JComboBox<>(SampleRate.values());
        mComboSampleRate.setEnabled(false);
        mComboSampleRate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                SampleRate sampleRate =
                    (SampleRate)mComboSampleRate.getSelectedItem();
                try
                {

                    mController.setSampleRate(sampleRate);
                    save();
                }
                catch(SourceException | LibUsbException eSampleRate)
                {
                    JOptionPane.showMessageDialog(
                        R820TTunerEditor.this,
                        "R820T Tuner Controller - couldn't apply the sample "
                            + "rate setting [" + sampleRate.getLabel() + "] " +
                            eSampleRate.getLocalizedMessage());

                    mLog.error("R820T Tuner Controller - couldn't apply sample "
                            + "rate setting [" + sampleRate.getLabel() + "]",
                        eSampleRate);
                }
            }
        });
        add(new JLabel("Sample Rate:"));
        add(mComboSampleRate);

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
                }
                catch(SourceException e1)
                {
                    mLog.error("Error setting frequency correction value", e1);
                }

                save();
            }
        });

        add(new JLabel("PPM:"));
        add(mFrequencyCorrection);

        add(new JLabel("")); //Space filler
        add(new JLabel("")); //Space filler
        add(new JLabel("")); //Space filler
        mAutoPPMEnabled = new JCheckBox("PPM Auto-Correction");
        mAutoPPMEnabled.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean enabled = mAutoPPMEnabled.isSelected();
                mController.getFrequencyErrorCorrectionManager().setEnabled(enabled);
                save();
            }
        });
        add(mAutoPPMEnabled);

        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");

        /**
         * Gain Controls
         */
        add(new JLabel("Gain"), "wrap");

        /* Master Gain Control */
        mComboMasterGain = new JComboBox<R820TGain>(R820TGain.values());
        mComboMasterGain.setEnabled(false);
        mComboMasterGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    R820TGain gain = (R820TGain)mComboMasterGain.getSelectedItem();

                    mController.setGain((R820TGain)mComboMasterGain.getSelectedItem(), true);

                    if(gain == R820TGain.MANUAL)
                    {
                        mComboMixerGain.setSelectedItem(gain.getMixerGain());
                        mComboMixerGain.setEnabled(true);

                        mComboLNAGain.setSelectedItem(gain.getLNAGain());
                        mComboLNAGain.setEnabled(true);

                        mComboVGAGain.setSelectedItem(gain.getVGAGain());
                        mComboVGAGain.setEnabled(true);
                    }
                    else
                    {
                        mComboMixerGain.setEnabled(false);
                        mComboMixerGain.setSelectedItem(gain.getMixerGain());

                        mComboLNAGain.setEnabled(false);
                        mComboLNAGain.setSelectedItem(gain.getLNAGain());

                        mComboVGAGain.setEnabled(false);
                        mComboVGAGain.setSelectedItem(gain.getVGAGain());
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(
                        R820TTunerEditor.this,
                        "R820T Tuner Controller - couldn't apply the gain "
                            + "setting - " + e.getLocalizedMessage());

                    mLog.error("R820T Tuner Controller - couldn't apply "
                        + "gain setting - ", e);
                }
            }
        });
        mComboMasterGain.setToolTipText("<html>Select <b>AUTOMATIC</b> for auto "
            + "gain, <b>MANUAL</b> to enable<br> independent control of "
            + "<i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>"
            + "settings, or one of the individual gain settings for<br>"
            + "semi-manual gain control</html>");
        add(new JLabel("Master:"));
        add(mComboMasterGain);

        /* Mixer Gain Control */
        mComboMixerGain = new JComboBox<R820TMixerGain>(R820TMixerGain.values());
        mComboMixerGain.setEnabled(false);
        mComboMixerGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    R820TMixerGain mixerGain =
                        (R820TMixerGain)mComboMixerGain.getSelectedItem();

                    if(mixerGain == null)
                    {
                        mixerGain = DEFAULT_GAIN.getMixerGain();
                    }

                    if(mComboMixerGain.isEnabled())
                    {
                        mController.setMixerGain(mixerGain, true);
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(R820TTunerEditor.this,
                        "R820T Tuner Controller - couldn't apply the mixer "
                            + "gain setting - " + e.getLocalizedMessage());

                    mLog.error("R820T Tuner Controller - couldn't apply mixer "
                        + "gain setting - ", e);
                }
            }
        });
        mComboMixerGain.setToolTipText("<html>Mixer Gain.  Set master gain "
            + "to <b>MANUAL</b> to enable adjustment</html>");

        add(new JLabel("Mixer:"));
        add(mComboMixerGain);

        /* LNA Gain Control */
        mComboLNAGain = new JComboBox<R820TLNAGain>(R820TLNAGain.values());
        mComboLNAGain.setEnabled(false);
        mComboLNAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    R820TLNAGain lnaGain =
                        (R820TLNAGain)mComboLNAGain.getSelectedItem();

                    if(lnaGain == null)
                    {
                        lnaGain = DEFAULT_GAIN.getLNAGain();
                    }

                    if(mComboLNAGain.isEnabled())
                    {
                        mController.setLNAGain(lnaGain, true);
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(R820TTunerEditor.this,
                        "R820T Tuner Controller - couldn't apply the LNA "
                            + "gain setting - " + e.getLocalizedMessage());

                    mLog.error("R820T Tuner Controller - couldn't apply LNA "
                        + "gain setting - ", e);
                }
            }
        });
        mComboLNAGain.setToolTipText("<html>LNA Gain.  Set master gain "
            + "to <b>MANUAL</b> to enable adjustment</html>");

        add(new JLabel("LNA:"));
        add(mComboLNAGain);

        /* VGA Gain Control */
        mComboVGAGain = new JComboBox<R820TVGAGain>(R820TVGAGain.values());
        mComboVGAGain.setEnabled(false);
        mComboVGAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    R820TVGAGain vgaGain =
                        (R820TVGAGain)mComboVGAGain.getSelectedItem();

                    if(vgaGain == null)
                    {
                        vgaGain = DEFAULT_GAIN.getVGAGain();
                    }

                    if(mComboVGAGain.isEnabled())
                    {
                        mController.setVGAGain(vgaGain, true);
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(R820TTunerEditor.this,
                        "R820T Tuner Controller - couldn't apply the VGA "
                            + "gain setting - " + e.getLocalizedMessage());

                    mLog.error("R820T Tuner Controller - couldn't apply VGA "
                        + "gain setting", e);
                }
            }
        });
        mComboVGAGain.setToolTipText("<html>VGA Gain.  Set master gain "
            + "to <b>MANUAL</b> to enable adjustment</html>");
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

        if(mAutoPPMEnabled.isEnabled() != enabled)
        {
            mAutoPPMEnabled.setEnabled(enabled);
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

        if(mComboMasterGain.isEnabled() != enabled)
        {
            mComboMasterGain.setEnabled(enabled);
        }

        if(mComboMixerGain.isEnabled() != enabled)
        {
            mComboMixerGain.setEnabled(enabled);
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
        StringBuilder sb = new StringBuilder();

        RTL2832TunerController.Descriptor descriptor = mController.getDescriptor();

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
        if(hasItem() && !mLoading)
        {
            R820TTunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);

            config.setAutoPPMCorrectionEnabled(mAutoPPMEnabled.isSelected());

            config.setSampleRate((SampleRate)mComboSampleRate.getSelectedItem());

            R820TGain gain = (R820TGain)mComboMasterGain.getSelectedItem();
            config.setMasterGain(gain);

            R820TMixerGain mixerGain = (R820TMixerGain)mComboMixerGain.getSelectedItem();
            config.setMixerGain(mixerGain);

            R820TLNAGain lnaGain = (R820TLNAGain)mComboLNAGain.getSelectedItem();
            config.setLNAGain(lnaGain);

            R820TVGAGain vgaGain = (R820TVGAGain)mComboVGAGain.getSelectedItem();
            config.setVGAGain(vgaGain);

            getTunerConfigurationModel().broadcast(
                new TunerConfigurationEvent(getConfiguration(), TunerConfigurationEvent.Event.CHANGE));
        }
    }

    @Override
    public void setItem(TunerConfiguration tunerConfiguration)
    {
        super.setItem(tunerConfiguration);

        //Toggle loading so that we don't fire a change event and schedule a settings file save
        mLoading = true;

        if(hasItem())
        {
            R820TTunerConfiguration config = getConfiguration();

            if(tunerConfiguration.isAssigned())
            {
                setControlsEnabled(true);
                mConfigurationName.setText(config.getName());
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                mAutoPPMEnabled.setSelected(config.getAutoPPMCorrectionEnabled());
                mComboSampleRate.setSelectedItem(config.getSampleRate());
                mComboMasterGain.setSelectedItem(config.getMasterGain());
                mComboMixerGain.setSelectedItem(config.getMixerGain());
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
}