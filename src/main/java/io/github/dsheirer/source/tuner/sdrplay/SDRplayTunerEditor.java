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
package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;
import io.github.sammy1am.sdrplay.ApiException;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.Bw_MHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.If_kHzT;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;

public class SDRplayTunerEditor extends TunerConfigurationEditor
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(SDRplayTunerEditor.class);

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JComboBox<Integer> mComboSampleRate;
    private JSpinner mFrequencyCorrection;
    
    private JComboBox<If_kHzT> mComboIfMode;
    private JComboBox<Bw_MHzT> mComboBandwidth;
    
    private JToggleButton mAmplifier;
    private JComboBox<HackRFLNAGain> mComboLNAGain;
    private JComboBox<HackRFVGAGain> mComboVGAGain;
    private boolean mLoading;

    private final SDRplayTunerController mController;

    public SDRplayTunerEditor(TunerConfigurationModel tunerConfigurationModel, SDRplayTuner tuner)
    {
        super(tunerConfigurationModel);

        mController = tuner.getController();

        init();
    }

    private SDRplayTunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (SDRplayTunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][][][][grow]"));

        add(new JLabel("SDRplay Tuner Configuration"), "span,align center");

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
                JOptionPane.showMessageDialog(SDRplayTunerEditor.this, getTunerInfo(),
                    "Tuner Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(mTunerInfo);


        Integer[] validRates = {2000000,3000000,4000000,5000000,6000000,7000000,8000000,9000000,10000000};
        mComboSampleRate = new JComboBox<>(validRates);
        mComboSampleRate.setEnabled(false);
        mComboSampleRate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int sampleRate = (Integer)mComboSampleRate.getSelectedItem();

                try
                {
                    mController.setSampleRate(sampleRate);
                    save();
                }
                catch(SourceException e2)
                {
                    mLog.error("SDRplay Tuner Controller - couldn't apply sample rate setting [" +
                        sampleRate + "]", e);
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
                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "HackRF Tuner Controller"
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
        

        If_kHzT[] ifModes = {If_kHzT.IF_Zero, If_kHzT.IF_0_450, If_kHzT.IF_1_620, If_kHzT.IF_2_048};
        mComboIfMode = new JComboBox<>(ifModes);
        mComboIfMode.setEnabled(false);
        mComboIfMode.addActionListener((ActionEvent e) -> {
            If_kHzT ifMode = (If_kHzT)mComboIfMode.getSelectedItem();
            
            try
            {
                mController.setIfType(ifMode);
                save();
            }
            catch(ApiException ex)
            {
                mLog.error("SDRplay Tuner Controller - couldn't apply IF mode setting [" +
                        ifMode + "]", ex);
            }
        });
        add(new JLabel("IF Mode:"));
        add(mComboIfMode);
        
        Bw_MHzT[] bwModes = {Bw_MHzT.BW_0_200,Bw_MHzT.BW_0_300,Bw_MHzT.BW_0_600,Bw_MHzT.BW_1_536,Bw_MHzT.BW_5_000,Bw_MHzT.BW_6_000,Bw_MHzT.BW_7_000,Bw_MHzT.BW_8_000};
        mComboBandwidth = new JComboBox<>(bwModes);
        mComboBandwidth.setEnabled(false);
        mComboBandwidth.addActionListener((ActionEvent e) -> {
            Bw_MHzT bwMode = (Bw_MHzT)mComboBandwidth.getSelectedItem();
            
            try
            {
                mController.setIFBandwidth(bwMode);
                save();
            }
            catch(ApiException ex)
            {
                mLog.error("SDRplay Tuner Controller - couldn't apply IF bandwidth setting [" +
                        bwMode + "]", ex);
            }
        });
        add(new JLabel("IF Bandwidth:"));
        add(mComboBandwidth);
        
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
//                try
//                {
//                    mController.setAmplifierEnabled(mAmplifier.isSelected());
//                    save();
//                }
//                catch(UsbException e)
//                {
//                    mLog.error("couldn't enable/disable amplifier", e);
//
//                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "Couldn't change amplifier setting",
//                        "Error changing amplifier setting", JOptionPane.ERROR_MESSAGE);
//                }
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
//                try
//                {
//                    HackRFLNAGain lnaGain = (HackRFLNAGain)mComboLNAGain.getSelectedItem();
//
//                    if(lnaGain == null)
//                    {
//                        lnaGain = HackRFLNAGain.GAIN_16;
//                    }
//
//                    mController.setLNAGain(lnaGain);
//                    save();
//                }
//                catch(UsbException e)
//                {
//                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "HackRF Tuner Controller"
//                        + " - couldn't apply the LNA gain setting - " + e.getLocalizedMessage());
//
//                    mLog.error("HackRF Tuner Controller - couldn't apply LNA gain setting - ", e);
//                }
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
//                try
//                {
//                    HackRFVGAGain vgaGain = (HackRFVGAGain)mComboVGAGain.getSelectedItem();
//
//                    if(vgaGain == null)
//                    {
//                        vgaGain = HackRFVGAGain.GAIN_16;
//                    }
//
//                    mController.setVGAGain(vgaGain);
//                    save();
//                }
//                catch(UsbException e)
//                {
//                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "HackRF Tuner Controller"
//                        + " - couldn't apply the VGA gain setting - " + e.getLocalizedMessage());
//
//                    mLog.error("HackRF Tuner Controller - couldn't apply VGA gain setting", e);
//                }
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
        
        if(mComboIfMode.isEnabled() != enabled)
        {
            mComboIfMode.setEnabled(enabled);
        }
        
        if(mComboBandwidth.isEnabled() != enabled)
        {
            mComboBandwidth.setEnabled(enabled);
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
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>SDRplay Tuner</h3>");

        sb.append("<b>Model: </b>");
        sb.append(mController.getModel());
        sb.append("<br>");

        sb.append("<b>Serial: </b>");
        sb.append(mController.getSerial());
        sb.append("<br>");

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
            SDRplayTunerConfiguration config = getConfiguration();

            if(tunerConfiguration.isAssigned())
            {
                setControlsEnabled(true);

                mConfigurationName.setText(config.getName());
                mComboSampleRate.setSelectedItem(config.getSampleRate());
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                
                mComboIfMode.setSelectedItem(config.getIfType());
                mComboBandwidth.setSelectedItem(config.getBwType());
                
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
            SDRplayTunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setSampleRate((Integer)mComboSampleRate.getSelectedItem());
            
            config.setIfType((If_kHzT)mComboIfMode.getSelectedItem());
            config.setBwType((Bw_MHzT)mComboBandwidth.getSelectedItem());
            
            config.setAmplifierEnabled(mAmplifier.isSelected());
            config.setLNAGain((HackRFLNAGain)mComboLNAGain.getSelectedItem());
            config.setVGAGain((HackRFVGAGain)mComboVGAGain.getSelectedItem());

            getTunerConfigurationModel().broadcast(
                new TunerConfigurationEvent(getConfiguration(), TunerConfigurationEvent.Event.CHANGE));
        }
    }
}