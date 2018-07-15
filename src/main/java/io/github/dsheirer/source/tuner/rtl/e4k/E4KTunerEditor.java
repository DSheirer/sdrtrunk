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
package io.github.dsheirer.source.tuner.rtl.e4k;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KEnhanceGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KLNAGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KMixerGain;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import javax.swing.JButton;
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

public class E4KTunerEditor extends TunerConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(E4KTunerEditor.class);

    private static final long serialVersionUID = 1L;

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JComboBox<SampleRate> mComboSampleRate;
    private JSpinner mFrequencyCorrection;
    private JComboBox<E4KGain> mComboMasterGain;
    private JComboBox<E4KMixerGain> mComboMixerGain;
    private JComboBox<E4KLNAGain> mComboLNAGain;
    private JComboBox<E4KEnhanceGain> mComboEnhanceGain;

    private E4KTunerController mController;
    private boolean mLoading;

    public E4KTunerEditor(TunerConfigurationModel tunerConfigurationModel, RTL2832Tuner tuner)
    {
        super(tunerConfigurationModel);
        mController = (E4KTunerController)tuner.getController();

        init();
    }

    private E4KTunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (E4KTunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][][][grow]"));

        add(new JLabel("E4000 Tuner Configuration"), "span,align center");

        /**
         * Tuner Configuration Name
         */
        add(new JLabel("Name:"));
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

        add(mConfigurationName, "span 2");

        mTunerInfo = new JButton("Tuner Info");
        mTunerInfo.setEnabled(false);
        mTunerInfo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JOptionPane.showMessageDialog(E4KTunerEditor.this, getTunerInfo(),
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
                SampleRate sampleRate = (SampleRate)mComboSampleRate.getSelectedItem();

                try
                {
                    mController.setSampleRate(sampleRate);
                    save();
                }
                catch(SourceException | LibUsbException eSampleRate)
                {
                    JOptionPane.showMessageDialog(E4KTunerEditor.this,
                        "E4K Tuner Controller - couldn't apply the sample rate setting [" +
                            sampleRate.getLabel() + "] " + eSampleRate.getLocalizedMessage());

                    mLog.error("E4K Tuner Controller - couldn't apply sample rate setting [" +
                        sampleRate.getLabel() + "]", eSampleRate);
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
                    save();
                }
                catch(SourceException e1)
                {
                    JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4K Tuner Controller - "
                        + "couldn't apply frequency correction value: " + value + e1.getLocalizedMessage());

                    mLog.error("E4K Tuner Controller - couldn't apply frequency correction value:"
                        + value, e1);
                }
            }
        });

        add(new JLabel("PPM:"));
        add(mFrequencyCorrection);

        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");
        add(new JLabel("Gain"), "wrap");

        mComboMasterGain = new JComboBox<E4KGain>(E4KGain.values());
        mComboMasterGain.setEnabled(false);
        mComboMasterGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    E4KGain gain = (E4KGain)mComboMasterGain.getSelectedItem();

                    mController.setGain((E4KGain)mComboMasterGain.getSelectedItem(), true);

                    if(gain == E4KGain.MANUAL)
                    {
                        mComboMixerGain.setSelectedItem(mController.getMixerGain(true));
                        mComboMixerGain.setEnabled(true);

                        mComboLNAGain.setSelectedItem(mController.getLNAGain(true));
                        mComboLNAGain.setEnabled(true);

                        mComboEnhanceGain.setSelectedItem(mController.getEnhanceGain(true));
                        mComboEnhanceGain.setEnabled(true);
                    }
                    else
                    {
                        mComboMixerGain.setEnabled(false);
                        mComboMixerGain.setSelectedItem(gain.getMixerGain());

                        mComboLNAGain.setEnabled(false);
                        mComboLNAGain.setSelectedItem(gain.getLNAGain());

                        mComboEnhanceGain.setEnabled(false);
                        mComboEnhanceGain.setSelectedItem(gain.getEnhanceGain());
                    }

                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4K Tuner Controller - "
                        + "couldn't apply the gain setting - " + e.getLocalizedMessage());

                    mLog.error("E4K Tuner Controller - couldn't apply gain setting", e);
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

        mComboMixerGain = new JComboBox<E4KMixerGain>(E4KMixerGain.values());
        mComboMixerGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    E4KMixerGain mixerGain = (E4KMixerGain)mComboMixerGain.getSelectedItem();
                    mController.setMixerGain(mixerGain, true);
                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4K Tuner Controller - "
                        + "couldn't apply the mixer gain setting - " + e.getLocalizedMessage());

                    mLog.error("E4K Tuner Controller - couldn't apply mixer gain setting", e);
                }
            }
        });
        mComboMixerGain.setToolTipText("<html>Mixer Gain.  Set master gain "
            + "to <b>MASTER</b> to enable adjustment</html>");
        mComboMixerGain.setEnabled(false);
        add(new JLabel("Mixer:"));
        add(mComboMixerGain);

        mComboLNAGain = new JComboBox<E4KLNAGain>(E4KLNAGain.values());
        mComboLNAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    E4KLNAGain lnaGain = (E4KLNAGain)mComboLNAGain.getSelectedItem();
                    mController.setLNAGain(lnaGain, true);
                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4K Tuner Controller - "
                        + "couldn't apply the LNA gain setting - " + e.getLocalizedMessage());

                    mLog.error("E4K Tuner Controller - couldn't apply LNA gain setting - ", e);
                }
            }
        });
        mComboLNAGain.setToolTipText("<html>LNA Gain.  Set master gain "
            + "to <b>MANUAL</b> to enable adjustment</html>");
        mComboLNAGain.setEnabled(false);
        add(new JLabel("LNA:"));
        add(mComboLNAGain);

        mComboEnhanceGain = new JComboBox<E4KEnhanceGain>(E4KEnhanceGain.values());
        mComboEnhanceGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    E4KEnhanceGain enhance = (E4KEnhanceGain)mComboEnhanceGain.getSelectedItem();
                    mController.setEnhanceGain(enhance, true);
                    save();
                }
                catch(UsbException e)
                {
                    JOptionPane.showMessageDialog(E4KTunerEditor.this, "E4K Tuner Controller - "
                        + "couldn't apply the enhance gain setting - " + e.getLocalizedMessage());

                    mLog.error("E4K Tuner Controller - couldn't apply enhance gain setting", e);
                }
            }
        });
        mComboEnhanceGain.setToolTipText("<html>Enhance Gain.  Set master gain "
            + "to <b>MANUAL</b> to enable adjustment</html>");
        mComboEnhanceGain.setEnabled(false);
        add(new JLabel("Enhance:"));
        add(mComboEnhanceGain);
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

        if(mComboEnhanceGain.isEnabled() != enabled)
        {
            mComboEnhanceGain.setEnabled(enabled);
        }
    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        RTL2832TunerController.Descriptor descriptor = mController.getDescriptor();

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
        if(hasItem() && !mLoading)
        {
            E4KTunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            config.setSampleRate((SampleRate)mComboSampleRate.getSelectedItem());

            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);

            config.setMasterGain((E4KGain)mComboMasterGain.getSelectedItem());
            config.setMixerGain((E4KMixerGain)mComboMixerGain.getSelectedItem());
            config.setLNAGain((E4KLNAGain)mComboLNAGain.getSelectedItem());
            config.setEnhanceGain((E4KEnhanceGain)mComboEnhanceGain.getSelectedItem());

            getTunerConfigurationModel().broadcast(new TunerConfigurationEvent(
                getConfiguration(), TunerConfigurationEvent.Event.CHANGE));
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
            E4KTunerConfiguration config = getConfiguration();

            if(tunerConfiguration.isAssigned())
            {
                setControlsEnabled(true);

                mConfigurationName.setText(config.getName());
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                mComboSampleRate.setSelectedItem(config.getSampleRate());
                mComboMasterGain.setSelectedItem(config.getMasterGain());
                mComboMixerGain.setSelectedItem(config.getMixerGain());
                mComboLNAGain.setSelectedItem(config.getLNAGain());
                mComboEnhanceGain.setSelectedItem(config.getEnhanceGain());

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