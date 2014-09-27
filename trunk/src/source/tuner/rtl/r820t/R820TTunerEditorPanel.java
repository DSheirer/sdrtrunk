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

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceException;
import source.tuner.FrequencyChangeEvent;
import source.tuner.FrequencyChangeListener;
import source.tuner.FrequencyChangeEvent.Attribute;
import source.tuner.rtl.RTL2832InfoPanel;

import com.jidesoft.swing.JideTabbedPane;

import controller.ResourceManager;

public class R820TTunerEditorPanel extends JPanel implements FrequencyChangeListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( R820TTunerEditorPanel.class );

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
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow]", "[][][grow]" ) );
		
		add( new JLabel( "RTL2832 with R820T Tuner" ), "span 2, align center, wrap" );

        mFrequencyControl = new JFrequencyControl();
        
        mFrequencyControl.addListener( this );
        
        /* Add frequency control as frequency change listener. */
        mController.addListener( mFrequencyControl );
        
        mFrequencyControl.setFrequency( mController.getFrequency(), false );

        add( mFrequencyControl, "span 2, align center, wrap" );
        
		JideTabbedPane tabs = new JideTabbedPane();
        tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );
		
		tabs.addTab( "Configuration", 
			new R820TTunerConfigurationPanel( mResourceManager, mController ) );

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
		if( event.getAttribute() == Attribute.FREQUENCY )
		{
			final int frequency = (int)event.getValue();
			
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
			        	mLog.error( "R820T Tuner Controller - error setting frequency [" + 
			        			frequency + "]", e );
			        }
	            }
			} );
		}
    }
}
