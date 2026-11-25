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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.gui.control.FrequencyControl;
import io.github.dsheirer.gui.control.FrequencyEntry;
import io.github.dsheirer.gui.control.FrequencyField;
import io.github.dsheirer.gui.spectrum.MainSpectralDisplay;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.record.wave.IRecordingStatusListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.util.ThreadPool;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.function.LongConsumer;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base JavaFX tuner configuration editor.
 *
 * Layout, top to bottom: a header with the tuner name, status, enable switch and actions (spectrum, recording,
 * restart), then a set of titled sections.  The base class provides the Tuning section (frequency control and
 * lock notice), and closes the editor with the Frequency Correction (PPM) and Frequency Limits sections.  Editor
 * implementations add their own rows to the Tuning section (e.g. sample rate) and their own sections (gain,
 * hardware options) in between with {@link #addSection(String)}.
 *
 * Implementations build their controls in the constructor and then call {@link #refresh()}.  All methods must be
 * invoked on the JavaFX application thread.  Tuner notifications that arrive on other threads are moved to the
 * JavaFX thread by this class.
 */
public abstract class TunerEditor<T extends Tuner,C extends TunerConfiguration> extends VBox
        implements IDiscoveredTunerStatusListener, Listener<TunerEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerEditor.class);
    private static final PseudoClass STATUS_ENABLED = PseudoClass.getPseudoClass("enabled");
    private static final PseudoClass STATUS_DISABLED = PseudoClass.getPseudoClass("disabled-tuner");
    private static final PseudoClass STATUS_ERROR = PseudoClass.getPseudoClass("error");
    private static final PseudoClass STATUS_REMOVED = PseudoClass.getPseudoClass("removed");

    private final Settings mSettings;
    private final TunerManager mTunerManager;
    private final DiscoveredTuner mDiscoveredTuner;
    private C mTunerConfiguration;
    private boolean mLoading = false;
    private boolean mDisposed = false;

    //Header
    private final Label mTunerIdLabel = new Label();
    private final Label mTunerClassLabel = new Label();
    private final Label mTunerStatusLabel = new Label();
    private final Label mErrorLabel = new Label();
    private final HBox mHeaderExtras = new HBox();
    private Button mInfoButton;
    private ToggleSwitch mEnabledSwitch;
    private Button mViewSpectrumButton;
    private Button mNewSpectrumButton;
    private ToggleButton mRecordButton;
    private Button mRestartTunerButton;
    private Label mRecordingStatusLabel;

    //Sections
    private final VBox mSections = new VBox();
    private TunerSection mTuningSection;
    private TunerSection mCorrectionSection;
    private TitledPane mLimitsPane;

    //Tuning
    private FrequencyControl mFrequencyControl;
    private final LongConsumer mFrequencyRequestListener = this::frequencyRequested;
    private TunerController mFrequencyControlRegistration;
    private Label mTunerLockedStatusLabel;
    private Spinner<Double> mFrequencyCorrectionSpinner;
    private CheckBox mAutoPPMCheckBox;
    private Label mMeasuredPPMLabel;
    private FrequencyField mMinimumFrequencyField;
    private FrequencyField mMaximumFrequencyField;
    private Button mResetFrequenciesButton;

    /**
     * Constructs an instance
     * @param settings for wide-band recordings
     * @param tunerManager for requesting configuration saves.
     * @param discoveredTuner to edit
     */
    public TunerEditor(Settings settings, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        mSettings = settings;
        mTunerManager = tunerManager;
        mDiscoveredTuner = discoveredTuner;

        if(mDiscoveredTuner != null && mDiscoveredTuner.hasTunerConfiguration())
        {
            mTunerConfiguration = (C)mDiscoveredTuner.getTunerConfiguration();
        }

        getStyleClass().add("tuner-editor");
        URL stylesheet = TunerEditor.class.getResource("/tuner_editor.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        mSections.getStyleClass().add("tuner-sections");
        getChildren().addAll(createHeader(), mSections);

        if(hasTuningControls())
        {
            mSections.getChildren().addAll(getTuningSection(), getCorrectionSection(), getLimitsPane());
        }

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.addTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().addTunerEventListener(this);

                if(!mDiscoveredTuner.hasTunerConfiguration())
                {
                    mLog.warn("Tuner does not have a tuner configuration ....");
                }
            }
        }
    }

    /**
     * Minimum tunable frequency supported by the tuner.
     * @return minimum frequency hertz
     */
    public abstract long getMinimumTunableFrequency();

    /**
     * Maximum tunable frequency supported by the tuner.
     * @return maximum frequency hertz
     */
    public abstract long getMaximumTunableFrequency();

    /**
     * Saves the editor's settings to the tuner configuration.  Implementations can use
     * {@link #saveCommonSettings(TunerConfiguration)} for the settings managed by this base class.
     */
    protected abstract void save();

    /**
     * Notification that the status of the discovered tuner has changed and that the editor implementation must
     * refresh its controls with the current tuner state.  Invoked by {@link #refresh()} while loading, after the
     * header and the frequency controls are updated.
     */
    protected abstract void tunerStatusUpdated();

    /**
     * Notification that the tuner's lock state changed, after the frequency controls are updated.  Implementations
     * enable or disable their sample rate controls.
     * @param locked true when channels are active and the frequency and sample rate can't change.
     */
    protected void tunerLockStateUpdated(boolean locked)
    {
    }

    /**
     * Indicates if this editor shows the Tuning, Frequency Correction and Frequency Limits sections.  Editors for
     * tuners that can't be used (unsupported or disabled tuner types) override this to return false.
     */
    protected boolean hasTuningControls()
    {
        return true;
    }

    /**
     * Tuner information to show from the header's Info button, as simple HTML (see {@link TunerInfoDialog}), or null
     * when it isn't available yet.  The Info button is shown, while the editor has a tuner, for editors that override
     * this method.  It is only invoked when the user clicks the button.
     */
    protected String getTunerInfo()
    {
        return null;
    }

    /**
     * Indicates if this editor overrides {@link #getTunerInfo()}, so that the Info button is shown.
     */
    private boolean providesTunerInfo()
    {
        for(Class<?> type = getClass(); type != null && type != TunerEditor.class; type = type.getSuperclass())
        {
            try
            {
                type.getDeclaredMethod("getTunerInfo");
                return true;
            }
            catch(NoSuchMethodException e)
            {
                //Check the superclass
            }
        }

        return false;
    }

    /**
     * Current sample rate for the tuner.
     * @return sample rate in hertz.
     */
    public int getCurrentSampleRate()
    {
        if(hasTuner())
        {
            return (int)getTuner().getTunerController().getSampleRate();
        }

        return 0;
    }

    /**
     * Indicates if the controls are currently being loaded with values.
     */
    protected boolean isLoading()
    {
        return mLoading;
    }

    /**
     * Changes the loading status
     */
    protected void setLoading(boolean loading)
    {
        mLoading = loading;
    }

    /**
     * Adds a titled section to the editor, after the Tuning section and before the frequency correction section.
     */
    protected TunerSection addSection(String title)
    {
        TunerSection section = new TunerSection(title);
        int index = hasTuningControls() ? mSections.getChildren().indexOf(getCorrectionSection()) :
                mSections.getChildren().size();
        mSections.getChildren().add(index, section);
        return section;
    }

    /**
     * Adds a node (e.g. a button) to the header, next to the Info button.
     */
    protected void addHeaderNode(Node node)
    {
        mHeaderExtras.getChildren().add(node);
    }

    /**
     * Section with the frequency control.  Implementations add their sample rate row(s) to it.
     */
    protected TunerSection getTuningSection()
    {
        if(mTuningSection == null)
        {
            mTuningSection = new TunerSection("Tuning");
            HBox frequencyRow = new HBox(getFrequencyControl());
            frequencyRow.getStyleClass().add("tuner-frequency-row");
            frequencyRow.setAlignment(Pos.CENTER_LEFT);
            mTuningSection.addFullRow(frequencyRow);
            mTuningSection.addFullRow(getTunerLockedStatusLabel());
        }

        return mTuningSection;
    }

    /**
     * PPM frequency correction section.
     */
    private TunerSection getCorrectionSection()
    {
        if(mCorrectionSection == null)
        {
            mCorrectionSection = new TunerSection("Frequency Correction");
            mCorrectionSection.addRow("PPM", getFrequencyCorrectionSpinner(), getMeasuredPPMLabel());
            mCorrectionSection.addControl(getAutoPPMCheckBox());
        }

        return mCorrectionSection;
    }

    /**
     * Collapsible frequency limits section.
     */
    private TitledPane getLimitsPane()
    {
        if(mLimitsPane == null)
        {
            TunerSection section = new TunerSection("");
            section.getTitleLabel().setVisible(false);
            section.getTitleLabel().setManaged(false);
            section.getStyleClass().add("tuner-section-embedded");

            Label hint = new Label("Restricts the frequencies that this tuner can be tuned to, e.g. to keep a " +
                    "tuner on one band when several tuners are available.");
            hint.getStyleClass().add("tuner-hint");
            hint.setWrapText(true);
            section.addFullRow(hint);
            section.addRow("Minimum (MHz)", getMinimumFrequencyField());
            section.addRow("Maximum (MHz)", getMaximumFrequencyField(), getResetFrequenciesButton());

            mLimitsPane = new TitledPane("Frequency Limits", section);
            mLimitsPane.getStyleClass().add("tuner-limits-pane");
            mLimitsPane.setExpanded(false);
            mLimitsPane.setAnimated(false);
        }

        return mLimitsPane;
    }

    private Node createHeader()
    {
        mTunerIdLabel.getStyleClass().add("tuner-title");
        mTunerClassLabel.getStyleClass().add("tuner-subtitle");
        mTunerStatusLabel.getStyleClass().add("tuner-status-chip");
        mErrorLabel.getStyleClass().add("tuner-error");
        mErrorLabel.setWrapText(true);
        mErrorLabel.managedProperty().bind(mErrorLabel.visibleProperty());
        mErrorLabel.setVisible(false);
        mHeaderExtras.getStyleClass().add("tuner-header-extras");
        mHeaderExtras.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox titles = new VBox(mTunerIdLabel, mTunerClassLabel);
        titles.getStyleClass().add("tuner-titles");

        HBox titleRow = new HBox(titles, spacer, mHeaderExtras, getInfoButton());
        titleRow.getStyleClass().add("tuner-title-row");
        titleRow.setAlignment(Pos.TOP_LEFT);

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        HBox statusRow = new HBox(mTunerStatusLabel, statusSpacer, getEnabledSwitch());
        statusRow.getStyleClass().add("tuner-status-row");
        statusRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane actions = new FlowPane(getViewSpectrumButton(), getNewSpectrumButton(), getRecordButton(),
                getRestartTunerButton());
        actions.getStyleClass().add("tuner-actions");

        VBox header = new VBox(titleRow, statusRow, mErrorLabel, actions, getRecordingStatusLabel());
        header.getStyleClass().add("tuner-header");
        return header;
    }

    /**
     * Updates the whole editor from the current tuner state: header, frequency controls and then the
     * implementation's controls via {@link #tunerStatusUpdated()}.
     */
    protected final void refresh()
    {
        setLoading(true);

        try
        {
            updateHeader();

            if(hasTuningControls())
            {
                updateTuningControls();
            }

            tunerStatusUpdated();
        }
        finally
        {
            setLoading(false);
        }
    }

    private void updateHeader()
    {
        if(mDiscoveredTuner == null)
        {
            return;
        }

        mTunerIdLabel.setText(hasTuner() ? getTuner().getPreferredName() : mDiscoveredTuner.getId());
        mTunerClassLabel.setText(mDiscoveredTuner.getTunerClass().toString() +
                (hasTuner() && !getTuner().getPreferredName().contains(mDiscoveredTuner.getId()) ?
                        "  ·  " + mDiscoveredTuner.getId() : ""));

        TunerStatus status = mDiscoveredTuner.getTunerStatus();
        mTunerStatusLabel.setText(status.toString());
        mTunerStatusLabel.pseudoClassStateChanged(STATUS_ENABLED, status == TunerStatus.ENABLED);
        mTunerStatusLabel.pseudoClassStateChanged(STATUS_DISABLED, status == TunerStatus.DISABLED);
        mTunerStatusLabel.pseudoClassStateChanged(STATUS_ERROR, status == TunerStatus.ERROR);
        mTunerStatusLabel.pseudoClassStateChanged(STATUS_REMOVED, status == TunerStatus.REMOVED);

        mErrorLabel.setVisible(mDiscoveredTuner.hasErrorMessage());
        mErrorLabel.setText(mDiscoveredTuner.hasErrorMessage() ? mDiscoveredTuner.getErrorMessage() : "");

        boolean available = status.isAvailable() && mDiscoveredTuner.hasTuner();
        getEnabledSwitch().setSelected(mDiscoveredTuner.isEnabled());
        getRecordButton().setDisable(!available);
        getViewSpectrumButton().setDisable(!available);
        getNewSpectrumButton().setDisable(!available);
        getRestartTunerButton().setVisible(status == TunerStatus.ERROR);
        getInfoButton().setVisible(hasTuner() && providesTunerInfo());

        if(!getRecordButton().isSelected())
        {
            getRecordingStatusLabel().setVisible(false);
        }
    }

    /**
     * Updates the state of the frequency, PPM and frequency limits controls.
     */
    protected void updateTuningControls()
    {
        boolean wasLoading = isLoading();
        setLoading(true);

        try
        {
            boolean hasTunerUnlocked = hasTuner() && !getTuner().getTunerController().isLockedSampleRate();
            getFrequencyControl().setDisable(!hasTunerUnlocked);
            getMinimumFrequencyField().setDisable(!hasTunerUnlocked);
            getMaximumFrequencyField().setDisable(!hasTunerUnlocked);
            getResetFrequenciesButton().setDisable(!hasTunerUnlocked);
            getTunerLockedStatusLabel().setVisible(hasTuner() && getTuner().getTunerController().isLockedSampleRate());
            getFrequencyCorrectionSpinner().setDisable(!hasTuner());
            getAutoPPMCheckBox().setDisable(!hasTuner());

            Tuner tuner = getTuner();

            if(tuner != null)
            {
                TunerController controller = tuner.getTunerController();
                registerFrequencyControl(controller);
                getFrequencyControl().setRange(controller.getMinimumFrequency(), controller.getMaximumFrequency());
                getFrequencyControl().setFrequency(controller.getFrequency());
                getMinimumFrequencyField().setFrequency(controller.getMinimumFrequency());
                getMaximumFrequencyField().setFrequency(controller.getMaximumFrequency());
                getFrequencyCorrectionSpinner().getValueFactory().setValue(controller.getFrequencyCorrection());
                getAutoPPMCheckBox().setSelected(controller.getTunerFrequencyErrorManager().isEnabled());
                getMeasuredPPMLabel().setText(controller.getMeasuredErrorStatus());
            }
            else
            {
                registerFrequencyControl(null);
                getFrequencyControl().setFrequency(hasConfiguration() ? getConfiguration().getFrequency() : 0);
                getFrequencyCorrectionSpinner().getValueFactory().setValue(0.0);
                getAutoPPMCheckBox().setSelected(false);
                getMeasuredPPMLabel().setText("");
            }
        }
        finally
        {
            setLoading(wasLoading);
        }
    }

    /**
     * Registers the frequency control to receive frequency change notifications from the tuner controller,
     * replacing any previous registration.
     */
    private void registerFrequencyControl(TunerController controller)
    {
        if(mFrequencyControlRegistration != controller)
        {
            if(mFrequencyControlRegistration != null)
            {
                mFrequencyControlRegistration.removeListener(getFrequencyControl());
            }

            mFrequencyControlRegistration = controller;

            if(controller != null)
            {
                controller.addListener(getFrequencyControl());
            }
        }
    }

    /**
     * Applies a frequency requested with the frequency control.
     */
    private void frequencyRequested(long frequency)
    {
        if(!hasTuner() || isLoading())
        {
            return;
        }

        try
        {
            TunerController controller = getTuner().getTunerController();
            controller.setFrequency(frequency);

            //Show the frequency that the tuner actually uses, in case it didn't change or it adjusted the request
            getFrequencyControl().setFrequency(controller.getFrequency());
            save();
        }
        catch(SourceException se)
        {
            mLog.error("Error setting tuner frequency to " + frequency, se);
            getFrequencyControl().setFrequency(getTuner().getTunerController().getFrequency());
            showError("Unable to tune to " + FrequencyEntry.format(frequency) + " MHz - " + se.getMessage());
        }
    }

    /**
     * Sets the lock state for the tuner so that the frequency and sample rate controls can be enabled/disabled.
     * @param locked true if the tuner is locked.
     */
    public final void setTunerLockState(boolean locked)
    {
        updateTuningControls();
        tunerLockStateUpdated(locked);
    }

    /**
     * Frequency control
     */
    protected FrequencyControl getFrequencyControl()
    {
        if(mFrequencyControl == null)
        {
            mFrequencyControl = new FrequencyControl();
            mFrequencyControl.addListener(mFrequencyRequestListener);
        }

        return mFrequencyControl;
    }

    /**
     * Notice shown while channels are active and the tuner frequency and sample rate are locked.
     */
    protected Label getTunerLockedStatusLabel()
    {
        if(mTunerLockedStatusLabel == null)
        {
            mTunerLockedStatusLabel = new Label("Channels are active - the frequency and sample rate are locked");
            mTunerLockedStatusLabel.getStyleClass().add("tuner-locked-notice");
            mTunerLockedStatusLabel.setWrapText(true);
            mTunerLockedStatusLabel.setTooltip(new Tooltip("The tuner is providing channels to decoders.  Stop the " +
                    "channels to change the tuner frequency or sample rate."));
            mTunerLockedStatusLabel.setVisible(false);
        }

        return mTunerLockedStatusLabel;
    }

    /**
     * Measured PPM value received from any decoders that may be running.
     */
    protected Label getMeasuredPPMLabel()
    {
        if(mMeasuredPPMLabel == null)
        {
            mMeasuredPPMLabel = new Label("");
            mMeasuredPPMLabel.getStyleClass().add("tuner-value-label");
            mMeasuredPPMLabel.setTooltip(new Tooltip("Measured frequency error and PPM, when provided by " +
                    "compatible channel decoders"));
        }

        return mMeasuredPPMLabel;
    }

    /**
     * Frequency correction (PPM) spinner
     */
    protected Spinner<Double> getFrequencyCorrectionSpinner()
    {
        if(mFrequencyCorrectionSpinner == null)
        {
            SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(-1000.0, 1000.0, 0.0, 0.1);
            factory.setConverter(new StringConverter<>()
            {
                @Override
                public String toString(Double value)
                {
                    return value == null ? "" : String.format("%.1f", value);
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
            mFrequencyCorrectionSpinner = new Spinner<>(factory);
            mFrequencyCorrectionSpinner.setEditable(true);
            mFrequencyCorrectionSpinner.setPrefWidth(110);
            mFrequencyCorrectionSpinner.setDisable(true);
            mFrequencyCorrectionSpinner.setTooltip(new Tooltip("Adjust the PPM value to compensate for tuner " +
                    "frequency error"));
            //Commit typed values when focus leaves the editor
            mFrequencyCorrectionSpinner.focusedProperty().addListener((observable, wasFocused, focused) -> {
                if(!focused)
                {
                    mFrequencyCorrectionSpinner.increment(0);
                }
            });
            mFrequencyCorrectionSpinner.valueProperty().addListener((observable, oldValue, value) -> {
                if(hasTuner() && !isLoading() && value != null)
                {
                    try
                    {
                        getTuner().getTunerController().setFrequencyCorrection(value);
                    }
                    catch(SourceException se)
                    {
                        mLog.error("Error setting frequency correction value", se);
                    }

                    save();
                }
            });
        }

        return mFrequencyCorrectionSpinner;
    }

    /**
     * Check box for enable/disable automatic PPM adjustment from decoder(s) frequency error feedback.
     */
    protected CheckBox getAutoPPMCheckBox()
    {
        if(mAutoPPMCheckBox == null)
        {
            mAutoPPMCheckBox = new CheckBox("Let decoders auto-adjust the PPM");
            mAutoPPMCheckBox.setTooltip(new Tooltip("Allow decoders to measure channel frequency error and " +
                    "correct the tuner PPM"));
            mAutoPPMCheckBox.setOnAction(e -> {
                if(!isLoading() && hasTuner())
                {
                    getTuner().getTunerController().getTunerFrequencyErrorManager()
                            .setEnabled(mAutoPPMCheckBox.isSelected());
                    save();
                }
            });
        }

        return mAutoPPMCheckBox;
    }

    /**
     * Minimum frequency limit field
     */
    protected FrequencyField getMinimumFrequencyField()
    {
        if(mMinimumFrequencyField == null)
        {
            mMinimumFrequencyField = new FrequencyField(getMinimumTunableFrequency());
            mMinimumFrequencyField.setTooltip(new Tooltip("Lowest frequency that this tuner may be tuned to"));
            mMinimumFrequencyField.setCommitHandler(minimum -> applyFrequencyLimits(true));
        }

        return mMinimumFrequencyField;
    }

    /**
     * Maximum frequency limit field
     */
    protected FrequencyField getMaximumFrequencyField()
    {
        if(mMaximumFrequencyField == null)
        {
            mMaximumFrequencyField = new FrequencyField(getMaximumTunableFrequency());
            mMaximumFrequencyField.setTooltip(new Tooltip("Highest frequency that this tuner may be tuned to"));
            mMaximumFrequencyField.setCommitHandler(maximum -> applyFrequencyLimits(false));
        }

        return mMaximumFrequencyField;
    }

    /**
     * Validates and applies the minimum and maximum frequency limits after the user edits one of them.
     * @param minimumEdited true if the minimum was edited, false if the maximum was edited.
     * @return true if the limits were applied, false if the edited value was rejected.
     */
    private boolean applyFrequencyLimits(boolean minimumEdited)
    {
        if(isLoading())
        {
            return true;
        }

        long minimum = getMinimumFrequencyField().getFrequency();
        long maximum = getMaximumFrequencyField().getFrequency();
        int sampleRate = getCurrentSampleRate();

        if(minimumEdited)
        {
            if(minimum < getMinimumTunableFrequency())
            {
                showError("The minimum frequency " + FrequencyEntry.format(minimum) + " MHz is below the " +
                        "tuner's lowest frequency of " + FrequencyEntry.format(getMinimumTunableFrequency()) + " MHz");
                return false;
            }

            if(minimum + sampleRate > maximum)
            {
                long newMaximum = minimum + sampleRate;

                if(newMaximum > getMaximumTunableFrequency())
                {
                    showError("The minimum frequency " + FrequencyEntry.format(minimum) + " MHz leaves too little " +
                            "room below the tuner's highest frequency for the current sample rate");
                    return false;
                }

                maximum = newMaximum;
                getMaximumFrequencyField().setFrequency(maximum);
            }
        }
        else
        {
            if(maximum > getMaximumTunableFrequency())
            {
                showError("The maximum frequency " + FrequencyEntry.format(maximum) + " MHz is above the " +
                        "tuner's highest frequency of " + FrequencyEntry.format(getMaximumTunableFrequency()) + " MHz");
                return false;
            }

            if(maximum - sampleRate < minimum)
            {
                long newMinimum = maximum - sampleRate;

                if(newMinimum < getMinimumTunableFrequency())
                {
                    showError("The maximum frequency " + FrequencyEntry.format(maximum) + " MHz leaves too little " +
                            "room above the tuner's lowest frequency for the current sample rate");
                    return false;
                }

                minimum = newMinimum;
                getMinimumFrequencyField().setFrequency(minimum);
            }
        }

        setFrequencyLimits(minimum, maximum);
        return true;
    }

    /**
     * Applies the frequency limits to the tuner, moves the tuner frequency inside them and saves.
     */
    private void setFrequencyLimits(long minimum, long maximum)
    {
        if(hasTuner())
        {
            TunerController controller = getTuner().getTunerController();
            controller.setFrequencyExtents(minimum, maximum);
            getFrequencyControl().setRange(minimum, maximum);

            try
            {
                if(controller.getFrequency() < minimum)
                {
                    controller.setFrequency(minimum);
                }
                else if(controller.getFrequency() > maximum)
                {
                    controller.setFrequency(maximum);
                }
            }
            catch(SourceException se)
            {
                mLog.error("Error adjusting frequency", se);
            }
        }

        save();
    }

    /**
     * Adjusts the minimum and maximum frequency values to ensure the gap is wide enough for the sample rate.
     * @param sampleRate to adjust for.
     */
    protected void adjustForSampleRate(int sampleRate)
    {
        long minimum = getMinimumFrequencyField().getFrequency();
        long maximum = getMaximumFrequencyField().getFrequency();

        if(maximum - minimum < sampleRate)
        {
            long newMaximum = minimum + sampleRate;

            if(newMaximum <= getMaximumTunableFrequency())
            {
                getMaximumFrequencyField().setFrequency(newMaximum);
            }
            else
            {
                long newMinimum = maximum - sampleRate;

                if(newMinimum >= getMinimumTunableFrequency())
                {
                    getMinimumFrequencyField().setFrequency(newMinimum);
                }
                else
                {
                    showError("Unable to adjust the tuner's minimum and maximum frequency limits to fit the new " +
                            "sample rate [" + sampleRate + "]");
                }
            }
        }
    }

    /**
     * Resets the minimum and maximum frequency limits to the tuner's full range.
     */
    protected Button getResetFrequenciesButton()
    {
        if(mResetFrequenciesButton == null)
        {
            mResetFrequenciesButton = new Button("Reset");
            mResetFrequenciesButton.setTooltip(new Tooltip("Reset the limits to the tuner's full frequency range"));
            mResetFrequenciesButton.setOnAction(e -> {
                long min = getMinimumTunableFrequency();
                long max = getMaximumTunableFrequency();
                getMinimumFrequencyField().setFrequency(min);
                getMaximumFrequencyField().setFrequency(max);
                setFrequencyLimits(min, max);
            });
        }

        return mResetFrequenciesButton;
    }

    private Button getInfoButton()
    {
        if(mInfoButton == null)
        {
            mInfoButton = new Button("Info");
            mInfoButton.setTooltip(new Tooltip("Show tuner hardware information"));
            mInfoButton.managedProperty().bind(mInfoButton.visibleProperty());
            mInfoButton.setVisible(false);
            mInfoButton.setOnAction(e -> {
                String info = null;

                try
                {
                    info = hasTuner() ? getTunerInfo() : null;
                }
                catch(Exception ex)
                {
                    mLog.error("Error reading tuner information", ex);
                }

                if(info != null)
                {
                    TunerInfoDialog.show(getWindow(), info);
                }
                else
                {
                    showMessage("Tuner Info", "Tuner information isn't available until the tuner has started.");
                }
            });
        }

        return mInfoButton;
    }

    /**
     * Switch to enable or disable the tuner.
     */
    protected ToggleSwitch getEnabledSwitch()
    {
        if(mEnabledSwitch == null)
        {
            mEnabledSwitch = new ToggleSwitch("Enabled");
            mEnabledSwitch.getStyleClass().add("tuner-enabled-switch");
            mEnabledSwitch.setTooltip(new Tooltip("Enable or disable the tuner for use by sdrtrunk"));
            mEnabledSwitch.selectedProperty().addListener((observable, wasSelected, selected) -> {
                if(!isLoading() && getDiscoveredTuner() != null && selected != getDiscoveredTuner().isEnabled())
                {
                    DiscoveredTuner discoveredTuner = getDiscoveredTuner();
                    mLog.info((selected ? "Enabling " : "Disabling ") + discoveredTuner.getTunerClass() + " tuner");
                    //Starting or stopping a tuner can block, so do it off of the JavaFX thread.  The discovered
                    //tuner editor replaces this editor when the tuner status changes.
                    mEnabledSwitch.setDisable(true);
                    ThreadPool.CACHED.submit(() -> discoveredTuner.setEnabled(selected));
                }
            });
        }

        return mEnabledSwitch;
    }

    /**
     * Button requesting to show tuner in main spectral display
     */
    protected Button getViewSpectrumButton()
    {
        if(mViewSpectrumButton == null)
        {
            mViewSpectrumButton = new Button("View Spectrum");
            mViewSpectrumButton.setTooltip(new Tooltip("Show this tuner in the main spectral display"));
            mViewSpectrumButton.setOnAction(e -> {
                Tuner tuner = getTuner();

                if(tuner != null)
                {
                    MainSpectralDisplay.setTunerEnabled(true);
                    ThreadPool.CACHED.submit(() -> mTunerManager.getDiscoveredTunerModel().broadcast(
                            new TunerEvent(tuner, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY)));
                }
            });
        }

        return mViewSpectrumButton;
    }

    /**
     * Button requesting to show tuner in new spectral display
     */
    protected Button getNewSpectrumButton()
    {
        if(mNewSpectrumButton == null)
        {
            mNewSpectrumButton = new Button("New Spectrum Window");
            mNewSpectrumButton.setTooltip(new Tooltip("Show this tuner in a new (separate) spectral display window"));
            mNewSpectrumButton.setOnAction(e -> {
                Tuner tuner = getTuner();

                if(tuner != null)
                {
                    ThreadPool.CACHED.submit(() -> mTunerManager.getDiscoveredTunerModel().broadcast(
                            new TunerEvent(tuner, TunerEvent.Event.REQUEST_NEW_SPECTRAL_DISPLAY)));
                }
            });
        }

        return mNewSpectrumButton;
    }

    /**
     * Restart button, shown when the tuner is in an error state.
     */
    protected Button getRestartTunerButton()
    {
        if(mRestartTunerButton == null)
        {
            mRestartTunerButton = new Button("Restart Tuner");
            mRestartTunerButton.getStyleClass().add("tuner-restart-button");
            mRestartTunerButton.managedProperty().bind(mRestartTunerButton.visibleProperty());
            mRestartTunerButton.setVisible(false);
            mRestartTunerButton.setTooltip(new Tooltip("Attempt to restart this tuner to recover from the error"));
            mRestartTunerButton.setOnAction(e -> {
                if(!hasTuner() && getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
                {
                    DiscoveredTuner discoveredTuner = getDiscoveredTuner();
                    mLog.info("Restarting " + discoveredTuner.getTunerClass() + " tuner");
                    mRestartTunerButton.setDisable(true);
                    ThreadPool.CACHED.submit(discoveredTuner::restart);
                }
            });
        }

        return mRestartTunerButton;
    }

    /**
     * Records the tuner's wide-band sample stream.
     */
    protected ToggleButton getRecordButton()
    {
        if(mRecordButton == null)
        {
            mRecordButton = new ToggleButton("Record");
            mRecordButton.getStyleClass().add("tuner-record-button");
            mRecordButton.setTooltip(new Tooltip("Create a baseband recording for this tuner"));
            mRecordButton.setDisable(true);
            mRecordButton.selectedProperty().addListener((observable, wasSelected, selected) -> {
                if(hasTuner())
                {
                    if(selected)
                    {
                        getRecordingStatusLabel().setVisible(true);
                        getRecordingStatusLabel().setText("Recording ...");
                        getTuner().getTunerController().startRecorder(mSettings, new RecordingStatusListener(),
                                getDiscoveredTuner().getTunerClass().name());
                    }
                    else
                    {
                        getTuner().getTunerController().stopRecorder();
                    }
                }
            });
        }

        return mRecordButton;
    }

    /**
     * Label to display current file size for a wide-band recording
     */
    protected Label getRecordingStatusLabel()
    {
        if(mRecordingStatusLabel == null)
        {
            mRecordingStatusLabel = new Label();
            mRecordingStatusLabel.getStyleClass().add("tuner-recording-status");
            mRecordingStatusLabel.setTooltip(new Tooltip("Status of the latest baseband recording"));
            mRecordingStatusLabel.managedProperty().bind(mRecordingStatusLabel.visibleProperty());
            mRecordingStatusLabel.setVisible(false);
        }

        return mRecordingStatusLabel;
    }

    /**
     * Turns off the recorder, if it's currently recording.
     */
    protected void turnOffRecorder()
    {
        if(mRecordButton != null && mRecordButton.isSelected())
        {
            mRecordButton.setSelected(false);
        }
    }

    /**
     * Copies the settings managed by this base class (frequency, frequency limits and PPM) to the configuration.
     */
    protected void saveCommonSettings(TunerConfiguration config)
    {
        config.setFrequency(getFrequencyControl().getFrequency());
        config.setMinimumFrequency(getMinimumFrequencyField().getFrequency());
        config.setMaximumFrequency(getMaximumFrequencyField().getFrequency());
        Double ppm = getFrequencyCorrectionSpinner().getValue();
        config.setFrequencyCorrection(ppm != null ? ppm : 0.0);
        config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
    }

    /**
     * Access the tuner configuration
     */
    protected C getConfiguration()
    {
        return mTunerConfiguration;
    }

    /**
     * Indicates if this editor has a tuner configuration
     */
    protected boolean hasConfiguration()
    {
        return mTunerConfiguration != null;
    }

    /**
     * Discovered tuner for this editor
     */
    protected DiscoveredTuner getDiscoveredTuner()
    {
        return mDiscoveredTuner;
    }

    /**
     * Tuner manager
     */
    protected TunerManager getTunerManager()
    {
        return mTunerManager;
    }

    /**
     * User preferences
     */
    protected Settings getSettings()
    {
        return mSettings;
    }

    /**
     * Indicates if the discovered tuner has a usable tuner
     */
    public boolean hasTuner()
    {
        return mDiscoveredTuner != null && mDiscoveredTuner.hasTuner();
    }

    /**
     * Access the usable tuner from the discovered tuner
     * Note: use hasTuner() to check before invoking this method.
     * @return tuner or null.
     */
    public T getTuner()
    {
        if(hasTuner())
        {
            return (T)mDiscoveredTuner.getTuner();
        }

        return null;
    }

    /**
     * Request to save the state of this configuration.
     */
    protected void saveConfiguration()
    {
        mTunerManager.saveConfigurations();
    }

    /**
     * Window that contains this editor, for dialog ownership.
     */
    protected Window getWindow()
    {
        return getScene() != null ? getScene().getWindow() : null;
    }

    /**
     * Shows an error dialog.
     */
    protected void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(getWindow());
        alert.setTitle("Tuner Error");
        alert.setHeaderText(null);
        alert.show();
    }

    /**
     * Action that changes a tuner setting and may fail.
     */
    @FunctionalInterface
    protected interface TunerAction
    {
        void run() throws Exception;
    }

    /**
     * Applies a setting to the tuner, logging and showing any error.
     * @param description of the setting, used in the error message, e.g. "sample rate"
     * @param action that applies the setting
     * @return true if the setting was applied
     */
    protected boolean applyToTuner(String description, TunerAction action)
    {
        try
        {
            action.run();
            return true;
        }
        catch(Exception e)
        {
            String tuner = getDiscoveredTuner() != null ? getDiscoveredTuner().getTunerClass().toString() : "Tuner";
            mLog.error(tuner + " - couldn't apply the " + description + " setting", e);
            showError("Couldn't apply the " + description + " setting - " + e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Shows an information dialog.
     */
    protected void showMessage(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.initOwner(getWindow());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.show();
    }

    /**
     * Runs the runnable on the JavaFX application thread, unless this editor has been disposed.
     */
    protected void runOnFxThread(Runnable runnable)
    {
        if(Platform.isFxApplicationThread())
        {
            if(!mDisposed)
            {
                runnable.run();
            }
        }
        else
        {
            Platform.runLater(() -> {
                if(!mDisposed)
                {
                    runnable.run();
                }
            });
        }
    }

    /**
     * Indicates if this editor has been disposed.
     */
    protected boolean isDisposed()
    {
        return mDisposed;
    }

    /**
     * Implements the tuner status listener interface.  The discovered tuner editor replaces this editor when the
     * status changes, so this only needs to keep listening to a tuner that was (re)started.
     */
    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        runOnFxThread(() -> {
            if(current == TunerStatus.ENABLED && hasTuner())
            {
                getTuner().addTunerEventListener(this);
            }

            refresh();
        });
    }

    @Override
    public void receive(TunerEvent tunerEvent)
    {
        switch(tunerEvent.getEvent())
        {
            case UPDATE_MEASURED_FREQUENCY_ERROR -> runOnFxThread(() -> getMeasuredPPMLabel()
                    .setText(hasTuner() ? getTuner().getTunerController().getMeasuredErrorStatus() : ""));
            case UPDATE_FREQUENCY_ERROR -> runOnFxThread(() -> {
                if(hasTuner())
                {
                    boolean wasLoading = isLoading();
                    setLoading(true);
                    getFrequencyCorrectionSpinner().getValueFactory()
                            .setValue(getTuner().getTunerController().getFrequencyCorrection());
                    setLoading(wasLoading);
                }
            });
            case UPDATE_LOCK_STATE -> runOnFxThread(() -> {
                if(hasTuner())
                {
                    setTunerLockState(getTuner().getTunerController().isLockedSampleRate());
                }
            });
            default -> {}
        }
    }

    /**
     * Prepare this editor for disposal
     */
    public void dispose()
    {
        turnOffRecorder();
        mDisposed = true;
        getFrequencyControl().removeListener(mFrequencyRequestListener);
        registerFrequencyControl(null);

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.removeTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().removeTunerEventListener(this);
            }
        }
    }

    /**
     * Implements status listener to receive updates for wide-band recordings
     */
    public class RecordingStatusListener implements IRecordingStatusListener
    {
        @Override
        public void update(int fileCount, String file, long size)
        {
            final String status = "Recording " + humanReadableByteCount(size) + "  ·  File #" + fileCount + ": " + file;
            Platform.runLater(() -> getRecordingStatusLabel().setText(status));
        }
    }

    /**
     * Formats a byte count, e.g. 1.5 MB
     */
    public static String humanReadableByteCount(long bytes)
    {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);

        if(absB < 1024)
        {
            return bytes + " B";
        }

        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");

        for(int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10)
        {
            value >>= 10;
            ci.next();
        }

        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }
}
