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
package controller;

import java.awt.Component;
import java.awt.Image;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class ControllerTreeCellRenderer extends DefaultTreeCellRenderer
{
    private static final long serialVersionUID = 1L;

    public static final int MAX_ICON_HEIGHT = 16;
    private HashMap<String,ImageIcon> mIcons = new HashMap<String,ImageIcon>();
    
    public ControllerTreeCellRenderer()
    {
    }

    @Override
    public Component getTreeCellRendererComponent( JTree tree,
    		Object value, boolean selected, boolean expanded, boolean isLeaf,
    		int row, boolean focused )
    {
    	Component c = super.getTreeCellRendererComponent( tree, value, selected, 
    			expanded, isLeaf, row, focused );
    	
    	if( value instanceof BaseNode )
    	{
    		ImageIcon icon = getIcon( (BaseNode)value );
    		
    		if( icon != null )
    		{
    			if( icon.getIconHeight() > 16 )
    			{
    				
    			}
    			else
    			{
            		setIcon( icon );
    			}
    		}
    	}
    	
    	return c;
    }
    
    private ImageIcon getIcon( BaseNode node )
    {
    	ImageIcon icon = null;
    	
    	String filepath = node.getIconPath();
    	
    	if( filepath != null )
    	{
        	if( mIcons.containsKey( filepath ) )
        	{
        		icon = mIcons.get( filepath );
        	}
        	else
        	{
            	icon = new ImageIcon( filepath );
            	
            	if( icon.getIconHeight() > MAX_ICON_HEIGHT )
            	{
            		Image image = icon.getImage();  
            		
            		double scale = (double)icon.getIconHeight() / (double)MAX_ICON_HEIGHT;
            		
            		int scaledWidth = (int)( (double)icon.getIconWidth() / scale );
            		
            		Image scaledImage = image.getScaledInstance( scaledWidth, 
            				MAX_ICON_HEIGHT, java.awt.Image.SCALE_SMOOTH );  
            		
                	mIcons.put( filepath, new ImageIcon( scaledImage ) );
            	}
            	else
            	{
                	mIcons.put( filepath, icon );
            	}
            	
        	}
    	}
    	
    	return icon;
    }
}
