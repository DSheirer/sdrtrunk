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
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.IP25ChannelGrantDetailProvider;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit-to-Unit voice channel grant - extended LCCH
 */
public class UnitToUnitVoiceServiceChannelGrantExtendedLCCH extends MacStructureMultiFragment
        implements IFrequencyBandReceiver, IP25ChannelGrantDetailProvider, IServiceOptionsProvider
{
    private static final IntField SERVICE_OPTIONS = IntField.length8(OCTET_4_BIT_24);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_5_BIT_32);
    private static final IntField SOURCE_SUID_WACN = IntField.range(OCTET_8_BIT_56, OCTET_8_BIT_56 + 20);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.range(76, 87);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_12_BIT_88);
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.range(OCTET_15_BIT_112, OCTET_15_BIT_112 + 4);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.range(116, 127);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.range(OCTET_17_BIT_128, OCTET_17_BIT_128 + 4);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.range(132, 143);
    private static final IntField FRAGMENT_0_TARGET_ADDRESS = IntField.length24(OCTET_3_BIT_16);
    private static final IntField FRAGMENT_0_FULLY_QUALIFIED_TARGET_WACN = IntField.range(OCTET_6_BIT_40, OCTET_6_BIT_40 + 20);
    private static final IntField FRAGMENT_0_FULLY_QUALIFIED_TARGET_SYSTEM = IntField.range(60, 71);
    private static final IntField FRAGMENT_0_FULLY_QUALIFIED_TARGET_RADIO = IntField.length24(OCTET_10_BIT_72);
    private static final IntField FRAGMENT_0_MULTI_FRAGMENT_CRC = IntField.range(96, 111);

    private ServiceOptions mServiceOptions;
    private APCO25Channel mChannel;
    private APCO25FullyQualifiedRadioIdentifier mSourceAddress;
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public UnitToUnitVoiceServiceChannelGrantExtendedLCCH(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("UNIT-TO-UNIT VOICE SERVICE CHANNEL GRANT EXTENDED (1/2)");
        sb.append(" FM:").append(getSourceAddress());

        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }
        sb.append(" CHAN:").append(getChannel());
        return sb.toString();
    }

    /**
     * Service options for the voice call.
     */
    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new VoiceServiceOptions(getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Fully qualified radio source identify with aliased with a local persona radio address.
     */
    public APCO25FullyQualifiedRadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            int localAddress = getInt(SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int radio = getInt(SOURCE_SUID_ID);
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(localAddress, wacn, system, radio);
        }

        return mSourceAddress;
    }

    /**
     * Fully qualified radio source identify with aliased with a local persona radio address.
     */
    public APCO25FullyQualifiedRadioIdentifier getTargetAddress()
    {
        if(mTargetAddress == null && hasFragment(0))
        {
            int localAddress = getFragment(0).getInt(FRAGMENT_0_TARGET_ADDRESS);
            int wacn = getFragment(0).getInt(FRAGMENT_0_FULLY_QUALIFIED_TARGET_WACN);
            int system = getFragment(0).getInt(FRAGMENT_0_FULLY_QUALIFIED_TARGET_SYSTEM);
            int radio = getFragment(0).getInt(FRAGMENT_0_FULLY_QUALIFIED_TARGET_RADIO);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(localAddress, wacn, system, radio);
        }

        return mTargetAddress;
    }

    /**
     * Channel with explicit transmit and receive frequency bands.
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getInt(TRANSMIT_FREQUENCY_BAND), getInt(TRANSMIT_CHANNEL_NUMBER),
                getInt(RECEIVE_FREQUENCY_BAND), getInt(RECEIVE_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        //Note: this has to be dynamically constructed each time to account for late-add continuation fragments.
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(getSourceAddress());

        if(getTargetAddress() != null)
        {
            identifiers.add(getTargetAddress());
        }

        identifiers.add(getChannel());

        return identifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
