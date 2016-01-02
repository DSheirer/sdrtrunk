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
package controller.channel;

import java.awt.Color;
import java.text.DecimalFormat;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.TunerChannel;
import controller.ConfigurableNode;
import controller.site.SiteNode;
import controller.system.SystemNode;

/**
 * A channel has all of the pieces needed to wire together a source, decoder,
 * event logger and recorder and be started and stopped.
 */
public class ChannelNode extends ConfigurableNode implements ChannelEventListener
{
    private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( ChannelNode.class );
    
	private static DecimalFormat sFORMAT = new DecimalFormat( "0.0000" );

	public ChannelNode( Channel channel )
	{
		super( channel );
	}
	
	public void init()
	{
		/* Set the owning system and site names for the channel */
		SiteNode siteNode = (SiteNode)getParent();
		
		if( siteNode != null )
		{
			getChannel().setSite( siteNode.getSite().getName() );
			
			SystemNode systemNode = (SystemNode)siteNode.getParent();
			
			if( systemNode != null )
			{
				getChannel().setSystem( systemNode.getSystem().getName() );
			}
		}
		
		/* Add this node as listener to receive changes from underlying channel */
//		getChannel().addListener( this );

		/* Add the resource manager to the channel so that the channel
		 * can provide channel change events to all system resources */
	    getChannel().setResourceManager( getModel().getResourceManager() );
	}

	/**
	 * Listener method to receive channel changes from the underlying chnannel
	 */
	@Override
    public void channelChanged( ChannelEvent event )
    {
		switch( event.getEvent() )
		{
			/* Refresh the node for each of these events */
			case NOTIFICATION_SELECTION_CHANGE:
			case NOTIFICATION_PROCESSING_START:
			case NOTIFICATION_PROCESSING_STOP:
				this.refresh();
				break;
			/* We're being deleted, so cleanup */
			case NOTIFICATION_DELETE:
				delete();
				break;
			default:
				break;
		}
    }

	@Override
    public String getIconPath()
    {
		return getChannel().getDecodeConfiguration().getDecoderType()
				.getIconFilename();
    }

	@Override
	public Color getBackgroundColor()
	{
		if( getChannel().getEnabled() )
		{
			if( getChannel().isProcessing() )
			{
				return Color.GREEN;
			}
			else
			{
				return Color.RED;
			}
		}
		
		return null;
	}
	
    @Override
    public JPanel getEditor()
    {
        return new ChannelEditor( this, 
    		getChannel().getResourceManager() == null ? null : 
    			getChannel().getResourceManager().getPlaylistManager() );
    }
    
	public Channel getChannel()
	{
		return (Channel)this.getUserObject();
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getChannel().getName() );
		
		TunerChannel channel = getChannel().getTunerChannel();
	
		if( channel != null )
		{
			sb.append( " (" );
			sb.append( sFORMAT.format( (double)channel.getFrequency() / 1000000.0d ) + " MHz" );
			sb.append( ")" );

		}
		
		return sb.toString();
	}
	
	public JPopupMenu getContextMenu()
	{
		JPopupMenu popupMenu = new JPopupMenu();

		JMenu channelMenu = ChannelUtils.getContextMenu( 
			getModel().getResourceManager().getPlaylistManager(), 
				getChannel(), getModel().getTree() );

		if( channelMenu != null )
		{
			popupMenu.add( channelMenu );
		}
		
		return popupMenu;
	}
	
	public void delete()
	{
		((SiteNode)getParent()).getSite().removeChannel( getChannel() );
		
		save();
		
		getModel().removeNodeFromParent( ChannelNode.this );
	}
}
