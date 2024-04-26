/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
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
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 2 channel configuration editor
 */
public class P25P2ConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2ConfigurationEditor.class);
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private IntegerTextField mWacnTextField;
    private IntegerTextField mSystemTextField;
    private IntegerTextField mNacTextField;
    private ToggleSwitch mIgnoreDataCallsButton;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;

    /**
     * Constructs an instance
     * @param playlistManager
     * @param userPreferences
     */
    public P25P2ConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.P25_PHASE2;
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
            mDecoderPane.setText("Decoder: P25 Phase 2 for TDMA control or TDMA traffic channels.");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            int row = 0;

            Label poolSizeLabel = new Label("Max Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 0, row);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 1, row);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            GridPane.setConstraints(getIgnoreDataCallsButton(), 2, row);
            gridPane.getChildren().add(getIgnoreDataCallsButton());

            Label directionLabel = new Label("Ignore Data Calls");
            GridPane.setHalignment(directionLabel, HPos.LEFT);
            GridPane.setConstraints(directionLabel, 3, row);
            gridPane.getChildren().add(directionLabel);

            Label wacnLabel = new Label("WACN");
            GridPane.setHalignment(wacnLabel, HPos.RIGHT);
            GridPane.setConstraints(wacnLabel, 0, ++row);
            gridPane.getChildren().add(wacnLabel);

            GridPane.setConstraints(getWacnTextField(), 1, row);
            gridPane.getChildren().add(getWacnTextField());

            Label systemLabel = new Label("System");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 2, row);
            gridPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getSystemTextField(), 3, row);
            gridPane.getChildren().add(getSystemTextField());

            Label nacLabel = new Label("NAC");
            GridPane.setHalignment(nacLabel, HPos.RIGHT);
            GridPane.setConstraints(nacLabel, 4, row);
            gridPane.getChildren().add(nacLabel);

            GridPane.setConstraints(getNacTextField(), 5, row);
            gridPane.getChildren().add(getNacTextField());

            Label noteLabel = new Label("Note: WACN/System/NAC values are auto-detected (ie not required) from " +
                    "the control channel and are only required when decoding individual traffic channels");
            GridPane.setHalignment(noteLabel, HPos.LEFT);
            GridPane.setConstraints(noteLabel, 1, ++row, 6, 1);
            gridPane.getChildren().add(noteLabel);


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
            mSourceConfigurationEditor = new FrequencyEditor(mTunerManager);

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

            mEventLogConfigurationEditor = new EventLogConfigurationEditor(types);
            mEventLogConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mEventLogConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEventLogConfigurationEditor;
    }

    private ToggleSwitch getIgnoreDataCallsButton()
    {
        if(mIgnoreDataCallsButton == null)
        {
            mIgnoreDataCallsButton = new ToggleSwitch();
            mIgnoreDataCallsButton.setDisable(true);
            mIgnoreDataCallsButton.selectedProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mIgnoreDataCallsButton;
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

    private IntegerTextField getWacnTextField()
    {
        if(mWacnTextField == null)
        {
            mWacnTextField = new IntegerTextField();
            mWacnTextField.setDisable(true);
            mWacnTextField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mWacnTextField;
    }

    private IntegerTextField getSystemTextField()
    {
        if(mSystemTextField == null)
        {
            mSystemTextField = new IntegerTextField();
            mSystemTextField.setDisable(true);
            mSystemTextField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSystemTextField;
    }

    private IntegerTextField getNacTextField()
    {
        if(mNacTextField == null)
        {
            mNacTextField = new IntegerTextField();
            mNacTextField.setDisable(true);
            mNacTextField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mNacTextField;
    }

    private RecordConfigurationEditor getRecordConfigurationEditor()
    {
        if(mRecordConfigurationEditor == null)
        {
            List<RecorderType> types = new ArrayList<>();
            types.add(RecorderType.BASEBAND);
            types.add(RecorderType.DEMODULATED_BIT_STREAM);
            types.add(RecorderType.MBE_CALL_SEQUENCE);
            types.add(RecorderType.TRAFFIC_BASEBAND);
            types.add(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM);
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
        if(config instanceof DecodeConfigP25Phase2 decodeConfig)
        {
            getWacnTextField().setDisable(false);
            getSystemTextField().setDisable(false);
            getNacTextField().setDisable(false);

            ScrambleParameters scrambleParameters = decodeConfig.getScrambleParameters();

            if(scrambleParameters != null)
            {
                getWacnTextField().set(scrambleParameters.getWACN());
                getSystemTextField().set(scrambleParameters.getSystem());
                getNacTextField().set(scrambleParameters.getNAC());
            }
            else
            {
                getWacnTextField().set(0);
                getSystemTextField().set(0);
                getNacTextField().set(0);
            }

            getIgnoreDataCallsButton().setDisable(false);
            getIgnoreDataCallsButton().setSelected(decodeConfig.getIgnoreDataCalls());
            getTrafficChannelPoolSizeSpinner().setDisable(false);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(decodeConfig.getTrafficChannelPoolSize());
        }
        else
        {
            getWacnTextField().set(0);
            getSystemTextField().set(0);
            getNacTextField().set(0);
            getWacnTextField().setDisable(true);
            getSystemTextField().setDisable(true);
            getNacTextField().setDisable(true);
            getIgnoreDataCallsButton().setDisable(true);
            getTrafficChannelPoolSizeSpinner().setDisable(true);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigP25Phase2 config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigP25Phase2 p2)
        {
            config = p2;
        }
        else
        {
            config = new DecodeConfigP25Phase2();
        }

        config.setAutoDetectScrambleParameters(false);
        int wacn = getWacnTextField().get();
        int system = getSystemTextField().get();
        int nac = getNacTextField().get();
        config.setScrambleParameters(new ScrambleParameters(wacn, system, nac));
        config.setIgnoreDataCalls(getIgnoreDataCallsButton().isSelected());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());

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
