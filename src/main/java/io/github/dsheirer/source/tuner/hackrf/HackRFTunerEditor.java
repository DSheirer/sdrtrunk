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
package io.github.dsheirer.source.tuner.hackrf;

import io.github.dsheirer.hackrf.HackRf;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HackRF tuner editor
 */
public class HackRFTunerEditor extends TunerEditor<HackRFTuner,HackRFTunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(HackRFTunerEditor.class);
    private ComboBox<HackRFSampleRate> mSampleRateCombo;
    private CheckBox mAmplifierCheckBox;
    private CheckBox mBiasTCheckBox;
    private ComboBox<HackRFLNAGain> mLnaGainCombo;
    private ComboBox<HackRFVGAGain> mVgaGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable HackRF tuner and controller
     */
    public HackRFTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return HackRFTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return HackRFTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());

        TunerSection gain = addSection("Gain");
        gain.addRow("LNA", getLnaGainCombo());
        gain.addRow("VGA", getVgaGainCombo());
        gain.addControl(getAmplifierCheckBox());

        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
    }

    @Override
    protected void tunerStatusUpdated()
    {
        getSampleRateCombo().setDisable(!hasTuner() || getTuner().getTunerController().isLockedSampleRate());
        updateSampleRateToolTip();
        getAmplifierCheckBox().setDisable(!hasTuner());
        getBiasTCheckBox().setDisable(!hasTuner());
        getLnaGainCombo().setDisable(!hasTuner());
        getVgaGainCombo().setDisable(!hasTuner());

        if(hasConfiguration())
        {
            getSampleRateCombo().setValue(getConfiguration().getSampleRate());
            getAmplifierCheckBox().setSelected(getConfiguration().getAmplifierEnabled());
            getBiasTCheckBox().setSelected(getConfiguration().isBiasT());
            getLnaGainCombo().setValue(getConfiguration().getLNAGain());
            getVgaGainCombo().setValue(getConfiguration().getVGAGain());
        }
    }

    private ComboBox<HackRFVGAGain> getVgaGainCombo()
    {
        if(mVgaGainCombo == null)
        {
            mVgaGainCombo = new ComboBox<>(FXCollections.observableArrayList(HackRFVGAGain.values()));
            mVgaGainCombo.setMaxWidth(Double.MAX_VALUE);
            mVgaGainCombo.setTooltip(new Tooltip("VGA gain.  Adjust to set the baseband gain"));
            mVgaGainCombo.setDisable(true);
            mVgaGainCombo.setOnAction(e -> {
                if(!isLoading() && hasTuner())
                {
                    HackRFVGAGain vgaGain = mVgaGainCombo.getValue() != null ? mVgaGainCombo.getValue() :
                            HackRFVGAGain.GAIN_16;

                    if(applyToTuner("VGA gain", () -> getTuner().getController().setVGAGain(vgaGain)))
                    {
                        save();
                    }
                }
            });
        }

        return mVgaGainCombo;
    }

    private ComboBox<HackRFLNAGain> getLnaGainCombo()
    {
        if(mLnaGainCombo == null)
        {
            mLnaGainCombo = new ComboBox<>(FXCollections.observableArrayList(HackRFLNAGain.values()));
            mLnaGainCombo.setMaxWidth(Double.MAX_VALUE);
            mLnaGainCombo.setTooltip(new Tooltip("LNA gain.  Adjust to set the IF gain"));
            mLnaGainCombo.setDisable(true);
            mLnaGainCombo.setOnAction(e -> {
                if(!isLoading() && hasTuner())
                {
                    HackRFLNAGain lnaGain = mLnaGainCombo.getValue() != null ? mLnaGainCombo.getValue() :
                            HackRFLNAGain.GAIN_16;

                    if(applyToTuner("LNA gain", () -> getTuner().getController().setLNAGain(lnaGain)))
                    {
                        save();
                    }
                }
            });
        }

        return mLnaGainCombo;
    }

    private CheckBox getAmplifierCheckBox()
    {
        if(mAmplifierCheckBox == null)
        {
            mAmplifierCheckBox = new CheckBox("RF amplifier");
            mAmplifierCheckBox.setTooltip(new Tooltip("Enable or disable the RF gain amplifier"));
            mAmplifierCheckBox.setDisable(true);
            mAmplifierCheckBox.setOnAction(e -> {
                if(!isLoading() && hasTuner() && applyToTuner("amplifier",
                        () -> getTuner().getController().setAmplifierEnabled(mAmplifierCheckBox.isSelected())))
                {
                    save();
                }
            });
        }

        return mAmplifierCheckBox;
    }

    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setTooltip(new Tooltip("Enable or disable Bias-T power (3.3V) on the antenna port for " +
                    "active antennas or LNAs"));
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setOnAction(e -> {
                if(!isLoading() && hasTuner() && applyToTuner("Bias-T",
                        () -> getTuner().getController().setBiasTEnabled(mBiasTCheckBox.isSelected())))
                {
                    save();
                }
            });
        }

        return mBiasTCheckBox;
    }

    private ComboBox<HackRFSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>(FXCollections.observableArrayList(HackRFSampleRate.VALID_SAMPLE_RATES));
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setOnAction(e -> {
                HackRFSampleRate sampleRate = mSampleRateCombo.getValue();

                if(!isLoading() && hasTuner() && sampleRate != null &&
                    applyToTuner("sample rate [" + sampleRate.getLabel() + "]",
                        () -> getTuner().getController().setSampleRate(sampleRate)))
                {
                    adjustForSampleRate(sampleRate.getRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getController().isLockedSampleRate())
        {
            getSampleRateCombo().setTooltip(new Tooltip("Sample rate is locked.  Disable decoding channels to unlock."));
        }
        else
        {
            getSampleRateCombo().setTooltip(new Tooltip("Select a sample rate for the tuner"));
        }
    }

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        getSampleRateCombo().setDisable(!hasTuner() || locked);
        updateSampleRateToolTip();
    }

    @Override
    protected String getTunerInfo()
    {
        HackRFTunerController controller = hasTuner() ? getTuner().getController() : null;
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h3>HackRF Tuner</h3>");
        appendInfo(sb, "Board", controller != null ? controller.getBoardName() : null);
        appendInfo(sb, "Serial", controller != null ? controller.getSerialNumber() : null);
        appendInfo(sb, "Part", controller != null ? controller.getPartId() : null);
        appendInfo(sb, "Firmware", controller != null ? controller.getFirmwareVersion() : null);

        try
        {
            appendInfo(sb, "Library", "libhackrf " + HackRf.libraryVersion() + " (" + HackRf.libraryRelease() + ")");
        }
        catch(RuntimeException | LinkageError e)
        {
            mLog.error("couldn't read libhackrf version", e);
        }

        return sb.toString();
    }

    private static void appendInfo(StringBuilder sb, String label, String value)
    {
        sb.append("<b>").append(label).append(": </b>").append(value != null ? value : "Unknown").append("<br>");
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            HackRFTunerConfiguration config = getConfiguration();
            saveCommonSettings(config);
            config.setSampleRate(getSampleRateCombo().getValue());
            config.setAmplifierEnabled(getAmplifierCheckBox().isSelected());
            config.setBiasT(getBiasTCheckBox().isSelected());
            config.setLNAGain(getLnaGainCombo().getValue());
            config.setVGAGain(getVgaGainCombo().getValue());
            saveConfiguration();
        }
    }
}
