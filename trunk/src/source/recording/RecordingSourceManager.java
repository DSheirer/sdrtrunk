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
package source.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import log.Log;
import settings.SettingsManager;
import source.Source;
import source.SourceException;
import source.config.SourceConfigRecording;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import controller.ResourceManager;
import controller.channel.ProcessingChain;

public class RecordingSourceManager
{
	private ResourceManager mResourceManager;
	
	private ArrayList<Recording> mRecordings = new ArrayList<Recording>();

    public RecordingSourceManager( ResourceManager resourceManager )
	{
    	mResourceManager = resourceManager;
    	
    	loadRecordings();
	}

    /**
     * Iterates current recordings to get a tuner channel source for the frequency
     * specified in the channel config's source config object
     */
    public Source getSource( ProcessingChain processingChain ) 
    					throws SourceException
    {
    	TunerChannelSource retVal = null;

    	if( processingChain.getChannel().getSourceConfiguration()
    						instanceof SourceConfigRecording )
    	{
        	TunerChannel tunerChannel = 
        			processingChain.getChannel().getTunerChannel();

    		SourceConfigRecording config = (SourceConfigRecording)processingChain
    				.getChannel().getSourceConfiguration();

    		Recording recording = getRecordingFromAlias( config.getRecordingAlias() );
    		
    		if( recording != null )
    		{
    			retVal = recording.getChannel( 
    					mResourceManager.getThreadPoolManager(),
    					tunerChannel );
    		}
    		else
    		{
    			throw new SourceException( "Recording with alias name [" + 
					config.getRecordingAlias() + "] is not currently available" );
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
    	Recording recording = new Recording( mResourceManager, config );

    	mRecordings.add( recording );
    	
    	return recording;
    }
    
    public void removeRecording( Recording recording )
    {
    	/* Remove the config from the settings manager */
    	mResourceManager.getSettingsManager().removeRecordingConfiguration( 
    			recording.getRecordingConfiguration() );
    	
    	mRecordings.remove( recording );
    }

    private void loadRecordings()
    {
    	if( mResourceManager != null )
    	{
    		SettingsManager settingsManager = mResourceManager.getSettingsManager();
    		
    		if( settingsManager != null )
    		{
    			ArrayList<RecordingConfiguration> recordingConfigurations = 
    					settingsManager.getRecordingConfigurations();

    			Log.info( "RecordingSourceManager - discovered [" + recordingConfigurations.size() + "] recording configurations" );
    			
    			for( RecordingConfiguration config: recordingConfigurations )
    			{
    				Recording recording = new Recording( mResourceManager, config );
    				
    				mRecordings.add( recording );
    			}
    		}
    	}
    }
}
