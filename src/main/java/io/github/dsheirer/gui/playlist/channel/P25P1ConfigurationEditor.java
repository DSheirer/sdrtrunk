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

import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.Modulation;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 1 channel configuration editor
 */
public class P25P1ConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1ConfigurationEditor.class);
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private ToggleSwitch mIgnoreDataCallsButton;
    private ToggleSwitch mNacFilterButton;
    private javafx.scene.control.TextField mNacTextField;
    private Spinner<Integer> mTalkgroupSpinner;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private SegmentedButton mModulationSegmentedButton;
    private ToggleButton mC4FMToggleButton;
    private ToggleButton mLSMToggleButton;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public P25P1ConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.P25_PHASE1;
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
            mDecoderPane.setText("Decoder: P25 Phase 1 (also used for P25 Phase 2 system with FDMA control channels)");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            Label modulationLabel = new Label("Modulation");
            GridPane.setHalignment(modulationLabel, HPos.RIGHT);
            GridPane.setConstraints(modulationLabel, 0, 0);
            gridPane.getChildren().add(modulationLabel);

            GridPane.setConstraints(getModulationSegmentedButton(), 1, 0);
            gridPane.getChildren().addAll(getModulationSegmentedButton());

            Label poolSizeLabel = new Label("Max Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 2, 0);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 3, 0);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            GridPane.setConstraints(getIgnoreDataCallsButton(), 4, 0);
            gridPane.getChildren().add(getIgnoreDataCallsButton());

            Label directionLabel = new Label("Ignore Data Calls");
            GridPane.setHalignment(directionLabel, HPos.LEFT);
            GridPane.setConstraints(directionLabel, 5, 0);
            gridPane.getChildren().add(directionLabel);

            Label modulationHelpLabel = new Label("C4FM: repeaters and non-simulcast trunked systems.  LSM: simulcast trunked systems.");
            GridPane.setConstraints(modulationHelpLabel, 0, 1, 6, 1);
            gridPane.getChildren().add(modulationHelpLabel);

            //NAC Filter row
            GridPane.setConstraints(getNacFilterButton(), 0, 2);
            gridPane.getChildren().add(getNacFilterButton());

            Label nacLabel = new Label("NAC Filter");
            GridPane.setHalignment(nacLabel, HPos.LEFT);
            GridPane.setConstraints(nacLabel, 1, 2);
            gridPane.getChildren().add(nacLabel);

            Label nacValueLabel = new Label("Allowed NACs (hex/dec, comma separated):");
            GridPane.setHalignment(nacValueLabel, HPos.RIGHT);
            GridPane.setConstraints(nacValueLabel, 2, 2);
            gridPane.getChildren().add(nacValueLabel);

            GridPane.setConstraints(getNacTextField(), 3, 2, 3, 1);
            gridPane.getChildren().add(getNacTextField());

            //Talkgroup Override row
            Label tgLabel = new Label("Talkgroup Override");
            GridPane.setHalignment(tgLabel, HPos.RIGHT);
            GridPane.setConstraints(tgLabel, 0, 3, 2, 1);
            gridPane.getChildren().add(tgLabel);

            GridPane.setConstraints(getTalkgroupSpinner(), 2, 3);
            gridPane.getChildren().add(getTalkgroupSpinner());

            Label tgHelpLabel = new Label("0 = auto (use decoded talkgroup)");
            GridPane.setHalignment(tgHelpLabel, HPos.LEFT);
            GridPane.setConstraints(tgHelpLabel, 3, 3, 3, 1);
            gridPane.getChildren().add(tgHelpLabel);

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
                DecodeConfigP25Phase1.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigP25Phase1.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigP25Phase1.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

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

    private SegmentedButton getModulationSegmentedButton()
    {
        if(mModulationSegmentedButton == null)
        {
            mModulationSegmentedButton = new SegmentedButton();
            mModulationSegmentedButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mModulationSegmentedButton.getButtons().addAll(getC4FMToggleButton(), getLSMToggleButton());
            mModulationSegmentedButton.getToggleGroup().selectedToggleProperty().addListener(new ChangeListener<Toggle>()
            {
                @Override
                public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue)
                {
                    if(newValue == null)
                    {
                        //Ensure at least one toggle is always selected
                        oldValue.setSelected(true);
                    }
                    else if(oldValue != null && newValue != null)
                    {
                        //Only set modified if the toggle changed from one to the other
                        modifiedProperty().set(true);
                    }
                }
            });
        }

        return mModulationSegmentedButton;
    }

    private ToggleButton getC4FMToggleButton()
    {
        if(mC4FMToggleButton == null)
        {
            mC4FMToggleButton = new ToggleButton("C4FM");
        }

        return mC4FMToggleButton;
    }

    private ToggleButton getLSMToggleButton()
    {
        if(mLSMToggleButton == null)
        {
            mLSMToggleButton = new ToggleButton("LSM");
        }

        return mLSMToggleButton;
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
        getIgnoreDataCallsButton().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);
        getNacFilterButton().setDisable(config == null);
        getNacTextField().setDisable(config == null);
        getTalkgroupSpinner().setDisable(config == null);

        if(config instanceof DecodeConfigP25Phase1)
        {
            DecodeConfigP25Phase1 decodeConfig = (DecodeConfigP25Phase1)config;
            getIgnoreDataCallsButton().setSelected(decodeConfig.getIgnoreDataCalls());
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(decodeConfig.getTrafficChannelPoolSize());
            getNacFilterButton().setSelected(decodeConfig.isNacFilterEnabled());
            getTalkgroupSpinner().getValueFactory().setValue(decodeConfig.getTalkgroup());

            //Format NAC list for display
            StringBuilder sb = new StringBuilder();
            for(Integer nac : decodeConfig.getAllowedNACs())
            {
                if(sb.length() > 0) sb.append(", ");
                sb.append("x").append(String.format("%03X", nac));
            }
            getNacTextField().setText(sb.toString());

            if(decodeConfig.getModulation() == Modulation.C4FM)
            {
                getC4FMToggleButton().setSelected(true);
                getLSMToggleButton().setSelected(false);
            }
            else
            {
                getC4FMToggleButton().setSelected(false);
                getLSMToggleButton().setSelected(true);
            }
        }
        else
        {
            getIgnoreDataCallsButton().setSelected(false);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(0);
            getNacFilterButton().setSelected(false);
            getNacTextField().setText("");
            getTalkgroupSpinner().getValueFactory().setValue(0);
        }
    }

    private ToggleSwitch getNacFilterButton()
    {
        if(mNacFilterButton == null)
        {
            mNacFilterButton = new ToggleSwitch();
            mNacFilterButton.setDisable(true);
            mNacFilterButton.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }
        return mNacFilterButton;
    }

    private javafx.scene.control.TextField getNacTextField()
    {
        if(mNacTextField == null)
        {
            mNacTextField = new javafx.scene.control.TextField();
            mNacTextField.setDisable(true);
            mNacTextField.setPromptText("e.g. x293, 0xD12, 3346");
            mNacTextField.setTooltip(new Tooltip("Enter NAC values in hex (x293, 0x293) or decimal (659), separated by commas"));
            mNacTextField.textProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }
        return mNacTextField;
    }

    private Spinner<Integer> getTalkgroupSpinner()
    {
        if(mTalkgroupSpinner == null)
        {
            mTalkgroupSpinner = new Spinner<>();
            mTalkgroupSpinner.setDisable(true);
            mTalkgroupSpinner.setTooltip(new Tooltip("Talkgroup override for conventional channels (0 = auto)"));
            mTalkgroupSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65535, 0);
            mTalkgroupSpinner.setValueFactory(svf);
            mTalkgroupSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }
        return mTalkgroupSpinner;
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigP25Phase1 config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigP25Phase1 p1)
        {
            config = p1;
        }
        else
        {
            config = new DecodeConfigP25Phase1();
        }

        config.setIgnoreDataCalls(getIgnoreDataCallsButton().isSelected());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());
        config.setModulation(getC4FMToggleButton().isSelected() ? Modulation.C4FM : Modulation.CQPSK);
        config.setNacFilterEnabled(getNacFilterButton().isSelected());
        config.setTalkgroup(getTalkgroupSpinner().getValue());

        //Parse NAC text field
        config.getAllowedNACs().clear();
        String nacText = getNacTextField().getText();
        if(nacText != null && !nacText.trim().isEmpty())
        {
            for(String token : nacText.split(","))
            {
                token = token.trim();
                try
                {
                    int nac;
                    if(token.startsWith("0x") || token.startsWith("0X"))
                    {
                        nac = Integer.parseInt(token.substring(2), 16);
                    }
                    else if(token.startsWith("x") || token.startsWith("X"))
                    {
                        nac = Integer.parseInt(token.substring(1), 16);
                    }
                    else
                    {
                        nac = Integer.parseInt(token);
                    }
                    config.addAllowedNAC(nac);
                }
                catch(NumberFormatException e)
                {
                    //Skip invalid entries
                }
            }
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
