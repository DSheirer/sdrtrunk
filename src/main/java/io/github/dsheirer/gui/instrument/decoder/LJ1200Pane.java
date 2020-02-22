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
import io.github.dsheirer.module.decode.lj1200.LJ1200DecoderInstrumented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LJ1200Pane extends AbstractAFSK1200Pane
{
    private final static Logger mLog = LoggerFactory.getLogger(LJ1200Pane.class);

    private LJ1200DecoderInstrumented mDecoder;

    public LJ1200Pane()
    {
        super(DecoderType.LJ_1200);
    }

    protected LJ1200DecoderInstrumented getDecoder()
    {
        if(mDecoder == null)
        {
            mDecoder = new LJ1200DecoderInstrumented();
            mDecoder.getTowerMessageFramer().setSyncDetectListener(new ISyncDetectListener()
            {
                @Override
                public void syncDetected(int bitErrors)
                {
                    mLog.debug("Tower Sync Detected!");
                }

                @Override
                public void syncLost(int bitsProcessed)
                {
                    //no-op
                }
            });
            mDecoder.getTransponderMessageFramer().setSyncDetectListener(new ISyncDetectListener()
            {
                @Override
                public void syncDetected(int bitErrors)
                {
                    mLog.debug("Transponder Sync Detected!");
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
