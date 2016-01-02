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
package module.decode.event;

import java.awt.EventQueue;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;
import controller.channel.Channel;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEvent.Event;
import controller.channel.ChannelEventListener;

public class CallEventPanel extends JPanel implements ChannelEventListener
{
    private static final long serialVersionUID = 1L;

    private JScrollPane mEmptyScroller;
    private Channel mDisplayedChannel;
    private CallEventAliasCellRenderer mRenderer;

	public CallEventPanel( SettingsManager settingsManager )
	{
    	setLayout( new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]") );

    	JTable table = new JTable( new CallEventModel() );
    	table.setAutoCreateRowSorter( true );
    	table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
    	
    	mRenderer = new CallEventAliasCellRenderer( settingsManager );
		
		table.getColumnModel().getColumn( CallEventModel.FROM_ALIAS )
				.setCellRenderer( mRenderer );

		table.getColumnModel().getColumn( CallEventModel.TO_ALIAS )
				.setCellRenderer( mRenderer );

    	mEmptyScroller = new JScrollPane( table );

		add( mEmptyScroller );
	}

	@Override
    public void channelChanged( final ChannelEvent event )
    {
		if( event.getEvent() == Event.NOTIFICATION_SELECTION_CHANGE && 
			event.getChannel().isSelected() )
		{
			if( mDisplayedChannel == null || 
				( mDisplayedChannel != null && 
				  mDisplayedChannel != event.getChannel() ) )
			{
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						removeAll();
						
						JTable table = new JTable(
								event.getChannel().getCallEventModel() );

						table.setAutoCreateRowSorter( true );
				    	table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
						
						table.getColumnModel().getColumn( CallEventModel.FROM_ALIAS )
						.setCellRenderer( mRenderer );

						table.getColumnModel().getColumn( CallEventModel.TO_ALIAS )
						.setCellRenderer( mRenderer );
						
						add( new JScrollPane( table ) );
						
						mDisplayedChannel = event.getChannel();
						
						revalidate();
						repaint();
					}
				} );
				
			}
		}
		else if( event.getEvent() == Event.NOTIFICATION_PROCESSING_STOP ||
				 event.getEvent() == Event.REQUEST_DISABLE )
		{
			if( mDisplayedChannel != null && 
				mDisplayedChannel == event.getChannel() )
			{
				mDisplayedChannel = null;
				
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						removeAll();
						add( mEmptyScroller );

						revalidate();
						repaint();
					}
				} );
			}
		}
    }
}
