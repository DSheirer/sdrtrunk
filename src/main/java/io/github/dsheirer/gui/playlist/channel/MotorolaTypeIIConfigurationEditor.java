/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.gui.playlist.decoder.AuxDecoderConfigurationEditor;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.moto.AnalogDeemphasis;
import io.github.dsheirer.module.decode.moto.BandplanType;
import io.github.dsheirer.module.decode.moto.DecodeConfigMotorolaTypeII;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Motorola Type II trunking channel configuration editor
 */
public class MotorolaTypeIIConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(MotorolaTypeIIConfigurationEditor.class);
    private TitledPane mAuxDecoderPane;
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private AuxDecoderConfigurationEditor mAuxDecoderConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private ComboBox<BandplanType> mBandplanComboBox;
    private TextField mObtBaseFrequencyField;
    private TextField mObtSpacingField;
    private TextField mObtOffsetField;
    private Label mObtBaseFrequencyLabel;
    private Label mObtSpacingLabel;
    private Label mObtOffsetLabel;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private ComboBox<AnalogDeemphasis> mAnalogDeemphasisComboBox;

    public MotorolaTypeIIConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                              UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
        getTitledPanesBox().getChildren().add(getSourcePane());
        getTitledPanesBox().getChildren().add(getDecoderPane());
        getTitledPanesBox().getChildren().add(getAuxDecoderPane());
        getTitledPanesBox().getChildren().add(getEventLogPane());
        getTitledPanesBox().getChildren().add(getRecordPane());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MOTOROLA_TYPE_II;
    }

    private TitledPane getSourcePane()
    {
        if(mSourcePane == null)
        {
            mSourcePane = new TitledPane("Source", getSourceConfigurationEditor());
            mSourcePane.setExpanded(true);
        }

        return mSourcePane;
    }

    private TitledPane getDecoderPane()
    {
        if(mDecoderPane == null)
        {
            mDecoderPane = new TitledPane();
            mDecoderPane.setText("Decoder: Motorola Type II");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            int row = 0;

            Label bandplanLabel = new Label("Bandplan");
            GridPane.setHalignment(bandplanLabel, HPos.RIGHT);
            GridPane.setConstraints(bandplanLabel, 0, row);
            gridPane.getChildren().add(bandplanLabel);

            GridPane.setConstraints(getBandplanComboBox(), 1, row);
            gridPane.getChildren().add(getBandplanComboBox());

            Label poolSizeLabel = new Label("Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 2, row);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 3, row);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            row++;

            Label deemphasisLabel = new Label("Analog De-emphasis");
            GridPane.setHalignment(deemphasisLabel, HPos.RIGHT);
            GridPane.setConstraints(deemphasisLabel, 0, row);
            gridPane.getChildren().add(deemphasisLabel);

            GridPane.setConstraints(getAnalogDeemphasisComboBox(), 1, row);
            gridPane.getChildren().add(getAnalogDeemphasisComboBox());

            row++;

            mObtBaseFrequencyLabel = new Label("Base Freq (MHz)");
            GridPane.setHalignment(mObtBaseFrequencyLabel, HPos.RIGHT);
            GridPane.setConstraints(mObtBaseFrequencyLabel, 0, row);
            gridPane.getChildren().add(mObtBaseFrequencyLabel);

            GridPane.setConstraints(getObtBaseFrequencyField(), 1, row);
            gridPane.getChildren().add(getObtBaseFrequencyField());

            mObtSpacingLabel = new Label("Spacing (kHz)");
            GridPane.setHalignment(mObtSpacingLabel, HPos.RIGHT);
            GridPane.setConstraints(mObtSpacingLabel, 2, row);
            gridPane.getChildren().add(mObtSpacingLabel);

            GridPane.setConstraints(getObtSpacingField(), 3, row);
            gridPane.getChildren().add(getObtSpacingField());

            row++;

            mObtOffsetLabel = new Label("Offset");
            GridPane.setHalignment(mObtOffsetLabel, HPos.RIGHT);
            GridPane.setConstraints(mObtOffsetLabel, 0, row);
            gridPane.getChildren().add(mObtOffsetLabel);

            GridPane.setConstraints(getObtOffsetField(), 1, row);
            gridPane.getChildren().add(getObtOffsetField());

            Label obtHelpLabel = new Label("OBT parameters are only used when bandplan is set to 'Other Band Trunking'");
            GridPane.setConstraints(obtHelpLabel, 0, row + 1, 4, 1);
            gridPane.getChildren().add(obtHelpLabel);

            Label analogOnlyLabel = new Label("Analog voice only. Digital voice calls are listed as events and not played.");
            GridPane.setConstraints(analogOnlyLabel, 0, row + 2, 4, 1);
            gridPane.getChildren().add(analogOnlyLabel);

            updateObtFieldsVisibility();

            mDecoderPane.setContent(gridPane);
        }

        return mDecoderPane;
    }

    private TitledPane getEventLogPane()
    {
        if(mEventLogPane == null)
        {
            mEventLogPane = new TitledPane("Logging", getEventLogConfigurationEditor());
            mEventLogPane.setExpanded(false);
        }

        return mEventLogPane;
    }

    private TitledPane getAuxDecoderPane()
    {
        if(mAuxDecoderPane == null)
        {
            mAuxDecoderPane = new TitledPane("Additional Decoders", getAuxDecoderConfigurationEditor());
            mAuxDecoderPane.setExpanded(false);
        }

        return mAuxDecoderPane;
    }

    private TitledPane getRecordPane()
    {
        if(mRecordPane == null)
        {
            mRecordPane = new TitledPane();
            mRecordPane.setText("Recording");
            mRecordPane.setExpanded(false);

            Label notice = new Label("Note: use aliases to control call audio recording");
            notice.setPadding(new Insets(10, 10, 0, 10));

            VBox vBox = new VBox();
            vBox.getChildren().addAll(getRecordConfigurationEditor(), notice);

            mRecordPane.setContent(vBox);
        }

        return mRecordPane;
    }

    private SourceConfigurationEditor getSourceConfigurationEditor()
    {
        if(mSourceConfigurationEditor == null)
        {
            mSourceConfigurationEditor = new FrequencyEditor(mTunerManager,
                DecodeConfigMotorolaTypeII.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigMotorolaTypeII.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigMotorolaTypeII.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

            mSourceConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSourceConfigurationEditor;
    }

    private EventLogConfigurationEditor getEventLogConfigurationEditor()
    {
        if(mEventLogConfigurationEditor == null)
        {
            List<EventLogType> types = new ArrayList<>();
            types.add(EventLogType.CALL_EVENT);
            types.add(EventLogType.DECODED_MESSAGE);
            types.add(EventLogType.TRAFFIC_CALL_EVENT);
            types.add(EventLogType.TRAFFIC_DECODED_MESSAGE);

            mEventLogConfigurationEditor = new EventLogConfigurationEditor(types);
            mEventLogConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mEventLogConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEventLogConfigurationEditor;
    }

    private AuxDecoderConfigurationEditor getAuxDecoderConfigurationEditor()
    {
        if(mAuxDecoderConfigurationEditor == null)
        {
            List<DecoderType> types = new ArrayList<>();
            types.add(DecoderType.FLEETSYNC2);
            types.add(DecoderType.MDC1200);
            mAuxDecoderConfigurationEditor = new AuxDecoderConfigurationEditor(types);
            mAuxDecoderConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mAuxDecoderConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAuxDecoderConfigurationEditor;
    }

    private RecordConfigurationEditor getRecordConfigurationEditor()
    {
        if(mRecordConfigurationEditor == null)
        {
            List<RecorderType> types = new ArrayList<>();
            types.add(RecorderType.BASEBAND);
            types.add(RecorderType.TRAFFIC_BASEBAND);
            mRecordConfigurationEditor = new RecordConfigurationEditor(types);
            mRecordConfigurationEditor.setDisable(true);
            mRecordConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordConfigurationEditor;
    }

    private ComboBox<BandplanType> getBandplanComboBox()
    {
        if(mBandplanComboBox == null)
        {
            mBandplanComboBox = new ComboBox<>();
            mBandplanComboBox.getItems().addAll(BandplanType.values());
            mBandplanComboBox.getSelectionModel().select(BandplanType.EIGHT_HUNDRED_REBANDED);
            mBandplanComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    modifiedProperty().set(true);
                    updateObtFieldsVisibility();
                });
        }

        return mBandplanComboBox;
    }

    private TextField getObtBaseFrequencyField()
    {
        if(mObtBaseFrequencyField == null)
        {
            mObtBaseFrequencyField = new TextField();
            mObtBaseFrequencyField.setPromptText("e.g. 406.0625");
            mObtBaseFrequencyField.setTooltip(new Tooltip("Base frequency in MHz for OBT bandplan"));
            mObtBaseFrequencyField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mObtBaseFrequencyField;
    }

    private TextField getObtSpacingField()
    {
        if(mObtSpacingField == null)
        {
            mObtSpacingField = new TextField();
            mObtSpacingField.setPromptText("e.g. 25.0");
            mObtSpacingField.setTooltip(new Tooltip("Channel spacing in kHz for OBT bandplan"));
            mObtSpacingField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mObtSpacingField;
    }

    private TextField getObtOffsetField()
    {
        if(mObtOffsetField == null)
        {
            mObtOffsetField = new TextField();
            mObtOffsetField.setPromptText("e.g. 380");
            mObtOffsetField.setTooltip(new Tooltip("Transmit/receive offset in channels for OBT bandplan"));
            mObtOffsetField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mObtOffsetField;
    }

    private Spinner<Integer> getTrafficChannelPoolSizeSpinner()
    {
        if(mTrafficChannelPoolSizeSpinner == null)
        {
            mTrafficChannelPoolSizeSpinner = new Spinner();
            mTrafficChannelPoolSizeSpinner.setTooltip(
                new Tooltip("Maximum number of traffic channels that can be created by the decoder"));
            mTrafficChannelPoolSizeSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 4);
            mTrafficChannelPoolSizeSpinner.setValueFactory(svf);
            mTrafficChannelPoolSizeSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTrafficChannelPoolSizeSpinner;
    }

    private ComboBox<AnalogDeemphasis> getAnalogDeemphasisComboBox()
    {
        if(mAnalogDeemphasisComboBox == null)
        {
            mAnalogDeemphasisComboBox = new ComboBox<>();
            mAnalogDeemphasisComboBox.getItems().addAll(AnalogDeemphasis.values());
            mAnalogDeemphasisComboBox.getSelectionModel().select(DecodeConfigMotorolaTypeII.ANALOG_DEEMPHASIS_DEFAULT);
            mAnalogDeemphasisComboBox.setTooltip(new Tooltip("De-emphasis for analog voice.  More hides more of " +
                "the noise from simulcast fades and dulls the voice more.  Settings other than None also remove " +
                "audio above 3 kHz.  Takes effect on the next call."));
            mAnalogDeemphasisComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAnalogDeemphasisComboBox;
    }

    private void updateObtFieldsVisibility()
    {
        boolean obtSelected = mBandplanComboBox != null &&
            mBandplanComboBox.getSelectionModel().getSelectedItem() == BandplanType.OBT;

        if(mObtBaseFrequencyField != null)
        {
            mObtBaseFrequencyField.setVisible(obtSelected);
            mObtBaseFrequencyField.setManaged(obtSelected);
        }
        if(mObtSpacingField != null)
        {
            mObtSpacingField.setVisible(obtSelected);
            mObtSpacingField.setManaged(obtSelected);
        }
        if(mObtOffsetField != null)
        {
            mObtOffsetField.setVisible(obtSelected);
            mObtOffsetField.setManaged(obtSelected);
        }
        if(mObtBaseFrequencyLabel != null)
        {
            mObtBaseFrequencyLabel.setVisible(obtSelected);
            mObtBaseFrequencyLabel.setManaged(obtSelected);
        }
        if(mObtSpacingLabel != null)
        {
            mObtSpacingLabel.setVisible(obtSelected);
            mObtSpacingLabel.setManaged(obtSelected);
        }
        if(mObtOffsetLabel != null)
        {
            mObtOffsetLabel.setVisible(obtSelected);
            mObtOffsetLabel.setManaged(obtSelected);
        }
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);
        getBandplanComboBox().setDisable(config == null);
        getAnalogDeemphasisComboBox().setDisable(config == null);

        if(config instanceof DecodeConfigMotorolaTypeII decodeConfig)
        {
            getBandplanComboBox().getSelectionModel().select(decodeConfig.getBandplanType());
            getObtBaseFrequencyField().setText(String.valueOf(decodeConfig.getObtBaseFrequency()));
            getObtSpacingField().setText(String.valueOf(decodeConfig.getObtSpacing()));
            getObtOffsetField().setText(String.valueOf(decodeConfig.getObtOffset()));
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(decodeConfig.getTrafficChannelPoolSize());
            getAnalogDeemphasisComboBox().getSelectionModel().select(decodeConfig.getAnalogDeemphasis());
        }
        else
        {
            getBandplanComboBox().getSelectionModel().select(BandplanType.EIGHT_HUNDRED_REBANDED);
            getObtBaseFrequencyField().setText("");
            getObtSpacingField().setText("");
            getObtOffsetField().setText("");
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(4);
            getAnalogDeemphasisComboBox().getSelectionModel().select(DecodeConfigMotorolaTypeII.ANALOG_DEEMPHASIS_DEFAULT);
        }

        updateObtFieldsVisibility();
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigMotorolaTypeII config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigMotorolaTypeII existing)
        {
            config = existing;
        }
        else
        {
            config = new DecodeConfigMotorolaTypeII();
        }

        config.setBandplanType(getBandplanComboBox().getSelectionModel().getSelectedItem());

        try
        {
            String baseFreqText = getObtBaseFrequencyField().getText();
            if(baseFreqText != null && !baseFreqText.isEmpty())
            {
                config.setObtBaseFrequency(Double.parseDouble(baseFreqText));
            }
        }
        catch(NumberFormatException e)
        {
            mLog.error("Invalid OBT base frequency value: " + getObtBaseFrequencyField().getText());
        }

        try
        {
            String spacingText = getObtSpacingField().getText();
            if(spacingText != null && !spacingText.isEmpty())
            {
                config.setObtSpacing(Double.parseDouble(spacingText));
            }
        }
        catch(NumberFormatException e)
        {
            mLog.error("Invalid OBT spacing value: " + getObtSpacingField().getText());
        }

        try
        {
            String offsetText = getObtOffsetField().getText();
            if(offsetText != null && !offsetText.isEmpty())
            {
                config.setObtOffset(Integer.parseInt(offsetText));
            }
        }
        catch(NumberFormatException e)
        {
            mLog.error("Invalid OBT offset value: " + getObtOffsetField().getText());
        }

        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());
        config.setAnalogDeemphasis(getAnalogDeemphasisComboBox().getSelectionModel().getSelectedItem());

        getItem().setDecodeConfiguration(config);
    }

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config)
    {
        getEventLogConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveEventLogConfiguration()
    {
        getEventLogConfigurationEditor().save();

        if(getEventLogConfigurationEditor().getItem().getLoggers().isEmpty())
        {
            getItem().setEventLogConfiguration(null);
        }
        else
        {
            getItem().setEventLogConfiguration(getEventLogConfigurationEditor().getItem());
        }
    }

    @Override
    protected void setAuxDecoderConfiguration(AuxDecodeConfiguration config)
    {
        getAuxDecoderConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveAuxDecoderConfiguration()
    {
        getAuxDecoderConfigurationEditor().save();

        if(getAuxDecoderConfigurationEditor().getItem().getAuxDecoders().isEmpty())
        {
            getItem().setAuxDecodeConfiguration(null);
        }
        else
        {
            getItem().setAuxDecodeConfiguration(getAuxDecoderConfigurationEditor().getItem());
        }
    }

    @Override
    protected void setRecordConfiguration(RecordConfiguration config)
    {
        getRecordConfigurationEditor().setDisable(config == null);
        getRecordConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveRecordConfiguration()
    {
        getRecordConfigurationEditor().save();
        RecordConfiguration config = getRecordConfigurationEditor().getItem();
        getItem().setRecordConfiguration(config);
    }

    @Override
    protected void setSourceConfiguration(SourceConfiguration config)
    {
        getSourceConfigurationEditor().setSourceConfiguration(config);
    }

    @Override
    protected void saveSourceConfiguration()
    {
        getSourceConfigurationEditor().save();
        SourceConfiguration sourceConfiguration = getSourceConfigurationEditor().getSourceConfiguration();
        getItem().setSourceConfiguration(sourceConfiguration);
    }
}
