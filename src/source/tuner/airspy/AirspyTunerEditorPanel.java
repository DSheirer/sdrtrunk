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
package source.tuner.airspy;

import gui.control.JFrequencyControl;

import java.awt.Color;

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

import com.jidesoft.swing.JideTabbedPane;

public class AirspyTunerEditorPanel extends JPanel 
						implements IFrequencyChangeProcessor
{
    private static final long serialVersionUID = 1L;

	private final static Logger mLog = 
			LoggerFactory.getLogger( AirspyTunerEditorPanel.class );

	private JFrequencyControl mFrequencyControl;
    
    private AirspyTuner mTuner;
    private AirspyTunerController mController;
    private SettingsManager mSettingsManager;

	public AirspyTunerEditorPanel( AirspyTuner tuner, 
								   SettingsManager settingsManager )
	{
		mTuner = tuner;
		mController = mTuner.getController();
		mSettingsManager = settingsManager;
		
		initGUI();
	}

    private void initGUI()
    {
		setLayout( new MigLayout( "fill,wrap 2", 
								  "[right,grow][grow]", 
								  "[][][][][][grow]" ) );
		
		add( new JLabel( "Airspy Tuner" ), "span,align center" );
        
        mFrequencyControl = new JFrequencyControl();
        
        mFrequencyControl.addListener( this );
        
        /* Add frequency control as frequency change listener.  This creates a
         * feedback loop, so the control does not rebroadcast the event */
        mTuner.addFrequencyChangeProcessor( mFrequencyControl );
        
        mFrequencyControl.setFrequency( mController.getFrequency(), false );

        add( mFrequencyControl, "span,align center" );

        JideTabbedPane tabs = new JideTabbedPane();
        tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );
        
        add( tabs, "span,grow,push" );
        
        tabs.add( "Config", new AirspyTunerConfigurationPanel( 
        		mSettingsManager, mTuner.getController() ) );
        
        tabs.add( "Info", new AirspyInformationPanel( mTuner.getController() ) ); 
    }

	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		if( event.getEvent() == Event.NOTIFICATION_FREQUENCY_CHANGE )
		{
			try
	        {
		        mController.setFrequency( event.getValue().longValue() );
	        }
	        catch ( SourceException e )
	        {
	        	mLog.error( "error setting frequency [" + 
	        			event.getValue().longValue() + "]", e );
	        }
		}
    }
}
