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
package alias.uniqueID;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import alias.AliasIDNode;
import alias.AliasNode;

public class UniqueIDNode extends AliasIDNode
{
    private static final long serialVersionUID = 1L;
    
    public UniqueIDNode( UniqueID id )
	{
    	super( id );
	}
    
    @Override
    public JPanel getEditor()
    {
        return new UniqueIDEditor( this );
    }
    
    public UniqueID getUniqueID()
    {
    	return (UniqueID)getUserObject();
    }
    
    public String toString()
    {
    	return "Unique ID " + getUniqueID().getUid();
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();
		
		JMenuItem addSystemItem = new JMenuItem( "Delete" );
		addSystemItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				int n = JOptionPane.showConfirmDialog( getModel().getTree(),
				    "Are you sure you want to permanently delete this node?" );				
				
				if( n == JOptionPane.YES_OPTION )
				{
					AliasNode parent = (AliasNode)getParent();
					
					parent.getAlias().removeAliasID( getUniqueID() );

					save();

					getModel().removeNodeFromParent( UniqueIDNode.this );
				}
            }
		} );
		
		retVal.add( addSystemItem );
		
		return retVal;
	}
}
