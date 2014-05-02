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

import java.text.DecimalFormat;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import log.Log;
import source.tuner.TunerChannel;
import controller.ConfigurableNode;
import controller.channel.Channel.ChannelEvent;
import controller.site.SiteNode;
import controller.system.SystemNode;

/**
 * A channel has all of the pieces needed to wire together a source, decoder,
 * event logger and recorder and be started and stopped.
 */
public class ChannelNode extends ConfigurableNode implements ChannelListener
{
    private static final long serialVersionUID = 1L;
    
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
			getChannel().setSite( siteNode.getSite(), false );
			
			SystemNode systemNode = (SystemNode)siteNode.getParent();
			
			if( systemNode != null )
			{
				getChannel().setSystem( systemNode.getSystem(),  false );
			}
		}
		
		/* Add this node as listener to receive changes from underlying channel */
		getChannel().addListener( this );

		/* Add the resource manager to the channel so that the channel
		 * can provide channel change events to all system resources */
	    getChannel().setResourceManager( getModel().getResourceManager() );
	}

	/**
	 * Listener method to receive channel changes from the underlying chnannel
	 */
	@Override
    public void occurred( Channel channel, ChannelEvent event )
    {
		switch( event )
		{
			/* Refresh the node for each of these events */
			case CHANGE_DECODER:
			case CHANGE_ENABLED:
			case CHANGE_NAME:
			case CHANGE_SELECTED:
			case CHANGE_SITE:
			case CHANGE_SYSTEM:
			case PROCESSING_STARTED:
			case PROCESSING_STOPPED:
				this.refresh();
				break;
			/* We're being deleted, so cleanup */
			case CHANNEL_DELETED:
				delete();
				break;
			default:
				break;
		}
    }

	@Override
    public String getIconPath()
    {
		StringBuilder sb = new StringBuilder();
		
		Channel channel = getChannel();
		
		sb.append( channel.getDecodeConfiguration().getDecoderType().getIconPrefix() );
		
		if( channel.getEnabled() )
		{
			sb.append( channel.isProcessing() ? "_green" : "_red" );
		}
		
		sb.append( ".png" );
		
    	return sb.toString();
    }
	
    @Override
    public JPanel getEditor()
    {
        return new ChannelEditor( this );
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
		JPopupMenu popupMenu = new JPopupMenu( "Channel Menu" );
		popupMenu.add( getChannel().getContextMenu() );
		
		return popupMenu;
	}
	
	public void delete()
	{
		((SiteNode)getParent()).getSite().removeChannel( getChannel() );
		
		save();
		
		getModel().removeNodeFromParent( ChannelNode.this );
	}
}
