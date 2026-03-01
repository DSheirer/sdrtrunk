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

import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.playlist.decoder.AuxDecoderConfigurationEditor;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import javafx.scene.control.ComboBox;
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

/**
 * Narrow-Band FM channel configuration editor
 */
public class NBFMConfigurationEditor extends ChannelConfigurationEditor
{
    private TitledPane mAuxDecoderPane;
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private TextField mTalkgroupField;
    private ToggleSwitch mAudioFilterEnable;
    private ToggleSwitch mRequireAliasMatchSwitch;
    private TextFormatter<Integer> mTalkgroupTextFormatter;
    private ToggleSwitch mBasebandRecordSwitch;
    private SegmentedButton mBandwidthButton;
    private ComboBox<SquelchMode> mSquelchModeComboBox;
    private ComboBox<Object> mToneCodeComboBox;
    private Label mToneCodeLabel;

    private SourceConfigurationEditor mSourceConfigurationEditor;
    private AuxDecoderConfigurationEditor mAuxDecoderConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private final TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();
    private final IntegerFormatter mDecimalFormatter = new IntegerFormatter(1, 65535);
    private final HexFormatter mHexFormatter = new HexFormatter(1, 65535);

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public NBFMConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.NBFM;
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
            mDecoderPane.setText("Decoder: NBFM");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            Label bandwidthLabel = new Label("Channel Bandwidth");
            GridPane.setHalignment(bandwidthLabel, HPos.RIGHT);
            GridPane.setConstraints(bandwidthLabel, 0, 0);
            gridPane.getChildren().add(bandwidthLabel);

            GridPane.setConstraints(getBandwidthButton(), 1, 0);
            gridPane.getChildren().add(getBandwidthButton());

            Label talkgroupLabel = new Label("Talkgroup To Assign");
            GridPane.setHalignment(talkgroupLabel, HPos.RIGHT);
            GridPane.setConstraints(talkgroupLabel, 0, 1);
            gridPane.getChildren().add(talkgroupLabel);

            GridPane.setConstraints(getTalkgroupField(), 1, 1);
            gridPane.getChildren().add(getTalkgroupField());

            GridPane.setConstraints(getAudioFilterEnable(), 2, 1);
            gridPane.getChildren().add(getAudioFilterEnable());

            GridPane.setConstraints(getRequireAliasMatchSwitch(), 2, 2);
            gridPane.getChildren().add(getRequireAliasMatchSwitch());

            Label squelchModeLabel = new Label("Squelch Mode");
            GridPane.setHalignment(squelchModeLabel, HPos.RIGHT);
            GridPane.setConstraints(squelchModeLabel, 0, 3);
            gridPane.getChildren().add(squelchModeLabel);

            GridPane.setConstraints(getSquelchModeComboBox(), 1, 3);
            gridPane.getChildren().add(getSquelchModeComboBox());

            GridPane.setHalignment(getToneCodeLabel(), HPos.RIGHT);
            GridPane.setConstraints(getToneCodeLabel(), 0, 4);
            gridPane.getChildren().add(getToneCodeLabel());

            GridPane.setConstraints(getToneCodeComboBox(), 1, 4);
            gridPane.getChildren().add(getToneCodeComboBox());

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

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            GridPane.setConstraints(getBasebandRecordSwitch(), 0, 0);
            gridPane.getChildren().add(getBasebandRecordSwitch());

            Label recordBasebandLabel = new Label("Channel (Baseband I&Q)");
            GridPane.setHalignment(recordBasebandLabel, HPos.LEFT);
            GridPane.setConstraints(recordBasebandLabel, 1, 0);
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

    private AuxDecoderConfigurationEditor getAuxDecoderConfigurationEditor()
    {
        if(mAuxDecoderConfigurationEditor == null)
        {
            mAuxDecoderConfigurationEditor = new AuxDecoderConfigurationEditor(DecoderType.AUX_DECODERS);
            mAuxDecoderConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mAuxDecoderConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAuxDecoderConfigurationEditor;
    }

    /**
     * Toggle switch for enable/disable the audio filtering in the audio module.
     * @return toggle switch.
     */
    private ToggleSwitch getAudioFilterEnable()
    {
        if(mAudioFilterEnable == null)
        {
            mAudioFilterEnable = new ToggleSwitch("High-Pass Audio Filter");
            mAudioFilterEnable.setTooltip(new Tooltip("High-pass filter to remove DC offset and sub-audible signalling"));
            mAudioFilterEnable.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAudioFilterEnable;
    }
    /**
     * Toggle switch for enable/disable requiring alias match for audio capture (tone squelch).
     * @return toggle switch.
     */
    private ToggleSwitch getRequireAliasMatchSwitch()
    {
        if(mRequireAliasMatchSwitch == null)
        {
            mRequireAliasMatchSwitch = new ToggleSwitch("Require Alias Match");
            mRequireAliasMatchSwitch.setTooltip(new Tooltip("Only capture audio when detected tone/code matches a configured alias"));
            mRequireAliasMatchSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRequireAliasMatchSwitch;
    }

    /**
     * ComboBox for selecting squelch mode
     */
    private ComboBox<SquelchMode> getSquelchModeComboBox()
    {
        if(mSquelchModeComboBox == null)
        {
            mSquelchModeComboBox = new ComboBox<>();
            mSquelchModeComboBox.getItems().addAll(SquelchMode.values());
            mSquelchModeComboBox.setValue(SquelchMode.CSQ);
            mSquelchModeComboBox.setTooltip(new Tooltip("Select squelch mode"));
            mSquelchModeComboBox.setDisable(true);
            mSquelchModeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                modifiedProperty().set(true);
                updateToneCodeComboBox(newValue);
            });
        }
        return mSquelchModeComboBox;
    }

    /**
     * ComboBox for selecting tone or code based on squelch mode
     */
    private ComboBox<Object> getToneCodeComboBox()
    {
        if(mToneCodeComboBox == null)
        {
            mToneCodeComboBox = new ComboBox<>();
            mToneCodeComboBox.setTooltip(new Tooltip("Select tone or code"));
            mToneCodeComboBox.setDisable(true);
            mToneCodeComboBox.setVisible(false);
            mToneCodeComboBox.setPromptText("Select Tone...");
            mToneCodeComboBox.setButtonCell(new PromptButtonCell<>("Select Tone..."));
            mToneCodeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }
        return mToneCodeComboBox;
    }

    /**
     * Label for tone/code combo box
     */
    private Label getToneCodeLabel()
    {
        if(mToneCodeLabel == null)
        {
            mToneCodeLabel = new Label("Tone/Code");
            mToneCodeLabel.setVisible(false);
        }
        return mToneCodeLabel;
    }

    /**
     * Updates the tone/code combo box based on selected squelch mode
     */
   private void updateToneCodeComboBox(SquelchMode mode)
    {
        getToneCodeComboBox().getItems().clear();
        getToneCodeComboBox().setValue(null);
        
        if(mode == SquelchMode.CTCSS)
        {
            getToneCodeComboBox().getItems().addAll(CTCSSCode.STANDARD_CODES);
            getToneCodeLabel().setText("CTCSS Tone");
            getToneCodeLabel().setVisible(true);
            getToneCodeComboBox().setPromptText("Select Tone...");
            getToneCodeComboBox().setButtonCell(new PromptButtonCell<>("Select Tone..."));
            getToneCodeComboBox().setVisible(true);
            getToneCodeComboBox().setDisable(false);
        }
        else if(mode == SquelchMode.DCS)
        {
            getToneCodeComboBox().getItems().addAll(DCSCode.STANDARD_CODES);
            getToneCodeLabel().setText("DCS Code");
            getToneCodeLabel().setVisible(true);
            getToneCodeComboBox().setPromptText("Select Code...");
            getToneCodeComboBox().setButtonCell(new PromptButtonCell<>("Select Code..."));
            getToneCodeComboBox().setVisible(true);
            getToneCodeComboBox().setDisable(false);
        }
        else if(mode == SquelchMode.DCS_INVERTED)
        {
            getToneCodeComboBox().getItems().addAll(DCSCode.INVERTED_CODES);
            getToneCodeLabel().setText("DCS Code (Inv)");
            getToneCodeLabel().setVisible(true);
            getToneCodeComboBox().setPromptText("Select Code...");
            getToneCodeComboBox().setButtonCell(new PromptButtonCell<>("Select Code..."));
            getToneCodeComboBox().setVisible(true);
            getToneCodeComboBox().setDisable(false);
        }
        else
        {
            getToneCodeLabel().setVisible(false);
            getToneCodeComboBox().setVisible(false);
            getToneCodeComboBox().setDisable(true);
        }
    }

    private SegmentedButton getBandwidthButton()
    {
        if(mBandwidthButton == null)
        {
            mBandwidthButton = new SegmentedButton();
            mBandwidthButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mBandwidthButton.setDisable(true);

            for(DecodeConfigNBFM.Bandwidth bandwidth : DecodeConfigNBFM.Bandwidth.FM_BANDWIDTHS)
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
            mBandwidthButton.getToggleGroup().getToggles().addListener((ListChangeListener<Toggle>)c ->
            {
                //This change event happens when the toggles are added -- we don't need to inspect the change event
                if(getItem() != null && getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM)
                {
                    //Capture current modified state so that we can reapply after adjusting control states
                    boolean modified = modifiedProperty().get();

                    DecodeConfigNBFM config = (DecodeConfigNBFM)getItem().getDecodeConfiguration();
                    DecodeConfigNBFM.Bandwidth bandwidth = config.getBandwidth();
                    if(bandwidth == null)
                    {
                        bandwidth = DecodeConfigNBFM.Bandwidth.BW_12_5;
                    }

                    for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
                    {
                        toggle.setSelected(toggle.getUserData() == bandwidth);
                    }

                    modifiedProperty().set(modified);
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

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(Protocol.NBFM);

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

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        if(config instanceof DecodeConfigNBFM)
        {
            getBandwidthButton().setDisable(false);
            DecodeConfigNBFM decodeConfigNBFM = (DecodeConfigNBFM)config;
            final DecodeConfigNBFM.Bandwidth bandwidth = (decodeConfigNBFM.getBandwidth() != null ?
                    decodeConfigNBFM.getBandwidth() : DecodeConfigNBFM.Bandwidth.BW_12_5);

            for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(toggle.getUserData() == bandwidth);
            }

            updateTextFormatter(decodeConfigNBFM.getTalkgroup());
            getAudioFilterEnable().setDisable(false);
            getAudioFilterEnable().setSelected(decodeConfigNBFM.isAudioFilter());
            getRequireAliasMatchSwitch().setDisable(false);
            getRequireAliasMatchSwitch().setSelected(decodeConfigNBFM.isRequireAliasMatch());
            getSquelchModeComboBox().setDisable(false);
            if(decodeConfigNBFM.hasCtcssTone())
            {
                getSquelchModeComboBox().setValue(SquelchMode.CTCSS);
                updateToneCodeComboBox(SquelchMode.CTCSS);
                getToneCodeComboBox().setValue(decodeConfigNBFM.getCtcssTone());
            }
            else if(decodeConfigNBFM.hasDcsTone())
            {
                DCSCode dcsCode = decodeConfigNBFM.getDcsTone();
                if(dcsCode.isInverted())
                {
                    getSquelchModeComboBox().setValue(SquelchMode.DCS_INVERTED);
                    updateToneCodeComboBox(SquelchMode.DCS_INVERTED);
                }
                else
                {
                    getSquelchModeComboBox().setValue(SquelchMode.DCS);
                    updateToneCodeComboBox(SquelchMode.DCS);
                }
                getToneCodeComboBox().setValue(dcsCode);
            }
            else
            {
                getSquelchModeComboBox().setValue(SquelchMode.CSQ);
                updateToneCodeComboBox(SquelchMode.CSQ);
            }
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
            getAudioFilterEnable().setDisable(true);
            getAudioFilterEnable().setSelected(false);
            getSquelchModeComboBox().setDisable(true);
            getSquelchModeComboBox().setValue(SquelchMode.CSQ);
            updateToneCodeComboBox(SquelchMode.CSQ);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigNBFM config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM)
        {
            config = (DecodeConfigNBFM)getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigNBFM();
        }

        DecodeConfigNBFM.Bandwidth bandwidth = DecodeConfigNBFM.Bandwidth.BW_12_5;

        if(getBandwidthButton().getToggleGroup().getSelectedToggle() != null)
        {
            bandwidth = (DecodeConfigNBFM.Bandwidth)getBandwidthButton().getToggleGroup().getSelectedToggle().getUserData();
        }

        config.setBandwidth(bandwidth);

        Integer talkgroup = mTalkgroupTextFormatter.getValue();

        if(talkgroup == null)
        {
            talkgroup = 1;
        }

        config.setTalkgroup(talkgroup);
        config.setAudioFilter(getAudioFilterEnable().isSelected());
        config.setRequireAliasMatch(getRequireAliasMatchSwitch().isSelected());
        getItem().setDecodeConfiguration(config);
        SquelchMode mode = getSquelchModeComboBox().getValue();
        if(mode == SquelchMode.CTCSS && getToneCodeComboBox().getValue() instanceof CTCSSCode)
        {
            config.setCtcssTone((CTCSSCode)getToneCodeComboBox().getValue());
            config.setDcsTone(null);
        }
        else if((mode == SquelchMode.DCS || mode == SquelchMode.DCS_INVERTED) && getToneCodeComboBox().getValue() instanceof DCSCode)
        {
            config.setCtcssTone(null);
            config.setDcsTone((DCSCode)getToneCodeComboBox().getValue());
        }
        else
        {
            config.setCtcssTone(null);
            config.setDcsTone(null);
        }
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
    /**
     * Squelch mode options
     */
    public enum SquelchMode
    {
        CSQ("Carrier Squelch"),
        CTCSS("CTCSS Tone"),
        DCS("DCS Code"),
        DCS_INVERTED("DCS Code (Inverted)");

        private final String mLabel;

        SquelchMode(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }
    /**
     * Custom button cell that shows prompt text when no value is selected
     */
    private static class PromptButtonCell<T> extends javafx.scene.control.ListCell<T>
    {
        private final String mPromptText;

        public PromptButtonCell(String promptText)
        {
            mPromptText = promptText;
        }

        @Override
        protected void updateItem(T item, boolean empty)
        {
            super.updateItem(item, empty);
            if(empty || item == null)
            {
                setText(mPromptText);
            }
            else
            {
                setText(item.toString());
            }
        }
    }
}
