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
package source.tuner.fcd;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import source.tuner.rtl.RTL2832TunerController.Descriptor;
import net.miginfocom.swing.MigLayout;

public class FCDTunerDetailsPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private JLabel mAddress;
    private JLabel mCellBlock;
    private JLabel mFirmware;

    public FCDTunerDetailsPanel( FCDTunerController controller )
    {
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "USB Address:" ) );
		add( new JLabel( controller.getAddress() ) );
        
		add( new JLabel( "Cellular Band:" ) );
		add( new JLabel( controller.getConfiguration().getBandBlocking()
				.getLabel() ) );

		add( new JLabel( "Firmware:" ) );
		add( new JLabel( controller.getConfiguration().getFirmware() ) );
    }
}
