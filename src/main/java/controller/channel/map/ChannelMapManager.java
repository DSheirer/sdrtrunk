package controller.channel.map;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import com.jidesoft.swing.JideSplitPane;

public class ChannelMapManager extends JPanel
{
	private static final long serialVersionUID = 1L;

	private ChannelMapModel mChannelMapModel;
	private JList<ChannelMap> mChannelMapList;
	private ChannelMapEditor mChannelMapEditor;
	private JButton mDeleteMapButton;
	private JButton mCloneMapButton;
	
	public ChannelMapManager( ChannelMapModel channelMapModel )
	{
		mChannelMapModel = channelMapModel;
		mChannelMapList = new JList<>( mChannelMapModel );
		mChannelMapEditor = new ChannelMapEditor( mChannelMapModel );
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 1 1 1 1", "[fill,grow]", "[fill,grow]" ) );

		JPanel listPanel = new JPanel();
		listPanel.setLayout( new MigLayout( "", 
				"[fill,grow][fill,grow][fill,grow]", "[fill,grow][]" ) );
		
		mChannelMapList.addListSelectionListener( mChannelMapEditor );
		mChannelMapList.addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent e )
			{
				if( mChannelMapList.getSelectedValue() != null )
				{
					mCloneMapButton.setEnabled( true );
					mDeleteMapButton.setEnabled( true );
				}
				else
				{
					mCloneMapButton.setEnabled( false );
					mDeleteMapButton.setEnabled( false );
				}
			}
		} );
		mChannelMapList.setLayoutOrientation( JList.VERTICAL );
		mChannelMapList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		
		JScrollPane listScroller = new JScrollPane( mChannelMapList );
		
		listPanel.add( listScroller, "span" );
		
		JButton newMapButton = new JButton( "New" );
		newMapButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				ChannelMap channelMap = new ChannelMap( "New Channel Map" );
				mChannelMapModel.addChannelMap( channelMap );
				mChannelMapList.setSelectedValue( channelMap, true );
			}
		} );
		
		listPanel.add( newMapButton );

		mCloneMapButton = new JButton( "Clone" );
		mCloneMapButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				ChannelMap selected = mChannelMapList.getSelectedValue();
				
				if( selected != null )
				{
					ChannelMap copy = selected.copyOf();
					mChannelMapModel.addChannelMap( copy );
					mChannelMapList.setSelectedValue( copy, true );
				}
			}
		} );
		listPanel.add( mCloneMapButton );

		mDeleteMapButton = new JButton( "Delete" );
		mDeleteMapButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				ChannelMap channelMap = mChannelMapList.getSelectedValue();
				
				if( channelMap != null )
				{
					int choice = JOptionPane.showConfirmDialog( mChannelMapList, 
						"Do you want to delete this channel map?", 
						"Delete Channel Map?", JOptionPane.YES_NO_OPTION );
					
					if( choice == JOptionPane.YES_OPTION )
					{
						mChannelMapModel.removeChannelMap( channelMap );
					}
				}
			}
		} );
		listPanel.add( mDeleteMapButton );

		JScrollPane editorScroller = new JScrollPane( mChannelMapEditor );
		
		JideSplitPane splitPane = new JideSplitPane();
		splitPane.setOrientation( JideSplitPane.HORIZONTAL_SPLIT );
		splitPane.add( listPanel );
		splitPane.add( editorScroller );
		
		add( splitPane );
	}
}
