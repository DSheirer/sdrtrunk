/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.source.tuner.fcd.proV1;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent.Event;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.MixerGain;
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

public class FCD1TunerEditor extends TunerConfigurationEditor
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(FCD1TunerEditor.class);

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JSpinner mFrequencyCorrection;
    private JComboBox<LNAGain> mComboLNAGain;
    private JComboBox<LNAEnhance> mComboLNAEnhance;
    private JComboBox<MixerGain> mComboMixerGain;
    private CorrectionSpinner mCorrectionDCI;
    private CorrectionSpinner mCorrectionDCQ;
    private CorrectionSpinner mCorrectionGain;
    private CorrectionSpinner mCorrectionPhase;
    private boolean mLoading;

    private FCD1TunerController mController;

    public FCD1TunerEditor(TunerConfigurationModel tunerConfigurationModel, FCDTuner tuner)
    {
        super(tunerConfigurationModel);
        mController = (FCD1TunerController)tuner.getController();

        init();
    }

    private FCD1TunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (FCD1TunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
            "[][][][][][][][][][][][grow]"));

        add(new JLabel("FCD Pro Tuner Configuration"), "span,align center");

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
                JOptionPane.showMessageDialog(FCD1TunerEditor.this, getTunerInfo(),
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
                    JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCD Pro Tuner "
                        + "Controller - couldn't apply frequency correction value: " + value +
                        e1.getLocalizedMessage());

                    mLog.error("FuncubeDonglePro Controller - couldn't apply "
                        + "frequency correction value: " + value, e1);
                }
            }
        });

        add(new JLabel("PPM:"));
        add(mFrequencyCorrection, "wrap");

        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");
        add(new JLabel("Gain"), "span,align center");

        mComboLNAGain = new JComboBox<LNAGain>(LNAGain.values());
        mComboLNAGain.setEnabled(false);
        mComboLNAGain.setToolTipText("Adjust the low noise amplifier gain setting.");
        mComboLNAGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                LNAGain gain = (LNAGain)mComboLNAGain.getSelectedItem();


                try
                {
                    mController.setLNAGain(gain);
                    save();
                }
                catch(Exception e)
                {
                    JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCD Pro Tuner Controller"
                        + " - error setting LNA gain [" + gain.toString() + "]");

                    mLog.error("FuncubeDonglePro Controller - error setting gain [" + gain.toString() + "]", e);
                }
            }
        });

        add(new JLabel("LNA:"));
        add(mComboLNAGain);

        /**
         * LNA Enhance
         */
        mComboLNAEnhance = new JComboBox<LNAEnhance>(LNAEnhance.values());
        mComboLNAEnhance.setToolTipText("Adjust the LNA enhance setting.  Default value is OFF");

        mComboLNAEnhance.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                LNAEnhance enhance = (LNAEnhance)mComboLNAEnhance.getSelectedItem();

                try
                {
                    mController.setLNAEnhance(enhance);
                    save();
                }
                catch(Exception e1)
                {
                    JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCD Pro Tuner"
                        + " error setting LNA enhance gain [" + enhance.toString() + "]");

                    mLog.error("FCDPro - error setting LNA enhance  [" + enhance.toString() + "]", e1);
                }
            }
        });

        add(new JLabel("Enhance:"));
        add(mComboLNAEnhance);

        /**
         * Mixer Gain
         */
        mComboMixerGain = new JComboBox<MixerGain>(MixerGain.values());
        mComboMixerGain.setToolTipText("Adjust mixer gain setting");


        mComboMixerGain.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                MixerGain gain = (MixerGain)mComboMixerGain.getSelectedItem();

                try
                {
                    mController.setMixerGain(gain);
                    save();
                }
                catch(Exception e1)
                {
                    JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCDPro - error setting"
                        + " mixer gain [" + gain.toString() + "]");

                    mLog.error("FCDPro - error setting mixer gain [" + gain.toString() + "]", e1);
                }
            }
        });

        add(new JLabel("Mixer:"));
        add(mComboMixerGain, "wrap");

        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");
        add(new JLabel("Correction"), "span,align center");

        /**
         * Inphase DC Correction
         */
        mCorrectionDCI = new CorrectionSpinner(Correction.DC_INPHASE, 0.0, 0.00001, 5);
        mCorrectionDCI.setEnabled(false);
        mCorrectionDCI.setToolTipText("DC Bias Correction/Inphase "
            + "Component: valid values are -1.0 to 1.0 (default: 0.0)");
        add(new JLabel("DC Inphase:"));
        add(mCorrectionDCI);

        /**
         * Quadrature DC Correction
         */
        mCorrectionDCQ = new CorrectionSpinner(Correction.DC_QUADRATURE, 0.0, 0.00001, 5);
        mCorrectionDCQ.setToolTipText("DC Bias Correction/Quadrature "
            + "Component: valid values are -1.0 to 1.0 (default: 0.0)");
        add(new JLabel("DC Quadrature:"));
        add(mCorrectionDCQ);

        /**
         * Gain Correction
         */
        mCorrectionGain = new CorrectionSpinner(Correction.GAIN, 0.0, 0.00001, 5);
        mCorrectionGain.setToolTipText("Gain Correction: valid values are "
            + "-1.0 to 1.0 (default: 0.0)");
        add(new JLabel("Gain:"));
        add(mCorrectionGain);

        /**
         * Phase Correction
         */
        mCorrectionPhase = new CorrectionSpinner(Correction.PHASE, 0.0, 0.00001, 5);
        mCorrectionPhase.setToolTipText("Phase Correction: valid values are "
            + "-1.0 to 1.0 (default: 0.0)");
        add(new JLabel("Phase:"));
        add(mCorrectionPhase);
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

        if(mComboLNAEnhance.isEnabled() != enabled)
        {
            mComboLNAEnhance.setEnabled(enabled);
        }

        if(mComboLNAGain.isEnabled() != enabled)
        {
            mComboLNAGain.setEnabled(enabled);
        }

        if(mComboMixerGain.isEnabled() != enabled)
        {
            mComboMixerGain.setEnabled(enabled);
        }

        if(mCorrectionDCI.isEnabled() != enabled)
        {
            mCorrectionDCI.setEnabled(enabled);
        }

        if(mCorrectionDCQ.isEnabled() != enabled)
        {
            mCorrectionDCQ.setEnabled(enabled);
        }

        if(mCorrectionGain.isEnabled() != enabled)
        {
            mCorrectionGain.setEnabled(enabled);
        }

        if(mCorrectionPhase.isEnabled() != enabled)
        {
            mCorrectionPhase.setEnabled(enabled);
        }
    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>Funcube Dongle Pro Tuner</h3>");

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
            FCD1TunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setLNAEnhance((LNAEnhance)mComboLNAEnhance.getSelectedItem());
            config.setLNAGain((LNAGain)mComboLNAGain.getSelectedItem());
            config.setMixerGain((MixerGain)mComboMixerGain.getSelectedItem());

            double dci = ((SpinnerNumberModel)mCorrectionDCI.getModel()).getNumber().doubleValue();
            config.setInphaseDCCorrection(dci);

            double dcq = ((SpinnerNumberModel)mCorrectionDCQ.getModel()).getNumber().doubleValue();
            config.setQuadratureDCCorrection(dcq);

            double gain = ((SpinnerNumberModel)mCorrectionGain.getModel()).getNumber().doubleValue();
            config.setGainCorrection(gain);

            double phase = ((SpinnerNumberModel)mCorrectionPhase.getModel()).getNumber().doubleValue();
            config.setPhaseCorrection(phase);

            getTunerConfigurationModel().broadcast(new TunerConfigurationEvent(getConfiguration(), Event.CHANGE));
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
            FCD1TunerConfiguration config = getConfiguration();

            if(tunerConfiguration.isAssigned())
            {
                setControlsEnabled(true);
                mConfigurationName.setText(config.getName());
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                mComboLNAEnhance.setSelectedItem(config.getLNAEnhance());
                mComboLNAGain.setSelectedItem(config.getLNAGain());
                mComboMixerGain.setSelectedItem(config.getMixerGain());
                mCorrectionDCI.setValue(config.getInphaseDCCorrection());
                mCorrectionDCQ.setValue(config.getQuadratureDCCorrection());
                mCorrectionGain.setValue(config.getGainCorrection());
                mCorrectionPhase.setValue(config.getPhaseCorrection());
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

    public enum Correction
    {
        GAIN, PHASE, DC_INPHASE, DC_QUADRATURE
    }

    ;

    public class CorrectionSpinner extends JSpinner
    {
        private static final long serialVersionUID = 1L;
        private static final double MIN_VALUE = -1.0d;
        private static final double MAX_VALUE = 1.0d;

        private Correction mCorrectionComponent;

        public CorrectionSpinner(Correction component, double initialValue, double step, int decimalPlaces)
        {
            mCorrectionComponent = component;

            SpinnerModel model = new SpinnerNumberModel(initialValue, MIN_VALUE, MAX_VALUE, step);
            setModel(model);

            JSpinner.NumberEditor editor = (JSpinner.NumberEditor)getEditor();

            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(decimalPlaces);

            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);

            addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    double value =
                        ((SpinnerNumberModel)getModel()).getNumber().doubleValue();

                    try
                    {
                        switch(mCorrectionComponent)
                        {
                            case DC_INPHASE:
                                mController.setDCCorrectionInPhase(value);
                                break;
                            case DC_QUADRATURE:
                                mController.setDCCorrectionQuadrature(value);
                                break;
                            case GAIN:
                                mController.setGainCorrection(value);
                                break;
                            case PHASE:
                                mController.setPhaseCorrection(value);
                                break;
                        }

                        save();
                    }
                    catch(Exception e1)
                    {
                        JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCDPro - error "
                            + "applying " + mCorrectionComponent.toString() + " correction value ["
                            + value + "]");

                        mLog.error("FCDPro - error applying " + mCorrectionComponent.toString()
                            + " correction value [" + value + "]", e1);
                    }
                }
            });
        }
    }
}