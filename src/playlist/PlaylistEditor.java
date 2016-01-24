package playlist;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
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

public class PlaylistEditor extends JPanel 
				implements ActionListener, ListSelectionListener
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( PlaylistEditor.class );

	private ChannelModel mChannelModel;
	private JTable mChannelTable;
	private TableFilterHeader mTableFilterHeader;
	private ChannelEditor mEditor;
	
	private static final String NEW_CHANNEL = "New";
	private static final String COPY_CHANNEL = "Copy";
	private static final String DELETE_CHANNEL = "Delete";
	
	private JButton mNewChannelButton = new JButton( NEW_CHANNEL );
	private JButton mCopyChannelButton = new JButton( COPY_CHANNEL );
	private JButton mDeleteChannelButton = new JButton( DELETE_CHANNEL );

	public PlaylistEditor( ChannelModel channelModel, 
						   PlaylistManager playlistManager, 
						   SourceManager sourceManager )
	{
		mChannelModel = channelModel;

    	mEditor = new ChannelEditor( channelModel, playlistManager, sourceManager );
    	mChannelModel.addListener( mEditor );

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
    	
    	mTableFilterHeader = new TableFilterHeader( mChannelTable, AutoChoices.ENABLED );
    	mTableFilterHeader.setFilterOnUpdates( true );
    	
    	JScrollPane channelScroller = new JScrollPane( mChannelTable );

    	JPanel buttonsPanel = new JPanel();
    	
    	buttonsPanel.setLayout( 
			new MigLayout( "insets 0 0 0 0", "[grow,fill][grow,fill][grow,fill]", "[]") );

    	mNewChannelButton.addActionListener( this );
    	mNewChannelButton.setToolTipText( "Adds a new default channel" );
    	buttonsPanel.add( mNewChannelButton );
    	
    	mCopyChannelButton.addActionListener( this );
    	mCopyChannelButton.setEnabled( false );
    	mCopyChannelButton.setToolTipText( "Creates a copy of the currently selected channel and adds it" );
    	buttonsPanel.add( mCopyChannelButton );

    	mDeleteChannelButton.addActionListener( this );
    	mDeleteChannelButton.setEnabled( false );
    	mDeleteChannelButton.setToolTipText( "Deletes the currently selected channel" );
    	buttonsPanel.add( mDeleteChannelButton );
    	
    	JPanel listAndButtonsPanel = new JPanel();

    	listAndButtonsPanel.setLayout( 
			new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill][]") );

    	listAndButtonsPanel.add( channelScroller, "wrap" );
    	listAndButtonsPanel.add( buttonsPanel );
		
		JideSplitPane splitPane = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
		splitPane.setDividerSize( 5 );
		splitPane.setShowGripper( true );
		splitPane.add( listAndButtonsPanel );
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
				
				Channel channel = mChannelModel.getChannelAtIndex( index );
				
				if( channel != null )
				{
					mEditor.setChannel( channel );
					mCopyChannelButton.setEnabled( true );
					mDeleteChannelButton.setEnabled( true );
				}
			}
			else
			{
				mEditor.setChannel( null );
				mCopyChannelButton.setEnabled( false );
				mDeleteChannelButton.setEnabled( false );
			}
		}
	}

	/**
	 * Selects the channel's table row and scrolls the table's viewport to show it
	 */
	private void addChannel( Channel channel )
	{
		//HACK: when inserting a row to the model, the JTable gets
		//notified and attempts to tell the coderazzi table filter 
		//adaptive choices filter to refresh before the table filter is 
		//notified of the row additions, causing an index out of bounds 
		//exception.  We turn off adaptive choices temporarily, add the
		//channel, and turn on adaptive choices again.
		mTableFilterHeader.setAdaptiveChoices( false );
		
		int index = mChannelModel.addChannel( channel );

		mTableFilterHeader.setAdaptiveChoices( true );

		if( index >= 0 )
		{
			int translatedIndex = mChannelTable.convertRowIndexToView( index );
			mChannelTable.setRowSelectionInterval( translatedIndex, translatedIndex );
			mChannelTable.scrollRectToVisible( 
				new Rectangle( mChannelTable.getCellRect( translatedIndex, 0, true ) ) );
		}
	}
	
	private Channel getSelectedChannel()
	{
		int index = mChannelTable.getSelectedRow();
		
		if( index >= 0 )
		{
			return mChannelModel.getChannelAtIndex( 
				mChannelTable.convertRowIndexToModel( index ) );
		}
		
		return null;
	}

	/**
	 * Responds to New, Copy and Delete Channel button invocations
	 */
	@Override
	public void actionPerformed( ActionEvent event )
	{
		switch( event.getActionCommand() )
		{
			case NEW_CHANNEL:
				addChannel( new Channel( "New Channel" ) );
				break;
			case COPY_CHANNEL:
				Channel selected = getSelectedChannel();
				
				if( selected != null )
				{
					addChannel( selected.copyOf() );
				}
				break;
			case DELETE_CHANNEL:
				Channel toDelete = getSelectedChannel();

				if( toDelete != null )
				{
					int choice = JOptionPane.showConfirmDialog( PlaylistEditor.this, 
						"Do you want to delete this channel?", "Delete Channel?", 
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
					
					if( choice == JOptionPane.YES_OPTION )
					{
						mChannelModel.removeChannel( toDelete );
					}
				}
				break;
			default:
				break;
		}
	}
}
