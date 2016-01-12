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
import javax.swing.JTextField;

import module.decode.AuxDecodeComponentEditor;
import module.decode.DecodeComponentEditor;
import module.log.EventLogComponentEditor;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import record.RecordComponentEditor;
import source.SourceComponentEditor;
import source.SourceManager;
import alias.AliasList;

import com.jidesoft.swing.JideTabbedPane;

public class OldChannelEditor extends ChannelConfigurationEditor implements ActionListener
{
	private final static Logger mLog = LoggerFactory.getLogger( OldChannelEditor.class );

	private static final long serialVersionUID = 1L;

    private DecodeComponentEditor mDecodeEditor;
    private AuxDecodeComponentEditor mAuxDecodeEditor;
    private EventLogComponentEditor mEventLogEditor;
    private RecordComponentEditor mRecordEditor;
    private SourceComponentEditor mSourceEditor;
    
	private JTextField mChannelName;
	private JCheckBox mChannelEnabled = new JCheckBox();
    private JComboBox<AliasList> mComboAliasLists;
    
	private Channel mChannel;
	private ChannelModel mChannelModel;
    private PlaylistManager mPlaylistManager;
    private SourceManager mSourceManager;
    
	public OldChannelEditor( Channel channel, 
						  ChannelModel channelModel,
						  PlaylistManager playlistManager,
						  SourceManager sourceManager )
	{
		super();
		
		mChannel = channel;
		mChannelModel = channelModel;
		mPlaylistManager = playlistManager;
		mSourceManager = sourceManager;
		
		initGUI();
	}

	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][][][grow][]" ) );
		
		add( new JLabel( "Channel Configuration" ), "span, align center" );

		add( new JLabel( "Enabled:" ) );

		mChannelEnabled.setSelected( mChannel.getEnabled() );
		mChannelEnabled.setComponentOrientation( ComponentOrientation.RIGHT_TO_LEFT );
		add( mChannelEnabled );

		add( new JLabel( "Name:" ) );

		mChannelName = new JTextField( mChannel.getName() );
		add( mChannelName, "growx,push" );

		/**
		 * ComboBox: Alias Lists
		 */
		add( new JLabel( "Alias List:" ) );

		mComboAliasLists = new JComboBox<AliasList>();

		List<AliasList> lists = mPlaylistManager.getPlayist()
				.getAliasDirectory().getAliasList();

		Collections.sort( lists );
		
		mComboAliasLists.setModel( new DefaultComboBoxModel<AliasList>( 
				lists.toArray( new AliasList[ lists.size() ] ) ) );

		String aliasListName = mChannel.getAliasListName();

    	if( aliasListName != null )
    	{
    		AliasList selected = mPlaylistManager.getPlayist().getAliasDirectory()
    			.getAliasList( aliasListName );

    		mComboAliasLists.setSelectedItem( selected );
    	}
		
		add( mComboAliasLists, "growx,push" );

		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

		mSourceEditor = new SourceComponentEditor( mSourceManager );
		tabs.addTab( "Source", mSourceEditor );
		
		mDecodeEditor = new DecodeComponentEditor( mPlaylistManager );
		tabs.addTab( "Decoder", mDecodeEditor );

		mAuxDecodeEditor = new AuxDecodeComponentEditor();
		tabs.addTab( "Aux", mAuxDecodeEditor );

		mEventLogEditor = new EventLogComponentEditor();
		tabs.addTab( "Event Log", mEventLogEditor );
		
		mRecordEditor = new RecordComponentEditor();
		tabs.addTab( "Record", mRecordEditor );

		add( tabs, "span,grow,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( OldChannelEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( OldChannelEditor.this );
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
//			reset();
		}
    }

	@Override
    public void save()
    {
//		try
//		{
//			/**
//			 * Validate the source configuration against the other component
//			 * configuration editors
//			 */
//			validate( mSourceEditor.getSourceEditor() );
//
//			mSourceEditor.save();
//			mDecodeEditor.save();
//			mAuxDecodeEditor.save();
//			mEventLogEditor.save();
//			mRecordEditor.save();
//
//			mChannel.setName( mChannelName.getText() );
//			mChannel.setEnabled( mChannelEnabled.isSelected() );
//
//			AliasList selected = mComboAliasLists.getItemAt( 
//					mComboAliasLists.getSelectedIndex() );
//			if( selected != null )
//			{
//				mChannel.setAliasListName( selected.getName() );
//			}
//		}
//		catch ( ConfigurationValidationException validationException )
//		{
//			/* ChannelValidationException can be thrown by any of the component
//			 * editors.  Show the validation error text in a popup menu to the
//			 * user */
//			JOptionPane.showMessageDialog( OldChannelEditor.this,
//			    validationException.getMessage(),
//			    "Channel Configuration Error",
//			    JOptionPane.ERROR_MESSAGE );			
//		}
    }

//	@Override
//    public void reset()
//    {
//		mChannelName.setText( mChannel.getName() );
//		mChannelName.requestFocus();
//		mChannelName.requestFocusInWindow();
//		
//		mChannelEnabled.setSelected( mChannel.getEnabled() );
//		mChannelEnabled.requestFocus();
//		mChannelEnabled.requestFocusInWindow();
//
//		String aliasListName = mChannel.getAliasListName();
//
//    	if( aliasListName != null )
//    	{
//    		AliasList selected = mPlaylistManager.getPlayist().getAliasDirectory()
//    			.getAliasList( aliasListName );
//
//    		mComboAliasLists.setSelectedItem( selected );
//    	}
//
//
//		mSourceEditor.reset();
//		mDecodeEditor.reset();
//		mAuxDecodeEditor.reset();
//		mEventLogEditor.reset();
//		mRecordEditor.reset();
//    }

	@Override
	public void setConfiguration( Channel channel )
	{
		
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
		mSourceEditor.validate();
		mDecodeEditor.validate();
		mAuxDecodeEditor.validate();
		mEventLogEditor.validate();
		mRecordEditor.validate();
	}
	
}
