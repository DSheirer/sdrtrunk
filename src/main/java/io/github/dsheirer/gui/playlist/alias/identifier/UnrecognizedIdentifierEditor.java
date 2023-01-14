/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.AliasID;
import javafx.scene.control.Label;

/**
 * Empty alias identifier editor
 */
public class UnrecognizedIdentifierEditor extends IdentifierEditor<AliasID>
{
    public UnrecognizedIdentifierEditor()
    {
        Label label = new Label("Unrecognized identifier type.  For unsupported legacy types, please delete and " +
            "recreate with a supported identifier type.");
        label.setWrapText(true);
        getChildren().add(label);
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }
}
