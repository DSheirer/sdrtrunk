package source.recording;

import java.io.File;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

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
	
	public RecordingConfiguration( String filePath, 
								   String alias,
								   long centerFrequency )
	{
		mFilePath = filePath;
		mAlias = alias;
		mCenterFrequency = centerFrequency;

		updateFile();
	}
	
	private void updateFile()
	{
		if( mFilePath != null )
		{
			try
			{
				mRecording = new File( mFilePath );
			}
			catch( Exception e )
			{
				throw new IllegalArgumentException( "There was an error while "
					+ "accessing the recording source [" + mFilePath + "]", e );
			}
		}
	}

	@XmlTransient
	public File getRecording()
	{
		return mRecording;
	}

	@XmlAttribute( name = "file_path" )
	public String getFilePath()
	{
		return mFilePath;
	}
	
	public void setFilePath( String filePath )
	{
		mFilePath = filePath;

		updateFile();
	}
	
	@XmlAttribute( name = "alias" )
	public String getAlias()
	{
		return mAlias;
	}
	
	public void setAlias( String alias )
	{
		mAlias = alias;
	}

	@XmlAttribute( name = "center_frequency" )
	public long getCenterFrequency()
	{
		return mCenterFrequency;
	}
	
	public void setCenterFrequency( long frequency )
	{
		mCenterFrequency = frequency;
	}
}
