/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.mpt1327.MPT1327DecoderInstrumented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MPT1327Pane extends AbstractAFSK1200Pane
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327Pane.class);

    private MPT1327DecoderInstrumented mDecoder;

    public MPT1327Pane()
    {
        super(DecoderType.MPT1327);
    }

    protected MPT1327DecoderInstrumented getDecoder()
    {
        if(mDecoder == null)
        {
            mDecoder = new MPT1327DecoderInstrumented();
            mDecoder.getControlMessageFramer().setSyncDetectListener(new ISyncDetectListener()
            {
                @Override
                public void syncDetected(int bitErrors)
                {
//                    mLog.debug("Control Sync Detected!");
                }

                @Override
                public void syncLost(int bitsProcessed)
                {
                    //no-op
                }
            });
            mDecoder.getTrafficMessageFramer().setSyncDetectListener(new ISyncDetectListener()
            {
                @Override
                public void syncDetected(int bitErrors)
                {
//                    mLog.debug("Traffic Sync Detected!");
                }

                @Override
                public void syncLost(int bitsProcessed)
                {
                    //no-op
                }
            });
        }

        return mDecoder;
    }
}
