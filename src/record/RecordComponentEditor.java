/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package record;


import javax.swing.JCheckBox;

import record.config.RecordConfiguration;
import controller.channel.AbstractChannelEditor;
import controller.channel.Channel;
import controller.channel.ConfigurationValidationException;

public class RecordComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;
    
    private JCheckBox mAudioRecorder = new JCheckBox( "Audio" );
    private JCheckBox mBasebandRecorder = new JCheckBox( "Baseband I/Q" );
    private JCheckBox mTrafficBasebandRecorder = new JCheckBox( "Traffic Channel Baseband I/Q" );

    public RecordComponentEditor( Channel channel )
	{
		super( channel );
		
		add( mAudioRecorder, "span" );
		add( mBasebandRecorder, "span" );
		add( mTrafficBasebandRecorder, "span" );
    	
		reset();
	}

	@Override
    public void save()
    {
		RecordConfiguration config = getChannel().getRecordConfiguration();
		
		config.clearRecorders();
		
		if( mAudioRecorder.isSelected() )
		{
			config.addRecorder( RecorderType.AUDIO );
		}
		
		if( mBasebandRecorder.isSelected() )
		{
			config.addRecorder( RecorderType.BASEBAND );
		}
		
		if( mTrafficBasebandRecorder.isSelected() )
		{
			config.addRecorder( RecorderType.TRAFFIC_BASEBAND );
		}
		
    }

	@Override
    public void reset() 
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
        		RecordConfiguration config = getChannel().getRecordConfiguration();

    			mAudioRecorder.setSelected( config.getRecorders()
        					.contains( RecorderType.AUDIO ) );

    			mBasebandRecorder.setSelected( config.getRecorders()
    					.contains( RecorderType.BASEBAND ) );
    			
    			mTrafficBasebandRecorder.setSelected( config.getRecorders()
    					.contains( RecorderType.TRAFFIC_BASEBAND ) );
            }
        });
    }

	@Override
	public void setConfiguration( Channel channel )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
		// TODO Auto-generated method stub
		
	}
}
