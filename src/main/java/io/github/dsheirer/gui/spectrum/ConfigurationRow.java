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

package io.github.dsheirer.gui.spectrum;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import org.controlsfx.control.ToggleSwitch;

/**
 * Labeled control that a display layer adds to its section of the configuration panel.
 *
 * @param label shown to the left of the control
 * @param control to change the setting
 */
public record ConfigurationRow(String label, Node control)
{
    private static final double SWITCH_WIDTH = 40;

    /**
     * Creates a row with an on/off switch bound to the property.
     */
    public static ConfigurationRow toggle(String label, BooleanProperty property)
    {
        ToggleSwitch toggleSwitch = new ToggleSwitch();
        toggleSwitch.selectedProperty().bindBidirectional(property);
        //The switch skin reserves space for its text even when there is none, so size it to the thumb track
        toggleSwitch.setMinWidth(SWITCH_WIDTH);
        toggleSwitch.setPrefWidth(SWITCH_WIDTH);
        toggleSwitch.setMaxWidth(SWITCH_WIDTH);
        return new ConfigurationRow(label, toggleSwitch);
    }
}
