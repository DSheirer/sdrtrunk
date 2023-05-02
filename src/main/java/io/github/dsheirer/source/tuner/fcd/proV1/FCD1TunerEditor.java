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
package io.github.dsheirer.source.tuner.fcd.proV1;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.MixerGain;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.text.DecimalFormat;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * Funcube Dongle Pro tuner editor
 */
public class FCD1TunerEditor extends TunerEditor<FCDTuner,FCD1TunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(FCD1TunerEditor.class);
    private JButton mTunerInfoButton;
    private JComboBox<LNAGain> mLnaGainCombo;
    private JComboBox<LNAEnhance> mLnaEnhanceCombo;
    private JComboBox<MixerGain> mMixerGainCombo;
    private CorrectionSpinner mCorrectionDCI;
    private CorrectionSpinner mCorrectionDCQ;
    private CorrectionSpinner mGainCorrectionSpinner;
    private CorrectionSpinner mPhaseCorrectionSpinner;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public FCD1TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private FCD1TunerController getController()
    {
        if(hasTuner())
        {
            return (FCD1TunerController)getTuner().getController();
        }

        return null;
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
        getTunerInfoButton().setEnabled(hasTuner());

        if(hasTuner() && hasConfiguration())
        {
            getMixerGainCombo().setSelectedItem(getConfiguration().getMixerGain());
            getLnaEnhanceCombo().setSelectedItem(getConfiguration().getLNAEnhance());
            getLnaGainCombo().setSelectedItem(getConfiguration().getLNAGain());
            getDcCorrectionSpinnerI().setValue(getConfiguration().getInphaseDCCorrection());
            getDcCorrectionSpinnerQ().setValue(getConfiguration().getQuadratureDCCorrection());
            getPhaseCorrectionSpinner().setValue(getConfiguration().getGainCorrection());
            getGainCorrectionSpinner().setValue(getConfiguration().getPhaseCorrection());
        }

        getMixerGainCombo().setEnabled(hasTuner());
        getLnaEnhanceCombo().setEnabled(hasTuner());
        getLnaGainCombo().setEnabled(hasTuner());
        getDcCorrectionSpinnerI().setEnabled(hasTuner());
        getDcCorrectionSpinnerQ().setEnabled(hasTuner());
        getPhaseCorrectionSpinner().setEnabled(hasTuner());
        getGainCorrectionSpinner().setEnabled(hasTuner());

        setLoading(false);
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Gain"), "span,align center");
        add(new JLabel("LNA:"));
        add(getLnaGainCombo(), "wrap");
        add(new JLabel("Enhance:"));
        add(getLnaEnhanceCombo(), "wrap");
        add(new JLabel("Mixer:"));
        add(getMixerGainCombo(), "wrap");

        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Correction"), "span,align center");
        add(new JLabel("DC Inphase:"));
        add(getDcCorrectionSpinnerI(), "wrap");
        add(new JLabel("DC Quadrature:"));
        add(getDcCorrectionSpinnerQ(), "wrap");
        add(new JLabel("Gain:"));
        add(getGainCorrectionSpinner(), "wrap");
        add(new JLabel("Phase:"));
        add(getPhaseCorrectionSpinner(), "wrap");
    }

    public JComboBox getLnaGainCombo()
    {
        if(mLnaGainCombo == null)
        {
            mLnaGainCombo = new JComboBox<>(LNAGain.values());
            mLnaGainCombo.setEnabled(false);
            mLnaGainCombo.setToolTipText("Adjust the low noise amplifier gain setting.");
            mLnaGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    LNAGain gain = (LNAGain) mLnaGainCombo.getSelectedItem();

                    try
                    {
                        getController().setLNAGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCD Pro Tuner " +
                                "Controller - error setting LNA gain [" + gain + "]");

                        mLog.error("FuncubeDonglePro Controller - error setting gain [" + gain + "]", e);
                    }
                }
            });
        }

        return mLnaGainCombo;
    }

    public JComboBox getLnaEnhanceCombo()
    {
        if(mLnaEnhanceCombo == null)
        {
            mLnaEnhanceCombo = new JComboBox<>(LNAEnhance.values());
            mLnaEnhanceCombo.setEnabled(false);
            mLnaEnhanceCombo.setToolTipText("Adjust the LNA enhance setting.  Default value is OFF");
            mLnaEnhanceCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    LNAEnhance enhance = (LNAEnhance) mLnaEnhanceCombo.getSelectedItem();

                    try
                    {
                        getController().setLNAEnhance(enhance);
                        save();
                    }
                    catch(Exception e1)
                    {
                        JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCD Pro Tuner"
                                + " error setting LNA enhance gain [" + enhance + "]");

                        mLog.error("FCDPro - error setting LNA enhance  [" + enhance + "]", e1);
                    }
                }
            });
        }

        return mLnaEnhanceCombo;
    }

    public JComboBox getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new JComboBox<>(MixerGain.values());
            mMixerGainCombo.setEnabled(false);
            mMixerGainCombo.setToolTipText("Adjust mixer gain setting");
            mMixerGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    MixerGain gain = (MixerGain) mMixerGainCombo.getSelectedItem();

                    try
                    {
                        getController().setMixerGain(gain);
                        save();
                    }
                    catch(Exception e1)
                    {
                        JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCDPro - error setting"
                                + " mixer gain [" + gain + "]");

                        mLog.error("FCDPro - error setting mixer gain [" + gain + "]", e1);
                    }
                }
            });
        }

        return mMixerGainCombo;
    }

    public CorrectionSpinner getDcCorrectionSpinnerQ()
    {
        if(mCorrectionDCQ == null)
        {
            mCorrectionDCQ = new CorrectionSpinner(Correction.DC_QUADRATURE, 0.0, 0.00001, 5);
            mCorrectionDCQ.setEnabled(false);
            mCorrectionDCQ.setToolTipText("DC Bias Correction/Quadrature Component: valid values are -1.0 to 1.0 (default: 0.0)");
        }

        return mCorrectionDCQ;
    }

    public CorrectionSpinner getDcCorrectionSpinnerI()
    {
        if(mCorrectionDCI == null)
        {
            mCorrectionDCI = new CorrectionSpinner(Correction.DC_INPHASE, 0.0, 0.00001, 5);
            mCorrectionDCI.setEnabled(false);
            mCorrectionDCI.setToolTipText("DC Bias Correction/Inphase Component: valid values are -1.0 to 1.0 (default: 0.0)");
        }

        return mCorrectionDCI;
    }

    public CorrectionSpinner getGainCorrectionSpinner()
    {
        if(mGainCorrectionSpinner == null)
        {
            mGainCorrectionSpinner = new CorrectionSpinner(Correction.GAIN, 0.0, 0.00001, 5);
            mGainCorrectionSpinner.setEnabled(false);
            mGainCorrectionSpinner.setToolTipText("Gain Correction: valid values are -1.0 to 1.0 (default: 0.0)");
        }

        return mGainCorrectionSpinner;
    }

    public CorrectionSpinner getPhaseCorrectionSpinner()
    {
        if(mPhaseCorrectionSpinner == null)
        {
            mPhaseCorrectionSpinner = new CorrectionSpinner(Correction.PHASE, 0.0, 0.00001, 5);
            mPhaseCorrectionSpinner.setEnabled(false);
            mPhaseCorrectionSpinner.setToolTipText("Phase Correction: valid values are -1.0 to 1.0 (default: 0.0)");
        }

        return mPhaseCorrectionSpinner;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

    public JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Tuner Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(FCD1TunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>Funcube Dongle Pro Tuner</h3>");

        if(hasTuner())
        {
            sb.append("<b>USB ID: </b>");
            sb.append(getController().getUSBID());
            sb.append("<br>");

            sb.append("<b>USB Address: </b>");
            sb.append(getController().getUSBAddress());
            sb.append("<br>");

            sb.append("<b>USB Speed: </b>");
            sb.append(getController().getUSBSpeed());
            sb.append("<br>");

            sb.append("<b>Cellular Band: </b>");
            sb.append(getController().getConfiguration().getBandBlocking());
            sb.append("<br>");

            sb.append("<b>Firmware: </b>");
            sb.append(getController().getConfiguration().getFirmware());
            sb.append("<br>");
        }

        return sb.toString();
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
            getConfiguration().setLNAEnhance((LNAEnhance) mLnaEnhanceCombo.getSelectedItem());
            getConfiguration().setLNAGain((LNAGain) mLnaGainCombo.getSelectedItem());
            getConfiguration().setMixerGain((MixerGain) mMixerGainCombo.getSelectedItem());
            double dci = ((SpinnerNumberModel)mCorrectionDCI.getModel()).getNumber().doubleValue();
            getConfiguration().setInphaseDCCorrection(dci);
            double dcq = ((SpinnerNumberModel)mCorrectionDCQ.getModel()).getNumber().doubleValue();
            getConfiguration().setQuadratureDCCorrection(dcq);
            double gain = ((SpinnerNumberModel) mGainCorrectionSpinner.getModel()).getNumber().doubleValue();
            getConfiguration().setGainCorrection(gain);
            double phase = ((SpinnerNumberModel) mPhaseCorrectionSpinner.getModel()).getNumber().doubleValue();
            getConfiguration().setPhaseCorrection(phase);
            saveConfiguration();
        }
    }

    public enum Correction {GAIN, PHASE, DC_INPHASE, DC_QUADRATURE};

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

            addChangeListener(e ->
            {
                if(!isLoading())
                {
                    double value = ((SpinnerNumberModel)getModel()).getNumber().doubleValue();

                    try
                    {
                        switch(mCorrectionComponent)
                        {
                            case DC_INPHASE:
                                getController().setDCCorrectionInPhase(value);
                                break;
                            case DC_QUADRATURE:
                                getController().setDCCorrectionQuadrature(value);
                                break;
                            case GAIN:
                                getController().setGainCorrection(value);
                                break;
                            case PHASE:
                                getController().setPhaseCorrection(value);
                                break;
                        }

                        save();
                    }
                    catch(Exception e1)
                    {
                        JOptionPane.showMessageDialog(FCD1TunerEditor.this, "FCDPro - error "
                                + "applying " + mCorrectionComponent.toString() + " correction value [" + value + "]");

                        mLog.error("FCDPro - error applying " + mCorrectionComponent.toString() + " correction value [" +
                                value + "]", e1);
                    }
                }
            });
        }
    }
}