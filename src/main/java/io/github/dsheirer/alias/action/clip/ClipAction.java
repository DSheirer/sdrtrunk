package io.github.dsheirer.alias.action.clip;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.message.Message;
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
    }

    @Override
    public void performAction(Alias alias, Message message)
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

                    AudioInputStream ais =
                        AudioSystem.getAudioInputStream(new File(mFilePath));

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
        if(mFilePath == null)
        {
            return "Play Clip";
        }
        else
        {
            return "Play Clip: " + mFilePath;
        }
    }
}
