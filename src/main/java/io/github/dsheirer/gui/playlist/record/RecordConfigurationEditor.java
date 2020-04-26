/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.record;

import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Editor for event logging configuration objects using a VBox to visually display a vertical list of boolean toggle
 * switches to turn on/off event logging for a custom set of event log types.
 */
public class RecordConfigurationEditor extends Editor<RecordConfiguration>
{
    private List<RecorderControl> mControls = new ArrayList<>();

    public RecordConfigurationEditor(Collection<RecorderType> types)
    {
        for(RecorderType type: types)
        {
            RecorderControl control = new RecorderControl(type);
            mControls.add(control);
            getChildren().add(control);
        }
    }

    @Override
    public void setItem(RecordConfiguration item)
    {
        if(item == null)
        {
            item = new RecordConfiguration();
        }

        super.setItem(item);

        for(RecorderControl control: mControls)
        {
            control.getToggleSwitch().setDisable(false);
            control.getToggleSwitch().setSelected(item.getRecorders().contains(control.getRecorderType()));
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        RecordConfiguration config = getItem();

        if(config == null)
        {
            config = new RecordConfiguration();
        }

        config.clearRecorders();

        for(RecorderControl control: mControls)
        {
            if(control.getToggleSwitch().isSelected())
            {
                config.addRecorder(control.getRecorderType());
            }
        }

        setItem(config);
    }

    @Override
    public void dispose()
    {

    }

    public class RecorderControl extends GridPane
    {
        private RecorderType mRecorderType;
        private ToggleSwitch mToggleSwitch;

        public RecorderControl(RecorderType type)
        {
            mRecorderType = type;

            setPadding(new Insets(5,5,5,0));
            setHgap(10);

            GridPane.setConstraints(getToggleSwitch(), 0, 0);
            getChildren().add(getToggleSwitch());

            Label label = new Label(mRecorderType.getDisplayString());
            GridPane.setHalignment(label, HPos.LEFT);
            GridPane.setConstraints(label, 1, 0);
            getChildren().add(label);
        }

        private ToggleSwitch getToggleSwitch()
        {
            if(mToggleSwitch == null)
            {
                mToggleSwitch = new ToggleSwitch();
                mToggleSwitch.setDisable(true);
                mToggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
            }

            return mToggleSwitch;
        }

        public RecorderType getRecorderType()
        {
            return mRecorderType;
        }
    }
}
