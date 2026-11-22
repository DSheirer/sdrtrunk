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
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.log.TextAreaLogAppender;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.calibration.VectorCalibrationSettings;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance settings: CPU vector (SIMD) operations and their calibration
 */
public class PerformanceSettingsPage extends SettingsPage
{
    private final static Logger mLog = LoggerFactory.getLogger(PerformanceSettingsPage.class);
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

    /**
     * Constructs an instance
     */
    public PerformanceSettingsPage(Settings settings)
    {
        super(SettingsPageType.PERFORMANCE, settings);
        MyEventBus.getGlobalEventBus().register(this);
        mSettings = settings.getVectorCalibrationSettings();

        SettingsSection vector = addSection("CPU Vector (SIMD) Operations");
        vector.addRow("Enable SIMD vector operations", getVectorEnabledToggleSwitch())
            .defaultValue(getVectorEnabledToggleSwitch().selectedProperty(),
                VectorCalibrationSettings.DEFAULT_VECTOR_ENABLED, getVectorEnabledToggleSwitch()::setSelected)
            .help("Allows sdrtrunk to use optimized vector operations when supported by your CPU.");

        SettingsSection calibration = addSection("Calibration");
        calibration.addHelp("Calibration measures the scalar and vector version of each operation on this computer " +
            "and selects the fastest.  Each calibration lasts 20 - 40 seconds.  Restart the application to use new " +
            "calibrations.");
        calibration.addRow("Calibrations to perform", getCalibrationsPendingValue());
        calibration.addRow("Don't show the calibration dialog when new calibrations are available",
            getHideDialogSwitch())
            .defaultValue(getHideDialogSwitch().selectedProperty(),
                VectorCalibrationSettings.DEFAULT_HIDE_CALIBRATION_DIALOG, getHideDialogSwitch()::setSelected);
        updateControls();

        HBox progress = new HBox(8, getCalibratingLabel(), getProgressBar());
        progress.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(getProgressBar(), Priority.ALWAYS);
        getProgressBar().setMaxWidth(Double.MAX_VALUE);
        //Only show the progress row while calibrating
        progress.visibleProperty().bind(getProgressBar().visibleProperty());
        progress.managedProperty().bind(getProgressBar().visibleProperty());
        HBox buttons = new HBox(10, getCalibrateButton(), getResetAllButton());
        calibration.addContent(buttons);
        calibration.addContent(progress);
        calibration.addContent(getConsoleTextArea());
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
            mCalibrateButton.setOnAction(event ->
            {
                getCalibrateButton().setDisable(true);
                getResetAllButton().setDisable(true);
                getConsoleTextArea().clear();
                getConsoleTextArea().appendText("Calibrating.  Each calibration lasts 20 - 40 seconds.");
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
                        getConsoleTextArea().appendText("\nCalibration Complete!\nNote: restart the application to use these new settings.");
                        getCalibratingLabel().setVisible(false);
                        getProgressBar().setVisible(false);
                        updateControls();
                        disableConsoleLogging();
                    });
                });
            });
        }

        return mCalibrateButton;
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
        getCalibrationsPendingValue().setText(String.valueOf(pending));
        getCalibrateButton().setDisable(pending == 0);
        getResetAllButton().setDisable(pending == total);
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
