/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.message.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple message carrier class that persists activity model column values so that they do not need
 * to be recalculated with each row update.
 */
public class MessageItem
{
    private final static Logger mLog = LoggerFactory.getLogger(MessageItem.class);
    private IMessage mMessage;
    private String mTimestamp;
    private String mProtocol;
    private String mText;

    public MessageItem(IMessage message)
    {
        mMessage = message;
    }

    public IMessage getMessage()
    {
        return mMessage;
    }

    public void dispose()
    {
        mMessage = null;
        mTimestamp = null;
        mProtocol = null;
        mText = null;
    }

    public String getTimestamp(SimpleDateFormat simpleDateFormat)
    {
        if(mTimestamp == null)
        {
            mTimestamp = simpleDateFormat.format(new Date(getMessage().getTimestamp()));
        }

        return mTimestamp;
    }

    public int getTimeslot()
    {
        return mMessage.getTimeslot();
    }

    public String getText()
    {
        if(mText == null)
        {
            try
            {
                mText = getMessage().toString();
            }
            catch(Throwable t)
            {
                mLog.error("Error accessing message toString() method - protocol [" + getProtocol() + "]", t);
                mText = "MESSAGE ITEM ENCOUNTERED PARSING ERROR";
            }
        }

        return mText;
    }

    public String getProtocol()
    {
        if(mProtocol == null)
        {
            mProtocol = getMessage().getProtocol().toString();
        }

        return mProtocol;
    }
}
