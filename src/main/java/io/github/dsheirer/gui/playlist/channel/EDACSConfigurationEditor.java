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
import io.github.dsheirer.module.decode.edacs.DecodeConfigEDACS;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EDACS channel configuration editor
 */
public class EDACSConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSConfigurationEditor.class);
    private TitledPane mAuxDecoderPane;
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private AuxDecoderConfigurationEditor mAuxDecoderConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private TextArea mLcnFrequencyEditor;
    private ComboBox<DecodeConfigEDACS.VoiceMode> mVoiceModeComboBox;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private TextField mVoiceMarkerTextField;

    public EDACSConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.EDACS;
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
            mDecoderPane.setText("Decoder: EDACS");
            mDecoderPane.setExpanded(false);

            VBox vbox = new VBox(5);
            vbox.setPadding(new Insets(10, 10, 10, 10));

            Label label = new Label("LCN Frequencies (one per line, Hz, LCN 1 first):");
            vbox.getChildren().add(label);

            mLcnFrequencyEditor = new TextArea();
            mLcnFrequencyEditor.setPrefRowCount(12);
            mLcnFrequencyEditor.setPrefColumnCount(20);
            mLcnFrequencyEditor.setPromptText("851175000\n851225000\n851425000\n...");
            mLcnFrequencyEditor.textProperty().addListener((obs, old, val) -> modifiedProperty().set(true));
            vbox.getChildren().add(mLcnFrequencyEditor);

            GridPane voiceGrid = new GridPane();
            voiceGrid.setHgap(10);
            voiceGrid.setVgap(5);
            voiceGrid.setPadding(new Insets(8, 0, 0, 0));

            Label voiceModeLabel = new Label("Voice Mode");
            GridPane.setHalignment(voiceModeLabel, HPos.RIGHT);
            GridPane.setConstraints(voiceModeLabel, 0, 0);
            voiceGrid.getChildren().add(voiceModeLabel);

            GridPane.setConstraints(getVoiceModeComboBox(), 1, 0);
            voiceGrid.getChildren().add(getVoiceModeComboBox());

            Label poolSizeLabel = new Label("Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 0, 1);
            voiceGrid.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 1, 1);
            voiceGrid.getChildren().add(getTrafficChannelPoolSizeSpinner());

            Label voiceMarkerLabel = new Label("ProVoice Voice Marker (hex)");
            GridPane.setHalignment(voiceMarkerLabel, HPos.RIGHT);
            GridPane.setConstraints(voiceMarkerLabel, 0, 2);
            voiceGrid.getChildren().add(voiceMarkerLabel);

            GridPane.setConstraints(getVoiceMarkerTextField(), 1, 2);
            voiceGrid.getChildren().add(getVoiceMarkerTextField());

            vbox.getChildren().add(voiceGrid);

            Label scopeLabel = new Label("Tested on EDACS Extended Addressing (EA) with ProVoice voice only. Standard/Networked " +
                "addressing, analog voice and narrowband are untested. The voice marker default is from one system and " +
                "may differ on others.");
            scopeLabel.setWrapText(true);
            scopeLabel.setPadding(new Insets(8, 0, 0, 0));
            vbox.getChildren().add(scopeLabel);

            mDecoderPane.setContent(vbox);
        }

        return mDecoderPane;
    }

    private ComboBox<DecodeConfigEDACS.VoiceMode> getVoiceModeComboBox()
    {
        if(mVoiceModeComboBox == null)
        {
            mVoiceModeComboBox = new ComboBox<>();
            mVoiceModeComboBox.getItems().addAll(DecodeConfigEDACS.VoiceMode.values());
            mVoiceModeComboBox.getSelectionModel().select(DecodeConfigEDACS.VoiceMode.ANALOG);
            mVoiceModeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mVoiceModeComboBox;
    }

    private TextField getVoiceMarkerTextField()
    {
        if(mVoiceMarkerTextField == null)
        {
            mVoiceMarkerTextField = new TextField();
            mVoiceMarkerTextField.setPrefColumnCount(4);
            mVoiceMarkerTextField.setPromptText("none");
            mVoiceMarkerTextField.setTooltip(new javafx.scene.control.Tooltip(
                    "16-bit marker (4 hex digits) carried by every ProVoice voice frame on this system. Frames without " +
                    "it are signalling and are not played. Leave empty to play every frame, then read the MARKER " +
                    "value from the traffic channel's decoded messages to find it."));
            //Up to 4 hex digits
            mVoiceMarkerTextField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("[0-9a-fA-F]{0,4}") ? change : null));
            mVoiceMarkerTextField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mVoiceMarkerTextField;
    }

    private Spinner<Integer> getTrafficChannelPoolSizeSpinner()
    {
        if(mTrafficChannelPoolSizeSpinner == null)
        {
            mTrafficChannelPoolSizeSpinner = new Spinner<>();
            mTrafficChannelPoolSizeSpinner.setTooltip(new javafx.scene.control.Tooltip(
                    "Maximum simultaneous EDACS traffic channels. Lower values reduce CPU load on busy systems."));
            mTrafficChannelPoolSizeSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,
                    DecodeConfigEDACS.TRAFFIC_CHANNEL_LIMIT_DEFAULT, DecodeConfigEDACS.TRAFFIC_CHANNEL_LIMIT_DEFAULT_EDACS);
            mTrafficChannelPoolSizeSpinner.setValueFactory(svf);
            mTrafficChannelPoolSizeSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTrafficChannelPoolSizeSpinner;
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
                DecodeConfigEDACS.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigEDACS.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigEDACS.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

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
            //Same options as the P25 Phase 1, DMR and NXDN editors.  Traffic channels only log the TRAFFIC types
            //(EventLogManager), so without them EDACS traffic channel events and messages could not be logged.
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
            types.add(RecorderType.TRAFFIC_MBE_CALL_SEQUENCE);
            mRecordConfigurationEditor = new RecordConfigurationEditor(types);
            mRecordConfigurationEditor.setDisable(true);
            mRecordConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordConfigurationEditor;
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        mLcnFrequencyEditor.setDisable(config == null);
        getVoiceModeComboBox().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);
        getVoiceMarkerTextField().setDisable(config == null);

        if(config instanceof DecodeConfigEDACS decodeConfigEDACS)
        {
            getVoiceMarkerTextField().setText(decodeConfigEDACS.getProVoiceVoiceMarker());
            String freqs = decodeConfigEDACS.getLcnFrequencies();
            mLcnFrequencyEditor.setText(freqs != null ? freqs.replace(",", "\n") : "");
            getVoiceModeComboBox().getSelectionModel().select(decodeConfigEDACS.getVoiceMode());
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(decodeConfigEDACS.getTrafficChannelPoolSize());
        }
        else
        {
            mLcnFrequencyEditor.setText("");
            getVoiceMarkerTextField().setText(DecodeConfigEDACS.PROVOICE_VOICE_MARKER_DEFAULT);
            getVoiceModeComboBox().getSelectionModel().select(DecodeConfigEDACS.VoiceMode.ANALOG);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(DecodeConfigEDACS.TRAFFIC_CHANNEL_LIMIT_DEFAULT_EDACS);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigEDACS config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigEDACS)
        {
            config = (DecodeConfigEDACS)getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigEDACS();
        }

        //Store as comma-separated: convert newlines to commas, strip whitespace
        String text = mLcnFrequencyEditor.getText().trim();
        String csv = text.replaceAll("[\\r\\n]+", ",").replaceAll("\\s", "");
        config.setLcnFrequencies(csv);
        config.setVoiceMode(getVoiceModeComboBox().getSelectionModel().getSelectedItem());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());
        config.setProVoiceVoiceMarker(getVoiceMarkerTextField().getText());

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
