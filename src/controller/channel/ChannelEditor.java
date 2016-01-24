/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

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
    
	private Channel mChannel;

	private JButton mEnableButton = new JButton( "Enable" );
	private JLabel mChannelName = new JLabel( "Channel:" );
    
	private ChannelModel mChannelModel;
    private PlaylistManager mPlaylistManager;
    private SourceManager mSourceManager;
    
    private boolean mChannelEnableRequested = false;
    
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
	
	/**
	 * Sets the channel configuration in each of the channel editor components
	 * Note: this method must be invoked on the swing event dispatch thread
	 */
	public void setChannel( final Channel channel )
	{
		mNameConfigurationEditor.setConfiguration( channel );
		mSourceEditor.setConfiguration( channel );
		mDecodeEditor.setConfiguration( channel );
		mAuxDecodeEditor.setConfiguration( channel );
		mEventLogEditor.setConfiguration( channel );
		mRecordEditor.setConfiguration( channel );
		
		if( channel != null )
		{
			mChannelName.setText( "Channel: " + channel.getName() );
			mEnableButton.setText( channel.getEnabled() ? "Disable" : "Enable" );
			mEnableButton.setEnabled( true );
			mEnableButton.setBackground( channel.getEnabled() ? Color.GREEN : getBackground() );
		}
		else
		{
			mChannelName.setText( "Channel: " );
			mEnableButton.setText( "Enable" );
			mEnableButton.setEnabled( false );
			mEnableButton.setBackground( getBackground() );
		}
		
		
		mChannel = channel;
	}

	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][][][grow][]" ) );
		
		mEnableButton.addActionListener( this );
		mEnableButton.setEnabled( false );
		add( mEnableButton, "growx,push" );
		add( mChannelName, "growx,push" );

		add( new JSeparator( JSeparator.HORIZONTAL ), "span,growx,push" );
		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

    	tabs.addTab( "Name/Alias", mNameConfigurationEditor );
    	
		mSourceEditor = new SourceComponentEditor( mSourceManager );
		tabs.addTab( "Source", mSourceEditor );
		
		mDecodeEditor = new DecodeComponentEditor( mPlaylistManager );
		tabs.addTab( "Decoder", mDecodeEditor );

		mAuxDecodeEditor = new AuxDecodeComponentEditor();
		tabs.addTab( "Aux Decoders", mAuxDecodeEditor );

		mEventLogEditor = new EventLogComponentEditor();
		tabs.addTab( "Logging", mEventLogEditor );
		
		mRecordEditor = new RecordComponentEditor();
		tabs.addTab( "Recording", mRecordEditor );

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
		if( mChannel != null && mChannel == event.getChannel() )
		{
			switch( event.getEvent() )
			{
				case NOTIFICATION_CONFIGURATION_CHANGE:
				case NOTIFICATION_PROCESSING_START:
				case NOTIFICATION_PROCESSING_STOP:
					setChannel( mChannel );
					mChannelEnableRequested = false;
					break;
				case NOTIFICATION_DELETE:
					mChannel = null;
					setChannel( mChannel );
					break;
				case NOTIFICATION_ENABLE_REJECTED:
					if( mChannelEnableRequested )
					{
						JOptionPane.showMessageDialog( ChannelEditor.this, 
								"Channel could not be enabled.  This is likely because "
								+ "there are no tuner channels available", "Couldn't "
								+ "enable channel", JOptionPane.INFORMATION_MESSAGE );
							
						mChannelEnableRequested = false;
					}
					break;
				default:
					break;
			}
		}
		
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Enable" ) )
		{
			if( mChannel != null )
			{
				mChannelEnableRequested = true;
				mChannelModel.broadcast( new ChannelEvent( mChannel, Event.REQUEST_ENABLE ) );
			}
		}
		else if( command.contentEquals( "Disable" ) )
		{
			if( mChannel != null )
			{
				mChannelModel.broadcast( new ChannelEvent( mChannel, Event.REQUEST_DISABLE ) );
			}
		}
		else if( command.contentEquals( "Save" ) )
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
			mSourceEditor.validateConfiguration();
			mDecodeEditor.validateConfiguration();
			mAuxDecodeEditor.validateConfiguration();
			mEventLogEditor.validateConfiguration();
			
			//Validate the source configuration against the decode editor to 
			//ensure that the source type is compatible with the decode type
			
			//Validate the aux decoders against the primary decoder
//TODO: make sure that no aux decoders are running with P25
			
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
		mSourceEditor.save();
		mDecodeEditor.save();
		mAuxDecodeEditor.save();
		mEventLogEditor.save();
		
		//Broadcast a channel configuration change event
		mChannelModel.broadcast( new ChannelEvent( mChannel, 
				Event.NOTIFICATION_CONFIGURATION_CHANGE ) );
    }
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
