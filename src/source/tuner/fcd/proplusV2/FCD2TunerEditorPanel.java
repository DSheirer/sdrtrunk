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
package source.tuner.fcd.proplusV2;

import gui.control.JFrequencyControl;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.FCDTunerDetailsPanel;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeProcessor;

import com.jidesoft.swing.JideTabbedPane;

public class FCD2TunerEditorPanel extends JPanel implements IFrequencyChangeProcessor
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FCD2TunerEditorPanel.class );
    private static final long serialVersionUID = 1L;
    
    private FCDTuner mTuner;
    private FCD2TunerController mController;
    private SettingsManager mSettingsManager;

    private JFrequencyControl mFrequencyControl;

    public FCD2TunerEditorPanel( FCDTuner tuner, SettingsManager settingsManager )
    {
    	mTuner = tuner;
        mController = (FCD2TunerController)mTuner.getController();
        mSettingsManager = settingsManager;
        
        initGUI();
    }

    private void initGUI()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][grow]" ) );
        
		add( new JLabel( "Funcube Dongle Pro Plus Tuner" ), "span,align center" );

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
        
        tabs.add( "Configuration", new FCD2TunerConfigurationPanel( 
				mSettingsManager, mController ) );
        
        tabs.add( "Info", new FCDTunerDetailsPanel( mController ) );       

        add( tabs, "span,grow,push" );
    }

	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		if( event.getEvent() == Event.NOTIFICATION_FREQUENCY_CHANGE )
		{
			//Disregard the bandwidth, which shouldn't be changing
			try
	        {
		        mController.setFrequency( event.getValue().longValue() );
	        }
	        catch ( SourceException e )
	        {
	        	mLog.error( "FCDProController - error setting frequency [" + 
	        			event.getValue().longValue() + "]", e );
	        }
		}
    }
}
