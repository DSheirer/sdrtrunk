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

package io.github.dsheirer.gui.preference.calibration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.CalibrateRequest;
import io.github.dsheirer.log.TextAreaLogAppender;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.calibration.VectorCalibrationPreference;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Preference settings for duplicate call audio handling
 */
public class VectorCalibrationPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(VectorCalibrationPreferenceEditor.class);
    private VectorCalibrationPreference mPreference;
    private GridPane mEditorPane;
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
    public VectorCalibrationPreferenceEditor(UserPreferences userPreferences)
    {
        MyEventBus.getGlobalEventBus().register(this);
        mPreference = userPreferences.getVectorCalibrationPreference();
        setMaxHeight(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);

        VBox vbox = new VBox();
        vbox.setMaxHeight(Double.MAX_VALUE);
        vbox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vbox, Priority.ALWAYS);

        VBox.setVgrow(getEditorPane(), Priority.NEVER);
        vbox.getChildren().add(getEditorPane());

        ScrollPane scrollPane = new ScrollPane(getConsoleTextArea());
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        vbox.getChildren().add(scrollPane);
        getChildren().add(vbox);
    }

    @Subscribe
    public void process(CalibrateRequest request)
    {
        getCalibrateButton().fire();
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            int row = 0;
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10));
            mEditorPane.setHgap(10);
            mEditorPane.setVgap(10);

            Label titleLabel = new Label("CPU Vector SIMD Preferences");
            GridPane.setConstraints(titleLabel, 0, row++, 2, 1);
            mEditorPane.getChildren().add(titleLabel);

            GridPane.setConstraints(getVectorEnabledToggleSwitch(), 0, row);
            GridPane.setHalignment(getVectorEnabledToggleSwitch(), HPos.RIGHT);
            mEditorPane.getChildren().add(getVectorEnabledToggleSwitch());

            Label enableSimdLabel = new Label("Enable SIMD Vector Operations");
            GridPane.setConstraints(enableSimdLabel, 1, row++);
            mEditorPane.getChildren().add((enableSimdLabel));

            GridPane.setConstraints(getHideDialogSwitch(), 0, row);
            GridPane.setHalignment(getHideDialogSwitch(), HPos.RIGHT);
            mEditorPane.getChildren().add(getHideDialogSwitch());

            Label hideDialogLabel = new Label("Don't Show Calibration Dialog When New Calibrations Are Available");
            GridPane.setConstraints(hideDialogLabel, 1, row++);
            mEditorPane.getChildren().add((hideDialogLabel));

            GridPane.setConstraints(getCalibrationsPendingValue(), 0, row);
            mEditorPane.getChildren().add(getCalibrationsPendingValue());

            Label calibrationsPendingLabel = new Label("Calibrations To Perform");
            GridPane.setHalignment(getCalibrationsPendingValue(), HPos.RIGHT);
            GridPane.setConstraints(calibrationsPendingLabel, 1, row++);
            mEditorPane.getChildren().add(calibrationsPendingLabel);
            updateControls();

            GridPane.setConstraints(getCalibratingLabel(), 0, row);
            mEditorPane.getChildren().add(getCalibratingLabel());

            GridPane.setConstraints(getProgressBar(), 1, row++);
            mEditorPane.getChildren().add(getProgressBar());

            HBox buttonsBox = new HBox();
            buttonsBox.setSpacing(10);
            buttonsBox.getChildren().addAll(getCalibrateButton(), getResetAllButton());
            GridPane.setHalignment(buttonsBox, HPos.CENTER);
            GridPane.setConstraints(buttonsBox, 0, row, 2, 1);
            mEditorPane.getChildren().add(buttonsBox);
        }

        return mEditorPane;
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
            mConsoleTextArea.setDisable(true);
            mConsoleTextArea.setMaxHeight(Double.MAX_VALUE);
            mConsoleTextArea.setMaxWidth(Double.MAX_VALUE);
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
        }

        return mCalibrationsPendingValue;
    }

    private ToggleSwitch getVectorEnabledToggleSwitch()
    {
        if(mVectorEnabled == null)
        {
            mVectorEnabled = new ToggleSwitch();
            mVectorEnabled.setTooltip(new Tooltip("Allow sdrtrunk to use optimized vector operations when supported by your CPU"));
            mVectorEnabled.setSelected(mPreference.isVectorEnabled());
            mVectorEnabled.selectedProperty()
                .addListener((observable, oldValue, enabled) -> mPreference.setVectorEnabled(enabled));
        }

        return mVectorEnabled;
    }

    private ToggleSwitch getHideDialogSwitch()
    {
        if(mHideDialogSwitch == null)
        {
            mHideDialogSwitch = new ToggleSwitch();
            mHideDialogSwitch.setTooltip(new Tooltip("Don't show dialog when new calibrations are available"));
            mHideDialogSwitch.setSelected(mPreference.isHideCalibrationDialog());
            mHideDialogSwitch.selectedProperty().addListener((observable, oldValue, hide) -> mPreference.setHideCalibrationDialog(hide));
        }

        return mHideDialogSwitch;
    }
}
