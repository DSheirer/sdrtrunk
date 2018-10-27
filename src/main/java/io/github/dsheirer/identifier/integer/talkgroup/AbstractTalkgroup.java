/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.identifier.integer.talkgroup;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.AbstractIntegerIdentifier;

import java.util.Collections;
import java.util.List;

public abstract class AbstractTalkgroup extends AbstractIntegerIdentifier
{
    /**
     * Abstract integer talkgroup identifier class.
     * @param value of the talkgroup
     */
    public AbstractTalkgroup(int value)
    {
        super(value);
    }

    @Override
    public Form getForm()
    {
        return Form.TALKGROUP;
    }

    /**
     * Indicates if this is a patch group or a group of talkgroups.
     */
    public boolean isPatchGroup()
    {
        return false;
    }

    /**
     * List of IIdentifiers that are included in a patch group
     */
    public List<IIdentifier> getPatchedGroups()
    {
        return Collections.EMPTY_LIST;
    }
}
