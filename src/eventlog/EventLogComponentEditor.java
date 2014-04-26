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
package eventlog;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import controller.channel.AbstractChannelEditor;
import controller.channel.ChannelNode;
import eventlog.config.EventLogConfiguration;

public class EventLogComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;
    
    private JCheckBox mBinaryLogger = new JCheckBox( "Binary Messages" );
    private JCheckBox mDecodedLogger = new JCheckBox( "Decoded Messages" );
    private JCheckBox mCallEventLogger = new JCheckBox( "Call Events" );

    public EventLogComponentEditor( ChannelNode channelNode )
	{
		super( channelNode );
		
		add( mBinaryLogger, "span" );
		add( mDecodedLogger, "span" );
		add( mCallEventLogger, "span" );
    	
		reset();
	}

    /**
     * Sets the combo box to the type stored in the config object, causing the
     * editor panel to reset with the stored config
     */
    public void reset() 
    {
        SwingUtilities.invokeLater(new Runnable() 
        {
        	final ArrayList<EventLogType> mLoggers = mChannelNode.getChannel()
        			.getEventLogConfiguration().getLoggers();
        	
            @Override
            public void run() 
            {
        		mBinaryLogger.setSelected(
        				mLoggers.contains( EventLogType.BINARY_MESSAGE ) );
        		mDecodedLogger.setSelected(
        				mLoggers.contains( EventLogType.DECODED_MESSAGE ) );
        		mCallEventLogger.setSelected(
        				mLoggers.contains( EventLogType.CALL_EVENT ) );
            }
        });
    }

    /**
     * Saves the current configuration as the stored configuration
     */
	@Override
    public void save()
    {
		EventLogConfiguration config = mChannelNode.getChannel()
				.getEventLogConfiguration();

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
}
