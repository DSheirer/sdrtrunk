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
package source.tuner.rtl.e4k;

import gui.control.JFrequencyControl;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeProcessor;
import source.tuner.rtl.RTL2832InfoPanel;

import com.jidesoft.swing.JideTabbedPane;

public class E4KTunerEditorPanel extends JPanel implements IFrequencyChangeProcessor
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( E4KTunerEditorPanel.class );
	
    private static final long serialVersionUID = 1L;
    
    private E4KTunerController mController;
    private SettingsManager mSettingsManager;

    private JFrequencyControl mFrequencyControl;

    public E4KTunerEditorPanel( E4KTunerController controller, 
    							SettingsManager settingsManager )
    {
    	mController = controller;
        mSettingsManager = settingsManager;
        
        initGUI();
    }

    private void initGUI()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow]", "[][][grow]" ) );
		
		add( new JLabel( "RTL2832 with E4000 Tuner" ), "span 2, align center, wrap" );

        mFrequencyControl = new JFrequencyControl();
        
        mFrequencyControl.addListener( this );
        
        /* Add frequency control as listener on the controller, to maintain
         * sync with the tuned frequency value */
        mController.addListener( mFrequencyControl );
        
        mFrequencyControl.setFrequency( mController.getFrequency(), false );

        add( mFrequencyControl, "span 2, align center, wrap" );
        
		JideTabbedPane tabs = new JideTabbedPane();
        tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );
		
		tabs.addTab( "Configuration", 
			new E4KTunerConfigurationPanel( mSettingsManager, mController ) );

		tabs.addTab( "Info", new RTL2832InfoPanel( mController ) );
		
		add( tabs, "grow,push,span" );
    }

    /**
     * Frequency change listener for the frequency control gui component to 
     * apply end-user requested frequency changes against the tuner
     */
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		if( event.getEvent() == Event.NOTIFICATION_FREQUENCY_CHANGE )
		{
			final long frequency = event.getValue().longValue();

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
			        	mLog.error( "E4KTunerController - error setting frequency [" + 
			        			frequency + "]", e );
			        }
	            }
			} );
		}
    }
}
