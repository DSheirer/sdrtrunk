package io.github.dsheirer.controller.channel;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.event.ActivitySummaryFrame;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChannelUtils
{
	/**
	 * Creates a context menu for the channel argument
	 */
	public static JMenu getContextMenu( final ChannelModel channelModel,
			final ChannelProcessingManager channelProcessingManager,
			final Channel channel,
			final Component anchor )
	{
		if( channel != null )
		{
			JMenu menu = new JMenu( "Channel: " + channel.getName() );
			
			if( channel.isProcessing() )
			{
				JMenuItem disable = new JMenuItem( "Disable" );
				disable.addActionListener( new ActionListener() 
				{
					@Override
	                public void actionPerformed( ActionEvent e )
	                {
						channelModel.broadcast( 
								new ChannelEvent( channel, ChannelEvent.Event.REQUEST_DISABLE ) );
	                }
				} );
				
				menu.add( disable );
				
				menu.add( new JSeparator() );

				JMenuItem actySummaryItem = 
						new JMenuItem( "Activity Summary" );

				actySummaryItem.addActionListener( new ActionListener() 
				{
					@Override
		            public void actionPerformed( ActionEvent e )
		            {
						StringBuilder sb = new StringBuilder();
						
						ProcessingChain chain = channelProcessingManager.getProcessingChain( channel );
						
						if( chain != null )
						{
							for( DecoderState decoderState: chain.getDecoderStates() )
							{
								sb.append( decoderState.getActivitySummary() );
							}
						}
						
						new ActivitySummaryFrame( sb.toString(), anchor );
		            }
				} );
					
				menu.add( actySummaryItem );
			}
			else
			{
				JMenuItem enable = new JMenuItem( "Enable" );
				enable.addActionListener( new ActionListener() 
				{
					@Override
	                public void actionPerformed( ActionEvent e )
	                {
						channelModel.broadcast( 
							new ChannelEvent( channel, ChannelEvent.Event.REQUEST_ENABLE ) );
	    			}	
				} );
				
				menu.add( enable );
			}
			
			menu.add( new JSeparator() );
			
			JMenuItem deleteItem = new JMenuItem( "Delete" );
			deleteItem.addActionListener( new ActionListener() 
			{
				@Override
	            public void actionPerformed( ActionEvent e )
	            {
					int response = JOptionPane.showConfirmDialog( anchor, 
						"Do you want to delete channel " + channel.getName() + "?",
						"Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION );
					
					if( response == JOptionPane.YES_OPTION )
					{
						channelModel.broadcast( 
							new ChannelEvent( channel, ChannelEvent.Event.REQUEST_DELETE ) );
					}
				}
			} );
			
			menu.add( deleteItem );
			
			return menu;
			
		}
		
		return null;
	}
	
}
