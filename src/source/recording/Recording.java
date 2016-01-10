package source.recording;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerChannel;
import source.tuner.ITunerChannelProvider;
import source.tuner.TunerChannelSource;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeProcessor;
import controller.ThreadPoolManager;

public class Recording implements Comparable<Recording>,
								  IFrequencyChangeProcessor, 
								  ITunerChannelProvider
{
	private RecordingConfiguration mConfiguration;
	
	private ArrayList<TunerChannelSource> mTunerChannels = 
			new ArrayList<TunerChannelSource>();
	
	private long mCenterFrequency;
	
	private SettingsManager mSettingsManager;
	
	public Recording( SettingsManager settingsManager,
					  RecordingConfiguration configuration )
	{
		mSettingsManager = settingsManager;
		mConfiguration = configuration;
		mCenterFrequency = mConfiguration.getCenterFrequency();
	}
	
	public void setAlias( String alias )
	{
		mConfiguration.setAlias( alias );
		mSettingsManager.save();
	}
	
	public void setRecordingFile( File file ) throws SourceException
	{
		if( hasChannels() )
		{
			throw new SourceException( "Recording source - can't change "
				+ "recording file while channels are enabled against the "
				+ "current recording." );
		}
		
		mConfiguration.setFilePath( file.getAbsolutePath() );
		mSettingsManager.save();
	}
	
	/**
	 * Indicates if this recording is currently playing/providing samples to
	 * tuner channel sources
	 */
	public boolean hasChannels()
	{
		return mTunerChannels.size() > 0;
	}
	
	@Override
    public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
            TunerChannel channel ) throws RejectedExecutionException,
            SourceException
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public void releaseChannel( TunerChannelSource source )
    {
	    // TODO Auto-generated method stub
    }
	
	public RecordingConfiguration getRecordingConfiguration()
	{
		return mConfiguration;
	}
	
	public String toString()
	{
		return mConfiguration.getAlias();
	}

	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		if( event.getEvent() == Event.NOTIFICATION_FREQUENCY_CHANGE )
		{
			long frequency = event.getValue().longValue();
			
			mConfiguration.setCenterFrequency( frequency );
			
			mSettingsManager.save();

			mCenterFrequency = frequency;

			for( TunerChannelSource channel: mTunerChannels )
			{
				channel.frequencyChanged( event );
			}
		}
    }

	@Override
    public int compareTo( Recording other )
    {
	    return getRecordingConfiguration().getAlias()
	    		.compareTo( other.getRecordingConfiguration().getAlias() );
    }
}
