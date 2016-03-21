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
package module.decode;

import gui.editor.Editor;
import gui.editor.EditorValidationException;
import gui.editor.EmptyValidatingEditor;
import gui.editor.ValidatingEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.Channel;
import controller.channel.map.ChannelMapModel;

public class DecodeConfigurationEditor extends ValidatingEditor<Channel>
{
	private final static Logger mLog = LoggerFactory.getLogger( DecodeConfigurationEditor.class );

	private static final long serialVersionUID = 1L;

    private JComboBox<DecoderType> mComboDecoders;
    private ValidatingEditor<Channel> mCurrentEditor = new EmptyValidatingEditor<>();

    public DecodeConfigurationEditor( final ChannelMapModel channelMapModel )
	{
    	init();
	}

    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 3", "[right][left][grow,fill]", "[][grow]" ) );

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
				setEditor( DecoderFactory.getEditor( selected ) );
           }
		});
		
    	add( new JLabel( "Decoder:" ) );
		add( mComboDecoders );
		add( mCurrentEditor );
    }
    
    private void setEditor( ValidatingEditor<Channel> editor )
    {
    	if( mCurrentEditor != editor )
		{
    		remove( mCurrentEditor );
    		mCurrentEditor = editor;
    		mCurrentEditor.setItem( getItem() );
    		add( mCurrentEditor );
    		
    		revalidate();
    		repaint();
		}
    }

    @Override
    public void save()
    {
    	if( hasItem() && mCurrentEditor != null )
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
