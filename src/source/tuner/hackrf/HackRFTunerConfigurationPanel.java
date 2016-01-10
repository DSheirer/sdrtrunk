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
package source.tuner.hackrf;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;
import source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;

public class HackRFTunerConfigurationPanel extends JPanel
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( HackRFTunerConfigurationPanel.class );

	private static final long serialVersionUID = 1L;
    
    private SettingsManager mSettingsManager;
    private HackRFTunerController mController;
    private HackRFTunerConfiguration mSelectedConfig;

    private JButton mNewConfiguration;
    private JButton mDeleteConfiguration;
    private JComboBox<HackRFTunerConfiguration> mComboConfigurations;

    private JTextField mName;

    private JSpinner mFrequencyCorrection;
    private JToggleButton mAmplifier;
    private JComboBox<HackRFLNAGain> mComboLNAGain;
    private JComboBox<HackRFVGAGain> mComboVGAGain;
    
    private JComboBox<HackRFSampleRate> mComboSampleRate;
    
    public HackRFTunerConfigurationPanel( SettingsManager settingsManager,
    							 		  HackRFTunerController controller )
    {
    	mSettingsManager = settingsManager;
    	mController = controller;
    	
    	init();
    }
    
    /**
     * Initializes the gui components
     */
    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[grow,right][grow]", "[][][][][][][grow]" ) );

        /**
         * Tuner configuration selector
         */
        mComboConfigurations = new JComboBox<HackRFTunerConfiguration>();

        mComboConfigurations.setModel( getModel() );

        /* Determine which tuner configuration should be selected/displayed */
        TunerConfigurationAssignment savedConfig = null;
        String serial = null;
        
        try
        {
	        serial = mController.getSerial().getSerialNumber();
        }
        catch ( UsbException e2 )
        {
        	mLog.error( "couldn't read hackrf serial number", e2 );
        }
        
        if( serial != null )
        {
            savedConfig = mSettingsManager.getSelectedTunerConfiguration( 
    				TunerType.HACKRF, serial );
        }
        
        if( savedConfig != null )
        {
        	mSelectedConfig = getNamedConfiguration( 
        			savedConfig.getTunerConfigurationName() );
        }

        /* If we couldn't determine the saved/selected config, use the first one */
        if( mSelectedConfig == null )
        {
        	mSelectedConfig = mComboConfigurations.getItemAt( 0 );

        	//Store this config as the default for this tuner at this address
        	
        	if( serial != null )
        	{
            	mSettingsManager
        		.setSelectedTunerConfiguration( TunerType.HACKRF, 
    				serial, mSelectedConfig );
        	}
        }

        mComboConfigurations.setSelectedItem( mSelectedConfig );

		mComboConfigurations.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				HackRFTunerConfiguration selected = 
						(HackRFTunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					update( selected );
				}
           }
		});

		add( new JLabel( "Config:" ) );
		add( mComboConfigurations, "growx,push" );
		

		/**
		 * Tuner Configuration Name
		 */
		mName = new JTextField();
		mName.setText( mSelectedConfig.getName() );
		
		mName.addFocusListener( new FocusListener() 
		{
			@Override
            public void focusLost( FocusEvent e )
            {
				mSelectedConfig.setName( mName.getText() );
				save();
            }
			@Override
            public void focusGained( FocusEvent e ) {}
		} );
		
		add( new JLabel( "Name:" ) );
		add( mName, "growx,push" );
		
        /**
         * Frequency Correction
         */
        SpinnerModel model =
                new SpinnerNumberModel(     0.0,   //initial value
                                        -1000.0,   //min
                                         1000.0,   //max
                                            0.1 ); //step

        mFrequencyCorrection = new JSpinner( model );

        JSpinner.NumberEditor editor = 
        		(JSpinner.NumberEditor)mFrequencyCorrection.getEditor();  
        
        DecimalFormat format = editor.getFormat();  
        format.setMinimumFractionDigits( 1 );  
        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );          

        mFrequencyCorrection.setValue( mSelectedConfig.getFrequencyCorrection() );

        mFrequencyCorrection.addChangeListener( new ChangeListener() 
        {
			@Override
            public void stateChanged( ChangeEvent e )
            {
				final double value = ((SpinnerNumberModel)mFrequencyCorrection
						.getModel()).getNumber().doubleValue();

				EventQueue.invokeLater( new Runnable() 
				{
					@Override
                    public void run()
                    {
						try
		                {
			                mController.setFrequencyCorrection( value );
			                mSelectedConfig.setFrequencyCorrection( value );
			                save();
		                }
		                catch ( SourceException e1 )
		                {
		                	JOptionPane.showMessageDialog( 
		                			HackRFTunerConfigurationPanel.this, 
		                			"HackRF Tuner Controller - couldn't apply "
		                			+ "frequency correction value: " + value + 
		                					e1.getLocalizedMessage() );  
		                	
		                	mLog.error( "HackRF Tuner Controller - couldn't apply "
	                			+ "frequency correction value: " + value, e1 ); 
		                }
                    }
				} );
            }
        } );
        
        add( new JLabel( "Correction PPM:" ) );
        add( mFrequencyCorrection, "growx,push" );

        /**
         * Sample Rate
         */
        mComboSampleRate = new JComboBox<>( HackRFSampleRate.values() );
        mComboSampleRate.setSelectedItem( mSelectedConfig.getSampleRate() );
        mComboSampleRate.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent e )
            {
				HackRFSampleRate sampleRate = 
						(HackRFSampleRate)mComboSampleRate.getSelectedItem();
				try
                {

					mController.setSampleRate( sampleRate );
					
					mSelectedConfig.setSampleRate( sampleRate );
	                save();
                }
                catch ( UsbException e2 )
                {
                	JOptionPane.showMessageDialog( 
                			HackRFTunerConfigurationPanel.this, 
                			"HackRF Tuner Controller - couldn't apply the sample "
                			+ "rate setting [" + sampleRate.getLabel() + "] " + 
                					e2.getLocalizedMessage() );  
                	
                	mLog.error( "HackRF Tuner Controller - couldn't apply sample "
            			+ "rate setting [" + sampleRate.getLabel() + "]", e );
                }
            }
        } );
        add( new JLabel( "Sample Rate:" ) );
        add( mComboSampleRate, "growx,push" );
        
        /**
         * Gain Controls 
         */
        JPanel gainPanel = new JPanel();
        gainPanel.setLayout( new MigLayout( "", 
        		"[][right][grow,fill][right][grow,fill]", "[grow,fill]" ) );
        gainPanel.setBorder( BorderFactory.createTitledBorder( "Gain" ) );

        /* Amplifier */
        mAmplifier = new JToggleButton( "Amp" );
        mAmplifier.setSelected( mSelectedConfig.getAmplifierEnabled() );
        mAmplifier.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
	                mController.setAmplifierEnabled( mAmplifier.isSelected() );
					mSelectedConfig.setAmplifierEnabled( mAmplifier.isSelected() );
					save();
                }
                catch ( UsbException e )
                {
                	mLog.error( "couldn't enable/disable amplifier", e );
                	mAmplifier.setEnabled( !mAmplifier.isSelected() );
                	
                	JOptionPane.showMessageDialog( 
            			HackRFTunerConfigurationPanel.this, 
            			"Couldn't change amplifier setting",  
            			"Error changing amplifier setting", 
            			JOptionPane.ERROR_MESSAGE );
                }
            }
        } );
        gainPanel.add( mAmplifier );
        
        /* LNA Gain Control */
        mComboLNAGain = new JComboBox<HackRFLNAGain>( HackRFLNAGain.values() );
        mComboLNAGain.setSelectedItem( mSelectedConfig.getLNAGain() );
        mComboLNAGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
					HackRFLNAGain lnaGain = 
							(HackRFLNAGain)mComboLNAGain.getSelectedItem();
					
					if ( lnaGain == null )
					{
						lnaGain = HackRFLNAGain.GAIN_0;
					}
					
					if( mComboLNAGain.isEnabled() )
					{
						mController.setLNAGain( lnaGain );
					}

					mSelectedConfig.setLNAGain( lnaGain );
					
	                save();
                }
                catch ( UsbException e )
                {
                	JOptionPane.showMessageDialog( 
                			HackRFTunerConfigurationPanel.this, 
                			"HackRF Tuner Controller - couldn't apply the LNA "
                			+ "gain setting - " + e.getLocalizedMessage() );  
                	
                	mLog.error( "HackRF Tuner Controller - couldn't apply LNA "
                			+ "gain setting - ", e );
                }
            }
        } );
        mComboLNAGain.setToolTipText( "<html>LNA Gain.  Adjust to set "
        		+ "the IF gain</html>" );
        gainPanel.add( new JLabel( "LNA" ) );
        gainPanel.add( mComboLNAGain );

        /* VGA Gain Control */
        mComboVGAGain = new JComboBox<HackRFVGAGain>( HackRFVGAGain.values() );
        mComboVGAGain.setSelectedItem( mSelectedConfig.getVGAGain() );
        mComboVGAGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
					HackRFVGAGain vgaGain = (HackRFVGAGain)mComboVGAGain.getSelectedItem();

					if( vgaGain == null )
					{
						vgaGain = HackRFVGAGain.GAIN_4;
					}
					
					if( mComboVGAGain.isEnabled() )
					{
						mController.setVGAGain( vgaGain );
					}
					
					mSelectedConfig.setVGAGain( vgaGain );
	                save();
                }
                catch ( UsbException e )
                {
                	JOptionPane.showMessageDialog( 
                			HackRFTunerConfigurationPanel.this, 
                			"HackRF Tuner Controller - couldn't apply the VGA "
                			+ "gain setting - " + e.getLocalizedMessage() );  
                	
                	mLog.error( "HackRF Tuner Controller - couldn't apply VGA "
                			+ "gain setting", e );
                }
            }
        } );
        mComboVGAGain.setToolTipText( "<html>VGA Gain.  Adjust to set the "
        		+ "baseband gain</html>" );
        gainPanel.add( new JLabel( "VGA" ) );
        gainPanel.add( mComboVGAGain, "wrap" );

        add( gainPanel, "span,growx" );

        /**
         * Create a new configuration
         */
        mNewConfiguration = new JButton( "New" );

		mNewConfiguration.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				TunerConfiguration config = 
						mSettingsManager
							.addNewTunerConfiguration( 
									TunerType.HACKRF, 
									"New Configuration" );
				
				mComboConfigurations.setModel( getModel() );
				
				mComboConfigurations.setSelectedItem( config );

				repaint();
            }
		} );
		
		add( mNewConfiguration, "growx,push" );

		/**
		 * Delete the currently selected configuration
		 */
		mDeleteConfiguration = new JButton( "Delete" );
		
		mDeleteConfiguration.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				HackRFTunerConfiguration selected = 
						(HackRFTunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					int n = JOptionPane.showConfirmDialog(
						    HackRFTunerConfigurationPanel.this,
						    "Are you sure you want to delete '"
					    		+ selected.getName() + "'?",
						    "Are you sure?",
						    JOptionPane.YES_NO_OPTION );

					if( n == JOptionPane.YES_OPTION )
					{
						mSettingsManager
							.deleteTunerConfiguration( selected );

						mComboConfigurations.setModel( getModel() );
						
						mComboConfigurations.setSelectedIndex( 0 );
						
						repaint();
					}
				}
            }
		} );

		add( mDeleteConfiguration, "growx,push,wrap" );
    }

    /**
     * Updates gui controls with the values from the tuner configuration
     * @param config - tuner configuration
     */
    private void update( HackRFTunerConfiguration config )
    {
    	mSelectedConfig = config;
    	
		try
        {
	        mController.apply( config );
	        
	        mName.setText( config.getName() );
	        mFrequencyCorrection.setValue( config.getFrequencyCorrection() );

	        mComboLNAGain.setSelectedItem( mSelectedConfig.getLNAGain() );
	        mComboVGAGain.setSelectedItem( mSelectedConfig.getVGAGain() );

	        mComboSampleRate.setSelectedItem( mSelectedConfig.getSampleRate() );

	        mSettingsManager.setSelectedTunerConfiguration( 
			TunerType.HACKRF, mController.getSerial().getSerialNumber(), config );
        }
        catch ( UsbException | SourceException e1 )
        {
        	JOptionPane.showMessageDialog( 
        			HackRFTunerConfigurationPanel.this, 
        			"HackRF Tuner Controller - couldn't "
        			+ "apply the tuner configuration settings - " + 
        					e1.getLocalizedMessage() );  
        	
        	mLog.error( "HackRF Tuner Controller - couldn't apply "
        			+ "config [" + config.getName() + "]", e1 );
        }
    }
    
    /**
     * Constructs a combo box model for the tuner configuration combo component
     * by assembling a list of the tuner configurations appropriate for this
     * specific tuner
     */
    private ComboBoxModel<HackRFTunerConfiguration> getModel()
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager
    			.getTunerConfigurations( TunerType.HACKRF );
    	
    	DefaultComboBoxModel<HackRFTunerConfiguration> model = 
    			new DefaultComboBoxModel<HackRFTunerConfiguration>();
    	
    	for( TunerConfiguration config: configs )
    	{
    		model.addElement( (HackRFTunerConfiguration)config );
    	}
    	
    	return model;
    }

    /**
     * Retrieves a specific (named) tuner configuration
     */
    private HackRFTunerConfiguration getNamedConfiguration( String name )
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager
    			.getTunerConfigurations( TunerType.HACKRF );
    	
    	for( TunerConfiguration config: configs )
    	{
    		if( config.getName().contentEquals( name ) )
    		{
    			return (HackRFTunerConfiguration)config;
    		}
    	}

    	return null;
    }
    
    /**
     * Saves current tuner configuration settings
     */
    private void save()
    {
    	mSettingsManager.save();
    }
}
