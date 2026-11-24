/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.settings.calibration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.settings.CalibrateRequest;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.gui.settings.ViewSettingsEditorRequest;
import io.github.dsheirer.log.TextAreaLogAppender;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.calibration.VectorCalibrationSettings;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationResult;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import jdk.incubator.vector.FloatVector;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance settings: CPU vector (SIMD) operations and their calibration
 */
public class PerformanceSettingsPage extends SettingsPage
{
    private final static Logger mLog = LoggerFactory.getLogger(PerformanceSettingsPage.class);
    private static final DecimalFormat SPEEDUP_FORMAT = new DecimalFormat("0.00");
    private VectorCalibrationSettings mSettings;
    private ToggleSwitch mHideDialogSwitch;
    private ToggleSwitch mVectorEnabled;
    private Label mCalibrationsPendingValue;
    private Button mResetAllButton;
    private Button mCalibrateButton;
    private ProgressBar mProgressBar;
    private Label mCalibratingLabel;
    private TextArea mConsoleTextArea;
    private TextAreaLogAppender mTextAreaLogAppender;
    private Label mChannelsProcessingLabel;
    private GridPane mResultsGrid;
    private final ChannelProcessingManager mChannelProcessingManager;
    private boolean mCalibrating;

    /**
     * Constructs an instance
     * @param settings to edit
     * @param channelProcessingManager to check that no channels are processing before calibrating, or null
     */
    public PerformanceSettingsPage(Settings settings, ChannelProcessingManager channelProcessingManager)
    {
        super(SettingsPageType.PERFORMANCE, settings);
        MyEventBus.getGlobalEventBus().register(this);
        mSettings = settings.getVectorCalibrationSettings();
        mChannelProcessingManager = channelProcessingManager;

        if(mChannelProcessingManager != null)
        {
            mChannelProcessingManager.addChannelEventListener(this::receive);
        }

        SettingsSection vector = addSection("CPU Vector (SIMD) Operations");
        vector.addRow("Enable SIMD vector operations", getVectorEnabledToggleSwitch())
            .defaultValue(getVectorEnabledToggleSwitch().selectedProperty(),
                VectorCalibrationSettings.DEFAULT_VECTOR_ENABLED, getVectorEnabledToggleSwitch()::setSelected)
            .help("Allows sdrtrunk to use optimized vector operations when supported by your CPU.");

        SettingsSection calibration = addSection("Calibration");
        calibration.addHelp("Calibration measures the scalar and vector version of each operation on this computer " +
            "and selects the fastest one that produces the same output as scalar.  Each calibration lasts a few " +
            "seconds.  Stop all channels before calibrating so that they don't compete for the CPU.  Calibrations " +
            "reset when the CPU, operating system or Java version changes.  Channels started after calibrating use " +
            "the new calibrations.  Restart the application to also use them for tuners that already started " +
            "channels.");
        calibration.addHelp("Computing environment: " + CalibrationManager.getEnvironment());
        calibration.addRow("Calibrations to perform", getCalibrationsPendingValue());
        calibration.addRow("Don't show the calibration dialog when new calibrations are available",
            getHideDialogSwitch())
            .defaultValue(getHideDialogSwitch().selectedProperty(),
                VectorCalibrationSettings.DEFAULT_HIDE_CALIBRATION_DIALOG, getHideDialogSwitch()::setSelected);

        HBox progress = new HBox(8, getCalibratingLabel(), getProgressBar());
        progress.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(getProgressBar(), Priority.ALWAYS);
        getProgressBar().setMaxWidth(Double.MAX_VALUE);
        //Only show the progress row while calibrating
        progress.visibleProperty().bind(getProgressBar().visibleProperty());
        progress.managedProperty().bind(getProgressBar().visibleProperty());
        HBox buttons = new HBox(10, getCalibrateButton(), getResetAllButton(), getChannelsProcessingLabel());
        buttons.setAlignment(Pos.CENTER_LEFT);
        calibration.addContent(buttons);
        calibration.addContent(progress);
        calibration.addContent(getConsoleTextArea());

        SettingsSection results = addSection("Calibration Results");
        results.addHelp("The implementation selected for each operation and its throughput relative to scalar.  " +
            "Vector implementations are named by their width in bits.  Other candidates lists the throughput of each " +
            "other implementation that was measured, and any that were rejected because their output didn't match " +
            "scalar.");
        results.addContent(getResultsGrid());
        updateControls();
    }

    /**
     * Updates the controls when channels start or stop processing.
     */
    private void receive(ChannelEvent channelEvent)
    {
        ChannelEvent.Event event = channelEvent.getEvent();

        if(event == ChannelEvent.Event.NOTIFICATION_PROCESSING_START ||
            event == ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP)
        {
            Platform.runLater(() ->
            {
                if(mCalibrating && event == ChannelEvent.Event.NOTIFICATION_PROCESSING_START)
                {
                    getConsoleTextArea().appendText("\nWarning: a channel started during calibration, which can " +
                        "make the measurements less accurate.  Reset and calibrate again after stopping all channels " +
                        "if the results look wrong.");
                }

                updateControls();
            });
        }
    }

    /**
     * Indicates if any channels are processing.
     */
    private boolean isProcessing()
    {
        return mChannelProcessingManager != null && mChannelProcessingManager.isProcessing();
    }

    private Label getChannelsProcessingLabel()
    {
        if(mChannelsProcessingLabel == null)
        {
            mChannelsProcessingLabel = new Label("Stop all channels to calibrate");
            mChannelsProcessingLabel.getStyleClass().add("settings-help");
            mChannelsProcessingLabel.managedProperty().bind(mChannelsProcessingLabel.visibleProperty());
            mChannelsProcessingLabel.setVisible(false);
        }

        return mChannelsProcessingLabel;
    }

    private GridPane getResultsGrid()
    {
        if(mResultsGrid == null)
        {
            mResultsGrid = new GridPane();
            mResultsGrid.setHgap(16);

            //Operation, selected and speedup columns keep their full width and other candidates wraps in the rest
            for(int column = 0; column < 3; column++)
            {
                ColumnConstraints constraints = new ColumnConstraints();
                constraints.setMinWidth(Region.USE_PREF_SIZE);
                mResultsGrid.getColumnConstraints().add(constraints);
            }

            ColumnConstraints others = new ColumnConstraints();
            others.setHgrow(Priority.ALWAYS);
            others.setMinWidth(0);
            mResultsGrid.getColumnConstraints().add(others);
            mResultsGrid.setVgap(4);
            mResultsGrid.getStyleClass().add("settings-results");
        }

        return mResultsGrid;
    }

    /**
     * Rebuilds the results grid from the current calibrations.
     */
    private void updateResults()
    {
        GridPane grid = getResultsGrid();
        grid.getChildren().clear();

        String[] headers = {"Operation", "Selected", "Speedup", "Other candidates"};

        for(int column = 0; column < headers.length; column++)
        {
            Label header = new Label(headers[column]);
            header.getStyleClass().add("settings-results-header");
            grid.add(header, column, 0);
        }

        CalibrationManager manager = CalibrationManager.getInstance();
        List<Calibration> calibrations = manager.getCalibrationTypes().stream()
            .sorted(Comparator.comparing(CalibrationType::getDescription))
            .map(manager::getCalibration).toList();
        int row = 1;

        for(Calibration calibration : calibrations)
        {
            Optional<CalibrationResult> result = calibration.getResult();
            grid.add(new Label(calibration.getType().getDescription()), 0, row);
            grid.add(value(getShortName(calibration.getImplementation())), 1, row);
            grid.add(value(result.map(r -> SPEEDUP_FORMAT.format(r.getSelectedSpeedup()) + "x").orElse("-")), 2, row);

            Label others = new Label(result.map(PerformanceSettingsPage::getOtherCandidates).orElse(
                calibration.isCalibrated() ? "Recalibrate to measure" : ""));
            others.getStyleClass().add("settings-help");
            others.setWrapText(true);
            grid.add(others, 3, row);
            row++;
        }
    }

    private static Label value(String text)
    {
        Label label = new Label(text);
        label.getStyleClass().add("settings-value");
        return label;
    }

    /**
     * Describes the measured and rejected candidates other than the selected implementation.
     */
    private static String getOtherCandidates(CalibrationResult result)
    {
        List<String> others = new ArrayList<>();

        for(Map.Entry<Implementation,Double> entry : result.speedups().entrySet())
        {
            if(entry.getKey() != result.selected())
            {
                others.add(getShortName(entry.getKey()) + " " + SPEEDUP_FORMAT.format(entry.getValue()) + "x");
            }
        }

        for(Implementation rejected : result.rejected())
        {
            others.add(getShortName(rejected) + " rejected");
        }

        return String.join(", ", others);
    }

    /**
     * Short display name for an implementation: the vector width without the word vector.
     */
    private static String getShortName(Implementation implementation)
    {
        return getName(implementation).replace("Vector ", "");
    }

    /**
     * Display name for an implementation.
     */
    private static String getName(Implementation implementation)
    {
        return switch(implementation)
        {
            case SCALAR -> "Scalar";
            case VECTOR_SIMD_PREFERRED -> "Vector " + FloatVector.SPECIES_PREFERRED.vectorBitSize() + "-bit (native)";
            case VECTOR_SIMD_64 -> "Vector 64-bit";
            case VECTOR_SIMD_128 -> "Vector 128-bit";
            case VECTOR_SIMD_256 -> "Vector 256-bit";
            case VECTOR_SIMD_512 -> "Vector 512-bit";
            case UNCALIBRATED -> "Uncalibrated";
        };
    }

    private void enableConsoleLogging()
    {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(Calibration.class);
        ((ch.qos.logback.classic.Logger)logger).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger)logger).addAppender(getTextAreaLogAppender());
        getTextAreaLogAppender().start();
    }

    private void disableConsoleLogging()
    {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(Calibration.class);
        ((ch.qos.logback.classic.Logger)logger).detachAppender(getTextAreaLogAppender());
        getTextAreaLogAppender().stop();
    }

    private TextAreaLogAppender getTextAreaLogAppender()
    {
        if(mTextAreaLogAppender == null)
        {
            mTextAreaLogAppender = new TextAreaLogAppender(getConsoleTextArea(), "My Console Logger");
        }

        return mTextAreaLogAppender;
    }

    private TextArea getConsoleTextArea()
    {
        if(mConsoleTextArea == null)
        {
            mConsoleTextArea = new TextArea();
            mConsoleTextArea.setEditable(false);
            mConsoleTextArea.setPrefRowCount(10);
            mConsoleTextArea.setWrapText(true);
            mConsoleTextArea.getStyleClass().add("settings-value");
        }

        return mConsoleTextArea;
    }

    private ProgressBar getProgressBar()
    {
        if(mProgressBar == null)
        {
            mProgressBar = new ProgressBar();
            mProgressBar.setVisible(false);
        }

        return mProgressBar;
    }

    private Button getResetAllButton()
    {
        if(mResetAllButton == null)
        {
            mResetAllButton = new Button("Reset All Calibrations");
            mResetAllButton.setTooltip(new Tooltip("Reset all calibrations to an Uncalibrated state"));
            mResetAllButton.setOnAction(event ->
            {
                CalibrationManager.getInstance().reset();
                updateControls();
            });
        }

        return mResetAllButton;
    }

    private Button getCalibrateButton()
    {
        if(mCalibrateButton == null)
        {
            mCalibrateButton = new Button("Calibrate");
            mCalibrateButton.setTooltip(new Tooltip("Calibrate the items that need calibrated"));
            mCalibrateButton.setOnAction(event -> startCalibration(null));
        }

        return mCalibrateButton;
    }

    /**
     * Calibrates the uncalibrated calibrations in the background, unless a calibration is already running, channels
     * are processing, or there is nothing to calibrate.
     * @param onComplete runs once calibration finishes, or immediately when calibration doesn't start.  May be null.
     */
    private void startCalibration(Runnable onComplete)
    {
        if(mCalibrating || isProcessing() || CalibrationManager.getInstance().getUncalibrated().isEmpty())
        {
            updateControls();
            complete(onComplete);
            return;
        }

        mCalibrating = true;
        getCalibrateButton().setDisable(true);
        getResetAllButton().setDisable(true);
        getConsoleTextArea().clear();
        getConsoleTextArea().appendText("Calibrating.  Each calibration lasts a few seconds.");
        enableConsoleLogging();
        getCalibratingLabel().setVisible(true);
        getProgressBar().setVisible(true);
        getProgressBar().setProgress(0.0);

        ThreadPool.CACHED.submit(() ->
        {
            try
            {
                CalibrationManager manager = CalibrationManager.getInstance();
                List<Calibration> calibrations = manager.getUncalibrated();
                int counter = 0;
                for(Calibration calibration: calibrations)
                {
                    counter++;

                    final String message = "\n\nCalibration [" + counter + "/" + calibrations.size() + "] - " + calibration.getType();
                    Platform.runLater(() -> getConsoleTextArea().appendText(message));

                    try
                    {
                        calibration.calibrate();

                        final double progress = (double)counter / (double)calibrations.size();
                        Platform.runLater(() -> getProgressBar().setProgress(progress));
                    }
                    catch(CalibrationException ce)
                    {
                        mLog.error("Calibration error for " + calibration.getType(), ce);
                    }
                }
            }
            catch(Throwable t)
            {
                mLog.error("Error while performing calibrations", t);
            }

            Platform.runLater(() ->
            {
                getConsoleTextArea().appendText("\nCalibration Complete!\nChannels started from now on use the new " +
                    "calibrations.  Restart the application to also use them for tuners that already started channels.");
                getCalibratingLabel().setVisible(false);
                getProgressBar().setVisible(false);
                mCalibrating = false;
                updateControls();
                disableConsoleLogging();
                complete(onComplete);
            });
        });
    }

    /**
     * Starts calibrating when the page is opened by a calibrate request, such as from the calibration dialog at
     * startup.
     */
    @Override
    public void process(ViewSettingsEditorRequest request)
    {
        if(request instanceof CalibrateRequest calibrateRequest)
        {
            startCalibration(calibrateRequest.getOnComplete());
        }
    }

    /**
     * Runs the completion action, if there is one, logging rather than propagating any error so that the page stays
     * usable.
     */
    private void complete(Runnable onComplete)
    {
        if(onComplete != null)
        {
            try
            {
                onComplete.run();
            }
            catch(Exception e)
            {
                mLog.error("Error running the action after calibration", e);
            }
        }
    }

    private Label getCalibratingLabel()
    {
        if(mCalibratingLabel == null)
        {
            mCalibratingLabel = new Label("Calibrating:");
            mCalibratingLabel.setVisible(false);
        }

        return mCalibratingLabel;
    }

    private void updateControls()
    {
        int pending = CalibrationManager.getInstance().getUncalibrated().size();
        int total = CalibrationManager.getInstance().getCalibrationTypes().size();
        boolean processing = isProcessing();
        getCalibrationsPendingValue().setText(String.valueOf(pending));
        getCalibrateButton().setDisable(mCalibrating || pending == 0 || processing);
        getResetAllButton().setDisable(mCalibrating || pending == total);
        getChannelsProcessingLabel().setVisible(!mCalibrating && pending > 0 && processing);
        updateResults();
    }

    private Label getCalibrationsPendingValue()
    {
        if(mCalibrationsPendingValue == null)
        {
            mCalibrationsPendingValue = new Label(" ");
            mCalibrationsPendingValue.getStyleClass().add("settings-value");
        }

        return mCalibrationsPendingValue;
    }

    private ToggleSwitch getVectorEnabledToggleSwitch()
    {
        if(mVectorEnabled == null)
        {
            mVectorEnabled = new ToggleSwitch();
            mVectorEnabled.setTooltip(new Tooltip("Allow sdrtrunk to use optimized vector operations when supported by your CPU"));
            mVectorEnabled.setSelected(mSettings.isVectorEnabled());
            mVectorEnabled.selectedProperty()
                .addListener((observable, oldValue, enabled) -> mSettings.setVectorEnabled(enabled));
        }

        return mVectorEnabled;
    }

    private ToggleSwitch getHideDialogSwitch()
    {
        if(mHideDialogSwitch == null)
        {
            mHideDialogSwitch = new ToggleSwitch();
            mHideDialogSwitch.setTooltip(new Tooltip("Don't show dialog when new calibrations are available"));
            mHideDialogSwitch.setSelected(mSettings.isHideCalibrationDialog());
            mHideDialogSwitch.selectedProperty().addListener((observable, oldValue, hide) -> mSettings.setHideCalibrationDialog(hide));
        }

        return mHideDialogSwitch;
    }
}
