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
package io.github.dsheirer.record;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.record.config.RecordConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class RecordConfigurationEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;
    
    private JCheckBox mAudioRecorder;
    private JCheckBox mBasebandRecorder;
    private JCheckBox mTrafficBasebandRecorder;

    public RecordConfigurationEditor()
	{
		
		init();
	}
    
    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 4", "", "[][grow]" ) );
		
		mAudioRecorder = new JCheckBox( "Audio" );
		mAudioRecorder.setEnabled( false );
		mAudioRecorder.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
	    add( mAudioRecorder );

	    mBasebandRecorder = new JCheckBox( "Baseband I/Q" );
	    mBasebandRecorder.setEnabled( false );
	    mBasebandRecorder.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mBasebandRecorder );

		mTrafficBasebandRecorder = new JCheckBox( "Traffic Channel Baseband I/Q" );
		mTrafficBasebandRecorder.setEnabled( false );
		mTrafficBasebandRecorder.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mTrafficBasebandRecorder );
    }
    
    private void setControlsEnabled( boolean enabled )
    {
    	if( mAudioRecorder.isEnabled() != enabled )
    	{
    		mAudioRecorder.setEnabled( enabled );
    	}
    	
    	if( mBasebandRecorder.isEnabled() != enabled )
    	{
    	    mBasebandRecorder.setEnabled( enabled );
    	}
    	
    	if( mTrafficBasebandRecorder.isEnabled() != enabled )
    	{
    		mTrafficBasebandRecorder.setEnabled( enabled );
    	}
    }

	@Override
    public void save()
    {
		if( hasItem() && isModified() )
		{
			RecordConfiguration config = getItem().getRecordConfiguration();
			
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
		
		setModified( false );
    }
	
	@Override
	public void setItem( Channel channel )
	{
		super.setItem( channel );
		
		if( hasItem() )
		{
			setControlsEnabled( true );
			
			List<RecorderType> recorders = getItem().getRecordConfiguration().getRecorders();
			mAudioRecorder.setSelected( recorders.contains( RecorderType.AUDIO ) );
			mBasebandRecorder.setSelected( recorders.contains( RecorderType.BASEBAND ) );
			mTrafficBasebandRecorder.setSelected( recorders.contains( RecorderType.TRAFFIC_BASEBAND ) );
		}
		else
		{
			setControlsEnabled( false );
		}
	}
}
