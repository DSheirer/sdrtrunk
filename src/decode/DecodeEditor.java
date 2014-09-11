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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import controller.Editor;
import decode.config.DecodeConfiguration;

public class DecodeEditor extends Editor
{
    private static final long serialVersionUID = 1L;

    protected DecodeConfiguration mConfig;
    
    protected JCheckBox mAFC;

	public DecodeEditor( DecodeConfiguration config )
	{
		mConfig = config;

		mAFC = new JCheckBox( "Automatic Frequency Control" );
		mAFC.setSelected( mConfig.isAFCEnabled() );
		mAFC.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent arg0 )
			{
				mConfig.setAFC( mAFC.isSelected() );				
			}
		} );

		add( mAFC, "wrap" );
	}

	public DecodeConfiguration getConfig()
	{
		return mConfig;
	}

	@Override
    public void save()
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void reset()
    {
	    // TODO Auto-generated method stub
	    
    }
}
