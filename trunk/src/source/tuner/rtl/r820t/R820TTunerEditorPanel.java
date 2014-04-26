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
package source.tuner.rtl.r820t;

import gui.control.JFrequencyControl;

import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import log.Log;
import net.miginfocom.swing.MigLayout;
import source.SourceException;
import source.tuner.FrequencyChangeListener;
import controller.ResourceManager;

public class R820TTunerEditorPanel extends JPanel implements FrequencyChangeListener
{
    private static final long serialVersionUID = 1L;
    
    private R820TTunerController mController;
    private ResourceManager mResourceManager;

    private JFrequencyControl mFrequencyControl;

    public R820TTunerEditorPanel( R820TTunerController controller, 
    							ResourceManager resourceManager )
    {
    	mController = controller;
        mResourceManager = resourceManager;
        
        initGUI();
    }

    private void initGUI()
    {
        setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill]" ) );
        
        setBorder( BorderFactory.createTitledBorder( "Rafael Micro R820T Tuner" ) );

        mFrequencyControl = new JFrequencyControl();
        
        mFrequencyControl.addListener( this );
        
        mFrequencyControl.setFrequency( mController.getFrequency(), false );

        add( mFrequencyControl, "wrap" );
        
        add( new R820TTunerConfigurationPanel( mResourceManager, mController ), 
        	 "wrap" );
    }

    /**
     * Frequency change listener for the frequency control gui component to 
     * apply end-user requested frequency changes against the tuner
     */
	@Override
    public void frequencyChanged( final int frequency, int bandwidth )
    {
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				try
		        {
			        mController.setFrequency( frequency );
		        }
		        catch ( SourceException e )
		        {
		        	Log.error( "E4KTunerController - error setting frequency [" + 
		        			frequency + "] - " + e.getLocalizedMessage() );
		        }
            }
		} );
    }
}
