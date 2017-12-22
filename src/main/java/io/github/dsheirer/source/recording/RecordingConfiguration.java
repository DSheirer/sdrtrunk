package io.github.dsheirer.source.recording;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.File;

@JacksonXmlRootElement(localName = "recording_configuration")
public class RecordingConfiguration
{
    private File mRecording;
    private String mFilePath;
    private String mAlias;
    private long mCenterFrequency;

    public RecordingConfiguration()
    {
        mAlias = "New Recording";
    }

    public RecordingConfiguration(String filePath, String alias, long centerFrequency)
    {
        mFilePath = filePath;
        mAlias = alias;
        mCenterFrequency = centerFrequency;

        updateFile();
    }

    private void updateFile()
    {
        if(mFilePath != null)
        {
            try
            {
                mRecording = new File(mFilePath);
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("There was an error while "
                    + "accessing the recording source [" + mFilePath + "]", e);
            }
        }
    }

    @JsonIgnore
    public File getRecording()
    {
        return mRecording;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "file_path")
    public String getFilePath()
    {
        return mFilePath;
    }

    public void setFilePath(String filePath)
    {
        mFilePath = filePath;

        updateFile();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "alias")
    public String getAlias()
    {
        return mAlias;
    }

    public void setAlias(String alias)
    {
        mAlias = alias;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "center_frequency")
    public long getCenterFrequency()
    {
        return mCenterFrequency;
    }

    public void setCenterFrequency(long frequency)
    {
        mCenterFrequency = frequency;
    }
}
