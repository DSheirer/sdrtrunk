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

package io.github.dsheirer.gui.playlist.eventlog;

import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
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
public class EventLogConfigurationEditor extends Editor<EventLogConfiguration>
{
    private List<EventLogControl> mControls = new ArrayList<>();

    public EventLogConfigurationEditor(Collection<EventLogType> types)
    {
        for(EventLogType type: types)
        {
            EventLogControl control = new EventLogControl(type);
            mControls.add(control);
            getChildren().add(control);
        }
    }

    @Override
    public void setItem(EventLogConfiguration item)
    {
        if(item == null)
        {
            item = new EventLogConfiguration();
        }

        super.setItem(item);

        for(EventLogControl control: mControls)
        {
            control.getToggleSwitch().setDisable(false);
            control.getToggleSwitch().setSelected(item.getLoggers().contains(control.getEventLogType()));
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        EventLogConfiguration config = getItem();

        if(config == null)
        {
            config = new EventLogConfiguration();
        }

        config.clear();

        for(EventLogControl control: mControls)
        {
            if(control.getToggleSwitch().isSelected())
            {
                config.addLogger(control.getEventLogType());
            }
        }

        setItem(config);
    }

    @Override
    public void dispose()
    {

    }

    public class EventLogControl extends GridPane
    {
        private EventLogType mEventLogType;
        private ToggleSwitch mToggleSwitch;

        public EventLogControl(EventLogType type)
        {
            mEventLogType = type;

            setPadding(new Insets(5,5,5,5));
            setHgap(10);

            GridPane.setConstraints(getToggleSwitch(), 0, 0);
            getChildren().add(getToggleSwitch());

            Label label = new Label(mEventLogType.getDisplayString());
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

        public EventLogType getEventLogType()
        {
            return mEventLogType;
        }
    }
}
