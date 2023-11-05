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

import io.github.dsheirer.gui.control.DbPowerMeter;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AM channel configuration editor
 */
public class AMConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(AMConfigurationEditor.class);
    private TitledPane mSourcePane;
    private TitledPane mDecoderPane;
    private TitledPane mRecordPane;
    private TitledPane mEventLogPane;
    private ToggleSwitch mBasebandRecordSwitch;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private SegmentedButton mBandwidthButton;
    private TextField mTalkgroupField;
    private TextField mSquelchThresholdField;
    private ToggleSwitch mSquelchAutoTrackSwitch;
    private TextFormatter<Integer> mTalkgroupTextFormatter;
    private IntegerFormatter mSquelchTextFormatter = new IntegerFormatter((int)DbPowerMeter.DEFAULT_MINIMUM_POWER,
            (int)DbPowerMeter.DEFAULT_MAXIMUM_POWER);
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();
    private IntegerFormatter mDecimalFormatter = new IntegerFormatter(1, 65535);
    private HexFormatter mHexFormatter = new HexFormatter(1, 65535);

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public AMConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.AM;
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
            mDecoderPane.setText("Decoder: AM");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            int row = 0;

            Label bandwidthLabel = new Label("Channel Bandwidth");
            GridPane.setHalignment(bandwidthLabel, HPos.RIGHT);
            GridPane.setConstraints(bandwidthLabel, 0, row);
            gridPane.getChildren().add(bandwidthLabel);

            GridPane.setConstraints(getBandwidthButton(), 1, row++, 3, 1);
            gridPane.getChildren().add(getBandwidthButton());

            Label squelchLabel = new Label("Squelch Threshold");
            GridPane.setHalignment(squelchLabel, HPos.RIGHT);
            GridPane.setConstraints(squelchLabel, 0, row);
            gridPane.getChildren().add(squelchLabel);

            GridPane.setConstraints(getSquelchThresholdField(), 1, row);
            gridPane.getChildren().add(getSquelchThresholdField());

            Label autoTrackLabel = new Label("Squelch Auto-Track");
            GridPane.setHalignment(autoTrackLabel, HPos.RIGHT);
            GridPane.setConstraints(autoTrackLabel, 2, row);
            gridPane.getChildren().add(autoTrackLabel);

            GridPane.setConstraints(getSquelchAutoTrackSwitch(), 3, row);
            GridPane.setHalignment(getSquelchAutoTrackSwitch(), HPos.LEFT);
            gridPane.getChildren().add(getSquelchAutoTrackSwitch());

            Label talkgroupLabel = new Label("Talkgroup To Assign");
            GridPane.setHalignment(talkgroupLabel, HPos.RIGHT);
            GridPane.setConstraints(talkgroupLabel, 4, row);
            gridPane.getChildren().add(talkgroupLabel);

            GridPane.setConstraints(getTalkgroupField(), 5, row);
            gridPane.getChildren().add(getTalkgroupField());

            mDecoderPane.setContent(gridPane);

            //Special handling - the pill button doesn't like to set a selected state if the pane is not expanded,
            //so detect when the pane is expanded and refresh the config view
            mDecoderPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue)
                {
                    //Reset the config so the editor gets updated
                    setDecoderConfiguration(getItem().getDecodeConfiguration());
                }
            });
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

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            GridPane.setConstraints(getBasebandRecordSwitch(), 0, 1);
            gridPane.getChildren().add(getBasebandRecordSwitch());

            Label recordBasebandLabel = new Label("Channel (Baseband I&Q)");
            GridPane.setHalignment(recordBasebandLabel, HPos.LEFT);
            GridPane.setConstraints(recordBasebandLabel, 1, 1);
            gridPane.getChildren().add(recordBasebandLabel);

            mRecordPane.setContent(gridPane);
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

    private ToggleSwitch getBasebandRecordSwitch()
    {
        if(mBasebandRecordSwitch == null)
        {
            mBasebandRecordSwitch = new ToggleSwitch();
            mBasebandRecordSwitch.setDisable(true);
            mBasebandRecordSwitch.setTextAlignment(TextAlignment.RIGHT);
            mBasebandRecordSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mBasebandRecordSwitch;
    }

    private SegmentedButton getBandwidthButton()
    {
        if(mBandwidthButton == null)
        {
            mBandwidthButton = new SegmentedButton();
            mBandwidthButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mBandwidthButton.setDisable(true);

            for(DecodeConfigNBFM.Bandwidth bandwidth : DecodeConfigNBFM.Bandwidth.AM_BANDWIDTHS)
            {
                ToggleButton toggleButton = new ToggleButton(bandwidth.toString());
                toggleButton.setUserData(bandwidth);
                mBandwidthButton.getButtons().add(toggleButton);
            }

            mBandwidthButton.getToggleGroup().selectedToggleProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));

            //Note: there is a weird timing bug with the segmented button where the toggles are not added to
            //the toggle group until well after the control is rendered.  We attempt to setItem() on the
            //decode configuration and we're unable to correctly set the bandwidth setting.  As a work
            //around, we'll listen for the toggles to be added and update them here.  This normally only
            //happens when we first instantiate the editor and load an item for editing the first time.
            mBandwidthButton.getToggleGroup().getToggles().addListener(new ListChangeListener<Toggle>()
            {
                @Override
                public void onChanged(Change<? extends Toggle> c)
                {
                    //This change event happens when the toggles are added -- we don't need to inspect the change event
                    if(getItem() != null && getItem().getDecodeConfiguration() instanceof DecodeConfigAM config)
                    {
                        //Capture current modified state so that we can reapply after adjusting control states
                        boolean modified = modifiedProperty().get();

                        DecodeConfigAM.Bandwidth bandwidth = config.getBandwidth();
                        if(bandwidth == null)
                        {
                            bandwidth = DecodeConfigNBFM.Bandwidth.BW_15_0;
                        }

                        for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
                        {
                            toggle.setSelected(toggle.getUserData() == bandwidth);
                        }

                        modifiedProperty().set(modified);
                    }
                }
            });
        }

        return mBandwidthButton;
    }

    private TextField getTalkgroupField()
    {
        if(mTalkgroupField == null)
        {
            mTalkgroupField = new TextField();
            mTalkgroupField.setTextFormatter(mTalkgroupTextFormatter);
        }

        return mTalkgroupField;
    }

    private TextField getSquelchThresholdField()
    {
        if(mSquelchThresholdField == null)
        {
            mSquelchThresholdField = new TextField();
            mSquelchThresholdField.setTextFormatter(mSquelchTextFormatter);
            mSquelchTextFormatter.valueProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSquelchThresholdField;
    }

    /**
     * Squelch noise floor auto-track feature.
     */
    private ToggleSwitch getSquelchAutoTrackSwitch()
    {
        if(mSquelchAutoTrackSwitch == null)
        {
            mSquelchAutoTrackSwitch = new ToggleSwitch();
            mSquelchAutoTrackSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSquelchAutoTrackSwitch;
    }

    /**
     * Updates the talkgroup editor's text formatter.
     * @param value to set in the control.
     */
    private void updateTextFormatter(int value)
    {
        if(mTalkgroupTextFormatter != null)
        {
            mTalkgroupTextFormatter.valueProperty().removeListener(mTalkgroupValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(Protocol.AM);

        if(format == null)
        {
            format = IntegerFormat.DECIMAL;
        }

        if(format == IntegerFormat.DECIMAL)
        {
            mTalkgroupTextFormatter = mDecimalFormatter;
            getTalkgroupField().setTooltip(new Tooltip("1 - 65,535"));
        }
        else
        {
            mTalkgroupTextFormatter = mDecimalFormatter;
            getTalkgroupField().setTooltip(new Tooltip("1 - FFFF"));
        }

        mTalkgroupTextFormatter.setValue(value);

        getTalkgroupField().setTextFormatter(mTalkgroupTextFormatter);
        mTalkgroupTextFormatter.valueProperty().addListener(mTalkgroupValueChangeListener);
    }

    /**
     * Change listener to detect when talkgroup value has changed and set modified property to true.
     */
    public class TalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            modifiedProperty().set(true);
        }
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        if(config instanceof DecodeConfigAM decodeConfigAM)
        {
            getBandwidthButton().setDisable(false);
            final DecodeConfigAM.Bandwidth bandwidth = (decodeConfigAM.getBandwidth() != null ?
                    decodeConfigAM.getBandwidth() : DecodeConfigNBFM.Bandwidth.BW_15_0);

            for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(toggle.getUserData() == bandwidth);
            }

            updateTextFormatter(decodeConfigAM.getTalkgroup());
            getSquelchThresholdField().setDisable(false);
            mSquelchTextFormatter.setValue(decodeConfigAM.getSquelchThreshold());
            getSquelchAutoTrackSwitch().setDisable(false);
            getSquelchAutoTrackSwitch().setSelected(decodeConfigAM.isSquelchAutoTrack());

        }
        else
        {
            getBandwidthButton().setDisable(true);

            for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(false);
            }

            updateTextFormatter(0);
            getTalkgroupField().setDisable(true);
            getSquelchThresholdField().setDisable(true);
            getSquelchAutoTrackSwitch().setDisable(true);
            getSquelchAutoTrackSwitch().setSelected(false);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigAM config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigAM decodeConfigAM)
        {
            config = decodeConfigAM;
        }
        else
        {
            config = new DecodeConfigAM();
        }

        DecodeConfigAM.Bandwidth bandwidth = DecodeConfigAM.Bandwidth.BW_15_0;

        if(getBandwidthButton().getToggleGroup().getSelectedToggle() != null)
        {
            bandwidth = (DecodeConfigAM.Bandwidth)getBandwidthButton().getToggleGroup().getSelectedToggle().getUserData();
        }

        config.setBandwidth(bandwidth);

        Integer talkgroup = mTalkgroupTextFormatter.getValue();

        if(talkgroup == null)
        {
            talkgroup = 1;
        }

        config.setTalkgroup(talkgroup);
        config.setSquelchThreshold(mSquelchTextFormatter.getValue());
        config.setSquelchAutoTrack(getSquelchAutoTrackSwitch().isSelected());
        getItem().setDecodeConfiguration(config);
    }

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config)
    {
        //no-op
    }

    @Override
    protected void saveEventLogConfiguration()
    {
        //no-op
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
        if(config != null)
        {
            getBasebandRecordSwitch().setDisable(false);
            getBasebandRecordSwitch().selectedProperty().set(config.contains(RecorderType.BASEBAND));
        }
        else
        {
            getBasebandRecordSwitch().selectedProperty().set(false);
            getBasebandRecordSwitch().setDisable(true);
        }
    }

    @Override
    protected void saveRecordConfiguration()
    {
        RecordConfiguration config = new RecordConfiguration();

        if(getBasebandRecordSwitch().selectedProperty().get())
        {
            config.addRecorder(RecorderType.BASEBAND);
        }

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
