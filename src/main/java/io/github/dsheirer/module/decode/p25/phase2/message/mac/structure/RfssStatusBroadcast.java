/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;

/**
 * RFSS status broadcast base implementation
 */
public abstract class RfssStatusBroadcast extends MacStructure
{
    private static final IntField LRA = IntField.length8(OCTET_2_BIT_8);
    private static final int R = 18;
    private static final int A = 19;
    private static final IntField SYSTEM_ID = IntField.length12(OCTET_3_BIT_16 + 4);
    private static final IntField RFSS_ID = IntField.length8(OCTET_5_BIT_32);
    private static final IntField SITE_ID = IntField.length8(OCTET_6_BIT_40);
    private Identifier mLRA;
    private Identifier mSystem;
    private Identifier mRFSS;
    private Identifier mSite;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RfssStatusBroadcast(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String getNetworkConnectionStatus()
    {
        return getMessage().get(A + getOffset()) ? "NETWORK CONNECTED" : "NETWORK DISCONNECTED";
    }

    public String getRoamingRadioReaccessMethod()
    {
        return getMessage().get(R + getOffset()) ? "ROAMING RADIO REACCESS ON LCCH" : "ROAMING RADIO REACCESS ON VCH";
    }

    public Identifier getLRA()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getInt(LRA));
        }

        return mLRA;
    }

    public Identifier getRFSS()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getInt(RFSS_ID));
        }

        return mRFSS;
    }

    public Identifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getInt(SITE_ID));
        }

        return mSite;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getInt(SYSTEM_ID));
        }

        return mSystem;
    }
}
