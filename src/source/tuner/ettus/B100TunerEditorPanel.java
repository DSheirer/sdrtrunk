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
package source.tuner.ettus;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;

public class B100TunerEditorPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    
    private B100Tuner mTuner;
    private SettingsManager mSettingsManager;

	public B100TunerEditorPanel( B100Tuner tuner, SettingsManager settingsManager )
	{
		mTuner = tuner;
		mSettingsManager = settingsManager;
		
		init();
	}

    private void init()
    {
        setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill]" ) );
        
        setBorder( BorderFactory.createTitledBorder( "Ettus B100 Tuner" ) );
        
        add( new JLabel( "Note: SDRTrunk can only recognize the B100 at this time.  It cannot use this tuner") );
    }
}
