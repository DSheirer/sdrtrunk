package controller.channel.map;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;

import com.jidesoft.swing.JideSplitPane;

public class ChannelMapManager extends JPanel
{
	private static final long serialVersionUID = 1L;

	private ChannelMapModel mChannelMapModel;
	private JList<ChannelMap> mChannelMapList;
	private ChannelMapEditor mChannelMapEditor;
	
	public ChannelMapManager( ChannelMapModel channelMapModel )
	{
		mChannelMapModel = channelMapModel;
		mChannelMapList = new JList( mChannelMapModel );
		mChannelMapEditor = new ChannelMapEditor();
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "", "[fill,grow]", "[fill,grow]" ) );

		mChannelMapList.addListSelectionListener( mChannelMapEditor );
		mChannelMapList.setLayoutOrientation( JList.VERTICAL );
		mChannelMapList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		JScrollPane listScroller = new JScrollPane( mChannelMapList );

		JScrollPane editorScroller = new JScrollPane( mChannelMapEditor );
		
		JideSplitPane splitPane = new JideSplitPane();
		splitPane.setOrientation( JideSplitPane.HORIZONTAL_SPLIT );
		splitPane.add( listScroller );
		splitPane.add( editorScroller );
		
		add( splitPane );
	}
}
