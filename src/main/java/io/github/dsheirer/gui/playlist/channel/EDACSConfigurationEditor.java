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

import io.github.dsheirer.dsp.filter.equalizer.GraphicEqualizer;
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
import java.util.Locale;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.dsheirer.module.decode.config.DecodeConfiguration.CALL_TIMEOUT_MAXIMUM;
import static io.github.dsheirer.module.decode.config.DecodeConfiguration.CALL_TIMEOUT_MINIMUM;

/**
 * Editor for an EDACS Extended Addressing control channel.
 */
public class EDACSConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSConfigurationEditor.class);
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private TextArea mLcnTextArea;
    private TitledPane mGraphicEQPane;
    private ToggleSwitch mGraphicEQEnabledSwitch;
    private Slider[] mEQBandSliders = new Slider[GraphicEqualizer.BAND_COUNT];
    private TextField[] mEQBandFields = new TextField[GraphicEqualizer.BAND_COUNT];
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private Spinner<Integer> mCallTimeoutSpinner;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public EDACSConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                    UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
        getTitledPanesBox().getChildren().add(getSourcePane());
        getTitledPanesBox().getChildren().add(getDecoderPane());
        getTitledPanesBox().getChildren().add(getGraphicEQPane());
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
            mDecoderPane.setText("Decoder: EDACS Extended Addressing (ProVoice and analog)");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            Label lcnLabel = new Label("LCN Frequencies (MHz)");
            GridPane.setHalignment(lcnLabel, HPos.RIGHT);
            GridPane.setValignment(lcnLabel, VPos.TOP);
            GridPane.setConstraints(lcnLabel, 0, 0);
            gridPane.getChildren().add(lcnLabel);

            GridPane.setConstraints(getLcnTextArea(), 1, 0, 3, 1,
                HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES);
            gridPane.getChildren().add(getLcnTextArea());

            Label poolSizeLabel = new Label("Max Voice Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 0, 1);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 1, 1);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            Label callTimeoutLabel = new Label("Call Limit Seconds");
            GridPane.setHalignment(callTimeoutLabel, HPos.RIGHT);
            GridPane.setConstraints(callTimeoutLabel, 2, 1);
            gridPane.getChildren().add(callTimeoutLabel);

            GridPane.setConstraints(getCallTimeoutSpinner(), 3, 1);
            gridPane.getChildren().add(getCallTimeoutSpinner());

            Label ignoreLabel = new Label("Ignore Unaliased Talkgroups");
            GridPane.setHalignment(ignoreLabel, HPos.RIGHT);
            GridPane.setConstraints(ignoreLabel, 0, 2);
            gridPane.getChildren().add(ignoreLabel);

            GridPane.setConstraints(getIgnoreUnaliasedSwitch(), 1, 2);
            gridPane.getChildren().add(getIgnoreUnaliasedSwitch());

            mDecoderPane.setContent(gridPane);
        }

        return mDecoderPane;
    }

    private TitledPane getGraphicEQPane()
    {
        if(mGraphicEQPane == null)
        {
            mGraphicEQPane = new TitledPane();
            mGraphicEQPane.setText("10-Band Graphic Equalizer (200 Hz - 3.5 kHz)");
            mGraphicEQPane.setExpanded(false);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10, 10, 10, 10));

            mGraphicEQEnabledSwitch = new ToggleSwitch("Enable Graphic Equalizer");
            mGraphicEQEnabledSwitch.setTooltip(new Tooltip("Apply a 10-band graphic EQ to the voice channel audio"));
            mGraphicEQEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
                modifiedProperty().set(true);
                for(int i = 0; i < GraphicEqualizer.BAND_COUNT; i++)
                {
                    mEQBandSliders[i].setDisable(!val);
                    mEQBandFields[i].setDisable(!val);
                }
            });
            content.getChildren().add(mGraphicEQEnabledSwitch);

            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);
            gridPane.setVgap(8);

            for(int i = 0; i < GraphicEqualizer.BAND_COUNT; i++)
            {
                final int band = i;

                Label bandLabel = new Label(GraphicEqualizer.BAND_LABELS[i]);
                GridPane.setHalignment(bandLabel, HPos.RIGHT);
                GridPane.setConstraints(bandLabel, 0, i);
                gridPane.getChildren().add(bandLabel);

                mEQBandSliders[i] = new Slider(-12, 12, 0);
                mEQBandSliders[i].setMajorTickUnit(6);
                mEQBandSliders[i].setMinorTickCount(5);
                mEQBandSliders[i].setShowTickMarks(true);
                mEQBandSliders[i].setShowTickLabels(true);
                mEQBandSliders[i].setPrefWidth(250);
                mEQBandSliders[i].setDisable(true);
                mEQBandSliders[i].setTooltip(new Tooltip(GraphicEqualizer.BAND_LABELS[i] +
                    " band gain (-12 to +12 dB)"));
                mEQBandSliders[i].valueProperty().addListener((obs, old, val) -> {
                    mEQBandFields[band].setText(String.format("%+.1f dB", val.doubleValue()));
                    modifiedProperty().set(true);
                });
                GridPane.setConstraints(mEQBandSliders[i], 1, i);
                gridPane.getChildren().add(mEQBandSliders[i]);

                mEQBandFields[i] = new TextField("+0.0 dB");
                mEQBandFields[i].setPrefWidth(80);
                mEQBandFields[i].setMaxWidth(80);
                mEQBandFields[i].setDisable(true);
                mEQBandFields[i].setOnAction(event -> commitEQBandField(band));
                mEQBandFields[i].focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if(!isFocused) commitEQBandField(band);
                });
                GridPane.setConstraints(mEQBandFields[i], 2, i);
                gridPane.getChildren().add(mEQBandFields[i]);
            }

            content.getChildren().add(gridPane);
            mGraphicEQPane.setContent(content);
        }

        return mGraphicEQPane;
    }

    /**
     * Commits the text field value to the corresponding EQ band slider.
     */
    private void commitEQBandField(int band)
    {
        try
        {
            String text = mEQBandFields[band].getText().trim()
                .replace("dB", "").replace("+", "").trim();
            double value = Double.parseDouble(text);
            value = Math.max(-12, Math.min(12, value));
            mEQBandSliders[band].setValue(value);
            mEQBandFields[band].setText(String.format("%+.1f dB", value));
        }
        catch(NumberFormatException e)
        {
            mEQBandFields[band].setText(String.format("%+.1f dB", mEQBandSliders[band].getValue()));
        }
    }

    /**
     * Shows the equalizer settings, or flat and disabled for null
     */
    private void setGraphicEQ(boolean enabled, double[] gains)
    {
        getGraphicEQPane();
        mGraphicEQEnabledSwitch.setSelected(enabled);

        for(int i = 0; i < GraphicEqualizer.BAND_COUNT; i++)
        {
            double gain = (gains != null && i < gains.length) ? gains[i] : 0.0;
            mEQBandSliders[i].setValue(gain);
            mEQBandFields[i].setText(String.format("%+.1f dB", gain));
            mEQBandSliders[i].setDisable(!enabled);
            mEQBandFields[i].setDisable(!enabled);
        }
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
                DecodeConfigEDACS.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigEDACS.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigEDACS.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

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

    private TextArea getLcnTextArea()
    {
        if(mLcnTextArea == null)
        {
            mLcnTextArea = new TextArea();
            mLcnTextArea.setDisable(true);
            mLcnTextArea.setPrefRowCount(5);
            mLcnTextArea.setWrapText(true);
            mLcnTextArea.setPromptText("851.175, 851.225, 851.425 ...");
            mLcnTextArea.setTooltip(new Tooltip("The system's frequencies in LCN order - LCN 1 first - separated by " +
                "commas, spaces or new lines, as Radio Reference lists them.  The control channel names voice " +
                "channels by LCN, so the order matters."));
            mLcnTextArea.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mLcnTextArea;
    }

    /**
     * Parses frequencies in MHz (or Hz) separated by commas, spaces, semicolons or new lines
     * @return frequencies in Hz, in the order entered
     */
    static List<Long> parseFrequencies(String text)
    {
        List<Long> frequencies = new ArrayList<>();

        if(text != null)
        {
            for(String token : text.split("[,;\\s]+"))
            {
                if(token.isBlank())
                {
                    continue;
                }

                try
                {
                    double value = Double.parseDouble(token.trim());
                    frequencies.add(value < 10_000 ? Math.round(value * 1_000_000.0) : Math.round(value));
                }
                catch(NumberFormatException nfe)
                {
                    //skip
                }
            }
        }

        return frequencies;
    }

    static String formatFrequencies(List<Long> frequencies)
    {
        List<String> values = new ArrayList<>();

        for(Long frequency : frequencies)
        {
            values.add(String.format(Locale.US, "%.5f", frequency / 1e6).replaceAll("0+$", "").replaceAll("\\.$", ""));
        }

        return String.join(", ", values);
    }

    private Spinner<Integer> getTrafficChannelPoolSizeSpinner()
    {
        if(mTrafficChannelPoolSizeSpinner == null)
        {
            mTrafficChannelPoolSizeSpinner = new Spinner();
            mTrafficChannelPoolSizeSpinner.setDisable(true);
            mTrafficChannelPoolSizeSpinner.setTooltip(
                new Tooltip("Maximum number of calls followed at the same time"));
            mTrafficChannelPoolSizeSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
            mTrafficChannelPoolSizeSpinner.setValueFactory(svf);
            mTrafficChannelPoolSizeSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTrafficChannelPoolSizeSpinner;
    }

    private ToggleSwitch mIgnoreUnaliasedSwitch;

    private ToggleSwitch getIgnoreUnaliasedSwitch()
    {
        if(mIgnoreUnaliasedSwitch == null)
        {
            mIgnoreUnaliasedSwitch = new ToggleSwitch();
            mIgnoreUnaliasedSwitch.setDisable(true);
            mIgnoreUnaliasedSwitch.setTooltip(new Tooltip("Calls to a talkgroup with no alias in this channel's alias " +
                "list are listed but not followed - no voice channel, no audio"));
            mIgnoreUnaliasedSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mIgnoreUnaliasedSwitch;
    }

    private Spinner<Integer> getCallTimeoutSpinner()
    {
        if(mCallTimeoutSpinner == null)
        {
            mCallTimeoutSpinner = new Spinner<>();
            mCallTimeoutSpinner.setDisable(true);
            mCallTimeoutSpinner.setTooltip(new Tooltip("Longest a followed call may last.  Calls normally end when the control channel stops " +
                "announcing them."));
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
        getLcnTextArea().setDisable(config == null);
        getCallTimeoutSpinner().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);
        getIgnoreUnaliasedSwitch().setDisable(config == null);

        if(config instanceof DecodeConfigEDACS edacs)
        {
            getLcnTextArea().setText(formatFrequencies(edacs.getLcnFrequencies()));
            getCallTimeoutSpinner().getValueFactory().setValue(edacs.getCallTimeoutSeconds());
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(edacs.getTrafficChannelPoolSize());
            getIgnoreUnaliasedSwitch().setSelected(edacs.getIgnoreUnaliasedTalkgroups());
            setGraphicEQ(edacs.isGraphicEQEnabled(), edacs.getGraphicEQBandGains());
        }
        else if(config != null)
        {
            getLcnTextArea().setText("");
            getCallTimeoutSpinner().getValueFactory().setValue(DecodeConfigEDACS.CALL_TIMEOUT_DEFAULT_SECONDS);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(DecodeConfigEDACS.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
            getIgnoreUnaliasedSwitch().setSelected(false);
            setGraphicEQ(false, null);
        }
        else
        {
            setGraphicEQ(false, null);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigEDACS config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigEDACS existing)
        {
            config = existing;
        }
        else
        {
            config = new DecodeConfigEDACS();
        }

        config.setLcnFrequencies(parseFrequencies(getLcnTextArea().getText()));
        config.setCallTimeoutSeconds(getCallTimeoutSpinner().getValue());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());
        config.setIgnoreUnaliasedTalkgroups(getIgnoreUnaliasedSwitch().isSelected());

        config.setGraphicEQEnabled(mGraphicEQEnabledSwitch.isSelected());
        double[] gains = new double[GraphicEqualizer.BAND_COUNT];
        for(int i = 0; i < GraphicEqualizer.BAND_COUNT; i++)
        {
            gains[i] = mEQBandSliders[i].getValue();
        }
        config.setGraphicEQBandGains(gains);

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
