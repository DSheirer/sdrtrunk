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

package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2AntennaSelection;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;

/**
 * RSP2 Tuner Editor
 */
public class Rsp2TunerEditor extends RspTunerEditor<Rsp2TunerConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(Rsp2TunerEditor.class);

    private JComboBox<RspSampleRate> mSampleRateCombo;
    private JCheckBox mBiasTCheckBox;
    private JCheckBox mExternalReferenceOutputCheckBox;
    private JCheckBox mRfNotchCheckBox;
    private JComboBox<Rsp2AntennaSelection> mAntennaSelectionCombo;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for state updates
     * @param discoveredTuner to edit or control.
     */
    public Rsp2TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel(), "wrap");

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JLabel("IF AGC Mode:"));
        JPanel gainPanel = new JPanel();
        gainPanel.setLayout(new MigLayout("insets 0","[grow,fill][]",""));
        gainPanel.add(getAgcModeCombo());
        gainPanel.add(getGainOverloadButton());
        add(gainPanel, "wrap");

        add(new JLabel("Gain:"));
        add(getGainSlider());
        add(getGainValueLabel());

        add(new JSeparator(), "span,growx,push");

        add(new JLabel());
        add(getBiasTCheckBox(), "wrap");
        add(new JLabel());
        add(getExternalReferenceOutputCheckBox(), "wrap");
        add(new JLabel());
        add(getRfNotchCheckBox(), "wrap");
        add(new JLabel());
        add(getAntennaSelectionCombo(), "wrap");
    }

    /**
     * Access tuner controller
     */
    private Rsp2TunerController getTunerController()
    {
        if(hasTuner())
        {
            return (Rsp2TunerController) getTuner().getTunerController();
        }

        return null;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setEnabled(!locked);
        updateSampleRateToolTip();
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        getTunerIdLabel().setText(getDiscoveredTuner().getId());

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        getSampleRateCombo().setEnabled(hasTuner() && !getTuner().getTunerController().isLockedSampleRate());
        getSampleRateCombo().setSelectedItem(hasTuner() ? getTunerController().getControlRsp().getSampleRateEnumeration() : null);
        updateSampleRateToolTip();

        getAgcModeCombo().setEnabled(hasTuner());
        if(hasTuner())
        {
            getAgcModeCombo().setSelectedItem(getTunerController().getControlRsp().getAgcMode());
            //Register to receive gain overload notifications
            getTunerController().getControlRsp().setGainOverloadListener(this);
        }

        getGainSlider().setEnabled(hasTuner());
        getGainValueLabel().setEnabled(hasTuner());
        getGainSlider().setValue(hasTuner() ? getTunerController().getControlRsp().getGain() : 0);

        getBiasTCheckBox().setEnabled(hasTuner());
        try
        {
            getBiasTCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isBiasT());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting Bias-T enabled state in editor");
        }

        getExternalReferenceOutputCheckBox().setEnabled(hasTuner());
        try
        {
            getExternalReferenceOutputCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isExternalReferenceOutput());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RF DAB Notch enabled state in editor");
        }

        getRfNotchCheckBox().setEnabled(hasTuner());
        try
        {
            getRfNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RF Notch enabled state in editor");
        }

        getAntennaSelectionCombo().setEnabled(hasTuner());
        try
        {
            getAntennaSelectionCombo().setSelectedItem(hasTuner() ? getTunerController().getControlRsp().getAntennaSelection() : null);
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting antenna selection in editor");
        }

        setLoading(false);
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
            getConfiguration().setSampleRate((RspSampleRate)getSampleRateCombo().getSelectedItem());
            getConfiguration().setBiasT(getBiasTCheckBox().isSelected());
            getConfiguration().setRfNotch(getRfNotchCheckBox().isSelected());
            getConfiguration().setAntennaSelection((Rsp2AntennaSelection)getAntennaSelectionCombo().getSelectedItem());
            getConfiguration().setGain(getGainSlider().getValue());
            getConfiguration().setAgcMode((AgcMode)getAgcModeCombo().getSelectedItem());

            saveConfiguration();
        }
    }

    /**
     * Sample rate selection combobox control
     */
    private JComboBox<RspSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            RspSampleRate[] rspSampleRates = RspSampleRate.SINGLE_TUNER_SAMPLE_RATES.toArray(new RspSampleRate[RspSampleRate.SINGLE_TUNER_SAMPLE_RATES.size()]);
            mSampleRateCombo = new JComboBox<>(rspSampleRates);
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    RspSampleRate selected = (RspSampleRate)mSampleRateCombo.getSelectedItem();

                    try
                    {
                        getTunerController().setSampleRate(selected);
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting sample rate for RSP2 tuner", se);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Checkbox control for Bias-T
     */
    private JCheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new JCheckBox("ANT B Bias-T Power");
            mBiasTCheckBox.setEnabled(false);
            mBiasTCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setBiasT(mBiasTCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSP2 Bias-T enabled to " + mBiasTCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mBiasTCheckBox;
    }

    /**
     * Checkbox control for RF notch
     */
    private JCheckBox getRfNotchCheckBox()
    {
        if(mRfNotchCheckBox == null)
        {
            mRfNotchCheckBox = new JCheckBox("FM Broadcast Band Filter (77-115 MHz)");
            mRfNotchCheckBox.setEnabled(false);
            mRfNotchCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setRfNotch(mRfNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSP2 RF notch enabled to " + mRfNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfNotchCheckBox;
    }

    /**
     * Checkbox control for External Reference Output
     */
    private JCheckBox getExternalReferenceOutputCheckBox()
    {
        if(mExternalReferenceOutputCheckBox == null)
        {
            mExternalReferenceOutputCheckBox = new JCheckBox("External Reference Output");
            mExternalReferenceOutputCheckBox.setEnabled(false);
            mExternalReferenceOutputCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setExternalReferenceOutput(mExternalReferenceOutputCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSP2 external reference output notch enabled to " +
                                mExternalReferenceOutputCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mExternalReferenceOutputCheckBox;
    }

    /**
     * Antenna selection combobox control
     */
    private JComboBox<Rsp2AntennaSelection> getAntennaSelectionCombo()
    {
        if(mAntennaSelectionCombo == null)
        {
            mAntennaSelectionCombo = new JComboBox<>(Rsp2AntennaSelection.values());
            mAntennaSelectionCombo.setEnabled(false);
            mAntennaSelectionCombo.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    Rsp2AntennaSelection selected = (Rsp2AntennaSelection)mAntennaSelectionCombo.getSelectedItem();

                    try
                    {
                        getTunerController().getControlRsp().setAntennaSelection(selected);
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting Antenna selection for RSP2", se);
                    }
                }
            });
        }

        return mAntennaSelectionCombo;
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
}
