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
package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.sdrplay.Tuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.GainSlider;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javafx.css.PseudoClass;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base SDRPlay RSP tuner editor.  Provides the sample rate control, the gain section (LNA state, IF gain reduction,
 * IF AGC, gain readout and overload indicator) and helpers for binding the model specific option check boxes and
 * choices to the device.
 */
public abstract class RspTunerEditor<C extends RspTunerConfiguration> extends TunerEditor<RspTuner,C>
        implements ITunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(RspTunerEditor.class);
    private static final PseudoClass OVERLOAD = PseudoClass.getPseudoClass("overload");
    private static final int IF_GAIN_SLIDER_MAX = 39;
    private ComboBox<RspSampleRate> mSampleRateCombo;
    private CheckBox mAgcCheckBox;
    private Label mGainValueLabel;
    private Label mGainOverloadLabel;
    private GainSlider mLNASlider;
    private GainSlider mIfGainSlider;
    private Label mLnaLabel;
    private Label mIfGainLabel;
    private boolean mGainOverloadAlert;
    private final List<Option> mOptions = new ArrayList<>();
    private final List<Choice<?>> mChoices = new ArrayList<>();

    /**
     * Constructs an instance.  Implementations add their own sections and then call {@link #refresh()}.
     */
    public RspTunerEditor(Settings settings, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());

        TunerSection gain = addSection("Gain");
        HBox readout = new HBox(getGainValueLabel(), getGainOverloadLabel());
        readout.getStyleClass().add("tuner-flow-row");
        gain.addRow("Gain", readout);
        mLnaLabel = gain.addRow("LNA", getLNASlider(), new Region());
        mIfGainLabel = gain.addRow("IF", getIfGainSlider(), getAgcCheckBox());
    }

    /**
     * Discovered RSP tuner
     */
    protected DiscoveredRspTuner getDiscoveredRspTuner()
    {
        return (DiscoveredRspTuner)getDiscoveredTuner();
    }

    /**
     * Device control for the tuner.  Only use when the editor has a tuner.
     */
    protected IControlRsp getControlRsp()
    {
        return getTuner().getRspTunerController().getControlRsp();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return RspTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return RspTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Sample rates to offer.  Defaults to the single tuner sample rates.
     */
    protected Collection<RspSampleRate> getSampleRates()
    {
        return RspSampleRate.SINGLE_TUNER_SAMPLE_RATES;
    }

    /**
     * Indicates if the user can change the sample rate (when it isn't locked by active channels).  Defaults to true.
     */
    protected boolean isSampleRateChangeable()
    {
        return true;
    }

    /**
     * Implementation specific updates after the common RSP controls are refreshed.  Invoked while loading.
     */
    protected void rspTunerStatusUpdated()
    {
    }

    @Override
    protected final void tunerStatusUpdated()
    {
        getSampleRateCombo().getItems().setAll(hasTuner() ? getSampleRates() : List.of());
        getSampleRateCombo().setValue(hasTuner() ? getControlRsp().getSampleRateEnumeration() : null);
        updateSampleRateControl();

        getAgcCheckBox().setDisable(!hasTuner());

        if(hasTuner())
        {
            RspAgcMode current = getControlRsp().getAgcMode();
            getAgcCheckBox().setSelected(current == null || current.equals(RspAgcMode.ENABLE));
            getLNASlider().setRange(0, getControlRsp().getMaximumLNASetting());
            setLNA(getControlRsp().getLNA());
            setGainReduction(getControlRsp().getBasebandGainReduction());
            //Register to receive gain overload notifications
            getControlRsp().setGainOverloadListener(this);
            updateGainLabel();
        }
        else
        {
            getGainValueLabel().setText("");
        }

        getLNASlider().setDisable(!hasTuner());
        mLnaLabel.setDisable(!hasTuner());
        boolean ifManual = hasTuner() && !getAgcCheckBox().isSelected();
        getIfGainSlider().setDisable(!ifManual);
        mIfGainLabel.setDisable(!ifManual);
        getGainValueLabel().setDisable(!hasTuner());

        for(Option option: mOptions)
        {
            option.refresh();
        }

        for(Choice<?> choice: mChoices)
        {
            choice.refresh();
        }

        rspTunerStatusUpdated();
    }

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        updateSampleRateControl();
    }

    private void updateSampleRateControl()
    {
        boolean locked = hasTuner() && getTuner().getTunerController().isLockedSampleRate();
        getSampleRateCombo().setDisable(!hasTuner() || !isSampleRateChangeable() || locked);

        if(locked)
        {
            getSampleRateCombo().setTooltip(new Tooltip("Sample rate is locked.  Disable decoding channels to unlock."));
        }
        else if(!hasTuner())
        {
            getSampleRateCombo().setTooltip(new Tooltip("No tuner available"));
        }
        else if(!isSampleRateChangeable())
        {
            getSampleRateCombo().setTooltip(new Tooltip("The sample rate is set by the master tuner"));
        }
        else
        {
            getSampleRateCombo().setTooltip(new Tooltip("Select a sample rate for the tuner"));
        }
    }

    /**
     * Sample rate selection
     */
    protected ComboBox<RspSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setOnAction(e -> {
                RspSampleRate selected = mSampleRateCombo.getValue();

                if(hasTuner() && !isLoading() && selected != null &&
                    applyToTuner("sample rate [" + selected + "]",
                        () -> getTuner().getRspTunerController().setSampleRate(selected)))
                {
                    //Adjust the min/max values for the sample rate.
                    adjustForSampleRate((int)selected.getSampleRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Copies the common RSP settings (sample rate and gain) and the base settings to the configuration.
     */
    protected void saveRspSettings(C config)
    {
        saveCommonSettings(config);

        if(getSampleRateCombo().getValue() != null)
        {
            config.setSampleRate(getSampleRateCombo().getValue());
        }

        config.setLNA(getLNA());
        config.setBasebandGainReduction(getGainReduction());
        config.setAgcMode(getAgcCheckBox().isSelected() ? RspAgcMode.ENABLE : RspAgcMode.DISABLE);
    }

    private Label getGainValueLabel()
    {
        if(mGainValueLabel == null)
        {
            mGainValueLabel = new Label();
            mGainValueLabel.getStyleClass().add("tuner-value-label");
            mGainValueLabel.setTooltip(new Tooltip("Current overall gain"));
        }

        return mGainValueLabel;
    }

    private Label getGainOverloadLabel()
    {
        if(mGainOverloadLabel == null)
        {
            mGainOverloadLabel = new Label("Overload");
            mGainOverloadLabel.getStyleClass().add("tuner-overload-indicator");
            mGainOverloadLabel.setTooltip(new Tooltip("Lights when the manual gain is set too high and the signal " +
                    "overloads the tuner.  Reduce the gain when this flashes."));
        }

        return mGainOverloadLabel;
    }

    /**
     * IF automatic gain control
     */
    protected CheckBox getAgcCheckBox()
    {
        if(mAgcCheckBox == null)
        {
            mAgcCheckBox = new CheckBox("AGC");
            mAgcCheckBox.setTooltip(new Tooltip("Automatic IF gain control.  Turn off to set the IF gain manually."));
            mAgcCheckBox.setDisable(true);
            mAgcCheckBox.setOnAction(e -> {
                boolean agc = mAgcCheckBox.isSelected();

                if(hasTuner() && !isLoading())
                {
                    if(applyToTuner("AGC mode",
                            () -> getControlRsp().setAgcMode(agc ? RspAgcMode.ENABLE : RspAgcMode.DISABLE)))
                    {
                        updateGainLabel();
                    }

                    save();
                }

                getIfGainSlider().setDisable(agc);
                mIfGainLabel.setDisable(agc);
            });
        }

        return mAgcCheckBox;
    }

    @Override
    public void notifyGainOverload(Tuner tuner)
    {
        if(hasTuner() && getTuner().getRspTunerController().getSdrplayTuner() == tuner)
        {
            runOnFxThread(() -> setGainOverloadAlert(true));
            ThreadPool.SCHEDULED.schedule(() -> runOnFxThread(() -> setGainOverloadAlert(false)), 600,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void frequencyUpdated()
    {
        runOnFxThread(this::updateLnaSlider);
    }

    private void setGainOverloadAlert(boolean alert)
    {
        if(alert != mGainOverloadAlert)
        {
            mGainOverloadAlert = alert;
            getGainOverloadLabel().pseudoClassStateChanged(OVERLOAD, alert);
        }
    }

    /**
     * Updates the LNA slider range for the current frequency, since the number of LNA states depends on the band.
     */
    protected void updateLnaSlider()
    {
        if(hasTuner())
        {
            int max = getControlRsp().getMaximumLNASetting();
            GainSlider slider = getLNASlider();

            if(max != (int)slider.getSlider().getMax())
            {
                boolean wasLoading = isLoading();
                setLoading(true);
                int lna = Math.min(getLNA(), max);
                slider.setRange(0, max);
                setLNA(lna);
                setLoading(wasLoading);
            }
        }
    }

    /**
     * LNA state slider.  The slider is inverted so that moving it right increases the gain: LNA state 0 is the
     * highest gain.
     */
    protected GainSlider getLNASlider()
    {
        if(mLNASlider == null)
        {
            mLNASlider = new GainSlider(0, 9, 9);
            mLNASlider.setApplyOnRelease(true);
            mLNASlider.setTooltip("LNA gain.  Move right for more gain.");
            mLNASlider.setFormatter(value -> "State " + ((int)mLNASlider.getSlider().getMax() - value));
            mLNASlider.setOnChange(value -> updateGain());
        }

        return mLNASlider;
    }

    /**
     * IF gain slider.  The slider is inverted gain reduction, so that moving it right increases the gain.
     */
    protected GainSlider getIfGainSlider()
    {
        if(mIfGainSlider == null)
        {
            mIfGainSlider = new GainSlider(0, IF_GAIN_SLIDER_MAX, 30);
            mIfGainSlider.setApplyOnRelease(true);
            mIfGainSlider.setTooltip("IF gain.  Move right for more gain.  Turn off AGC to adjust.");
            mIfGainSlider.setFormatter(value -> (IF_GAIN_SLIDER_MAX - value + 20) + " dB GR");
            mIfGainSlider.setOnChange(value -> updateGain());
        }

        return mIfGainSlider;
    }

    /**
     * Current LNA state from the slider
     */
    protected int getLNA()
    {
        return (int)getLNASlider().getSlider().getMax() - getLNASlider().getValue();
    }

    private void setLNA(int lna)
    {
        getLNASlider().setValue((int)getLNASlider().getSlider().getMax() - lna);
    }

    /**
     * Current baseband gain reduction from the IF gain slider
     */
    protected int getGainReduction()
    {
        return IF_GAIN_SLIDER_MAX - getIfGainSlider().getValue() + 20;
    }

    private void setGainReduction(int gainReduction)
    {
        getIfGainSlider().setValue(IF_GAIN_SLIDER_MAX - (gainReduction - 20));
    }

    private void updateGain()
    {
        int lna = getLNA();
        int gr = getGainReduction();

        if(hasTuner() && !isLoading() &&
            applyToTuner("gain (LNA " + lna + ", gain reduction " + gr + ")", () -> getControlRsp().setGain(lna, gr)))
        {
            save();
            updateGainLabel();
        }
    }

    /**
     * Updates the overall gain readout
     */
    protected void updateGainLabel()
    {
        try
        {
            getGainValueLabel().setText((int)getControlRsp().getCurrentGain() + " dB");
        }
        catch(Exception se)
        {
            mLog.error("Error accessing current gain value from RSP tuner", se);
        }
    }

    /**
     * Reads a device setting
     */
    @FunctionalInterface
    protected interface RspGetter<T>
    {
        T get() throws Exception;
    }

    /**
     * Applies a device setting
     */
    @FunctionalInterface
    protected interface RspSetter<T>
    {
        void set(T value) throws Exception;
    }

    /**
     * Adds a check box option to the section that's bound to a device setting.  The check box is refreshed from the
     * device when the editor refreshes, and changes are applied to the device and then saved.
     * @param section to add to
     * @param text for the check box
     * @param tooltip or null
     * @param getter reads the device setting
     * @param setter applies the device setting
     * @return the check box
     */
    protected CheckBox addOption(TunerSection section, String text, String tooltip, RspGetter<Boolean> getter,
                                 RspSetter<Boolean> setter)
    {
        Option option = new Option(text, getter, setter);

        if(tooltip != null)
        {
            option.mCheckBox.setTooltip(new Tooltip(tooltip));
        }

        mOptions.add(option);
        section.addControl(option.mCheckBox);
        return option.mCheckBox;
    }

    /**
     * Adds a choice (combo box) to the section that's bound to a device setting.
     * @param section to add to
     * @param label for the row
     * @param values to choose from
     * @param getter reads the device setting
     * @param setter applies the device setting
     * @return the combo box
     */
    protected <T> ComboBox<T> addChoice(TunerSection section, String label, Collection<T> values, RspGetter<T> getter,
                                        RspSetter<T> setter)
    {
        Choice<T> choice = new Choice<>(label, values, getter, setter);
        mChoices.add(choice);
        section.addRow(label, choice.mComboBox);
        return choice.mComboBox;
    }

    /**
     * Check box bound to a device setting
     */
    private class Option
    {
        private final CheckBox mCheckBox;
        private final String mText;
        private final RspGetter<Boolean> mGetter;

        Option(String text, RspGetter<Boolean> getter, RspSetter<Boolean> setter)
        {
            mText = text;
            mGetter = getter;
            mCheckBox = new CheckBox(text);
            mCheckBox.setDisable(true);
            mCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    boolean selected = mCheckBox.isSelected();

                    if(applyToTuner(mText, () -> setter.set(selected)))
                    {
                        save();
                    }
                    else
                    {
                        refresh();
                    }
                }
            });
        }

        void refresh()
        {
            mCheckBox.setDisable(!hasTuner());

            try
            {
                mCheckBox.setSelected(hasTuner() && mGetter.get());
            }
            catch(Exception e)
            {
                mLog.error("Error reading RSP setting [" + mText + "]", e);
            }
        }
    }

    /**
     * Combo box bound to a device setting
     */
    private class Choice<T>
    {
        private final ComboBox<T> mComboBox;
        private final String mLabel;
        private final RspGetter<T> mGetter;

        Choice(String label, Collection<T> values, RspGetter<T> getter, RspSetter<T> setter)
        {
            mLabel = label;
            mGetter = getter;
            mComboBox = new ComboBox<>();
            mComboBox.getItems().addAll(values);
            mComboBox.setMaxWidth(Double.MAX_VALUE);
            mComboBox.setDisable(true);
            mComboBox.setOnAction(e -> {
                T selected = mComboBox.getValue();

                if(hasTuner() && !isLoading() && selected != null)
                {
                    if(applyToTuner(mLabel.toLowerCase() + " [" + selected + "]", () -> setter.set(selected)))
                    {
                        save();
                    }
                    else
                    {
                        refresh();
                    }
                }
            });
        }

        void refresh()
        {
            mComboBox.setDisable(!hasTuner());

            try
            {
                mComboBox.setValue(hasTuner() ? mGetter.get() : null);
            }
            catch(Exception e)
            {
                mLog.error("Error reading RSP setting [" + mLabel + "]", e);
            }
        }
    }
}
