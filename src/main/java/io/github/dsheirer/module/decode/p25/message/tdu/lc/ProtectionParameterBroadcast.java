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
package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.reference.Encryption;

public class ProtectionParameterBroadcast extends TDULinkControlMessage
{

    public final static int[] ALGORITHM_ID = {112, 113, 114, 115, 116, 117, 118, 119};
    public final static int[] KEY_ID = {120, 121, 122, 123, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147};
    public static final int[] TARGET_ADDRESS = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187, 188, 189, 190, 190, 192, 193, 194, 195};

    private IIdentifier mTargetAddress;

    public ProtectionParameterBroadcast(TDULinkControlMessage source)
    {
        super(source);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ENCRYPTION:" + getEncryption().name());
        sb.append(" KEY:" + getEncryptionKey());
        sb.append(" ADDRESSS:" + getTargetAddress());

        return sb.toString();
    }

    public Encryption getEncryption()
    {
        return Encryption.fromValue(mMessage.getInt(ALGORITHM_ID));
    }

    public int getEncryptionKey()
    {
        return mMessage.getInt(KEY_ID);
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }
}
