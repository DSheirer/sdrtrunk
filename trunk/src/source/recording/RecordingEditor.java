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

import gui.control.JFrequencyControl;

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import source.SourceEditor;
import source.config.SourceConfigRecording;
import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;
import controller.ResourceManager;

public class RecordingEditor extends SourceEditor
{
    private static final long serialVersionUID = 1L;
    private JComboBox<Recording> mComboRecordings;
    private JFrequencyControl mFrequencyControl;
    
	public RecordingEditor( ResourceManager resourceManager, 
							SourceConfiguration config )
	{
		super( resourceManager, config );
		
		initGUI();
	}

	public void reset()
	{
		mFrequencyControl.setFrequency( 
				((SourceConfigTuner)mConfig).getFrequency(), false );
	}
	
	public void save()
	{
		SourceConfigRecording config = (SourceConfigRecording)mConfig;
		
		Recording selected = (Recording)mComboRecordings.getSelectedItem();
		
		if( selected != null )
		{
			config.setRecordingAlias( selected
					.getRecordingConfiguration().getAlias() );
		}
		
		config.setFrequency( mFrequencyControl.getFrequency() );
	}
	
	private void initGUI()
	{
		add( new JLabel( "Recording" ) );
		
		mComboRecordings = new JComboBox<Recording>();

		add( mComboRecordings, "wrap" );
		
		mFrequencyControl = new JFrequencyControl();
		
		mFrequencyControl.setFrequency( 
				((SourceConfigRecording)mConfig).getFrequency(), false );
		
		add( mFrequencyControl, "span" );
		
		resetRecordings();
	}
	
	private void resetRecordings()
	{
		SwingUtilities.invokeLater( new Runnable() 
        {
            @Override
            public void run() 
            {
            	List<Recording> recordings = getResourceManager()
            			.getRecordingSourceManager().getRecordings();

        		mComboRecordings.setModel( 
    				new DefaultComboBoxModel<Recording>( 
					recordings.toArray( new Recording[ recordings.size()] ) ) );
        		
        		SourceConfigRecording config = (SourceConfigRecording)mConfig;

        		String recordingAlias = config.getRecordingAlias();
        		
        		if( recordingAlias != null )
        		{
        			Recording selected = mResourceManager
    					.getRecordingSourceManager()
    					.getRecordingFromAlias( recordingAlias );

        			if( selected != null )
        			{
        				mComboRecordings.setSelectedItem( selected );
        			}
        		}
            }
        });
	}
}
