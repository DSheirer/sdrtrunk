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
package ua.in.smartjava.module.decode;

import ua.in.smartjava.gui.editor.Editor;
import ua.in.smartjava.gui.editor.EditorValidationException;
import ua.in.smartjava.gui.editor.EmptyValidatingEditor;
import ua.in.smartjava.gui.editor.ValidatingEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.controller.channel.Channel;
import ua.in.smartjava.controller.channel.map.ChannelMapModel;

public class DecodeConfigurationEditor extends ValidatingEditor<Channel>
{
	private static final long serialVersionUID = 1L;

    private JComboBox<DecoderType> mComboDecoders;
    private ValidatingEditor<Channel> mCurrentEditor = new EmptyValidatingEditor<>( "a decoder" );
    private ChannelMapModel mChannelMapModel;

    public DecodeConfigurationEditor( final ChannelMapModel channelMapModel )
	{
    	mChannelMapModel = channelMapModel;
    	init();
	}

    private void init()
    {
		setLayout( new MigLayout( "wrap 2", "[][grow,fill]", "[align top][grow,fill]" ) );

		mComboDecoders = new JComboBox<DecoderType>();
		mComboDecoders.setEnabled( false );
		
		DefaultComboBoxModel<DecoderType> model = new DefaultComboBoxModel<DecoderType>();
		
		for( DecoderType type: DecoderType.getPrimaryDecoders() )
		{
			model.addElement( type );
		}
		
		mComboDecoders.setModel( model );
		mComboDecoders.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				DecoderType selected = mComboDecoders.getItemAt( mComboDecoders.getSelectedIndex() );
				ValidatingEditor<Channel> editor = DecoderFactory.getEditor( selected, mChannelMapModel );
				setEditor( editor );
           }
		});
		
		add( mComboDecoders );
		add( mCurrentEditor );
    }
    
    private void setEditor( ValidatingEditor<Channel> editor )
    {
    	if( mCurrentEditor != editor )
		{
    		//Set ua.in.smartjava.channel to null in current editor to force a save prompt as required
    		if( mCurrentEditor.isModified() )
    		{
    			mCurrentEditor.setItem( null );
    		}
    		
    		remove( mCurrentEditor );
    		
    		mCurrentEditor = editor;
    		mCurrentEditor.setSaveRequestListener( this );
    		mCurrentEditor.setItem( getItem() );
    		
    		add( mCurrentEditor );
    		
    		revalidate();
    		repaint();
		}
    }

    @Override
    public void save()
    {
    	if( hasItem() )
    	{
    		mCurrentEditor.save();
    	}
    	
    	setModified( false );
    }

	@Override
	public void setItem( Channel channel )
	{
		super.setItem( channel );

		if( hasItem() )
		{
			mComboDecoders.setEnabled( true );
			mComboDecoders.setSelectedItem( channel.getDecodeConfiguration().getDecoderType() );
			mCurrentEditor.setItem( channel );
		}
		else
		{
			mComboDecoders.setEnabled( false );
		}
		
		setModified( false );
	}

	@Override
	public void validate( Editor<Channel> editor ) throws EditorValidationException
	{
		mCurrentEditor.validate( editor );
	}
}
