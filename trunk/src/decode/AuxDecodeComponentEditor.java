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
package decode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import controller.channel.AbstractChannelEditor;
import controller.channel.ChannelNode;
import decode.config.AuxDecodeConfiguration;

public class AuxDecodeComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;

    private HashMap<DecoderType,AuxDecoderCheckBox> mControls = 
    		new HashMap<DecoderType,AuxDecoderCheckBox>();
    
	public AuxDecodeComponentEditor( ChannelNode channelNode )
	{
		super( channelNode );
		
		List<DecoderType> decoders = DecoderType.getAuxDecoders();
		
		Collections.sort( decoders );
		
		for( DecoderType decoder: decoders )
		{
			AuxDecoderCheckBox control = new AuxDecoderCheckBox( decoder );
			
			if( getConfig().getAuxDecoders().contains( decoder ) )
			{
				control.setSelected( true );
			}
			
			add( control, "wrap" );

			mControls.put( decoder, control );
		}
	}

	public AuxDecodeConfiguration getConfig()
	{
		return getChannelNode().getChannel().getAuxDecodeConfiguration();
	}

	@Override
    public void save()
    {
		getConfig().clearAuxDecoders();
		
		for( DecoderType decoder: mControls.keySet() )
		{
			if( mControls.get( decoder ) .isSelected() )
			{
				getConfig().addAuxDecoder( decoder );
			}
		}
    }

	@Override
    public void reset()
    {
        SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
                for( DecoderType decoder: mControls.keySet() )
        		{
        			mControls.get( decoder ).setSelected( 
        					getConfig().getAuxDecoders().contains( decoder ) );
        		}
            }
        });

    }
	
	public class AuxDecoderCheckBox extends JCheckBox
	{
		private DecoderType mDecoderType;
		
		public AuxDecoderCheckBox( DecoderType decoder )
		{
			super( decoder.getDisplayString() );
			
			mDecoderType = decoder;
		}
	}
}
