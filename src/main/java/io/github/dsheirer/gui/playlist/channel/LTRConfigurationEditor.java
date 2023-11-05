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

import io.github.dsheirer.gui.playlist.decoder.AuxDecoderConfigurationEditor;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
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
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.SegmentedButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LTR (standard) channel configuration editor
 */
public class LTRConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRConfigurationEditor.class);
    private TitledPane mAuxDecoderPane;
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SegmentedButton mDirectionButton;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private AuxDecoderConfigurationEditor mAuxDecoderConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public LTRConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.LTR;
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
            mDecoderPane.setText("Decoder: LTR");
            mDecoderPane.setExpanded(false);
            mDecoderPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
                //There's a bug either in the toggle group or the segmented button where setting the toggle to selected
                //when the control is hidden in a titled pane prevents the selection, so we have to select the toggle
                //again when the pane is expanded.  This state is detected by a null selected toggle.
                if(newValue)
                {
                    if(getDirectionButton().getToggleGroup().getSelectedToggle() == null)
                    {
                        if(getItem().getDecodeConfiguration() instanceof DecodeConfigLTRStandard)
                        {
                            DecodeConfigLTRStandard decodeConfig = (DecodeConfigLTRStandard)getItem().getDecodeConfiguration();
                            MessageDirection direction = decodeConfig.getMessageDirection();

                            if(direction == null)
                            {
                                direction = MessageDirection.OSW;
                            }

                            //This will cause the modified property to trigger, so capture the modified state before
                            //and re-apply after
                            boolean modified = modifiedProperty().get();
                            for(Toggle toggle: getDirectionButton().getToggleGroup().getToggles())
                            {
                                toggle.setSelected(toggle.getUserData() == direction);
                            }
                            modifiedProperty().set(modified);
                        }
                    }
                }
            });

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);

            Label directionLabel = new Label("Direction");
            GridPane.setHalignment(directionLabel, HPos.LEFT);
            GridPane.setConstraints(directionLabel, 0, 0);
            gridPane.getChildren().add(directionLabel);

            GridPane.setConstraints(getDirectionButton(), 1, 0);
            gridPane.getChildren().add(getDirectionButton());

            Label instructions = new Label("OSW: repeater output signaling (default).  ISW: repeater input signaling");
            GridPane.setConstraints(instructions, 2, 0);
            gridPane.getChildren().addAll(instructions);

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
            List<DecoderType> types = new ArrayList<>();
            types.add(DecoderType.FLEETSYNC2);
            types.add(DecoderType.MDC1200);
            mAuxDecoderConfigurationEditor = new AuxDecoderConfigurationEditor(types);
            mAuxDecoderConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mAuxDecoderConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAuxDecoderConfigurationEditor;
    }

    private SegmentedButton getDirectionButton()
    {
        if(mDirectionButton == null)
        {
            mDirectionButton = new SegmentedButton();
            mDirectionButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mDirectionButton.setDisable(true);

            for(MessageDirection messageDirection: MessageDirection.ORDERED_VALUES)
            {
                ToggleButton toggleButton = new ToggleButton(messageDirection.name());
                toggleButton.setTooltip(new Tooltip(messageDirection.toString()));
                toggleButton.setUserData(messageDirection);
                mDirectionButton.getButtons().add(toggleButton);
            }

            mDirectionButton.getToggleGroup().selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mDirectionButton;
    }

    private RecordConfigurationEditor getRecordConfigurationEditor()
    {
        if(mRecordConfigurationEditor == null)
        {
            List<RecorderType> types = new ArrayList<>();
            types.add(RecorderType.BASEBAND);
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
        if(config instanceof DecodeConfigLTRStandard)
        {
            getDirectionButton().setDisable(false);
            DecodeConfigLTRStandard decodeConfig = (DecodeConfigLTRStandard)config;
            MessageDirection direction = decodeConfig.getMessageDirection();

            if(direction == null)
            {
                direction = MessageDirection.OSW;
            }

            for(Toggle toggle: getDirectionButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(toggle.getUserData() == direction);
            }
        }
        else
        {
            getDirectionButton().setDisable(true);

            for(Toggle toggle: getDirectionButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(false);
            }
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigLTRStandard config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigLTRStandard)
        {
            config = (DecodeConfigLTRStandard)getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigLTRStandard();
        }

        MessageDirection messageDirection = MessageDirection.OSW;

        if(getDirectionButton().getToggleGroup().getSelectedToggle() != null)
        {
            messageDirection = (MessageDirection)getDirectionButton().getToggleGroup().getSelectedToggle().getUserData();
        }

        config.setMessageDirection(messageDirection);
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
