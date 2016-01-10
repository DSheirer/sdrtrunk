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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

import controller.channel.ChannelEvent.Event;

public class ChannelEditor extends JPanel 
					implements ActionListener, ChannelEventListener
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( ChannelEditor.class );

	private ChannelConfigurationEditor mNameConfigurationEditor;
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
    
	public ChannelEditor( ChannelModel channelModel,
						  PlaylistManager playlistManager,
						  SourceManager sourceManager )
	{
		super();
		
		mChannelModel = channelModel;
		mPlaylistManager = playlistManager;
		mSourceManager = sourceManager;

		mNameConfigurationEditor = new NameConfigurationEditor( 
				mChannelModel, mPlaylistManager );
		
		initGUI();
	}
	
	public void setChannel( Channel channel )
	{
		mNameConfigurationEditor.setConfiguration( channel );
		
		mChannel = channel;
	}

	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][][][grow][]" ) );
		
		add( new JLabel( "Channel Configuration" ), "span, align center" );

		add( new JLabel( "Enabled:" ), "wrap" );

		add( mNameConfigurationEditor );

		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

    	tabs.addTab( "Name/Alias", mNameConfigurationEditor );
//		mSourceEditor = new SourceComponentEditor( mSourceManager, mChannel );
//		tabs.addTab( "Source", mSourceEditor );
//		
//		mDecodeEditor = new DecodeComponentEditor( mPlaylistManager, mChannel );
//		tabs.addTab( "Decoder", mDecodeEditor );
//
//		mAuxDecodeEditor = new AuxDecodeComponentEditor( mChannel );
//		tabs.addTab( "Aux", mAuxDecodeEditor );
//
//		mEventLogEditor = new EventLogComponentEditor( mChannel );
//		tabs.addTab( "Event Log", mEventLogEditor );
//		
//		mRecordEditor = new RecordComponentEditor( mChannel );
//		tabs.addTab( "Record", mRecordEditor );

		add( tabs, "span,grow,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ChannelEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( ChannelEditor.this );
		add( btnReset, "growx,push" );
	}
	
	@Override
	public void channelChanged( ChannelEvent event )
	{
		
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
			setChannel( mChannel );
		}
    }

    public void save()
    {
		try
		{
			//Validate each of the configuration editors to check for errors
			mNameConfigurationEditor.validateConfiguration();
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
		}
		catch ( ConfigurationValidationException validationException )
		{
			/* ChannelValidationException can be thrown by any of the component
			 * editors.  Show the validation error text in a popup menu to the
			 * user and set the focus on the error component */
			JOptionPane.showMessageDialog( ChannelEditor.this,
			    validationException.getMessage(),
			    "Channel Configuration Error",
			    JOptionPane.ERROR_MESSAGE );			

			Component component = validationException.getComponent();
			
			if( component != null )
			{
				component.requestFocus();
			}
			
			return;
		}

		//Save the contents of each editor to the channel configuration
		mNameConfigurationEditor.save();
		
		//Broadcast a channel configuration change event
		mChannelModel.broadcast( new ChannelEvent( mChannel, 
				Event.NOTIFICATION_CONFIGURATION_CHANGE ) );
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

//	/**
//	 * Validate the specified editor against each of the component editors
//	 */
//	@Override
//	public void validate( ChannelConfigurationEditor editor ) throws ConfigurationValidationException
//	{
//		mSourceEditor.validate( editor );
//		mDecodeEditor.validate( editor );
//		mAuxDecodeEditor.validate( editor );
//		mEventLogEditor.validate( editor );
//		mRecordEditor.validate( editor );
//	}
}
