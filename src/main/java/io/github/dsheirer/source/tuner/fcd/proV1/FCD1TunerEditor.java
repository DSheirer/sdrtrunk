/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.MixerGain;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/**
 * Funcube Dongle Pro tuner editor
 */
public class FCD1TunerEditor extends TunerEditor<FCDTuner,FCD1TunerConfiguration>
{
    private ComboBox<LNAGain> mLnaGainCombo;
    private ComboBox<LNAEnhance> mLnaEnhanceCombo;
    private ComboBox<MixerGain> mMixerGainCombo;
    private CheckBox mBiasTCheckBox;
    private CorrectionSpinner mCorrectionDCI;
    private CorrectionSpinner mCorrectionDCQ;
    private CorrectionSpinner mGainCorrectionSpinner;
    private CorrectionSpinner mPhaseCorrectionSpinner;

    /**
     * Constructs an instance
     * @param settings for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable tuner and controller
     */
    public FCD1TunerEditor(Settings settings, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        init();
        refresh();
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
    public long getMinimumTunableFrequency()
    {
        return FCD1TunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return FCD1TunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        TunerSection gain = addSection("Gain");
        gain.addRow("LNA", getLnaGainCombo());
        gain.addRow("LNA Enhance", getLnaEnhanceCombo());
        gain.addRow("Mixer", getMixerGainCombo());

        TunerSection correction = addSection("I/Q Correction");
        correction.addRow("DC Inphase", getDcCorrectionSpinnerI());
        correction.addRow("DC Quadrature", getDcCorrectionSpinnerQ());
        correction.addRow("Gain", getGainCorrectionSpinner());
        correction.addRow("Phase", getPhaseCorrectionSpinner());

        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
    }

    @Override
    protected void tunerStatusUpdated()
    {
        if(hasTuner() && hasConfiguration())
        {
            FCD1TunerConfiguration config = getConfiguration();
            getMixerGainCombo().setValue(config.getMixerGain());
            getLnaEnhanceCombo().setValue(config.getLNAEnhance());
            getLnaGainCombo().setValue(config.getLNAGain());
            getBiasTCheckBox().setSelected(config.isBiasT());
            getDcCorrectionSpinnerI().setCorrection(config.getInphaseDCCorrection());
            getDcCorrectionSpinnerQ().setCorrection(config.getQuadratureDCCorrection());
            getGainCorrectionSpinner().setCorrection(config.getGainCorrection());
            getPhaseCorrectionSpinner().setCorrection(config.getPhaseCorrection());
        }

        getMixerGainCombo().setDisable(!hasTuner());
        getLnaEnhanceCombo().setDisable(!hasTuner());
        getLnaGainCombo().setDisable(!hasTuner());
        getBiasTCheckBox().setDisable(!hasTuner());
        getDcCorrectionSpinnerI().setDisable(!hasTuner());
        getDcCorrectionSpinnerQ().setDisable(!hasTuner());
        getPhaseCorrectionSpinner().setDisable(!hasTuner());
        getGainCorrectionSpinner().setDisable(!hasTuner());
    }

    private ComboBox<LNAGain> getLnaGainCombo()
    {
        if(mLnaGainCombo == null)
        {
            mLnaGainCombo = new ComboBox<>(FXCollections.observableArrayList(LNAGain.values()));
            mLnaGainCombo.setMaxWidth(Double.MAX_VALUE);
            mLnaGainCombo.setDisable(true);
            mLnaGainCombo.setTooltip(new Tooltip("Adjust the low noise amplifier gain setting."));
            mLnaGainCombo.setOnAction(e -> {
                LNAGain gain = mLnaGainCombo.getValue();

                if(!isLoading() && hasTuner() && gain != null &&
                    applyToTuner("LNA gain [" + gain + "]", () -> getController().setLNAGain(gain)))
                {
                    save();
                }
            });
        }

        return mLnaGainCombo;
    }

    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setTooltip(new Tooltip("Enable Bias-T power output for active antennas"));
            mBiasTCheckBox.setOnAction(event -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("Bias-T", () -> getController().setBiasT(mBiasTCheckBox.isSelected())))
                {
                    save();
                }
            });
        }

        return mBiasTCheckBox;
    }

    private ComboBox<LNAEnhance> getLnaEnhanceCombo()
    {
        if(mLnaEnhanceCombo == null)
        {
            mLnaEnhanceCombo = new ComboBox<>(FXCollections.observableArrayList(LNAEnhance.values()));
            mLnaEnhanceCombo.setMaxWidth(Double.MAX_VALUE);
            mLnaEnhanceCombo.setDisable(true);
            mLnaEnhanceCombo.setTooltip(new Tooltip("Adjust the LNA enhance setting.  Default value is OFF"));
            mLnaEnhanceCombo.setOnAction(e -> {
                LNAEnhance enhance = mLnaEnhanceCombo.getValue();

                if(!isLoading() && hasTuner() && enhance != null &&
                    applyToTuner("LNA enhance [" + enhance + "]", () -> getController().setLNAEnhance(enhance)))
                {
                    save();
                }
            });
        }

        return mLnaEnhanceCombo;
    }

    private ComboBox<MixerGain> getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new ComboBox<>(FXCollections.observableArrayList(MixerGain.values()));
            mMixerGainCombo.setMaxWidth(Double.MAX_VALUE);
            mMixerGainCombo.setDisable(true);
            mMixerGainCombo.setTooltip(new Tooltip("Adjust mixer gain setting"));
            mMixerGainCombo.setOnAction(e -> {
                MixerGain gain = mMixerGainCombo.getValue();

                if(!isLoading() && hasTuner() && gain != null &&
                    applyToTuner("mixer gain [" + gain + "]", () -> getController().setMixerGain(gain)))
                {
                    save();
                }
            });
        }

        return mMixerGainCombo;
    }

    private CorrectionSpinner getDcCorrectionSpinnerQ()
    {
        if(mCorrectionDCQ == null)
        {
            mCorrectionDCQ = new CorrectionSpinner(Correction.DC_QUADRATURE);
            mCorrectionDCQ.setTooltip(new Tooltip("DC bias correction, quadrature component: -1.0 to 1.0 (default: 0.0)"));
        }

        return mCorrectionDCQ;
    }

    private CorrectionSpinner getDcCorrectionSpinnerI()
    {
        if(mCorrectionDCI == null)
        {
            mCorrectionDCI = new CorrectionSpinner(Correction.DC_INPHASE);
            mCorrectionDCI.setTooltip(new Tooltip("DC bias correction, inphase component: -1.0 to 1.0 (default: 0.0)"));
        }

        return mCorrectionDCI;
    }

    private CorrectionSpinner getGainCorrectionSpinner()
    {
        if(mGainCorrectionSpinner == null)
        {
            mGainCorrectionSpinner = new CorrectionSpinner(Correction.GAIN);
            mGainCorrectionSpinner.setTooltip(new Tooltip("Gain correction: -1.0 to 1.0 (default: 0.0)"));
        }

        return mGainCorrectionSpinner;
    }

    private CorrectionSpinner getPhaseCorrectionSpinner()
    {
        if(mPhaseCorrectionSpinner == null)
        {
            mPhaseCorrectionSpinner = new CorrectionSpinner(Correction.PHASE);
            mPhaseCorrectionSpinner.setTooltip(new Tooltip("Phase correction: -1.0 to 1.0 (default: 0.0)"));
        }

        return mPhaseCorrectionSpinner;
    }

    @Override
    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h3>Funcube Dongle Pro Tuner</h3>");
        sb.append("<b>USB ID: </b>").append(getController().getUSBID()).append("<br>");
        sb.append("<b>USB Address: </b>").append(getController().getUSBAddress()).append("<br>");
        sb.append("<b>USB Speed: </b>").append(getController().getUSBSpeed()).append("<br>");
        sb.append("<b>Cellular Band: </b>").append(getController().getConfiguration().getBandBlocking()).append("<br>");
        sb.append("<b>Firmware: </b>").append(getController().getConfiguration().getFirmware()).append("<br>");
        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            FCD1TunerConfiguration config = getConfiguration();
            saveCommonSettings(config);
            config.setLNAEnhance(getLnaEnhanceCombo().getValue());
            config.setLNAGain(getLnaGainCombo().getValue());
            config.setMixerGain(getMixerGainCombo().getValue());
            config.setBiasT(getBiasTCheckBox().isSelected());
            config.setInphaseDCCorrection(getDcCorrectionSpinnerI().getCorrection());
            config.setQuadratureDCCorrection(getDcCorrectionSpinnerQ().getCorrection());
            config.setGainCorrection(getGainCorrectionSpinner().getCorrection());
            config.setPhaseCorrection(getPhaseCorrectionSpinner().getCorrection());
            saveConfiguration();
        }
    }

    public enum Correction {GAIN, PHASE, DC_INPHASE, DC_QUADRATURE}

    /**
     * Spinner for a correction value in the range -1.0 to 1.0 with five decimal places.
     */
    public class CorrectionSpinner extends Spinner<Double>
    {
        private static final double MIN_VALUE = -1.0d;
        private static final double MAX_VALUE = 1.0d;
        private final Correction mCorrectionComponent;

        public CorrectionSpinner(Correction component)
        {
            mCorrectionComponent = component;
            SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(MIN_VALUE, MAX_VALUE, 0.0, 0.00001);
            factory.setConverter(new StringConverter<>()
            {
                @Override
                public String toString(Double value)
                {
                    return value == null ? "" : String.format("%.5f", value);
                }

                @Override
                public Double fromString(String text)
                {
                    try
                    {
                        return Double.parseDouble(text.trim().replace(',', '.'));
                    }
                    catch(Exception e)
                    {
                        return factory.getValue();
                    }
                }
            });
            setValueFactory(factory);
            setEditable(true);
            setDisable(true);
            setMaxWidth(Double.MAX_VALUE);
            focusedProperty().addListener((observable, wasFocused, focused) -> {
                if(!focused)
                {
                    increment(0);
                }
            });
            valueProperty().addListener((observable, oldValue, value) -> {
                if(!isLoading() && hasTuner() && value != null &&
                    applyToTuner(mCorrectionComponent + " correction [" + value + "]", () -> apply(value)))
                {
                    save();
                }
            });
        }

        private void apply(double value) throws Exception
        {
            switch(mCorrectionComponent)
            {
                case DC_INPHASE -> getController().setDCCorrectionInPhase(value);
                case DC_QUADRATURE -> getController().setDCCorrectionQuadrature(value);
                case GAIN -> getController().setGainCorrection(value);
                case PHASE -> getController().setPhaseCorrection(value);
            }
        }

        public void setCorrection(double value)
        {
            getValueFactory().setValue(value);
        }

        public double getCorrection()
        {
            return getValue() != null ? getValue() : 0.0;
        }
    }
}
