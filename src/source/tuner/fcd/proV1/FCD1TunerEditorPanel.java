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
package source.tuner.fcd.proV1;

import java.awt.Color;

import gui.control.JFrequencyControl;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jidesoft.swing.JideTabbedPane;

import log.Log;
import net.miginfocom.swing.MigLayout;
import source.SourceException;
import source.tuner.FrequencyChangeListener;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.FCDTunerDetailsPanel;
import controller.ResourceManager;

public class FCD1TunerEditorPanel extends JPanel implements FrequencyChangeListener
{
    private static final long serialVersionUID = 1L;
    
    private FCDTuner mTuner;
    private FCD1TunerController mController;
    private ResourceManager mResourceManager;

    private JFrequencyControl mFrequencyControl;

    public FCD1TunerEditorPanel( FCDTuner tuner, ResourceManager resourceManager )
    {
    	mTuner = tuner;
        mController = (FCD1TunerController)mTuner.getController();
        mResourceManager = resourceManager;
        
        initGUI();
    }

    private void initGUI()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[grow,right][grow]", "[][][grow]" ) );
        
		add( new JLabel( "Funcube Dongle Pro Tuner" ), "span, align center" );

        mFrequencyControl = new JFrequencyControl();
        
        mFrequencyControl.addListener( this );
        
        /* Add frequency control as listener to tuner to get frequency changes
         * that are invoked elsewhere, to keep the control in sync with the
         * frequency */
        mTuner.addListener( mFrequencyControl );
        
        mFrequencyControl.setFrequency( mController.getFrequency(), false );

        add( mFrequencyControl, "span,align center" );
        
        JideTabbedPane tabs = new JideTabbedPane();
        tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

        tabs.add( "Configuration", new FCD1TunerConfigurationPanel( 
        				mResourceManager, mController ) );
        
        tabs.add( "Info", new FCDTunerDetailsPanel( mController ) );       

        add( tabs, "span,push,grow" );
    }

	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		//Disregard the bandwidth value, which shouldn't be changing
		try
        {
	        mController.setFrequency( frequency );
        }
        catch ( SourceException e )
        {
        	Log.error( "FCTProController - error setting frequency [" + 
        			frequency + "] - " + e.getLocalizedMessage() );
        }
    }
}
