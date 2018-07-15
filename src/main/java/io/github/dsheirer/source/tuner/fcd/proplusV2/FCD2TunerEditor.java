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
package io.github.dsheirer.source.tuner.fcd.proplusV2;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent.Event;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;

public class FCD2TunerEditor extends TunerConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(FCD2TunerEditor.class);
    private static final long serialVersionUID = 1L;

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JSpinner mFrequencyCorrection;
    private JCheckBox mLNAGain;
    private JCheckBox mMixerGain;
    private boolean mLoading;

    private FCD2TunerController mController;

    public FCD2TunerEditor(TunerConfigurationModel tunerConfigurationModel, FCDTuner tuner)
    {
        super(tunerConfigurationModel);
        mController = (FCD2TunerController)tuner.getController();

        init();
    }

    private FCD2TunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (FCD2TunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][grow]"));

        add(new JLabel("FCD Pro+ Tuner Configuration"), "span,align center");

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
                JOptionPane.showMessageDialog(FCD2TunerEditor.this, getTunerInfo(),
                    "Tuner Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(mTunerInfo);

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
                double value = ((SpinnerNumberModel)mFrequencyCorrection
                    .getModel()).getNumber().doubleValue();

                try
                {
                    mController.setFrequencyCorrection(value);
                    save();
                }
                catch(SourceException e1)
                {
                    JOptionPane.showMessageDialog(FCD2TunerEditor.this, "FCD Pro Plus Tuner "
                        + "Controller - couldn't apply frequency correction value: " + value +
                        e1.getLocalizedMessage());

                    mLog.error("FuncubeDongleProPlus Controller - couldn't apply "
                        + "frequency correction value: " + value, e1);
                }
            }
        });

        add(new JLabel("PPM:"));
        add(mFrequencyCorrection, "wrap");

        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");

        mLNAGain = new JCheckBox("LNA Gain");
        mLNAGain.setEnabled(false);
        mLNAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                try
                {
                    mController.setLNAGain(mLNAGain.isSelected());
                    save();
                }
                catch(SourceException e)
                {
                    mLog.error("Couldn't set LNA gain for FCD2", e);
                }
            }
        });

        add(new JLabel(""));
        add(mLNAGain);

        /**
         * Mixer Gain
         */
        mMixerGain = new JCheckBox("Mixer Gain");

        mMixerGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                try
                {
                    mController.setMixerGain(mMixerGain.isSelected());
                    save();
                }
                catch(SourceException e)
                {
                    mLog.error("Couldn't set mixer gain for FCD2", e);
                }
            }
        });

        add(new JLabel(""));
        add(mMixerGain);
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        //no-op
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

        if(mLNAGain.isEnabled() != enabled)
        {
            mLNAGain.setEnabled(enabled);
        }

        if(mMixerGain.isEnabled() != enabled)
        {
            mMixerGain.setEnabled(enabled);
        }
    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>Funcube Dongle Pro Plus Tuner</h3>");

        sb.append("<b>USB ID: </b>");
        sb.append(mController.getUSBID());
        sb.append("<br>");

        sb.append("<b>USB Address: </b>");
        sb.append(mController.getUSBAddress());
        sb.append("<br>");

        sb.append("<b>USB Speed: </b>");
        sb.append(mController.getUSBSpeed());
        sb.append("<br>");

        sb.append("<b>Cellular Band: </b>");
        sb.append(mController.getConfiguration().getBandBlocking());
        sb.append("<br>");

        sb.append("<b>Firmware: </b>");
        sb.append(mController.getConfiguration().getFirmware());
        sb.append("<br>");

        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasItem() && !mLoading)
        {
            FCD2TunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);

            config.setGainLNA(mLNAGain.isSelected());
            config.setGainMixer(mMixerGain.isSelected());

            getTunerConfigurationModel().broadcast(
                new TunerConfigurationEvent(getConfiguration(), Event.CHANGE));
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
            FCD2TunerConfiguration config = getConfiguration();


            if(tunerConfiguration.isAssigned())
            {
                setControlsEnabled(true);
                mConfigurationName.setText(config.getName());
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                mLNAGain.setSelected(config.getGainLNA());
                mMixerGain.setSelected(config.getGainMixer());
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