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
package io.github.dsheirer.identifier.integer.message;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.integer.AbstractIntegerIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO-25 Unit or User status
 */
public class APCO25ShortDataMessage extends AbstractIntegerIdentifier
{
    /**
     * Constructs an APCO-25 status
     *
     * @param status value
     */
    public APCO25ShortDataMessage(int status)
    {
        super(status);
    }

    @Override
    public Form getForm()
    {
        return Form.SHORT_DATA_MESSAGE;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Creates a short data message
     *
     * @param messageId
     */
    public static APCO25ShortDataMessage create(int messageId)
    {
        return new APCO25ShortDataMessage(messageId);
    }
}
