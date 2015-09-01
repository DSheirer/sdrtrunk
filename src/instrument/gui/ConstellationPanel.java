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
package instrument.gui;

import gui.control.ConstellationViewer;

import java.awt.Dimension;

import javax.swing.JInternalFrame;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.Complex;
import source.wave.WaveSource.PositionListener;

public class ConstellationPanel extends JInternalFrame 
					  implements PositionListener, Listener<Complex>
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( ConstellationPanel.class );
	
	private ConstellationViewer mConstellation = 
							new ConstellationViewer( 48000, 4800 );

	public ConstellationPanel()
	{
		initGui();
	}
	
	private void initGui()
	{
        setLayout( new MigLayout( "insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]" ) );

		setTitle( "Constellation View" );
		setPreferredSize( new Dimension( 300, 300 ) );
		setSize( 300, 300 );

		setResizable( true );
		setClosable( true );
		setIconifiable( true );
		setMaximizable( false );
		
		add( mConstellation );
	}
	
	@Override
    public void positionUpdated( long position, boolean reset )
    {
    }

	@Override
	public void receive( Complex sample )
	{
		mConstellation.receive( sample );
	}
}
