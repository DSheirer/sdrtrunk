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

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyField;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.ObservableChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.Encoding;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.controlsfx.control.SegmentedButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN channel configuration editor
 */
public class NXDNConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(NXDNConfigurationEditor.class);
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private TableView<ObservableChannelFrequency> mChannelMapTable;
    private IntegerTextField mChannelField;
    private FrequencyField mDownlinkFrequencyField;
    private Button mAddTimeslotFrequencyButton;
    private Button mDeleteButton;
    private Spinner<Integer> mChannelRotationDelaySpinner;
    private SegmentedButton mTransmissionModeButton;
    private ComboBox<Encoding> mEncodingComboBox;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public NXDNConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.NXDN;
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
            mDecoderPane.setText("Decoder: NXDN");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            int row = 0;

            Label mode = new Label("Mode:");
            GridPane.setHalignment(mode, HPos.RIGHT);
            GridPane.setConstraints(mode, 0, row);
            gridPane.getChildren().add(mode);

            GridPane.setConstraints(getTransmissionModeButton(), 1, row);
            gridPane.getChildren().add(getTransmissionModeButton());

            Label encoding = new Label("Encoding:");
            GridPane.setHalignment(encoding, HPos.RIGHT);
            GridPane.setConstraints(encoding, 2, row);
            gridPane.getChildren().add(encoding);

            GridPane.setConstraints(getEncodingComboBox(), 3, row);
            gridPane.getChildren().add(getEncodingComboBox());

            Label poolSizeLabel = new Label("Max Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 4, row);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 5, row);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            Label channelMapTableLabel = new Label("Optional Channel Number (LCN) to Frequency Map that is only required for Channel-Mode systems");
            GridPane.setHalignment(channelMapTableLabel, HPos.LEFT);
            GridPane.setConstraints(channelMapTableLabel, 0, ++row, 6, 1);
            gridPane.getChildren().add(channelMapTableLabel);

            GridPane.setConstraints(getChannelMapTable(), 0, ++row, 6, 3);
            gridPane.getChildren().add(getChannelMapTable());

            VBox buttonsBox = new VBox();
            buttonsBox.setAlignment(Pos.CENTER);
            buttonsBox.setSpacing(10);
            buttonsBox.getChildren().addAll(getAddTimeslotFrequencyButton(), getDeleteButton());

            GridPane.setConstraints(buttonsBox, 6, row, 1, 3);
            gridPane.getChildren().addAll(buttonsBox);

            row += 3;

            HBox editorBox = new HBox();
            editorBox.setAlignment(Pos.CENTER_LEFT);
            editorBox.setSpacing(5);

            Label lcnLabel = new Label("Channel Number");
            editorBox.getChildren().addAll(lcnLabel, getChannelField());

            Label downlinkLabel = new Label("Downlink Frequency (MHz)");
            downlinkLabel.setPadding(new Insets(0,0,0,5));
            editorBox.getChildren().addAll(downlinkLabel,getDownlinkFrequencyField());

            GridPane.setConstraints(editorBox, 0, row, 4, 1);
            gridPane.getChildren().add(editorBox);

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
                DecodeConfigDMR.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigDMR.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigDMR.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

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

    /**
     * Talker alias encoding combo box
     * @return encoding combo box.
     */
    private ComboBox<Encoding> getEncodingComboBox()
    {
        if(mEncodingComboBox == null)
        {
            mEncodingComboBox = new ComboBox<>(FXCollections.observableArrayList(Arrays.stream(Encoding.values()).toList()));
            mEncodingComboBox.getSelectionModel().select(0);
            mEncodingComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
            mEncodingComboBox.setTooltip(new Tooltip("Talker Alias Encoding:  UTF-8:Latin/English, BIG5:Taiwan"));
        }

        return mEncodingComboBox;
    }

    private TableView<ObservableChannelFrequency> getChannelMapTable()
    {
        if(mChannelMapTable == null)
        {
            mChannelMapTable = new TableView<>(FXCollections.observableArrayList(ObservableChannelFrequency.extractor()));
            mChannelMapTable.setPrefHeight(100.0);

            TableColumn<ObservableChannelFrequency,Number> channelColumn = new TableColumn("Channel");
            channelColumn.setPrefWidth(100);
            channelColumn.setCellValueFactory(cellData -> cellData.getValue().channelProperty());
            mChannelMapTable.getColumns().add(channelColumn);
            mChannelMapTable.getSortOrder().add(channelColumn);

            TableColumn<ObservableChannelFrequency,Number> downlinkColumn = new TableColumn("Downlink Frequency (MHz)");
            downlinkColumn.setCellValueFactory(cellData -> cellData.getValue().downlinkProperty());
            downlinkColumn.setPrefWidth(200);
            mChannelMapTable.getColumns().add(downlinkColumn);

            mChannelMapTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setChannelFrequency(newValue));
        }

        return mChannelMapTable;
    }

    private SegmentedButton getTransmissionModeButton()
    {
        if(mTransmissionModeButton == null)
        {
            mTransmissionModeButton = new SegmentedButton();
            mTransmissionModeButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mTransmissionModeButton.setDisable(true);

            ToggleButton tb4800 = new ToggleButton(TransmissionMode.M4800.getLabel());
            tb4800.setUserData(TransmissionMode.M4800);
            mTransmissionModeButton.getButtons().add(tb4800);

            ToggleButton tb9600 = new ToggleButton(TransmissionMode.M9600.getLabel());
            tb9600.setUserData(TransmissionMode.M9600);
            mTransmissionModeButton.getButtons().add(tb9600);

            mTransmissionModeButton.getToggleGroup().selectedToggleProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));

            //Note: there is a weird timing bug with the segmented button where the toggles are not added to
            //the toggle group until well after the control is rendered.  We attempt to setItem() on the
            //decode configuration and we're unable to correctly set the bandwidth setting.  As a work
            //around, we'll listen for the toggles to be added and update them here.  This normally only
            //happens when we first instantiate the editor and load an item for editing the first time.
            mTransmissionModeButton.getToggleGroup().getToggles().addListener((ListChangeListener<Toggle>) c ->
            {
                //This change event happens when the toggles are added -- we don't need to inspect the change event
                if(getItem() != null && getItem().getDecodeConfiguration() instanceof DecodeConfigNXDN config)
                {
                    //Capture current modified state so that we can reapply after adjusting control states
                    boolean modified = modifiedProperty().get();

                    TransmissionMode transmissionMode = config.getTransmissionMode();

                    for(Toggle toggle: getTransmissionModeButton().getToggleGroup().getToggles())
                    {
                        toggle.setSelected(toggle.getUserData() == transmissionMode);
                    }

                    modifiedProperty().set(modified);
                }
            });
        }

        return mTransmissionModeButton;
    }



    /**
     * Sets the specified timeslot frequency into the editor
     */
    private void setChannelFrequency(ObservableChannelFrequency channelFrequency)
    {
        //Preserve the current modified flag state since setting values in the editor will change it.
        boolean modified = modifiedProperty().get();

        getChannelField().setDisable(channelFrequency == null);
        getDownlinkFrequencyField().setDisable(channelFrequency == null);
        getDeleteButton().setDisable(channelFrequency == null);

        if(channelFrequency != null)
        {
            getChannelField().set(channelFrequency.channelProperty().get());
            getDownlinkFrequencyField().set(channelFrequency.downlinkProperty().get());
        }
        else
        {
            getChannelField().set(0);
            getDownlinkFrequencyField().set(0);
        }

        modifiedProperty().set(modified);
    }

    private Button getAddTimeslotFrequencyButton()
    {
        if(mAddTimeslotFrequencyButton == null)
        {
            mAddTimeslotFrequencyButton = new Button("Add");
            mAddTimeslotFrequencyButton.setMaxWidth(Double.MAX_VALUE);
            mAddTimeslotFrequencyButton.setOnAction(event -> addChannelFrequency());
        }

        return mAddTimeslotFrequencyButton;
    }

    /**
     * Adds a new channel frequency entry
     */
    private void addChannelFrequency()
    {
        ObservableChannelFrequency ocf = new ObservableChannelFrequency();
        getChannelMapTable().getItems().add(ocf);
        getChannelMapTable().scrollTo(ocf);
        getChannelMapTable().getSelectionModel().select(ocf);
        modifiedProperty().set(true);
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.setDisable(true);
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setOnAction(event -> {
                ObservableChannelFrequency selected = getChannelMapTable().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    getChannelMapTable().getItems().remove(selected);
                    modifiedProperty().set(true);
                }
            });
        }

        return mDeleteButton;
    }

    private IntegerTextField getChannelField()
    {
        if(mChannelField == null)
        {
            mChannelField = new IntegerTextField();
            mChannelField.setDisable(true);
            mChannelField.setPrefWidth(65);
            mChannelField.textProperty().addListener((observable, oldValue, newValue) -> {
                ObservableChannelFrequency selected = getChannelMapTable().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    Integer value = mChannelField.get();

                    if(value != null)
                    {
                        selected.channelProperty().set(value);
                    }
                }

                modifiedProperty().set(true);
            });
        }

        return mChannelField;
    }

    private FrequencyField getDownlinkFrequencyField()
    {
        if(mDownlinkFrequencyField == null)
        {
            mDownlinkFrequencyField = new FrequencyField();
            mDownlinkFrequencyField.setDisable(true);
            mDownlinkFrequencyField.textProperty().addListener(new ChangeListener<String>()
            {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
                {
                    ObservableChannelFrequency selected = getChannelMapTable().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        selected.downlinkProperty().set(mDownlinkFrequencyField.get());
                    }

                    modifiedProperty().set(true);
                }
            });
        }

        return mDownlinkFrequencyField;
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

    /**
     * Channel rotation monitor delay value.  This dictates how long the decoder will remain on each frequency before
     * rotating to the next frequency in the list
     * @return spinner
     */
    private Spinner<Integer> getChannelRotationDelaySpinner()
    {
        if(mChannelRotationDelaySpinner == null)
        {
            mChannelRotationDelaySpinner = new Spinner();
            mChannelRotationDelaySpinner.setDisable(true);
            mChannelRotationDelaySpinner.setTooltip(
                new Tooltip("Delay on each frequency before rotating to next when seeking to next active channel frequency"));
            mChannelRotationDelaySpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(200, 2000, 200, 50);
            mChannelRotationDelaySpinner.setValueFactory(svf);
            mChannelRotationDelaySpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mChannelRotationDelaySpinner;
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
        getTransmissionModeButton().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);
        getChannelMapTable().getItems().clear();
        getChannelMapTable().setDisable(config == null);
        getAddTimeslotFrequencyButton().setDisable(config == null);
        getDeleteButton().setDisable(true);
        getChannelField().set(0);
        getChannelField().setDisable(true);
        getDownlinkFrequencyField().set(0);
        getDownlinkFrequencyField().setDisable(true);
        getChannelRotationDelaySpinner().setDisable(config == null);

        if(config instanceof DecodeConfigNXDN configNXDN)
        {
            for(ToggleButton toggle: getTransmissionModeButton().getButtons())
            {
                if(toggle.getUserData() == configNXDN.getTransmissionMode())
                {
                    toggle.setSelected(true);
                }
            }

            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(configNXDN.getTrafficChannelPoolSize());

            for(ChannelFrequency channelFrequency: configNXDN.getChannelMap())
            {
                getChannelMapTable().getItems().add(new ObservableChannelFrequency(channelFrequency));
            }
        }
        else
        {
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(0);
            getChannelRotationDelaySpinner().getValueFactory().setValue(200);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigNXDN config = null;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigNXDN configNXDN)
        {
            config = configNXDN;
        }
        else
        {
            config = new DecodeConfigNXDN(TransmissionMode.M9600);
        }

        TransmissionMode selected = (TransmissionMode)getTransmissionModeButton().getToggleGroup().selectedToggleProperty().get().getUserData();
        config.setTransmissionMode(selected);

        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());
        
        List<ChannelFrequency> channelFrequencies = new ArrayList<>();
        
        for(ObservableChannelFrequency ocf: getChannelMapTable().getItems())
        {
            channelFrequencies.add(ocf.getChannel());
        }
        
        config.setChannelMap(channelFrequencies);
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

    /**
     * Channel tuner channel source frequencies value factory
     */
    public class FrequencyCellValueFactory implements Callback<TableColumn.CellDataFeatures<TimeslotFrequency, String>,
            ObservableValue<String>>
    {
        private SimpleStringProperty mFrequency = new SimpleStringProperty();
        private boolean mIsDownlink;

        public FrequencyCellValueFactory(boolean isDownlink)
        {
            mIsDownlink = isDownlink;
        }

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TimeslotFrequency, String> param)
        {
            if(param.getValue() != null)
            {
                long frequency = (mIsDownlink ? param.getValue().getDownlinkFrequency() : param.getValue().getUplinkFrequency());
                mFrequency.set(String.valueOf(frequency / 1E6));
            }
            else
            {
                mFrequency.set(null);
            }

            return mFrequency;
        }
    }

    public class DownlinkPropertyValueFactory extends PropertyValueFactory<TimeslotFrequency,String>
    {
        private StringProperty mStringProperty = new SimpleStringProperty();

        public DownlinkPropertyValueFactory()
        {
            super("downlinkFrequency");
        }

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TimeslotFrequency,String> param)
        {
            if(param.getValue() != null)
            {
                mStringProperty.set(String.valueOf(param.getValue().getDownlinkFrequency() / 1E6));
            }
            else
            {
                mStringProperty.setValue(null);
            }

            return mStringProperty;
        }
    }

}
