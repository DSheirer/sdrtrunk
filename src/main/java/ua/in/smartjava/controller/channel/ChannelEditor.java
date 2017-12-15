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
package ua.in.smartjava.controller.channel;

import ua.in.smartjava.gui.editor.Editor;
import ua.in.smartjava.gui.editor.EditorValidationException;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import ua.in.smartjava.module.decode.AuxDecodeConfigurationEditor;
import ua.in.smartjava.module.decode.DecodeConfigurationEditor;
import ua.in.smartjava.module.log.EventLogConfigurationEditor;
import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.record.RecordConfigurationEditor;
import ua.in.smartjava.source.SourceConfigurationEditor;
import ua.in.smartjava.source.SourceManager;
import ua.in.smartjava.alias.AliasModel;

import com.jidesoft.swing.JideTabbedPane;

import ua.in.smartjava.controller.channel.ChannelEvent.Event;
import ua.in.smartjava.controller.channel.map.ChannelMapModel;

public class ChannelEditor extends Editor<Channel> implements ActionListener, ChannelEventListener
{
	private static final long serialVersionUID = 1L;

	private NameConfigurationEditor mNameConfigurationEditor;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private DecodeConfigurationEditor mDecodeConfigurationEditor;
    private AuxDecodeConfigurationEditor mAuxDecodeConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
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
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", "[grow,fill][grow,fill][grow,fill]", "[grow,fill][]" ) );
		
		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

    	tabs.setTabTrailingComponent( mChannelName );
    	
		mNameConfigurationEditor.setSaveRequestListener( this );
    	tabs.addTab( "Name/Alias", mNameConfigurationEditor );
    	
		mSourceConfigurationEditor = new SourceConfigurationEditor( mSourceManager );
		mSourceConfigurationEditor.setSaveRequestListener( this );
		tabs.addTab( "Source", mSourceConfigurationEditor );
		
		mDecodeConfigurationEditor = new DecodeConfigurationEditor( mChannelMapModel );
		mDecodeConfigurationEditor.setSaveRequestListener( this );
		tabs.addTab( "Decoder", mDecodeConfigurationEditor );

		mAuxDecodeConfigurationEditor = new AuxDecodeConfigurationEditor();
		mAuxDecodeConfigurationEditor.setSaveRequestListener( this );
		tabs.addTab( "Aux Decoders", mAuxDecodeConfigurationEditor );

		mEventLogConfigurationEditor = new EventLogConfigurationEditor();
		mEventLogConfigurationEditor.setSaveRequestListener( this );
		tabs.addTab( "Logging", mEventLogConfigurationEditor );
		
		mRecordConfigurationEditor = new RecordConfigurationEditor();
		mRecordConfigurationEditor.setSaveRequestListener( this );
		tabs.addTab( "Recording", mRecordConfigurationEditor );

		add( tabs, "span" );

		mEnableButton.addActionListener( this );
		mEnableButton.setEnabled( false );
		mEnableButton.setToolTipText( "Start the currently selected ua.in.smartjava.channel running/decoding" );
		add( mEnableButton );

		JButton btnSave = new JButton( "Save" );
		btnSave.setToolTipText( "Save changes to the currently selected ua.in.smartjava.channel" );
		btnSave.addActionListener( ChannelEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.setToolTipText( "Reload the currently selected ua.in.smartjava.channel" );
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
							+ "available", "Couldn't enable ua.in.smartjava.channel", JOptionPane.INFORMATION_MESSAGE );
							
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
				save();
				
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

	/**
	 * Sets the ua.in.smartjava.channel configuration in each of the ua.in.smartjava.channel editor components
	 * Note: this method must be invoked on the swing event dispatch thread
	 */
	public void setItem( final Channel channel )
	{
		super.setItem( channel );
		
		mNameConfigurationEditor.setItem( channel );
		mSourceConfigurationEditor.setItem( channel );
		mDecodeConfigurationEditor.setItem( channel );
		mAuxDecodeConfigurationEditor.setItem( channel );
		mEventLogConfigurationEditor.setItem( channel );
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
	
    public void save()
    {
    	if( hasItem() )
    	{
			mNameConfigurationEditor.save();
   			mSourceConfigurationEditor.save();
   			mDecodeConfigurationEditor.save();
   			mAuxDecodeConfigurationEditor.save();
   			mEventLogConfigurationEditor.save();
   			mRecordConfigurationEditor.save();
   			
    		try
    		{
    			mDecodeConfigurationEditor.validate( mSourceConfigurationEditor );
    			mDecodeConfigurationEditor.validate( mAuxDecodeConfigurationEditor );
    		}
    		catch( EditorValidationException e )
    		{
    			e.getEditor().requestFocus();
    			
    			JOptionPane.showMessageDialog( e.getEditor(), e.getReason(),
    					"Configuration Error", JOptionPane.ERROR_MESSAGE );			
    			
    			return;
    		}
    		

   			mChannelModel.broadcast( new ChannelEvent( getItem(), 
   	   					Event.NOTIFICATION_CONFIGURATION_CHANGE ) );
    	}

    	setModified( false );
    }
}
