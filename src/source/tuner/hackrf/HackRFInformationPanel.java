package source.tuner.hackrf;

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

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.hackrf.HackRFTunerController.Serial;

public class HackRFInformationPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

	private final static Logger mLog = 
			LoggerFactory.getLogger( HackRFInformationPanel.class );

	private HackRFTunerController mController;

	/**
	 * Displays HackRF firmware, serial number and part identifier 
	 */
	public HackRFInformationPanel( HackRFTunerController controller )
	{
		mController = controller;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", 
				  				  "[right,grow][grow]", 
								  "[][][][grow]" ) );

		Serial serial = null;
		
		try
		{
			serial = mController.getSerial();
		}
		catch( Exception e )
		{
			mLog.error( "couldn't read HackRF serial number", e );
		}
		
		if( serial != null )
		{
			add( new JLabel( "Serial:" ) );
			add( new JLabel( serial.getSerialNumber() ) );
			add( new JLabel( "Part:" ) );
			add( new JLabel( serial.getPartID() ) );
		}
		else
		{
			add( new JLabel( "Serial:" ) );
			add( new JLabel( "Unknown" ) );
			add( new JLabel( "Part:" ) );
			add( new JLabel( "Unknown" ) );
		}
		
		String firmware = null;
		
		try
		{
			firmware = mController.getFirmwareVersion();
		}
		catch( Exception e )
		{
			mLog.error( "couldn't read HackRF firmware version", e );
		}
		
		add( new JLabel( "Firmware:" ) );
		
		if( firmware != null )
		{
			add( new JLabel( firmware ) );
		}
		else
		{
			add( new JLabel( "Unknown" ) );
		}
	}
}
