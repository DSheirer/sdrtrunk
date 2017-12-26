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
package io.github.dsheirer.module.decode.am;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EditorValidationException;
import io.github.dsheirer.gui.editor.ValidatingEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class AMDecoderEditor extends ValidatingEditor<Channel>
{
    private static final long serialVersionUID = 1L;
    
	public AMDecoderEditor()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "",	"" ) );
		add( new JLabel( "AM Decoder" ) );
	}

	@Override
	public void validate( Editor<Channel> editor ) throws EditorValidationException
	{
	}

	@Override
	public void setItem( Channel item )
	{
		super.setItem( item );

		if( hasItem() )
		{
			if( getItem().getDecodeConfiguration() instanceof DecodeConfigAM )
			{
				setModified( false );
			}
			else
			{
				setModified( true );
			}
		}
		else
		{
			setModified( false );
		}
	}

	@Override
	public void save()
	{
		if( hasItem() && isModified() )
		{
			getItem().setDecodeConfiguration( new DecodeConfigAM() );
		}
		
		setModified( false );
	}
}
