/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 1 message assembler.  Processes an incoming stream of demodulated dibits/symbols and tracks the expected
 * message length against the actual received symbol count to indicate when the message is fully assembled.
 *
 * This assembler also supports fuzzy message type estimation.  When the NID can't be fully error corrected, we can make
 * the best guess for message type and then validate at the next sync interval that the quantity of elapsed symbols
 * matches the expected message length and then either accept the estimated data unit ID, or reassign a data unit ID
 * that more closely matches the quantity of elapsed symbols.
 */
public class NXDNFrameAssembler implements Listener<Dibit>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NXDNFrameAssembler.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private CorrectedBinaryMessage mMessage;

    /**
     * Constructs an instance
     *
     * Note: if the duid is PLACEHOLDER it indicates the NID did not pass error detection/correction and therefore the
     * duid is suspect and we'll close out the message assembly once the next sync is detected and then we can inspect
     * the previous/next duids to determine what is the logical duid that would have been transmitted in between, and
     * also check the dibit count to determine the correct duid.
     */
    public NXDNFrameAssembler()
    {
        mMessage = new CorrectedBinaryMessage(364);
    }

    /**
     * Access the completed message.
     * @return message.
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Primary input method for demodulated dibits/symbols to be appended to the message under assembly.
     * @param dibit to add to the message.
     */
    public void receive(Dibit dibit)
    {
        if(mMessage.isFull())
        {
            LOGGING_SUPPRESSOR.error("NXDN-Full", 3, "NXDN frame under assembly is full - can't " +
                    "add additional dibits.");
        }
        else
        {
            mMessage.add(dibit.getBit1(), dibit.getBit2());
        }
    }

    /**
     * Indicates if the message is fully assembled meaning that the quantity of assembled dibits matches the data unit
     * ID message length.
     * @return true if complete.
     */
    public boolean isComplete()
    {
        return getMessage().isFull();
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage().toString());
        return sb.toString();
    }
}
