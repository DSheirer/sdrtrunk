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

package io.github.dsheirer.gui.control;

import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import org.controlsfx.control.ToggleSwitch;

/**
 * ControlsFX toggle switch that exposes its role and on/off state to screen readers.
 *
 * The ControlsFX toggle switch doesn't provide an accessible role or selected state, so assistive technology sees it
 * as a generic node with no state.  This version reports itself as a toggle button, reports the selected state, and
 * supports the accessible fire (toggle) action.  The accessible name comes from the switch text, or from a label that
 * identifies this switch via Label.setLabelFor().
 */
public class AccessibleToggleSwitch extends ToggleSwitch
{
    /**
     * Constructs an instance with no text.  Use Label.setLabelFor() or setAccessibleText() to provide an accessible name.
     */
    public AccessibleToggleSwitch()
    {
        this(null);
    }

    /**
     * Constructs an instance
     * @param text to display alongside the switch, also used as the accessible name.
     */
    public AccessibleToggleSwitch(String text)
    {
        super(text);
        setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);
        selectedProperty().addListener((observable, oldValue, newValue) ->
            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED));
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters)
    {
        if(attribute == AccessibleAttribute.SELECTED)
        {
            return isSelected();
        }

        return super.queryAccessibleAttribute(attribute, parameters);
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters)
    {
        if(action == AccessibleAction.FIRE)
        {
            fire();
        }
        else
        {
            super.executeAccessibleAction(action, parameters);
        }
    }
}
