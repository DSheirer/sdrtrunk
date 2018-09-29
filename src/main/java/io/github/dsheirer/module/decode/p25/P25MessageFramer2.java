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
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class P25MessageFramer2 implements Listener<Dibit>, IDataUnitDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25MessageFramer2.class);
    private P25DataUnitDetector mDataUnitDetector;
    private CorrectedBinaryMessage mBinaryMessage;
    private DataUnitID mDataUnitID;
    private boolean mAssemblingMessage = false;
    private int mNAC;
    private int mStatusSymbolDibitCounter = 0;
    private int mStatusSymbolsDetected = 0;

    private P25MessageFramer2(IPhaseLockedLoop phaseLockedLoop)
    {
        mDataUnitDetector = new P25DataUnitDetector(this, phaseLockedLoop);
    }

    public P25DataUnitDetector getDataUnitDetector()
    {
        return mDataUnitDetector;
    }

    @Override
    public void receive(Dibit dibit)
    {
        if(mAssemblingMessage)
        {
            //Strip out the status symbol dibit after every 70 bits or 35 dibits
            if(mStatusSymbolDibitCounter >  35)
            {
                mStatusSymbolDibitCounter = 0;
                mStatusSymbolsDetected++;
                return;
            }

            mStatusSymbolDibitCounter++;

            if(mBinaryMessage.isFull())
            {
                processMessage();
            }
            else
            {
                try
                {
                    mBinaryMessage.add(dibit.getBit1());
                    mBinaryMessage.add(dibit.getBit2());
                }
                catch(BitSetFullException bsfe)
                {
                    mLog.debug("Message full exception - unexpected");
                }
            }
        }
        else
        {
            mDataUnitDetector.receive(dibit);
        }
    }

    private void processMessage()
    {
        mLog.debug(mDataUnitID.name() + " - Message Complete: " + mBinaryMessage.toString());
        //TODO: use the message factory and then send it on its way
        reset();
        mDataUnitDetector.reset();
    }

    private void reset()
    {
        mBinaryMessage = null;
        mAssemblingMessage = false;
        mDataUnitID = null;
        mNAC = 0;
        mStatusSymbolDibitCounter = 0;

        mLog.debug("Status Symbols Removed: " + mStatusSymbolsDetected);
        mStatusSymbolsDetected = 0;
    }

    public void receive(ReusableByteBuffer buffer)
    {
        for(byte value: buffer.getBytes())
        {
            for(int x = 0; x <=3; x++)
            {
                receive(Dibit.parse(value, x));
            }
        }

        buffer.decrementUserCount();
    }

    @Override
    public void dataUnitDetected(DataUnitID dataUnitID, int nac, int bitErrors, long discardedDibits)
    {
        mLog.debug("Data Unit Detected [" + dataUnitID +
            "] NAC [" + Integer.toHexString(nac).toUpperCase() +
            "] Bit Errors [" + bitErrors +
            "] Discarded Dibits [" + discardedDibits +
            "] Discarded Bits [" + (discardedDibits * 2) + "]");

        if(dataUnitID.getMessageLength() < 0)
        {
            mLog.debug("Unrecogized Data Unit Id: " + dataUnitID.name());
            return;
        }

        mDataUnitID = dataUnitID;
        mNAC = nac;
        mBinaryMessage = new CorrectedBinaryMessage(dataUnitID.getMessageLength());
        mBinaryMessage.incrementCorrectedBitCount(bitErrors);
        mAssemblingMessage = true;
        mStatusSymbolDibitCounter = 21;
    }

//TODO: make syncLost() method report the number of bits/dibits processed with each check (ie lost)
    @Override
    public void syncLost()
    {
//        mLog.debug("Sync Lost!");
    }

    public static void main(String[] args)
    {
        P25MessageFramer2 messageFramer = new P25MessageFramer2(null);

        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_045614_CNYICC_Onondaga Simulcast_LCN 09.bits");

        try(BinaryReader reader = new BinaryReader(path, 200))
        {
            while(reader.hasNext())
            {
                messageFramer.receive(reader.next());
            }
        }
        catch(Exception ioe)
        {
            ioe.printStackTrace();
        }


        mLog.debug("NIDS Detected: " + messageFramer.getDataUnitDetector().getNIDDetectionCount());
    }
}
