/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.sample.Listener;

/**
 * Processes P25 status dibits to determine if the channel being monitored is the output of a repeater or if
 * the channel is from a mobile subscriber.
 *
 * Dibit 00 is used by the subscriber
 * Dibits 01 and 11 are used by the repeater.
 * Dibit 10 is used by both and is ignored by this processor.
 */
public class P25P1ChannelStatusProcessor implements Listener<Dibit>
{
    private int mSubscriberCount = 0;
    private int mRepeaterCount = 0;
    private Direction mDirection = Direction.OUTBOUND;

    public void receive(Dibit status)
    {
        switch(status)
        {
            case D00_PLUS_1:
                mSubscriberCount++;
                break;
            case D01_PLUS_3:
            case D11_MINUS_3:
                mRepeaterCount++;
                break;
        }

        update();
    }

    private void update()
    {
        //This doesn't appear to be reliable and sometimes it fails on Harris systems.
//        if(mRepeaterCount > mSubscriberCount)
//        {
//            mDirection = Direction.OUTBOUND;
//
//            if(mRepeaterCount == Integer.MAX_VALUE)
//            {
//                mRepeaterCount = 1000;
//                mSubscriberCount = 0;
//            }
//        }
//        else
//        {
//            mDirection = Direction.INBOUND;
//
//            if(mSubscriberCount == Integer.MAX_VALUE)
//            {
//                mSubscriberCount = 1000;
//                mRepeaterCount = 0;
//            }
//        }
    }

    public Direction getDirection()
    {
        return mDirection;
    }
}
