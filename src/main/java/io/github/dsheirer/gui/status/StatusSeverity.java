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

package io.github.dsheirer.gui.status;

import javafx.scene.Node;

/**
 * Severity of a status bar item, shown as the color of its status dot or level bar.
 */
public enum StatusSeverity
{
    /**
     * Nothing to show: off, not configured or idle
     */
    NEUTRAL("status-neutral"),
    /**
     * Working
     */
    OK("status-ok"),
    /**
     * Needs attention soon
     */
    WARNING("status-warning"),
    /**
     * Not working, or out of a resource
     */
    ERROR("status-error");

    private final String mStyleClass;

    StatusSeverity(String styleClass)
    {
        mStyleClass = styleClass;
    }

    /**
     * Style class for the severity
     */
    public String getStyleClass()
    {
        return mStyleClass;
    }

    /**
     * Applies the severity's style class to the node, replacing any other severity style class.
     */
    public void apply(Node node)
    {
        for(StatusSeverity severity: values())
        {
            if(severity != this)
            {
                node.getStyleClass().remove(severity.getStyleClass());
            }
        }

        if(!node.getStyleClass().contains(mStyleClass))
        {
            node.getStyleClass().add(mStyleClass);
        }
    }

    /**
     * The more severe of the two.
     */
    public StatusSeverity max(StatusSeverity other)
    {
        return other != null && other.ordinal() > ordinal() ? other : this;
    }

    /**
     * Severity for a level that has warning and error thresholds.
     * @param level to rate
     * @param warning threshold, inclusive
     * @param error threshold, inclusive
     * @return WARNING or ERROR when the level reaches the threshold, otherwise OK
     */
    public static StatusSeverity forLevel(double level, double warning, double error)
    {
        if(level >= error)
        {
            return ERROR;
        }
        else if(level >= warning)
        {
            return WARNING;
        }

        return OK;
    }
}
