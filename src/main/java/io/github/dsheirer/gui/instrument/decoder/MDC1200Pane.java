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
import io.github.dsheirer.module.decode.mdc1200.MDCDecoderInstrumented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDC1200Pane extends AbstractAFSK1200Pane
{
    private final static Logger mLog = LoggerFactory.getLogger(MDC1200Pane.class);

    private MDCDecoderInstrumented mDecoder;

    public MDC1200Pane()
    {
        super(DecoderType.MDC1200);
    }

    protected MDCDecoderInstrumented getDecoder()
    {
        if(mDecoder == null)
        {
            mDecoder = new MDCDecoderInstrumented();
            mDecoder.getMessageFramer().setSyncDetectListener(new ISyncDetectListener()
            {
                @Override
                public void syncDetected(int bitErrors)
                {
                    mLog.debug("Sync Detected!");
                }

                @Override
                public void syncLost(int bitsProcessed)
                {
                    mLog.debug("Sync Lost!");
                }
            });
        }

        return mDecoder;
    }
}
