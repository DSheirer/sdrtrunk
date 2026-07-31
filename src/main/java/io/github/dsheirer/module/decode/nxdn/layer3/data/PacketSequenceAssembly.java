/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.data;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.IPacketHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.UserData;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.NXDNCRC;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Packet data assembly.  Assembles either a short data call or a data call packet sequence that includes a header
 * and one or more data blocks.
 */
public class PacketSequenceAssembly
{
    private final IPacketHeader mHeader;
    private final List<UserData> mUserDataList = new ArrayList<>();
    private final EncryptionKeyIdentifier mEncryption;
    private CorrectedBinaryMessage mPayload;
    private String mShortDataIV;

    /**
     * Constructs an instance
     * @param header for the data/short data call.
     */
    public PacketSequenceAssembly(IPacketHeader header)
    {
        mHeader = header;
        mEncryption = mHeader.getEncryptionKeyIdentifier();
    }

    /**
     * Sets an optional encryption key for the short data call.
     * @param encryption to add
     */
    public void set(ShortDataInitializationVector encryption)
    {
        mShortDataIV = encryption.getInitializationVector();
    }

    /**
     * Indicates if this is a short data assembly
     */
    public boolean isShortData()
    {
        return mHeader instanceof ShortDataCallRequestHeader;
    }

    /**
     * Indicates if this is a data call assembly
     */
    public boolean isDataCall()
    {
        return mHeader instanceof DataCallHeader;
    }

    /**
     * Access the fully reassembled packet sequence.
     * @return sequence
     */
    public PacketSequence getPacketSequence()
    {
        if(isComplete())
        {
            return new PacketSequence(getPayload(), mHeader, mEncryption, mShortDataIV);
        }

        return null;
    }

    /**
     * Assembles the payload from the user data blocks.
     */
    private CorrectedBinaryMessage getPayload()
    {
        if(mUserDataList.isEmpty() || !isComplete())
        {
            return null;
        }

        if(mPayload == null)
        {
            mUserDataList.sort(Comparator.comparingInt(UserData::getBlockNumber).reversed());
            int padOctets = mHeader.getPacketInformation().getPadOctetCount();
            int messageOctets = mUserDataList.getFirst().getUserDataByteLength() * mUserDataList.size() - padOctets;
            CorrectedBinaryMessage payload = new CorrectedBinaryMessage(messageOctets * 8);

            int offset = 0;

            for(UserData userData: mUserDataList)
            {
                if(userData.getBlockNumber() == 0)
                {
                    BinaryMessage fragment = userData.getUserData(padOctets);
                    payload.load(offset, fragment);
                    offset += fragment.size();
                }
                else
                {
                    BinaryMessage fragment = userData.getUserData();
                    payload.load(offset, fragment);
                    offset += fragment.size();
                }
            }

            boolean passesCRC = NXDNCRC.checkMessage(payload, (messageOctets - 4) * 8);

            //Clone the extracted payload and reverse the byte order, leaving the CRC-32 intact at the end of the message
            mPayload = payload.getSubMessage(0, payload.size());
            mPayload.setCorrectedBitCount(passesCRC ? 0 : -1);

            int reverse = (messageOctets - 5) * 8;
            int forward = 0;

            while(reverse >= 0)
            {
                mPayload.setByte(reverse, payload.getByte(forward));
                forward += 8;
                reverse -= 8;
            }
        }

        return mPayload;
    }

    /**
     * Indicates if the sequence is complete with all expected data blocks.
     */
    public boolean isComplete()
    {
        return mUserDataList.size() == mHeader.getPacketInformation().getBlockCount() + 1;
    }

    /**
     * Adds the user data block to this assembly
     * @param userData block to add.
     */
    public void add(UserData userData)
    {
        mUserDataList.add(userData);
    }
}
