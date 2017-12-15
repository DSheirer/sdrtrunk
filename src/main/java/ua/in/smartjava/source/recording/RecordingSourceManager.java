/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package ua.in.smartjava.source.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import ua.in.smartjava.source.Source;
import ua.in.smartjava.source.SourceException;
import ua.in.smartjava.source.config.SourceConfigRecording;
import ua.in.smartjava.source.config.SourceConfiguration;
import ua.in.smartjava.source.tuner.TunerChannel;
import ua.in.smartjava.source.tuner.TunerChannelSource;

public class RecordingSourceManager
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RecordingSourceManager.class );

	private ArrayList<Recording> mRecordings = new ArrayList<Recording>();

	private SettingsManager mSettingsManager;

    public RecordingSourceManager( SettingsManager settingsManager )
	{
    	mSettingsManager = settingsManager;
    	loadRecordings();
	}

    /**
     * Iterates current recordings to get a tuner ua.in.smartjava.channel ua.in.smartjava.source for the frequency
     * specified in the ua.in.smartjava.channel config's ua.in.smartjava.source config object
     */
    public Source getSource( SourceConfiguration config, int bandwidth ) 
    					throws SourceException
    {
    	TunerChannelSource retVal = null;

    	if( config instanceof SourceConfigRecording )
    	{
    		SourceConfigRecording configRecording = (SourceConfigRecording)config;
    		
        	TunerChannel tunerChannel = configRecording.getTunerChannel();
        	
        	tunerChannel.setBandwidth( bandwidth );

    		Recording recording = getRecordingFromAlias( 
    				configRecording.getRecordingAlias() );
    		
    		if( recording != null )
    		{
    			retVal = recording.getChannel( tunerChannel );
    		}
    		else
    		{
    			throw new SourceException( "Recording with ua.in.smartjava.alias name [" +
					configRecording.getRecordingAlias() + "] is not currently available" );
    		}
    	}
    	
    	return retVal;
    }
    
    public Recording getRecordingFromAlias( String alias )
    {
    	for( Recording recording: mRecordings )
    	{
    		if( recording.getRecordingConfiguration()
    				.getAlias().contentEquals( alias ) )
    		{
    			return recording;
    		}
    	}
    	
    	return null;
    }

    /**
     * Get list of loaded IQ files
     */
    public List<Recording> getRecordings()
    {
    	Collections.sort( mRecordings );
    	
    	return mRecordings;
    }
    
    public void addRecording( Recording recording )
    {
    	mRecordings.add( recording );
    }
    
    public Recording addRecording( RecordingConfiguration config )
    {
    	Recording recording = new Recording( mSettingsManager, config );

    	mRecordings.add( recording );
    	
    	return recording;
    }
    
    public void removeRecording( Recording recording )
    {
    	/* Remove the config from the settings manager */
    	mSettingsManager.removeRecordingConfiguration( 
    			recording.getRecordingConfiguration() );
    	
    	mRecordings.remove( recording );
    }

    private void loadRecordings()
    {
		List<RecordingConfiguration> recordingConfigurations = 
				mSettingsManager.getRecordingConfigurations();

		mLog.info( "RecordingSourceManager - discovered [" + 
		recordingConfigurations.size() + "] recording configurations" );
		
		for( RecordingConfiguration config: recordingConfigurations )
		{
			Recording recording = new Recording( mSettingsManager, config );
			
			mRecordings.add( recording );
		}
	}
}
