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

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.protocol.Protocol;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.Set;


/**
 * Preference settings for channel event view
 */
public class TalkgroupFormatPreferenceEditor extends HBox
{
    private TalkgroupFormatPreference mTalkgroupFormatPreference;
    private GridPane mEditorPane;

    public TalkgroupFormatPreferenceEditor(UserPreferences userPreferences)
    {
        mTalkgroupFormatPreference = userPreferences.getTalkgroupFormatPreference();
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            int row = 0;

            mEditorPane.add(new Label("Protocol"), 0, row);
            mEditorPane.add(new Label("Display Format"), 1, row++);
            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);

            for(Protocol protocol : Protocol.TALKGROUP_PROTOCOLS)
            {
                Label label = new Label(protocol.toString());
                GridPane.setMargin(label, new Insets(0, 10, 0, 0));
                GridPane.setHalignment(label, HPos.LEFT);
                mEditorPane.add(label, 0, row);
                IntegerFormatEditor editor = new IntegerFormatEditor(mTalkgroupFormatPreference, protocol,
                    TalkgroupFormatPreference.getFormats(protocol));
                GridPane.setMargin(editor, new Insets(5,5,5,5));
                mEditorPane.add(editor, 1, row);
                FixedWidthEditor fixedWidthEditor = new FixedWidthEditor(mTalkgroupFormatPreference, protocol);
                GridPane.setMargin(fixedWidthEditor, new Insets(0,0,0,10));
                mEditorPane.add(fixedWidthEditor, 2, row);
                row++;
            }
        }

        return mEditorPane;
    }

    /**
     * Choice box control for tracking talkgroup format preference per protocol
     */
    public class IntegerFormatEditor extends ChoiceBox<IntegerFormat>
    {
        private TalkgroupFormatPreference mTalkgroupFormatPreference;
        private Protocol mProtocol;

        public IntegerFormatEditor(TalkgroupFormatPreference preference, Protocol protocol, Set<IntegerFormat> formats)
        {
            mTalkgroupFormatPreference = preference;
            mProtocol = protocol;
            setMaxWidth(Double.MAX_VALUE);
            getItems().addAll(formats);
            getSelectionModel().select(mTalkgroupFormatPreference.getTalkgroupFormat(mProtocol));
            setOnAction(event -> {
                IntegerFormat selected = getSelectionModel().getSelectedItem();
                mTalkgroupFormatPreference.setTalkgroupFormat(mProtocol, selected);
            });
        }
    }

    /**
     * Check box control for tracking fixed width preference per protocol
     */
    public class FixedWidthEditor extends CheckBox
    {
        private TalkgroupFormatPreference mTalkgroupFormatPreference;
        private Protocol mProtocol;

        public FixedWidthEditor(TalkgroupFormatPreference preference, Protocol protocol)
        {
            super("Fixed Width");
            mTalkgroupFormatPreference = preference;
            mProtocol = protocol;
            setSelected(mTalkgroupFormatPreference.isTalkgroupFixedWidth(mProtocol));
            setOnAction(event -> mTalkgroupFormatPreference.setTalkgroupFixedWidth(mProtocol, isSelected()));
        }
    }
}
