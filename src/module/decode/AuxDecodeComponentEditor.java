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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;

import module.decode.config.AuxDecodeConfiguration;
import controller.channel.Channel;
import controller.channel.ChannelConfigurationEditor;
import controller.channel.ConfigurationValidationException;

public class AuxDecodeComponentEditor extends ChannelConfigurationEditor
{
    private static final long serialVersionUID = 1L;

    private List<AuxDecoderCheckBox> mControls = new ArrayList<>();
    
    private Channel mChannel;
    
	public AuxDecodeComponentEditor()
	{
		List<DecoderType> decoders = DecoderType.getAuxDecoders();
		
		Collections.sort( decoders );

		for( DecoderType decoder: decoders )
		{
			AuxDecoderCheckBox control = new AuxDecoderCheckBox( decoder );
			
			add( control, "wrap" );

			mControls.add( control );
		}
	}

	public AuxDecodeConfiguration getConfig()
	{
		if( mChannel != null )
		{
			return mChannel.getAuxDecodeConfiguration();
		}
		
		return new AuxDecodeConfiguration();
	}

	@Override
    public void save()
    {
		if( mChannel != null )
		{
			AuxDecodeConfiguration config = mChannel.getAuxDecodeConfiguration();
			
			config.clearAuxDecoders();
			
			for( AuxDecoderCheckBox checkBox: mControls )
			{
				if( checkBox.isSelected() )
				{
					config.addAuxDecoder( checkBox.getDecoderType() );
				}
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
	public void setConfiguration( Channel channel )
	{
		mChannel = channel;

		for( AuxDecoderCheckBox checkBox: mControls )
		{
        	if( mChannel != null &&
    			mChannel.getAuxDecodeConfiguration().getAuxDecoders()
    				.contains( checkBox.getDecoderType() ) )
        	{
        		checkBox.setSelected( true );
        	}
        	else
        	{
        		checkBox.setSelected( false );
        	}
		}
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
	}
}
