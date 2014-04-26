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
package source.tuner.rtl;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import source.tuner.rtl.RTL2832TunerController.Descriptor;

public class RTL2832InfoPanel extends JPanel
{
	RTL2832TunerController mController;
	
	public RTL2832InfoPanel( RTL2832TunerController controller )
	{
		mController = controller;
		
		init();
	}
	
	public void dispose()
	{
		mController = null;
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][][][][grow]" ) );
		
		Descriptor descriptor = mController.getDescriptor();
		
		add( new JLabel( "RTL-2832 EEPROM Descriptor" ), "span,align center" );
		
		if( descriptor == null )
		{
			add( new JLabel( "No descriptor" ), "wrap" ) ;
		}
		else
		{
			add( new JLabel( "USB ID:" ) ) ;
			add( new JLabel( descriptor.getVendorID() + ":" + 
							 descriptor.getProductID() ), "wrap" ) ;

			add( new JLabel( "Vendor:" ) ) ;
			add( new JLabel( descriptor.getVendorLabel() ), "wrap" ) ;

			add( new JLabel( "Product:" ) ) ;
			add( new JLabel( descriptor.getProductLabel() ), "wrap" ) ;
			
			add( new JLabel( "Serial:" ) ) ;
			add( new JLabel( descriptor.getSerial() ), "wrap" ) ;

			add( new JLabel( "IR Enabled:" ) ) ;
			add( new JLabel( ( descriptor.irEnabled() ? "Yes" : "No" ) ), "wrap" ) ;
			
			add( new JLabel( "Remote Wake:" ) ) ;
			add( new JLabel( ( descriptor.remoteWakeupEnabled() ? "Yes" : "No" ) ), "wrap" ) ;
		}
	}
}
