/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.alias.action.clip;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.message.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class ClipAction extends RecurringAction
{
    private final static Logger mLog = LoggerFactory.getLogger(ClipAction.class);

    private String mFilePath;

    @JsonIgnore
    private Clip mClip;

    public ClipAction()
    {
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasActionType getType()
    {
        return AliasActionType.CLIP;
    }

    public String getPath()
    {
        return mFilePath;
    }

    public void setPath(String path)
    {
        mFilePath = path;
        updateValueProperty();
    }

    @Override
    public void performAction(Alias alias, IMessage message)
    {
        try
        {
            play();
        }
        catch(Exception e)
        {
            mLog.error("Couldn't play audio clip", e);
        }
    }

    public void play() throws Exception
    {
        try
        {
            if(mFilePath != null)
            {
                if(mClip == null)
                {
                    mClip = AudioSystem.getClip();

                    AudioInputStream ais = AudioSystem.getAudioInputStream(new File(mFilePath));

                    mClip.open(ais);
                }

                if(mClip.isRunning())
                {
                    mClip.stop();
                }

                mClip.setFramePosition(0);

                mClip.start();
            }
        }
        catch(Exception e)
        {
            mClip = null;

            mLog.error("Error playing sound clip [" + mFilePath + "] - " + e.getMessage());

            throw e;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Play Audio Clip");

        if(getInterval() != null)
        {
            switch(getInterval())
            {
                case ONCE:
                    sb.append(" Once");
                    break;
                case DELAYED_RESET:
                    sb.append(" Once, Reset After ").append(getPeriod()).append(" Seconds");
                    break;
                case UNTIL_DISMISSED:
                    sb.append(" Every ").append(getPeriod()).append(" Seconds Until Dismissed");
                    break;
            }
        }

        if(getPath() == null)
        {
            sb.append(" - (audio file empty)");
        }

        return sb.toString();
    }
}
