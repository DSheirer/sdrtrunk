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
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import module.decode.AuxDecodeComponentEditor;
import module.decode.DecodeComponentEditor;
import module.log.EventLogComponentEditor;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import record.RecordComponentEditor;
import source.SourceComponentEditor;
import alias.AliasList;

import com.jidesoft.swing.JideTabbedPane;

import controller.Editor;

public class ChannelEditor extends Editor implements ActionListener
{
	private final static Logger mLog = LoggerFactory.getLogger( ChannelEditor.class );

	private static final long serialVersionUID = 1L;
    private DefaultTreeModel mTreeModel;
    private ChannelNode mChannelNode;
    
    private DecodeComponentEditor mDecodeEditor;
    private AuxDecodeComponentEditor mAuxDecodeEditor;
    private EventLogComponentEditor mEventLogEditor;
    private RecordComponentEditor mRecordEditor;
    private SourceComponentEditor mSourceEditor;
    
	private JTextField mChannelName;
	private JCheckBox mChannelEnabled = new JCheckBox();
    private JComboBox<AliasList> mComboAliasLists;
    
    private PlaylistManager mPlaylistManager;
    
	public ChannelEditor( ChannelNode channel, PlaylistManager playlistManager )
	{
		super();
		
		mChannelNode = channel;
		
		mPlaylistManager = playlistManager;
		
		initGUI();
	}

	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][][][grow][]" ) );
		
		add( new JLabel( "Channel Configuration" ), "span, align center" );

		add( new JLabel( "Enabled:" ) );

		mChannelEnabled.setSelected( 
				mChannelNode.getChannel().getEnabled() );
		mChannelEnabled.setComponentOrientation( ComponentOrientation.RIGHT_TO_LEFT );
		add( mChannelEnabled );

		add( new JLabel( "Name:" ) );

		mChannelName = new JTextField( mChannelNode.getChannel().getName() );
		add( mChannelName, "growx,push" );

		/**
		 * ComboBox: Alias Lists
		 */
		add( new JLabel( "Alias List:" ) );

		mComboAliasLists = new JComboBox<AliasList>();

		List<AliasList> lists = mChannelNode.getModel().getResourceManager()
			.getPlaylistManager().getPlayist().getAliasDirectory().getAliasList();

		Collections.sort( lists );
		
		mComboAliasLists.setModel( new DefaultComboBoxModel<AliasList>( 
				lists.toArray( new AliasList[ lists.size() ] ) ) );

		String aliasListName = 
				mChannelNode.getChannel().getAliasListName();

    	if( aliasListName != null )
    	{
    		AliasList selected = mChannelNode.getModel().getResourceManager()
    			.getPlaylistManager().getPlayist().getAliasDirectory()
    			.getAliasList( aliasListName );

    		mComboAliasLists.setSelectedItem( selected );
    	}
		
		add( mComboAliasLists, "growx,push" );

		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

		mSourceEditor = new SourceComponentEditor( mChannelNode );
		tabs.addTab( "Source", mSourceEditor );
		
		mDecodeEditor = new DecodeComponentEditor( mChannelNode, mPlaylistManager );
		tabs.addTab( "Decoder", mDecodeEditor );

		mAuxDecodeEditor = new AuxDecodeComponentEditor( mChannelNode );
		tabs.addTab( "Aux", mAuxDecodeEditor );

		mEventLogEditor = new EventLogComponentEditor( mChannelNode );
		tabs.addTab( "Event Log", mEventLogEditor );
		
		mRecordEditor = new RecordComponentEditor( mChannelNode );
		tabs.addTab( "Record", mRecordEditor );

		add( tabs, "span,grow,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ChannelEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( ChannelEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			save();
		}
		else if( command.contentEquals( "Reset" ) )
		{
			reset();
		}

		if( mTreeModel != null )
		{
			mTreeModel.nodeChanged( mChannelNode );
		}
    }

	@Override
    public void save()
    {
		try
		{
			/**
			 * Validate the source configuration against the other component
			 * configuration editors
			 */
			validate( mSourceEditor.getSourceEditor() );

			boolean expanded = mChannelNode.getModel().getTree()
					.isExpanded( new TreePath( mChannelNode ) );

			mSourceEditor.save();
			mDecodeEditor.save();
			mAuxDecodeEditor.save();
			mEventLogEditor.save();
			mRecordEditor.save();

			mChannelNode.getChannel().setName( mChannelName.getText() );
			mChannelNode.getChannel().setEnabled( mChannelEnabled.isSelected() );

			AliasList selected = mComboAliasLists.getItemAt( 
					mComboAliasLists.getSelectedIndex() );
			if( selected != null )
			{
				mChannelNode.getChannel().setAliasListName( selected.getName() );
			}

			
			mChannelNode.save();
			mChannelNode.show();
			
			if( expanded )
			{
				mChannelNode.getModel().getTree().expandPath( new TreePath( mChannelNode ) );
			}
		}
		catch ( ChannelValidationException validationException )
		{
			/* ChannelValidationException can be thrown by any of the component
			 * editors.  Show the validation error text in a popup menu to the
			 * user */
			JOptionPane.showMessageDialog( ChannelEditor.this,
			    validationException.getMessage(),
			    "Channel Configuration Error",
			    JOptionPane.ERROR_MESSAGE );			
		}
    }

	@Override
    public void reset()
    {
		mChannelName.setText( 
				mChannelNode.getChannel().getName() );
		mChannelName.requestFocus();
		mChannelName.requestFocusInWindow();
		
		mChannelEnabled.setSelected( 
				mChannelNode.getChannel().getEnabled() );
		mChannelEnabled.requestFocus();
		mChannelEnabled.requestFocusInWindow();

		String aliasListName = 
				mChannelNode.getChannel().getAliasListName();

    	if( aliasListName != null )
    	{
    		AliasList selected = mChannelNode.getModel().getResourceManager()
    			.getPlaylistManager().getPlayist().getAliasDirectory()
    			.getAliasList( aliasListName );

    		mComboAliasLists.setSelectedItem( selected );
    	}


		mSourceEditor.reset();
		mDecodeEditor.reset();
		mAuxDecodeEditor.reset();
		mEventLogEditor.reset();
		mRecordEditor.reset();
    }

	/**
	 * Validate the specified editor against each of the component editors
	 */
	@Override
	public void validate( Editor editor ) throws ChannelValidationException
	{
		mSourceEditor.validate( editor );
		mDecodeEditor.validate( editor );
		mAuxDecodeEditor.validate( editor );
		mEventLogEditor.validate( editor );
		mRecordEditor.validate( editor );
	}
}
