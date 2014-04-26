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
package controller.system;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import controller.ConfigurableNode;

public class SystemListNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

    public SystemListNode( SystemList list )
	{
        super( list );
	}

    public SystemList getSystemList()
    {
        return (SystemList)getUserObject();
    }

    public void init()
    {
    	for( System system: getSystemList().getSystem() )
    	{
    		SystemNode node = new SystemNode( system );
    		
    		getModel().addNode( node, SystemListNode.this, getChildCount() );
    		
    		node.init();
    	}
    	
    	sort();
    }

    public String toString()
    {
    	return "Systems [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();
		
		JMenuItem addSystemItem = new JMenuItem( "Add System" );
		addSystemItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
			    System system = new System();

			    getSystemList().addSystem( system );
			    
				getModel().addNode( new SystemNode( system ), 
									SystemListNode.this, 
									SystemListNode.this.getChildCount() );
            }
		} );
		
		retVal.add( addSystemItem );

		return retVal;
	}
}
