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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;

import ua.in.smartjava.module.decode.config.AuxDecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.controller.channel.Channel;

public class AuxDecodeConfigurationEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;

    private List<AuxDecoderCheckBox> mControls = new ArrayList<>();
    
	public AuxDecodeConfigurationEditor()
	{
		init();
	}
	
	public AuxDecodeConfiguration getConfiguration()
	{
		return hasItem() ? getItem().getAuxDecodeConfiguration() : null;
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 4", "", "[][grow]" ) );

		List<DecoderType> decoders = DecoderType.getAuxDecoders();
		
		Collections.sort( decoders );

		for( DecoderType decoder: decoders )
		{
			AuxDecoderCheckBox control = new AuxDecoderCheckBox( decoder );
			control.setEnabled( false );
			control.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					setModified( true );
				}
			} );
			add( control );
			mControls.add( control );
		}
	}

	@Override
    public void save()
    {
		if( hasItem() )
		{
			AuxDecodeConfiguration config = getItem().getAuxDecodeConfiguration();
			
			config.clearAuxDecoders();
			
			for( AuxDecoderCheckBox checkBox: mControls )
			{
				if( checkBox.isSelected() )
				{
					config.addAuxDecoder( checkBox.getDecoderType() );
				}
			}
		}
		
		setModified( false );
    }
	
	private void setControlsEnabled( boolean enabled )
	{
		for( AuxDecoderCheckBox box: mControls )
		{
			if( box.isEnabled() != enabled )
			{
				box.setEnabled( enabled );
			}
		}
	}
	

	public class AuxDecoderCheckBox extends JCheckBox
	{
		private static final long serialVersionUID = 1L;
		
		private DecoderType mDecoderType;
		
		public AuxDecoderCheckBox( DecoderType decoder )
		{
			super( decoder.getDisplayString() );
			
			mDecoderType = decoder;
		}
		
		public DecoderType getDecoderType()
		{
			return mDecoderType;
		}
	}

	@Override
	public void setItem( Channel channel )
	{
		super.setItem( channel );
		
		if( hasItem() )
		{
			setControlsEnabled( true );
			
			for( AuxDecoderCheckBox cb: mControls )
			{
				if( getItem().getAuxDecodeConfiguration().getAuxDecoders().contains( cb.getDecoderType() ) )
	        	{
	        		cb.setSelected( true );
	        	}
	        	else
	        	{
	        		cb.setSelected( false );
	        	}
			}
		}
		else
		{
			setControlsEnabled( false );
		}
	}
}
