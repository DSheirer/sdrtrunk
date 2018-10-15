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
package io.github.dsheirer.identifier.string;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.protocol.Protocol;

public class APCO25TelephoneNumber extends AbstractStringIdentifier
{
    public APCO25TelephoneNumber(String telephoneNumber)
    {
        super(telephoneNumber);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public Form getForm()
    {
        return Form.TELEPHONE_NUMBER;
    }

    /**
     * Creates a new APCO-25 telephone number
     */
    public static IIdentifier create(String telephoneNumber)
    {
        return new APCO25TelephoneNumber(telephoneNumber);
    }
}
