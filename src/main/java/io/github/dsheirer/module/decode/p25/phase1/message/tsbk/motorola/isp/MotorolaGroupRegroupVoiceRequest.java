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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.VendorISPMessage;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Inbound request for a channel group for a super-group sent by the SU.
 */
public class MotorolaGroupRegroupVoiceRequest extends VendorISPMessage implements IServiceOptionsProvider
{
    private static final IntField SERVICE_OPTIONS = IntField.length8(OCTET_2_BIT_16);
    private static final IntField SUPERGROUP = IntField.length16(OCTET_5_BIT_40);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_7_BIT_56);

    private VoiceServiceOptions mServiceOptions;
    private TalkgroupIdentifier mSuperGroup;
    private RadioIdentifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an inbound (ISP) TSBK from the binary message sequence.
     *
     * @param dataUnitID TSBK1/2/3
     * @param message binary sequence
     * @param nac decoded from the NID
     * @param timestamp for the message
     */
    public MotorolaGroupRegroupVoiceRequest(P25P1DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitID, message, nac, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getServiceOptions().isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        sb.append(" GROUP VOICE CALL FM:").append(getSourceAddress());
        sb.append(" TO:").append(getSuperGroup());

        return super.toString();
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new VoiceServiceOptions(getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Super group for the call.
     */
    public TalkgroupIdentifier getSuperGroup()
    {
        if(mSuperGroup == null)
        {
            mSuperGroup = APCO25Talkgroup.create(getInt(SUPERGROUP));
        }

        return mSuperGroup;
    }

    /**
     * Requesting SU radio.
     */
    public RadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    /**
     * Collective identifiers available in this message.
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceAddress());
            mIdentifiers.add(getSuperGroup());
        }

        return mIdentifiers;
    }
}
