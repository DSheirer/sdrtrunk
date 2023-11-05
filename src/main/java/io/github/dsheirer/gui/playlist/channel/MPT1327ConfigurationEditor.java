/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channelMap.ViewChannelMapEditorRequest;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.dsheirer.module.decode.config.DecodeConfiguration.CALL_TIMEOUT_MAXIMUM;
import static io.github.dsheirer.module.decode.config.DecodeConfiguration.CALL_TIMEOUT_MINIMUM;

/**
 * MPT-1327 channel configuration editor
 */
public class MPT1327ConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327ConfigurationEditor.class);
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private ComboBox<ChannelMap> mChannelMapComboBox;
    private Button mChannelMapEditButton;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private Spinner<Integer> mCallTimeoutSpinner;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public MPT1327ConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                      UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
        getTitledPanesBox().getChildren().add(getSourcePane());
        getTitledPanesBox().getChildren().add(getDecoderPane());
        getTitledPanesBox().getChildren().add(getEventLogPane());
        getTitledPanesBox().getChildren().add(getRecordPane());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MPT1327;
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
            mDecoderPane.setText("Decoder: MPT-1327");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            Label channelMapLabel = new Label("Channel Map");
            GridPane.setHalignment(channelMapLabel, HPos.RIGHT);
            GridPane.setConstraints(channelMapLabel, 0, 0);
            gridPane.getChildren().add(channelMapLabel);

            GridPane.setConstraints(getChannelMapComboBox(), 1, 0, 2, 1,
                HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES);
            gridPane.getChildren().add(getChannelMapComboBox());

            GridPane.setConstraints(getChannelMapEditButton(), 3, 0);
            gridPane.getChildren().add(getChannelMapEditButton());

            Label poolSizeLabel = new Label("Max Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 0, 1);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 1, 1);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            Label callTimeoutLabel = new Label("Call Timeout Seconds");
            GridPane.setHalignment(callTimeoutLabel, HPos.RIGHT);
            GridPane.setConstraints(callTimeoutLabel, 2, 1);
            gridPane.getChildren().add(callTimeoutLabel);

            GridPane.setConstraints(getCallTimeoutSpinner(), 3, 1);
            gridPane.getChildren().add(getCallTimeoutSpinner());

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
                DecodeConfigMPT1327.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigMPT1327.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigMPT1327.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

            //Add a listener so that we can push change notifications up to this editor
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

    private RecordConfigurationEditor getRecordConfigurationEditor()
    {
        if(mRecordConfigurationEditor == null)
        {
            List<RecorderType> types = new ArrayList<>();
            types.add(RecorderType.BASEBAND);
            types.add(RecorderType.DEMODULATED_BIT_STREAM);
            types.add(RecorderType.TRAFFIC_BASEBAND);
            types.add(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM);
            mRecordConfigurationEditor = new RecordConfigurationEditor(types);
            mRecordConfigurationEditor.setDisable(true);
            mRecordConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordConfigurationEditor;
    }

    private ComboBox<ChannelMap> getChannelMapComboBox()
    {
        if(mChannelMapComboBox == null)
        {
            mChannelMapComboBox = new ComboBox<>();
            mChannelMapComboBox.setMaxWidth(Double.MAX_VALUE);
            mChannelMapComboBox.setDisable(true);
            mChannelMapComboBox.setTooltip(new Tooltip("Select a channel map to use for this system"));
            mChannelMapComboBox.setItems(getPlaylistManager().getChannelMapModel().getChannelMaps());
            mChannelMapComboBox.setDisable(true);
            mChannelMapComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mChannelMapComboBox;
    }

    private Button getChannelMapEditButton()
    {
        if(mChannelMapEditButton == null)
        {
            mChannelMapEditButton = new Button("Channel Map Editor");
            mChannelMapEditButton.setTooltip(new Tooltip("Open the channel map editor to add/edit maps"));
            mChannelMapEditButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    String channelMapName = null;

                    if(getChannelMapComboBox().getSelectionModel().getSelectedItem() != null)
                    {
                        channelMapName = getChannelMapComboBox().getSelectionModel().getSelectedItem().getName();
                    }

                    MyEventBus.getGlobalEventBus().post(new ViewChannelMapEditorRequest(channelMapName));
                }
            });
        }

        return mChannelMapEditButton;
    }

    private Spinner<Integer> getTrafficChannelPoolSizeSpinner()
    {
        if(mTrafficChannelPoolSizeSpinner == null)
        {
            mTrafficChannelPoolSizeSpinner = new Spinner();
            mTrafficChannelPoolSizeSpinner.setDisable(true);
            mTrafficChannelPoolSizeSpinner.setTooltip(
                new Tooltip("Maximum number of traffic channels that can be created by the decoder"));
            mTrafficChannelPoolSizeSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
            mTrafficChannelPoolSizeSpinner.setValueFactory(svf);
            mTrafficChannelPoolSizeSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTrafficChannelPoolSizeSpinner;
    }

    private Spinner<Integer> getCallTimeoutSpinner()
    {
        if(mCallTimeoutSpinner == null)
        {
            mCallTimeoutSpinner = new Spinner<>();
            mCallTimeoutSpinner.setDisable(true);
            mCallTimeoutSpinner.setTooltip(new Tooltip("Maximum call limit in seconds"));
            mCallTimeoutSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            var svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(CALL_TIMEOUT_MINIMUM, CALL_TIMEOUT_MAXIMUM);
            mCallTimeoutSpinner.setValueFactory(svf);
            mCallTimeoutSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mCallTimeoutSpinner;
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        getChannelMapComboBox().setDisable(config == null);
        getCallTimeoutSpinner().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);

        getChannelMapComboBox().getSelectionModel().select(null);

        if(config instanceof DecodeConfigMPT1327)
        {
            DecodeConfigMPT1327 decodeConfigMPT1327 = (DecodeConfigMPT1327)config;

            String channelMapName = decodeConfigMPT1327.getChannelMapName();

            if(channelMapName != null)
            {
                for(ChannelMap channelMap: getChannelMapComboBox().getItems())
                {
                    if(channelMap.getName().contentEquals(channelMapName))
                    {
                        getChannelMapComboBox().getSelectionModel().select(channelMap);
                    }
                }
            }

            int callTimeout = decodeConfigMPT1327.getCallTimeoutSeconds();
            getCallTimeoutSpinner().getValueFactory().setValue(callTimeout);

            int channelPoolSize = decodeConfigMPT1327.getTrafficChannelPoolSize();
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(channelPoolSize);
        }
        else if(config != null)
        {
            getCallTimeoutSpinner().getValueFactory().setValue(DecodeConfigMPT1327.DEFAULT_CALL_TIMEOUT_DELAY_SECONDS);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(DecodeConfigMPT1327.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigMPT1327 config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigMPT1327)
        {
            config = (DecodeConfigMPT1327)getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigMPT1327();
        }

        config.setCallTimeoutSeconds(getCallTimeoutSpinner().getValue());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());

        ChannelMap selected = getChannelMapComboBox().getSelectionModel().getSelectedItem();

        if(selected != null)
        {
            config.setChannelMapName(selected.getName());
        }
        else
        {
            config.setChannelMapName(null);
        }

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
        //no-op
    }

    @Override
    protected void saveAuxDecoderConfiguration()
    {
        //no-op
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
