/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.fleetsync1;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;

import java.util.Collections;
import java.util.List;

public class Fleetsync1Message extends Message
{
    private BinaryMessage mMessage;

    public Fleetsync1Message(BinaryMessage message)
    {
        mMessage = message;
    }

    public boolean isValid()
    {
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FS1 [" + mMessage.toString() + "]");
        return sb.toString();
    }

    @Override
    public String getProtocol()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Provides a listing of aliases contained in the message.
     */
    public List<Alias> getAliases()
    {
        return Collections.EMPTY_LIST;
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }

}
