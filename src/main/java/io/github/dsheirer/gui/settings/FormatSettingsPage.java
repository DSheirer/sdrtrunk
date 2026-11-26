/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.gui.settings;

import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.TimestampFormat;
import io.github.dsheirer.settings.event.DecodeEventSettings;
import io.github.dsheirer.settings.identifier.IntegerFormat;
import io.github.dsheirer.settings.identifier.TalkgroupFormatSettings;
import java.util.Date;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;

/**
 * Display format settings: talkgroups and radio identifiers for each protocol, and decode event timestamps
 */
public class FormatSettingsPage extends SettingsPage
{
    private TalkgroupFormatSettings mTalkgroupFormatSettings;
    private DecodeEventSettings mDecodeEventSettings;
    private ChoiceBox<DisplayableTimestamp> mTimestampFormatChoiceBox;

    public FormatSettingsPage(Settings settings)
    {
        super(SettingsPageType.FORMATS, settings);
        mTalkgroupFormatSettings = settings.getTalkgroupFormatSettings();
        mDecodeEventSettings = settings.getDecodeEventSettings();

        SettingsSection events = addSection("Channel Events");
        events.addRow("Timestamp format", getTimestampFormatChoiceBox())
            .track(Bindings.createBooleanBinding(() -> getTimestampFormatChoiceBox().getValue() != null &&
                    getTimestampFormatChoiceBox().getValue().getTimestampFormat() !=
                        DecodeEventSettings.DEFAULT_TIMESTAMP_FORMAT, getTimestampFormatChoiceBox().valueProperty()),
                () -> getTimestampFormatChoiceBox().getItems().stream()
                    .filter(item -> item.getTimestampFormat() == DecodeEventSettings.DEFAULT_TIMESTAMP_FORMAT)
                    .findFirst().ifPresent(getTimestampFormatChoiceBox()::setValue));

        SettingsSection section = addSection("Talkgroup & Radio ID");

        for(Protocol protocol : Protocol.TALKGROUP_PROTOCOLS)
        {
            IntegerFormatEditor formatEditor = new IntegerFormatEditor(mTalkgroupFormatSettings, protocol,
                TalkgroupFormatSettings.getFormats(protocol));
            FixedWidthEditor fixedWidthEditor = new FixedWidthEditor(mTalkgroupFormatSettings, protocol);
            section.addRow(protocol.toString(), formatEditor, fixedWidthEditor)
                .defaultValue(formatEditor.valueProperty(), TalkgroupFormatSettings.getDefaultFormat(protocol),
                    formatEditor::setValue)
                .defaultValue(fixedWidthEditor.selectedProperty(), TalkgroupFormatSettings.getDefaultFixedWidth(protocol),
                    fixedWidth -> {
                        fixedWidthEditor.setSelected(fixedWidth);
                        mTalkgroupFormatSettings.setTalkgroupFixedWidth(protocol, fixedWidth);
                    });
        }

        section.addHelp("Fixed width pads values with leading zeros to the full width of the protocol's value.");

    }

    /**
     * Choice box control for tracking talkgroup format setting per protocol
     */
    public class IntegerFormatEditor extends ChoiceBox<IntegerFormat>
    {
        private TalkgroupFormatSettings mTalkgroupFormatSettings;
        private Protocol mProtocol;

        public IntegerFormatEditor(TalkgroupFormatSettings preference, Protocol protocol, Set<IntegerFormat> formats)
        {
            mTalkgroupFormatSettings = preference;
            mProtocol = protocol;
            setPrefWidth(140);
            getItems().addAll(formats);
            getSelectionModel().select(mTalkgroupFormatSettings.getTalkgroupFormat(mProtocol));
            setOnAction(event -> {
                IntegerFormat selected = getSelectionModel().getSelectedItem();
                mTalkgroupFormatSettings.setTalkgroupFormat(mProtocol, selected);
            });
        }
    }

    /**
     * Check box control for tracking fixed width setting per protocol
     */
    public class FixedWidthEditor extends CheckBox
    {
        private TalkgroupFormatSettings mTalkgroupFormatSettings;
        private Protocol mProtocol;

        public FixedWidthEditor(TalkgroupFormatSettings preference, Protocol protocol)
        {
            super("Fixed Width");
            mTalkgroupFormatSettings = preference;
            mProtocol = protocol;
            setSelected(mTalkgroupFormatSettings.isTalkgroupFixedWidth(mProtocol));
            setOnAction(event -> mTalkgroupFormatSettings.setTalkgroupFixedWidth(mProtocol, isSelected()));
        }
    }

    private ChoiceBox<DisplayableTimestamp> getTimestampFormatChoiceBox()
    {
        if(mTimestampFormatChoiceBox == null)
        {
            mTimestampFormatChoiceBox = new ChoiceBox<>();

            for(TimestampFormat format: TimestampFormat.values())
            {
                mTimestampFormatChoiceBox.getItems().add(new DisplayableTimestamp(format));
            }

            TimestampFormat current = mDecodeEventSettings.getTimestampFormat();

            for(DisplayableTimestamp displayableTimestamp: mTimestampFormatChoiceBox.getItems())
            {
                if(displayableTimestamp.getTimestampFormat() == current)
                {
                    mTimestampFormatChoiceBox.getSelectionModel().select(displayableTimestamp);
                    continue;
                }
            }

            mTimestampFormatChoiceBox.setOnAction(event -> {
                DisplayableTimestamp selected = mTimestampFormatChoiceBox.getSelectionModel().getSelectedItem();
                mDecodeEventSettings.setTimestampFormat(selected.getTimestampFormat());
            });
        }

        return mTimestampFormatChoiceBox;
    }

    public class DisplayableTimestamp
    {
        private TimestampFormat mTimestampFormat;

        public DisplayableTimestamp(TimestampFormat timestampFormat)
        {
            mTimestampFormat = timestampFormat;
        }

        public TimestampFormat getTimestampFormat()
        {
            return mTimestampFormat;
        }

        public String toString()
        {
            return mTimestampFormat.getFormatter().format(new Date(System.currentTimeMillis()));
        }
    }
}
