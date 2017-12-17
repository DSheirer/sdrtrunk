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
package io.github.dsheirer.module.log;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class EventLogConfigurationEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;
    
    private JCheckBox mBinaryLogger;
    private JCheckBox mDecodedLogger;
    private JCheckBox mCallEventLogger;

    public EventLogConfigurationEditor()
	{
    	init();
	}
    
    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 4", "", "[][grow]" ) );

		mBinaryLogger = new JCheckBox( "Binary Messages" );
		mBinaryLogger.setEnabled( false );
		mBinaryLogger.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mBinaryLogger );
		
	    mDecodedLogger = new JCheckBox( "Decoded Messages" );
	    mDecodedLogger.setEnabled( false );
	    mDecodedLogger.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mDecodedLogger );
	    
	    mCallEventLogger = new JCheckBox( "Call Events" );
	    mCallEventLogger.setEnabled( false );
	    mCallEventLogger.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mCallEventLogger );
    }

    /**
     * Saves the current configuration as the stored configuration
     */
	@Override
    public void save()
    {
		if( hasItem() )
		{
			EventLogConfiguration config = getItem().getEventLogConfiguration();

			config.clear();
			
			if( mBinaryLogger.isSelected() )
			{
				config.addLogger( EventLogType.BINARY_MESSAGE );
			}
			if( mDecodedLogger.isSelected() )
			{
				config.addLogger( EventLogType.DECODED_MESSAGE );
			}
			if( mCallEventLogger.isSelected() )
			{
				config.addLogger( EventLogType.CALL_EVENT );
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

			List<EventLogType> loggers = getItem().getEventLogConfiguration().getLoggers();
    		mBinaryLogger.setSelected( loggers.contains( EventLogType.BINARY_MESSAGE ) );
    		mDecodedLogger.setSelected(	loggers.contains( EventLogType.DECODED_MESSAGE ) );
    		mCallEventLogger.setSelected( loggers.contains( EventLogType.CALL_EVENT ) );
		}
		else
		{
			setControlsEnabled( false );
		}
	}
	
	private void setControlsEnabled( boolean enabled )
	{
		if( mBinaryLogger.isEnabled() != enabled )
		{
			mBinaryLogger.setEnabled( enabled );
		}
		
		if( mDecodedLogger.isEnabled() != enabled )
		{
			mDecodedLogger.setEnabled( enabled );
		}
		
		if( mCallEventLogger.isEnabled() != enabled )
		{
			mCallEventLogger.setEnabled( enabled );
		}
	}
}
