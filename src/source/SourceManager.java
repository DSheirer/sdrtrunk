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
package source;

import settings.SettingsManager;
import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;
import source.mixer.MixerEditor;
import source.mixer.MixerManager;
import source.recording.RecordingEditor;
import source.recording.RecordingSourceManager;
import source.tuner.TunerEditorOld;
import source.tuner.TunerManager;
import source.tuner.TunerModel;
import controller.ThreadPoolManager;

public class SourceManager 
{
	private MixerManager mMixerManager;
	private RecordingSourceManager mRecordingSourceManager;
	private TunerManager mTunerManager;
	private TunerModel mTunerModel;

	public SourceManager( TunerModel tunerModel,
						  SettingsManager settingsManager, 
						  ThreadPoolManager threadPoolManager )
	{
		mTunerModel = tunerModel;
		mMixerManager = new MixerManager();
		mRecordingSourceManager = new RecordingSourceManager( settingsManager );
		mTunerManager = new TunerManager( mMixerManager, tunerModel, threadPoolManager );
		
		//TODO: change mixer & recording managers to be models and hand them
		//in via the constructor.  Perform loading outside of this class.
	}
	
	public MixerManager getMixerManager()
	{
		return mMixerManager;
	}

	public RecordingSourceManager getRecordingSourceManager()
	{
		return mRecordingSourceManager;
	}
	
	public TunerManager getTunerManager()
	{
		return mTunerManager;
	}
	
	public Source getSource( SourceConfiguration config, int bandwidth ) 
							throws SourceException
	{
		Source retVal = null;

		switch( config.getSourceType() )
		{
			case MIXER:
				retVal = mMixerManager.getSource( config ); 
				break;
			case TUNER:
				retVal = mTunerModel.getSource( (SourceConfigTuner)config, bandwidth );
				break;
			case RECORDING:
				retVal = mRecordingSourceManager.getSource( config, bandwidth );
			case NONE:
			default:
				break;
		}
		
		return retVal;
	}
	
	public SourceEditor getPanel( SourceConfiguration config )
	{
		SourceEditor configuredPanel;
		
		switch( config.getSourceType() )
		{
			case MIXER:
				configuredPanel = new MixerEditor( this, config );
				break;
			case TUNER:
				configuredPanel = new TunerEditorOld( this, config );
				break;
			case RECORDING:
				configuredPanel = new RecordingEditor( this, config );
				break;
			case NONE:
			default:
				configuredPanel = new EmptySourceEditor( this, config );
				break;
		}
		
		return configuredPanel;
	}
}
