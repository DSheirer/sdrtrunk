package playlist;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceManager;

import com.jidesoft.swing.JideSplitPane;

import controller.channel.Channel;
import controller.channel.ChannelEditor;
import controller.channel.ChannelModel;

public class PlaylistEditor extends JPanel implements ListSelectionListener
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( PlaylistEditor.class );

	private ChannelModel mChannelModel;
	private JTable mChannelTable;
	private ChannelEditor mEditor;

	public PlaylistEditor( ChannelModel channelModel, 
						   PlaylistManager playlistManager, 
						   SourceManager sourceManager )
	{
		mChannelModel = channelModel;

    	mEditor = new ChannelEditor( channelModel, playlistManager, sourceManager );

    	init();
	}
	
	private void init()
	{
    	setLayout( new MigLayout( "insets 0 0 0 0", 
								  "[grow,fill]", 
								  "[grow,fill]") );

		//System Configuration View and Editor
    	mChannelTable = new JTable( mChannelModel );
    	mChannelTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    	mChannelTable.getSelectionModel().addListSelectionListener( this );
    	mChannelTable.setAutoCreateRowSorter( true );
    	
    	TableFilterHeader channelHeaderFilter = 
    			new TableFilterHeader( mChannelTable, AutoChoices.ENABLED );
    	channelHeaderFilter.setFilterOnUpdates( true );
    	
    	JScrollPane channelScroller = new JScrollPane( mChannelTable );
		
		JideSplitPane splitPane = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
		splitPane.setDividerSize( 5 );
		splitPane.setShowGripper( true );
		splitPane.add( channelScroller );
		splitPane.add( mEditor );

		add( splitPane );
	}

	@Override
	public void valueChanged( ListSelectionEvent event )
	{
		//This limits event firing to only when selection is complete 
		if( !event.getValueIsAdjusting() )
		{
			int selectedRow = mChannelTable.getSelectedRow();
			
			if( selectedRow != -1 )
			{
				int index = mChannelTable.convertRowIndexToModel( mChannelTable.getSelectedRow() );
				
				Channel channel = mChannelModel.getChannels().get( index );
				
				if( channel != null )
				{
					mEditor.setChannel( channel );
				}
			}
			else
			{
				mEditor.setChannel( null );
			}
		}
	}
}
