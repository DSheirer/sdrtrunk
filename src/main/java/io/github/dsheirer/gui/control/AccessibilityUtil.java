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

import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.controlsfx.control.SegmentedButton;

/**
 * Screen reader (accessibility) utilities for JavaFX controls.
 */
public final class AccessibilityUtil
{
    private AccessibilityUtil()
    {
        //Not instantiable
    }

    /**
     * Provides an accessible name for each toggle button in a segmented button group, combining the group label with
     * the button text (e.g. "Channel Bandwidth: 12.5 kHz").
     *
     * A segmented button is a container, so a label can't identify it via Label.setLabelFor() in a way that screen
     * readers announce: each toggle button receives focus individually and would otherwise be announced by its own text
     * only, without the context of the group.  Buttons added to the group later are also named.
     *
     * @param groupLabel that visually identifies the segmented button group
     * @param segmentedButton containing the toggle buttons
     */
    public static void labelGroup(Label groupLabel, SegmentedButton segmentedButton)
    {
        String groupName = groupLabel.getText() == null ? "" : groupLabel.getText().trim();

        //Remove a trailing colon so that the name doesn't announce "colon colon"
        if(groupName.endsWith(":"))
        {
            groupName = groupName.substring(0, groupName.length() - 1).trim();
        }

        final String name = groupName;

        for(ToggleButton button: segmentedButton.getButtons())
        {
            setGroupName(name, button);
        }

        segmentedButton.getButtons().addListener((ListChangeListener<ToggleButton>)change -> {
            while(change.next())
            {
                for(ToggleButton added: change.getAddedSubList())
                {
                    setGroupName(name, added);
                }
            }
        });
    }

    /**
     * Sets the accessible text for the button as the group name and button text.
     */
    private static void setGroupName(String groupName, ToggleButton button)
    {
        String text = button.getText() == null ? "" : button.getText();
        button.setAccessibleText(groupName.isEmpty() ? text : groupName + ": " + text);
    }
}
