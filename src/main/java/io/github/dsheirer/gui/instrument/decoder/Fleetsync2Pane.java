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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.fleetsync2.Fleetsync2DecoderInstrumented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fleetsync2Pane extends AbstractAFSK1200Pane
{
    private final static Logger mLog = LoggerFactory.getLogger(Fleetsync2Pane.class);

    private Fleetsync2DecoderInstrumented mDecoder;

    public Fleetsync2Pane()
    {
        super(DecoderType.FLEETSYNC2);
    }

    protected Fleetsync2DecoderInstrumented getDecoder()
    {
        if(mDecoder == null)
        {
            mDecoder = new Fleetsync2DecoderInstrumented();
        }

        return mDecoder;
    }
}
