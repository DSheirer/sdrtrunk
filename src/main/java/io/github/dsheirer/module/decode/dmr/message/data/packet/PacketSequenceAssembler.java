/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.packet;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import io.github.dsheirer.module.decode.dmr.message.data.header.PacketSequenceHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;
import io.github.dsheirer.sample.Listener;

/**
 * Reassembles packet sequences
 */
public class PacketSequenceAssembler
{
    private Listener<IMessage> mMessageListener;
    private PacketSequence mTimeslot0Sequence;
    private PacketSequence mTimeslot1Sequence;

    /**
     * Constructs an instance
     */
    public PacketSequenceAssembler()
    {
    }

    /**
     * Gets the current packet sequence for the specified timeslot, constructing a new sequence as necessary.
     * @param timeslot of the packet sequence
     * @return timeslot sequence
     */
    private PacketSequence getPacketSequence(int timeslot)
    {
        if(timeslot == 0)
        {
            if(mTimeslot0Sequence == null)
            {
                mTimeslot0Sequence = new PacketSequence(1);
            }

            return mTimeslot0Sequence;
        }
        else
        {
            if(mTimeslot1Sequence == null)
            {
                mTimeslot1Sequence = new PacketSequence(2);
            }

            return mTimeslot1Sequence;
        }
    }

    /**
     * Sets the listener to receive packet packet sequence messages
     * @param listener to receive messages
     */
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Dispatches the packet sequence for the specified timeslot and sets the sequence to null.
     * @param timeslot to dispatch
     */
    private void dispatchPacketSequence(int timeslot)
    {
        PacketSequence packetSequence = (timeslot == 0 ? mTimeslot0Sequence : mTimeslot1Sequence);

        if(mMessageListener != null && packetSequence != null && packetSequence.isComplete())
        {
            IMessage message = PacketSequenceMessageFactory.create(packetSequence);

            if(message != null)
            {
                mMessageListener.receive(message);
            }
        }

        if(timeslot == 0)
        {
            mTimeslot0Sequence = null;
        }
        else
        {
            mTimeslot1Sequence = null;
        }
    }

    /**
     * Processes a packet sequence preamble.
     *
     * Note: DMR systems can transmit several preamble messages prior to the actual packet sequence.
     * @param preamble for a packet sequence
     */
    public void process(Preamble preamble)
    {
        int timeslot = preamble.getTimeslot();

        PacketSequence packetSequence = getPacketSequence(timeslot);

        //If we already have headers or data blocks for the current sequence, then this is a new sequence
        if(packetSequence.hasPacketSequenceHeader() ||
           packetSequence.hasProprietaryDataHeader() ||
           packetSequence.hasDataBlocks())
        {
            dispatchPacketSequence(timeslot);
            packetSequence = getPacketSequence(timeslot);
        }

        packetSequence.addPreamble(preamble);
    }

    /**
     * Processes a packet sequence header message.
     *
     * Note: a DMR packet sequence will have at least one header, but can also have a second header.
     * @param header to process
     */
    public void process(PacketSequenceHeader header)
    {
        int timeslot = header.getTimeslot();

        PacketSequence packetSequence = getPacketSequence(timeslot);

        //If we already have headers or data blocks for the current sequence, then this is a new sequence
        if(packetSequence.hasPacketSequenceHeader() ||
            packetSequence.hasProprietaryDataHeader() ||
            packetSequence.hasDataBlocks())
        {
            dispatchPacketSequence(timeslot);
            packetSequence = getPacketSequence(timeslot);
        }

        packetSequence.setPacketSequenceHeader(header);
    }

    /**
     * Processes a proprietary packet sequence header.
     */
    public void process(ProprietaryDataHeader proprietaryHeader)
    {
        int timeslot = proprietaryHeader.getTimeslot();

        PacketSequence packetSequence = getPacketSequence(timeslot);

        //If we already have headers or data blocks for the current sequence, then this is a new sequence
        if(packetSequence.hasProprietaryDataHeader() || packetSequence.hasDataBlocks())
        {
            dispatchPacketSequence(timeslot);
            packetSequence = getPacketSequence(timeslot);
        }

        packetSequence.setProprietaryHeader(proprietaryHeader);
    }

    /**
     * Processes a data block.
     * @param dataBlock to process
     */
    public void process(DataBlock dataBlock)
    {
        int timeslot = dataBlock.getTimeslot();
        PacketSequence packetSequence = getPacketSequence(timeslot);
        packetSequence.addDataBlock(dataBlock);

        if(packetSequence.isComplete())
        {
            dispatchPacketSequence(timeslot);
        }
    }
}
