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
package module.log;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import module.log.config.EventLogConfiguration;
import controller.channel.AbstractChannelEditor;
import controller.channel.Channel;
import controller.channel.ConfigurationValidationException;

public class EventLogComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;
    
    private JCheckBox mBinaryLogger = new JCheckBox( "Binary Messages" );
    private JCheckBox mDecodedLogger = new JCheckBox( "Decoded Messages" );
    private JCheckBox mCallEventLogger = new JCheckBox( "Call Events" );

    public EventLogComponentEditor( Channel channel )
	{
		super( channel );
		
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
        	final List<EventLogType> mLoggers = mChannel
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
		EventLogConfiguration config = mChannel.getEventLogConfiguration();

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
