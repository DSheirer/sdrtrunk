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
package decode.ltrstandard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import message.MessageDirection;
import decode.DecodeEditor;
import decode.config.DecodeConfigLTRStandard;
import decode.config.DecodeConfiguration;

public class LTRStandardEditor extends DecodeEditor
{
    private static final long serialVersionUID = 1L;

    private JComboBox<MessageDirection> mComboDirection;
    
	public LTRStandardEditor( DecodeConfiguration config )
	{
		super( config );
		
		initGUI();
	}
	
	private void initGUI()
	{
		mComboDirection = new JComboBox<MessageDirection>();

		mComboDirection.setModel( 
				new DefaultComboBoxModel<MessageDirection>( MessageDirection.values() ) );
		
		mComboDirection.setSelectedItem( ((DecodeConfigLTRStandard)mConfig).getMessageDirection() );
		
		mComboDirection.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				MessageDirection selected = mComboDirection
						.getItemAt( mComboDirection.getSelectedIndex() );
				
				if( selected != null )
				{
					((DecodeConfigLTRStandard)mConfig).setMessageDirection( selected );
				}
            }
		} );
		
		add( mComboDirection, "span 2" );
	}
}
