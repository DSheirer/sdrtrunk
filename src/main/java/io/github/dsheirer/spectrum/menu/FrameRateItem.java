/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.spectrum.ComplexDftProcessor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FrameRateItem extends JCheckBoxMenuItem
{
    private static final long serialVersionUID = 1L;

    private ComplexDftProcessor mComplexDftProcessor;
    private int mFrameRate;
    
    public FrameRateItem(ComplexDftProcessor processor, int frameRate )
    {
    	super( String.valueOf( frameRate ) );
    	
    	mComplexDftProcessor = processor;
    	mFrameRate = frameRate;

    	if( processor.getFrameRate() == mFrameRate )
    	{
    		setSelected( true );
    	}
    	
    	addActionListener( new ActionListener() 
    	{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				mComplexDftProcessor.setFrameRate( mFrameRate );
            }
		} );
    }
}

