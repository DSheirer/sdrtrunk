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


import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class OldConfigurationEditor extends JPanel 
											implements TreeSelectionListener
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Host for configuration editor display panels
     */
	public OldConfigurationEditor()
	{
		init();
	}

	public void init()
	{
		setPanel( new EmptyEditor() );
	}
	
	public void setPanel( JPanel display )
	{
		removeAll();
		
        setLayout( new BorderLayout() );
		
		JScrollPane scroll = new JScrollPane( display );
		
		add( scroll, BorderLayout.CENTER );

		revalidate();
		repaint();
	}

	@Override
    public void valueChanged( TreeSelectionEvent e )
    {
		JTree tree = (JTree)e.getSource();
		
		BaseNode node = 
				(BaseNode)tree.getLastSelectedPathComponent();

    	if( node != null )
    	{
    	    setPanel( node.getEditor() );
    	}
    }
}
