package source.tuner.airspy;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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

public class AirspyInformationPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

	private AirspyTunerController mController;

	/**
	 * Displays Airspy firmware, serial number and part identifier 
	 */
	public AirspyInformationPanel( AirspyTunerController controller )
	{
		mController = controller;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", 
				  				  "[right,grow][grow]", 
								  "[][][][][grow]" ) );

		AirspyDeviceInformation info = mController.getDeviceInfo();
		
		add( new JLabel( "Serial:" ) );
		add( new JLabel( info.getSerialNumber() ) );
		add( new JLabel( "Firmware:" ) );
		
		//Split the firmware on spaces and use first value -- expose full
		//string in a tool tip
		String[] firmware = info.getVersion().split( " " );
	
		JLabel firmwareLabel;
		
		if( firmware.length > 1 )
		{
			firmwareLabel = new JLabel( firmware[ 0 ] );
		}
		else
		{
			firmwareLabel = new JLabel( info.getVersion() );
		}
		
		firmwareLabel.setToolTipText( info.getVersion() );

		add( firmwareLabel );
		add( new JLabel( "Part:" ) );
		add( new JLabel( info.getPartNumber() ) );
		add( new JLabel( "Board ID:" ) );
		add( new JLabel( info.getBoardID().getLabel() ) );
	}
}
