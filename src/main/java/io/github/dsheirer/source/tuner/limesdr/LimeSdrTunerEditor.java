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
package io.github.dsheirer.source.tuner.limesdr;

import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.GainSlider;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;

/**
 * LimeSDR tuner editor
 */
public class LimeSdrTunerEditor extends TunerEditor<LimeSdrTuner, LimeSdrTunerConfiguration>
{
    private ComboBox<LimeSdrSampleRate> mSampleRateCombo;
    private ComboBox<String> mAntennaCombo;
    private GainSlider mGainSlider;

    /**
     * Constructs an instance
     * @param settings for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable LimeSDR tuner and controller
     */
    public LimeSdrTunerEditor(Settings settings, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        init();
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMinimumTunableFrequency() :
                LimeSdrTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMaximumTunableFrequency() :
                LimeSdrTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());
        getTuningSection().addRow("Antenna", getAntennaCombo());

        TunerSection gain = addSection("Gain");
        gain.addRow("Gain", getGainSlider());
    }

    @Override
    protected void tunerStatusUpdated()
    {
        if(hasStartedTuner())
        {
            LimeSdrTunerController controller = getTuner().getController();
            getSampleRateCombo().getItems().setAll(controller.getSampleRates());
            getSampleRateCombo().setValue(controller.getLimeSdrSampleRate());
            getSampleRateCombo().setDisable(controller.isLockedSampleRate());
            getAntennaCombo().getItems().setAll(controller.getAntennas());
            getAntennaCombo().setValue(controller.getAntenna());
            getAntennaCombo().setDisable(false);
            getGainSlider().setValue(controller.getGain());
            getGainSlider().setDisable(false);
        }
        else
        {
            getSampleRateCombo().getItems().clear();
            getSampleRateCombo().setDisable(true);
            getAntennaCombo().getItems().clear();
            getAntennaCombo().setDisable(true);
            getGainSlider().setDisable(true);
        }
    }

    /**
     * Indicates if the tuner is started, which is when the controller knows the selected antenna.
     */
    private boolean hasStartedTuner()
    {
        return hasTuner() && getTuner().getController().getAntenna() != null;
    }

    private ComboBox<LimeSdrSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setTooltip(new Tooltip("Select a sample rate for the tuner"));
            mSampleRateCombo.setOnAction(e -> {
                LimeSdrSampleRate rate = mSampleRateCombo.getValue();

                if(hasTuner() && !isLoading() && rate != null &&
                    applyToTuner("sample rate [" + rate + "]", () -> getTuner().getController().setSampleRate(rate)))
                {
                    adjustForSampleRate(rate.getRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    private ComboBox<String> getAntennaCombo()
    {
        if(mAntennaCombo == null)
        {
            mAntennaCombo = new ComboBox<>();
            mAntennaCombo.setMaxWidth(Double.MAX_VALUE);
            mAntennaCombo.setDisable(true);
            mAntennaCombo.setTooltip(new Tooltip("Receive port: LNAW wideband (default), LNAL low band, LNAH high band"));
            mAntennaCombo.setOnAction(e -> {
                String antenna = mAntennaCombo.getValue();

                if(hasTuner() && !isLoading() && antenna != null &&
                    applyToTuner("antenna [" + antenna + "]", () -> getTuner().getController().setAntenna(antenna)))
                {
                    save();
                }
            });
        }

        return mAntennaCombo;
    }

    private GainSlider getGainSlider()
    {
        if(mGainSlider == null)
        {
            mGainSlider = new GainSlider(LimeSdrTunerController.GAIN_MINIMUM, LimeSdrTunerController.GAIN_MAXIMUM,
                    LimeSdrTunerController.GAIN_DEFAULT);
            mGainSlider.setDisable(true);
            mGainSlider.setFormatter(value -> value + " dB");
            mGainSlider.setOnChange(gain -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("gain", () -> getTuner().getController().setGain(gain)))
                {
                    save();
                }
            });
        }

        return mGainSlider;
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            LimeSdrTunerConfiguration config = getConfiguration();
            saveCommonSettings(config);

            if(getSampleRateCombo().getValue() instanceof LimeSdrSampleRate rate)
            {
                config.setSampleRate(rate.getRate());
            }

            if(getAntennaCombo().getValue() instanceof String antenna)
            {
                config.setAntenna(antenna);
            }

            config.setGain(getGainSlider().getValue());
            saveConfiguration();
        }
    }

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        getSampleRateCombo().setDisable(!hasStartedTuner() || locked);
        getSampleRateCombo().setTooltip(new Tooltip(locked ? "Sample rate is locked.  Disable decoding channels " +
                "to unlock." : "Select a sample rate for the tuner"));
    }

    @Override
    protected String getTunerInfo()
    {
        if(!hasStartedTuner())
        {
            return null;
        }

        LimeSdrDeviceInformation info = getTuner().getController().getDeviceInfo();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h3>LimeSDR Tuner</h3>");
        sb.append("<b>Board: </b>").append(info.board()).append("<br>");
        sb.append("<b>Serial: </b>").append(info.serial()).append("<br>");
        sb.append("<b>Hardware: </b>").append(info.hardware()).append("<br>");
        sb.append("<b>Firmware: </b>").append(info.firmware()).append("<br>");
        sb.append("<b>Gateware: </b>").append(info.gateware()).append("<br>");
        sb.append("<b>Connection: </b>").append(info.connection()).append("<br>");
        sb.append("<b>LimeSuiteNG: </b>").append(info.library()).append("<br>");
        return sb.toString();
    }
}
