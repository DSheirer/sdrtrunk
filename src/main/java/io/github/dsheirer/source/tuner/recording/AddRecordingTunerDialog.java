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
package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.gui.control.FrequencyEntry;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredRecordingTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.source.wave.SDRSharpIQRecording;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for adding a recording tuner that replays a baseband recording file.
 */
public class AddRecordingTunerDialog extends Dialog<ButtonType>
{
    private final static Logger mLog = LoggerFactory.getLogger(AddRecordingTunerDialog.class);
    private static final String SELECT_A_FILE = "No recording selected";
    private static final String LAST_FILE_BROWSE_LOCATION_KEY = "AddRecordingTunerDialog.lastBrowseLocation";
    private static final ButtonType ADD = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
    private static final Pattern TUNER_RECORDING_PATTERN = Pattern.compile(".*_(\\d*)_baseband_\\d{8}_\\d{6}\\.wav");
    private final Settings mSettings;
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final TunerConfigurationManager mTunerConfigurationManager;
    private final Label mRecordingFileLabel = new Label(SELECT_A_FILE);
    private final TextField mFrequencyField = new TextField();
    private final Label mValidationLabel = new Label();
    private File mSelectedRecording;
    private Node mAddButton;

    /**
     * Constructs an instance
     * @param owner window for the dialog
     * @param settings for the default recording directory
     * @param discoveredTunerModel to add the recording tuner to
     * @param tunerConfigurationManager to add the recording tuner configuration to
     */
    public AddRecordingTunerDialog(Window owner, Settings settings, DiscoveredTunerModel discoveredTunerModel,
                                   TunerConfigurationManager tunerConfigurationManager)
    {
        Validate.notNull(settings, "Settings cannot be null");
        Validate.notNull(discoveredTunerModel, "TunerModel cannot be null");
        Validate.notNull(tunerConfigurationManager, "TunerConfigurationManager cannot be null");
        mDiscoveredTunerModel = discoveredTunerModel;
        mSettings = settings;
        mTunerConfigurationManager = tunerConfigurationManager;

        initOwner(owner);
        setTitle("Add Recording Tuner");
        setHeaderText("Replay a baseband recording (.wav or SDR# .iq) as a tuner");

        Button selectFileButton = new Button("Select ...");
        selectFileButton.setOnAction(e -> selectFile());
        mRecordingFileLabel.setWrapText(true);
        mFrequencyField.setPromptText("e.g. 851.0125");
        mValidationLabel.setWrapText(true);
        mValidationLabel.setStyle("-fx-text-fill: #d9443a;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.addRow(0, selectFileButton, mRecordingFileLabel);
        grid.addRow(1, new Label("Frequency (MHz)"), mFrequencyField);
        grid.add(mValidationLabel, 0, 2, 2, 1);
        GridPane.setHgrow(mFrequencyField, Priority.ALWAYS);
        grid.setPrefWidth(460);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ADD, ButtonType.CANCEL);
        mAddButton = getDialogPane().lookupButton(ADD);
        mAddButton.setDisable(true);
        mFrequencyField.textProperty().addListener((observable, oldValue, newValue) -> validate());

        setResultConverter(buttonType -> {
            if(buttonType == ADD)
            {
                addTuner();
            }
            return buttonType;
        });
    }

    private void selectFile()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Recording File");
        String lastBrowsedDirectory = SystemProperties.getInstance().get(LAST_FILE_BROWSE_LOCATION_KEY, "");
        File browseDirectory = (lastBrowsedDirectory != null && !lastBrowsedDirectory.isEmpty()) ?
                new File(lastBrowsedDirectory) : mSettings.getDirectorySettings().getDefaultRecordingDirectory().toFile();

        if(browseDirectory.isDirectory())
        {
            fileChooser.setInitialDirectory(browseDirectory);
        }

        FileChooser.ExtensionFilter basebandFilter = new FileChooser.ExtensionFilter("Baseband Recordings (*.wav, *.iq)",
                "*.wav", "*." + SDRSharpIQRecording.FILE_EXTENSION);
        fileChooser.getExtensionFilters().addAll(basebandFilter,
                new FileChooser.ExtensionFilter("Wave Recordings (*.wav)", "*.wav"),
                new FileChooser.ExtensionFilter("SDRSharp I/Q Recordings (*.iq)", "*." + SDRSharpIQRecording.FILE_EXTENSION));
        fileChooser.setSelectedExtensionFilter(basebandFilter);

        File selected = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());

        if(selected == null)
        {
            return;
        }

        if(selected.getParentFile() != null)
        {
            SystemProperties.getInstance().set(LAST_FILE_BROWSE_LOCATION_KEY, selected.getParentFile().getAbsolutePath());
        }

        mSelectedRecording = selected;

        if(SDRSharpIQRecording.hasIQExtension(selected))
        {
            SDRSharpIQRecording iqRecording = SDRSharpIQRecording.parse(selected);

            if(iqRecording == null)
            {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to determine the sample rate and center " +
                        "frequency from the I/Q recording file name.\n\nExpected format: " +
                        "yyyyMMdd-HHmmss_label_TUNER_123456789Hz_10MSps.iq");
                alert.initOwner(getDialogPane().getScene().getWindow());
                alert.setTitle("Unsupported I/Q Recording");
                alert.setHeaderText(null);
                alert.showAndWait();
                mSelectedRecording = null;
            }
            else
            {
                mLog.info("Selected " + iqRecording);

                if(mFrequencyField.getText() == null || mFrequencyField.getText().isEmpty())
                {
                    mFrequencyField.setText(FrequencyEntry.format(iqRecording.getCenterFrequency()));
                }
            }
        }
        else if(mFrequencyField.getText() == null || mFrequencyField.getText().isEmpty())
        {
            Matcher m = TUNER_RECORDING_PATTERN.matcher(selected.getName());

            if(m.matches() && !m.group(1).isEmpty())
            {
                try
                {
                    mFrequencyField.setText(FrequencyEntry.format(Long.parseLong(m.group(1))));
                }
                catch(NumberFormatException nfe)
                {
                    //Leave the frequency for the user to enter
                }
            }
        }

        mRecordingFileLabel.setText(mSelectedRecording != null ? mSelectedRecording.getName() : SELECT_A_FILE);
        validate();
    }

    private void validate()
    {
        String error = null;

        if(mSelectedRecording == null)
        {
            error = "Select a recording file";
        }
        else
        {
            Long frequency = FrequencyEntry.parse(mFrequencyField.getText());

            if(frequency == null || frequency <= 0 || frequency > Integer.MAX_VALUE)
            {
                error = "Enter the recording's center frequency, up to 2147.483647 MHz";
            }
        }

        mValidationLabel.setText(error == null ? "" : error);
        mAddButton.setDisable(error != null);
    }

    private void addTuner()
    {
        long frequency = FrequencyEntry.parse(mFrequencyField.getText());
        mLog.info("Adding recording tuner = frequency [" + frequency + "] recording [" +
                mSelectedRecording.getAbsolutePath() + "]");

        try
        {
            RecordingTunerConfiguration config = RecordingTunerConfiguration.create();
            config.setFrequency(frequency);
            config.setPath(mSelectedRecording.getAbsolutePath());
            mTunerConfigurationManager.addTunerConfiguration(config);
            DiscoveredRecordingTuner discoveredRecordingTuner = new DiscoveredRecordingTuner(mSettings, config);
            mDiscoveredTunerModel.addDiscoveredTuner(discoveredRecordingTuner);
        }
        catch(Exception ex)
        {
            mLog.error("Error adding recording tuner", ex);
        }
    }
}
