/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.radioreference;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Popup alert window that informs the user that the radio reference site is currently unavailable
 */
public class RadioReferenceUnavailableAlert extends Alert
{
    public RadioReferenceUnavailableAlert(Node ownerNode)
    {
        super(AlertType.INFORMATION, "This may be caused by a network outage or there was an error " +
            "accessing the service.  If this problem occurs each time you attempt to access a " +
            "specific system or agency, please notify the developer.  Details for this issue " +
            "are located in the sdrtrunk application log.", ButtonType.OK);

        setTitle("Service Unavailable");
        setHeaderText("Radio Reference Error or Service Is Currently Unavailable");
        initOwner(ownerNode.getScene().getWindow());
    }
}
