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

import java.util.List;

import javax.swing.JCheckBox;

import record.config.RecordConfiguration;
import controller.channel.Channel;
import controller.channel.ChannelConfigurationEditor;
import controller.channel.ConfigurationValidationException;

public class RecordComponentEditor extends ChannelConfigurationEditor
{
    private static final long serialVersionUID = 1L;
    
    private JCheckBox mAudioRecorder = new JCheckBox( "Audio" );
    private JCheckBox mBasebandRecorder = new JCheckBox( "Baseband I/Q" );
    private JCheckBox mTrafficBasebandRecorder = new JCheckBox( "Traffic Channel Baseband I/Q" );

    private Channel mChannel;
    
    public RecordComponentEditor()
	{
		add( mAudioRecorder, "span" );
		add( mBasebandRecorder, "span" );
		add( mTrafficBasebandRecorder, "span" );
	}

	@Override
    public void save()
    {
		if( mChannel != null )
		{
			RecordConfiguration config = mChannel.getRecordConfiguration();
			
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
    }
	@Override
	public void setConfiguration( Channel channel )
	{
		mChannel = channel;
		
		if( mChannel != null )
		{
			List<RecorderType> recorders = mChannel.getRecordConfiguration().getRecorders();

			mAudioRecorder.setSelected( recorders.contains( RecorderType.AUDIO ) );
			mBasebandRecorder.setSelected( recorders.contains( RecorderType.BASEBAND ) );
			mTrafficBasebandRecorder.setSelected( recorders.contains( RecorderType.TRAFFIC_BASEBAND ) );
		}
		else
		{
			mAudioRecorder.setSelected( false );
			mBasebandRecorder.setSelected( false );
			mTrafficBasebandRecorder.setSelected( false );
		}
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
	}
}
