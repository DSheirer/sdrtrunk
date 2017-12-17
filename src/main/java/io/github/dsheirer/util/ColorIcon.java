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
package io.github.dsheirer.util;

import javax.swing.*;
import java.awt.*;

public class ColorIcon implements Icon 
{
	private static final int sSIZE = 12;
	private static final int sARC_SIZE = 3;
	private Color mColor;

    public ColorIcon( Color color ) 
    {
        mColor = color;
    }
    
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) 
    {
        Color temp = g.getColor();
        
        g.setColor( mColor );

        g.fillRoundRect( x, y, sSIZE, sSIZE, sARC_SIZE, sARC_SIZE );
        
        g.setColor( temp );
    }

	@Override
    public int getIconWidth()
    {
	    return sSIZE;
    }

	@Override
    public int getIconHeight()
    {
	    return sSIZE;
    }
}
