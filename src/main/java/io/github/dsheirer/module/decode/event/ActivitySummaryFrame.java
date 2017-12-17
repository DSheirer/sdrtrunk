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
package io.github.dsheirer.module.decode.event;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ActivitySummaryFrame extends JFrame
{
    private static final long serialVersionUID = 1L;

    public ActivitySummaryFrame( String summary )
    {
    	this( summary, null );
    }
    
	public ActivitySummaryFrame( String summary, Component displayOver )
	{
		setTitle( "Activity Summary" );
		setLocationRelativeTo( displayOver );
		setSize( 400, 400 );
    	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    	setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill][]" ) );

    	JTextArea summaryText = new JTextArea( summary );
		
		JScrollPane scroller = new JScrollPane( summaryText );
		scroller.setViewportView( summaryText );
		
		add( scroller, "wrap" );
		
		JButton close = new JButton( "Close" );
		close.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				dispose();
            }
		} );
		
		add( close );

		EventQueue.invokeLater( new Runnable() 
        {
            public void run()
            {
        		setVisible( true );
            }
        } );
	}
}
