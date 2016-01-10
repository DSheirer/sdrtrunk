package module.decode.state;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import settings.SettingsManager;
import controller.channel.Channel;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;

/**
 * Channel Collection assembles a primary channel panel and any related decoder
 * state panels, provides contextual access to the channel and supports channel 
 * selection.
 */
public class ChannelCollectionPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private final static Logger mLog = LoggerFactory.getLogger( ChannelCollectionPanel.class );

	private ChannelModel mChannelModel;
	private ChannelProcessingManager mChannelProcessingManager;
	private PlaylistManager mPlaylistManager;
	private SettingsManager mSettingsManager;
	
	private List<ChannelStatePanel> mChannelPanels = new ArrayList<ChannelStatePanel>();
	
	public ChannelCollectionPanel( ChannelModel channelModel,
								   ChannelProcessingManager channelProcessingManager,
								   PlaylistManager playlistManager,
								   SettingsManager settingsManager, 
								   Channel channel )
	{
		mChannelModel = channelModel;
		mChannelProcessingManager = channelProcessingManager;
		mPlaylistManager = playlistManager;
		mSettingsManager = settingsManager;
		
		init( channel );
	}
	
	protected void init( Channel channel )
	{
    	setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[]0[]0[]") );
    	
    	setOpaque( false );

    	ChannelStatePanel csp = new ChannelStatePanel( mChannelModel, 
			mChannelProcessingManager, mPlaylistManager, mSettingsManager, channel );
    	
    	mChannelPanels.add( csp );
    	
    	add( csp, "span,grow" );
    	
	}	

	public Channel getChannel()
	{
		if( mChannelPanels.size() >= 1 )
		{
			return mChannelPanels.get( 0 ).getChannel();
		}
		
		return null;
	}
	
	public void dispose()
	{
		for( ChannelStatePanel panel: mChannelPanels )
		{
			panel.dispose();
		}
		
		mChannelPanels.clear();
		
		mPlaylistManager = null;
		mSettingsManager = null;
	}

	/**
	 * Toggles the selection state of the channel if it matches the channel 
	 * argument.  Otherwise, sets the selection state to false.
	 */
	public void setSelectedChannel( Channel channel )
	{
		for( ChannelStatePanel panel: mChannelPanels )
		{
			if( panel.getChannel() == channel )
			{
				/* Toggle the selection state */
				panel.getChannel().setSelected( !panel.getChannel().isSelected() );
				panel.repaint();
			}
			else
			{
				if( panel.getChannel().isSelected() )
				{
					panel.getChannel().setSelected( false );
					panel.repaint();
				}
			}
		}
	}

	/**
	 * Returns the channel represented by the channel panel at the gui point
	 * location, to selecting the channel
	 * 
	 * @param point - current location of the mouse
	 * 
	 * @return - channel or null
	 */
	public Channel getChannelAt( Point point )
	{
		Component c = this.getComponentAt( point );

		if( c instanceof ChannelCollectionPanel )
		{
			return ((ChannelCollectionPanel)c).getChannel();
		}
		else
		{
			return getChannel();
		}
	}
	
	protected SettingsManager getSettingsManager()
	{
		return mSettingsManager;
	}
	
	
	public JMenu getContextMenu()
	{
		if( !mChannelPanels.isEmpty() )
		{
			return mChannelPanels.get( 0 ).getContextMenu();
		}
		
		return null;
	}
}
