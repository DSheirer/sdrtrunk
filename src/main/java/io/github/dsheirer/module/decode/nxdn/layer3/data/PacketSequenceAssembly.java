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
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.UserData;
import io.github.dsheirer.module.decode.nxdn.layer3.type.PacketInformation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Packet data assembly
 */
public class PacketSequenceAssembly
{
    private PacketInformation mPacketInformation;
    private EncryptionKeyIdentifier mEncryption;
    private DataCallHeader mDataCallHeader;
    private ShortDataCallRequestHeader mShortDataHeader;
    private List<UserData> mUserDataList = new ArrayList<>();
    private CorrectedBinaryMessage mPayload;
    private String mShortDataIV;

    /**
     * Constructs an instance for a data call.
     * @param header for the data call.
     */
    public PacketSequenceAssembly(DataCallHeader header)
    {
        mDataCallHeader = header;
        mPacketInformation = mDataCallHeader.getPacketInformation();
        mEncryption = mDataCallHeader.getEncryptionKeyIdentifier();
    }

    /**
     * Constructs an instance for a short data call.
     * @param header for the short data call.
     */
    public PacketSequenceAssembly(ShortDataCallRequestHeader header)
    {
        mShortDataHeader = header;
        mPacketInformation = mShortDataHeader.getPacketInformation();
        mEncryption = mShortDataHeader.getEncryptionKeyIdentifier();
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
        return mShortDataHeader != null;
    }

    /**
     * Indicates if this is a data call assembly
     */
    public boolean isDataCall()
    {
        return mDataCallHeader != null;
    }

    /**
     * Access the fully reassembled packet sequence.
     * @return sequence
     */
    public PacketSequence getPacketSequence()
    {
        if(isComplete())
        {
            return new PacketSequence(getPayload(), mShortDataHeader != null ? mShortDataHeader : mDataCallHeader,
                    mEncryption, mShortDataIV);
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
            int padOctets = mPacketInformation.getPadOctetCount();
            mUserDataList.sort(Comparator.comparingInt(UserData::getBlockNumber));
            mPayload = new CorrectedBinaryMessage((mUserDataList.getFirst().getUserDataByteLength() * mUserDataList.size() - padOctets) * 8);

            int offset = 0;

            for(int block = 0; block < mUserDataList.size(); block++)
            {
                UserData userData = mUserDataList.get(block);

                if(block == 0)
                {
                    BinaryMessage fragment = userData.getUserData(padOctets);
                    mPayload.load(offset, fragment);
                    offset += fragment.size();
                }
                else
                {
                    BinaryMessage fragment = userData.getUserData();
                    mPayload.load(offset, fragment);
                    offset += fragment.size();
                }
            }
        }

        return mPayload;
    }

    /**
     * Indicates if the sequence is complete with all expected data blocks.
     */
    public boolean isComplete()
    {
        return mUserDataList.size() == mPacketInformation.getBlockCount() + 1;
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
