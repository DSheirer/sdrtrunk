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

import gui.editor.Editor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import module.decode.AuxDecodeConfigurationEditor;
import module.decode.DecodeConfigurationEditor;
import module.log.EventLogConfigurationEditor;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.RecordConfigurationEditor;
import source.SourceConfigurationEditor;
import source.SourceManager;
import alias.AliasModel;

import com.jidesoft.swing.JideTabbedPane;

import controller.channel.ChannelEvent.Event;
import controller.channel.map.ChannelMapModel;

public class ChannelEditor extends Editor<Channel> implements ActionListener, ChannelEventListener
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( ChannelEditor.class );

	private NameConfigurationEditor mNameConfigurationEditor;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private DecodeConfigurationEditor mDecodeConfigurationEditor;
    private AuxDecodeConfigurationEditor mAuxDecodeConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfiguraitonEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    
	private JButton mEnableButton = new JButton( "Enable" );
	private JLabel mChannelName = new JLabel( "Channel:" );
    
	private ChannelModel mChannelModel;
	private ChannelMapModel mChannelMapModel;
    private SourceManager mSourceManager;
    
    private boolean mChannelEnableRequested = false;
    
	public ChannelEditor( ChannelModel channelModel,
						  ChannelMapModel channelMapModel,
						  SourceManager sourceManager,
						  AliasModel aliasModel )
	{
		mChannelModel = channelModel;
		mChannelMapModel = channelMapModel;
		mSourceManager = sourceManager;
		mNameConfigurationEditor = new NameConfigurationEditor( aliasModel,	mChannelModel );
		
		init();
	}
	
	/**
	 * Sets the channel configuration in each of the channel editor components
	 * Note: this method must be invoked on the swing event dispatch thread
	 */
	public void setItem( final Channel channel )
	{
		//Check if the current channel configuration has changed
		if( hasItem() &&
			( mNameConfigurationEditor.isModified() ) )
		{
			int option = JOptionPane.showConfirmDialog( ChannelEditor.this, 
					"This channel configuration has changed.  Do you want to save these changes?", 
					"Save Changes?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
				
			if( option == JOptionPane.YES_OPTION )
			{
				save();
			}
			
			setModified( false );
		}
		
		mNameConfigurationEditor.setItem( channel );
		mSourceConfigurationEditor.setItem( channel );
		mDecodeConfigurationEditor.setItem( channel );
		mAuxDecodeConfigurationEditor.setItem( channel );
		mEventLogConfiguraitonEditor.setItem( channel );
		mRecordConfigurationEditor.setItem( channel );
		
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
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", "[grow,fill][grow,fill][grow,fill]", "[][][][][grow]" ) );
		
		add( mChannelName, "wrap" );

		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

    	tabs.setTabTrailingComponent( mChannelName );
    	tabs.addTab( "Name/Alias", mNameConfigurationEditor );
    	
		mSourceConfigurationEditor = new SourceConfigurationEditor( mSourceManager );
		tabs.addTab( "Source", mSourceConfigurationEditor );
		
		mDecodeConfigurationEditor = new DecodeConfigurationEditor( mChannelMapModel );
		tabs.addTab( "Decoder", mDecodeConfigurationEditor );

		mAuxDecodeConfigurationEditor = new AuxDecodeConfigurationEditor();
		tabs.addTab( "Aux Decoders", mAuxDecodeConfigurationEditor );

		mEventLogConfiguraitonEditor = new EventLogConfigurationEditor();
		tabs.addTab( "Logging", mEventLogConfiguraitonEditor );
		
		mRecordConfigurationEditor = new RecordConfigurationEditor();
		tabs.addTab( "Recording", mRecordConfigurationEditor );

		add( tabs, "span" );

		mEnableButton.addActionListener( this );
		mEnableButton.setEnabled( false );
		add( mEnableButton );

		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ChannelEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( ChannelEditor.this );
		add( btnReset );
	}
	
	@Override
	public void channelChanged( ChannelEvent event )
	{
		if( hasItem() && getItem() == event.getChannel() )
		{
			switch( event.getEvent() )
			{
				case NOTIFICATION_CONFIGURATION_CHANGE:
				case NOTIFICATION_PROCESSING_START:
				case NOTIFICATION_PROCESSING_STOP:
					setItem( getItem() );
					mChannelEnableRequested = false;
					break;
				case NOTIFICATION_DELETE:
					setItem( null );
					break;
				case NOTIFICATION_ENABLE_REJECTED:
					if( mChannelEnableRequested )
					{
						JOptionPane.showMessageDialog( ChannelEditor.this, "Channel could not be "
							+ "enabled.  This is likely because there are no tuner channels "
							+ "available", "Couldn't enable channel", JOptionPane.INFORMATION_MESSAGE );
							
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
			if( hasItem() )
			{
				mChannelEnableRequested = true;
				mChannelModel.broadcast( new ChannelEvent( getItem(), Event.REQUEST_ENABLE ) );
			}
		}
		else if( command.contentEquals( "Disable" ) )
		{
			if( hasItem() )
			{
				mChannelModel.broadcast( new ChannelEvent( getItem(), Event.REQUEST_DISABLE ) );
			}
		}
		else if( command.contentEquals( "Save" ) )
		{
			save();
		}
		else if( command.contentEquals( "Reset" ) )
		{
			setItem( getItem() );
		}
    }

    public void save()
    {
    	if( hasItem() )
    	{
//    		try
//    		{
//TODO: validate the source editor against the decode editor
//TODO: validate the aux editor against the decode editor - verify no aux decoders with P25
//    		}
//    		catch( ConfigurationValidationException e )
//    		{
//    			/* ChannelValidationException can be thrown by any of the component
//    			 * editors.  Show the validation error text in a popup menu to the
//    			 * user and set the focus on the error component */
//    			JOptionPane.showMessageDialog( ChannelEditor.this,
//    			    validationException.getMessage(),
//    			    "Channel Configuration Error",
//    			    JOptionPane.ERROR_MESSAGE );			
//
//    			Component component = validationException.getComponent();
//    			
//    			if( component != null )
//    			{
//    				component.requestFocus();
//    			}
//    			
//    			return;
//    		}

			mNameConfigurationEditor.save();
   			mSourceConfigurationEditor.save();
   			mDecodeConfigurationEditor.save();
   			mAuxDecodeConfigurationEditor.save();
   			mEventLogConfiguraitonEditor.save();
   			mRecordConfigurationEditor.save();

   			//Broadcast a channel configuration change event
   			mChannelModel.broadcast( new ChannelEvent( getItem(), Event.NOTIFICATION_CONFIGURATION_CHANGE ) );
    	}

    	setModified( false );
    }
}
