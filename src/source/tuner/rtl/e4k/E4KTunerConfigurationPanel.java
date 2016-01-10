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
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;
import source.tuner.rtl.RTL2832TunerController.SampleRate;
import source.tuner.rtl.e4k.E4KTunerController.E4KEnhanceGain;
import source.tuner.rtl.e4k.E4KTunerController.E4KGain;
import source.tuner.rtl.e4k.E4KTunerController.E4KLNAGain;
import source.tuner.rtl.e4k.E4KTunerController.E4KMixerGain;

public class E4KTunerConfigurationPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( E4KTunerConfigurationPanel.class );

	private SettingsManager mSettingsManager;
    private E4KTunerController mController;
    private E4KTunerConfiguration mSelectedConfig;

    private JButton mNewConfiguration;
    private JButton mDeleteConfiguration;
    private JComboBox<E4KTunerConfiguration> mComboConfigurations;

    private JTextField mName;

    private JSpinner mFrequencyCorrection;
    private JSpinner mSampleRateCorrection;
    private JComboBox<E4KGain> mComboMasterGain;
    private JComboBox<E4KMixerGain> mComboMixerGain;
    private JComboBox<E4KLNAGain> mComboLNAGain;
    private JComboBox<E4KEnhanceGain> mComboEnhanceGain;
    
    private JComboBox<SampleRate> mComboSampleRate;
    
    public E4KTunerConfigurationPanel( SettingsManager settingsManager,
    								   E4KTunerController controller )
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
        mComboConfigurations = new JComboBox<E4KTunerConfiguration>();

        mComboConfigurations.setModel( getModel() );

        /* Determine which tuner configuration should be selected/displayed */
        TunerConfigurationAssignment savedConfig = mSettingsManager
        		.getSelectedTunerConfiguration( 
        				TunerType.ELONICS_E4000, mController.getUniqueID() );
        
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
        	mSettingsManager
        		.setSelectedTunerConfiguration( 
        				TunerType.ELONICS_E4000, 
        				mController.getUniqueID(), mSelectedConfig );
        }

        mComboConfigurations.setSelectedItem( mSelectedConfig );

		mComboConfigurations.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				E4KTunerConfiguration selected = 
						(E4KTunerConfiguration)mComboConfigurations
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
		add( new JLabel( "Name:" ) );
		mName = new JTextField();
		mName.setText( mSelectedConfig.getName() );
		
		mName.addFocusListener( new FocusListener() 
		{
			@Override
            public void focusLost( FocusEvent e )
            {
				/* Update the config name */
				mSelectedConfig.setName( mName.getText() );

				/* Reset this named config as the assigned config for this
				 * tuner */
	        	mSettingsManager
        		.setSelectedTunerConfiguration( 
        				TunerType.ELONICS_E4000, 
        				mController.getUniqueID(), mSelectedConfig );
				
				save();
            }
			@Override
            public void focusGained( FocusEvent e ) {}
		} );
		
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
		                			E4KTunerConfigurationPanel.this, 
		                			"E4K Tuner Controller - couldn't apply "
		                			+ "frequency correction value: " + value + 
		                					e1.getLocalizedMessage() );  
		                	
		                	mLog.error( "E4K Tuner Controller - couldn't apply "
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
        mComboSampleRate = new JComboBox<>( SampleRate.values() );
        mComboSampleRate.setSelectedItem( mSelectedConfig.getSampleRate() );
        mComboSampleRate.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent e )
            {
				SampleRate sampleRate = 
						(SampleRate)mComboSampleRate.getSelectedItem();
				try
                {

					mController.setSampleRate( sampleRate );
					
					mSelectedConfig.setSampleRate( sampleRate );
	                save();
                }
                catch ( SourceException | LibUsbException eSampleRate )
                {
                	JOptionPane.showMessageDialog( 
                			E4KTunerConfigurationPanel.this, 
                			"E4K Tuner Controller - couldn't apply the sample "
                			+ "rate setting [" + sampleRate.getLabel() + "] " + 
                					eSampleRate.getLocalizedMessage() );  
                	
                	mLog.error( "E4K Tuner Controller - couldn't apply sample "
                			+ "rate setting [" + sampleRate.getLabel() + "]", 
                			eSampleRate );
                }
            }
        } );
        add( new JLabel( "Sample Rate:" ) );
        add( mComboSampleRate, "growx,push" );
        
//        /**
//         * Sample Rate Correction
//         */
//        SpinnerModel sampleRateCorrectionModel =
//                new SpinnerNumberModel(     0,   //initial value
//                                        -32768,   //min
//                                         32767,   //max
//                                            1 ); //step
//
//        mSampleRateCorrection = new JSpinner( sampleRateCorrectionModel );
//
//        JSpinner.NumberEditor sampleRateCorrectionEditor = 
//        		(JSpinner.NumberEditor)mSampleRateCorrection.getEditor();  
//        
//        DecimalFormat sampleRateCorrectionFormat = 
//        						sampleRateCorrectionEditor.getFormat();  
//        
//        sampleRateCorrectionFormat.setMinimumFractionDigits( 0 );  
//        
//        sampleRateCorrectionEditor.getTextField()
//        			.setHorizontalAlignment( SwingConstants.CENTER );          
//
//        mSampleRateCorrection.addChangeListener( new ChangeListener() 
//        {
//			@Override
//            public void stateChanged( ChangeEvent e )
//            {
//				final int value = ((SpinnerNumberModel)mSampleRateCorrection
//						.getModel()).getNumber().intValue();
//
//				EventQueue.invokeLater( new Runnable() 
//				{
//					@Override
//                    public void run()
//                    {
//						try
//		                {
//			                mController.setSampleRateFrequencyCorrection( value );
//		                }
//		                catch ( SourceException | UsbException e1 )
//		                {
//		                	JOptionPane.showMessageDialog( 
//		                			E4KTunerConfigurationPanel.this, 
//		                			"E4K Tuner Controller - couldn't apply "
//		                			+ "sample rate correction value: " + value + 
//		                					e1.getLocalizedMessage() );  
//		                	
//		                	Log.error( "E4K Tuner Controller - couldn't apply "
//		                			+ "sample rate correction value: " + value + 
//		                			e1.getLocalizedMessage() );
//		                }
//                    }
//				} );
//            }
//        } );
//        
//        add( mSampleRateCorrection );
//        add( new JLabel( "Sample Rate Correction (ppm)" ), "wrap" );
        
        /**
         * Gain Controls 
         */
        JPanel gainPanel = new JPanel();
        gainPanel.setLayout( new MigLayout( "fill", "[right][left][right][left]", "" ) );
        gainPanel.setBorder( BorderFactory.createTitledBorder( "Gain" ) );
        
        /* Master Gain Control */
        mComboMasterGain = new JComboBox<E4KGain>( E4KGain.values() );
        mComboMasterGain.setSelectedItem( mSelectedConfig.getMasterGain() );
        
        mComboMasterGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
					E4KGain gain = (E4KGain)mComboMasterGain.getSelectedItem();
					
	                mController.setGain( (E4KGain)mComboMasterGain.getSelectedItem(), true );
	                
	                if( gain == E4KGain.MANUAL )
	                {
	                	mComboMixerGain.setSelectedItem( 
	                			mController.getMixerGain( true ) );
	                	mComboMixerGain.setEnabled( true );
	                	
	                	mComboLNAGain.setSelectedItem( 
	                			mController.getLNAGain( true ) );
	                	mComboLNAGain.setEnabled( true );

	                	mComboEnhanceGain.setEnabled( true );
	                	mComboEnhanceGain.setSelectedItem( 
	                			mController.getEnhanceGain( true ) );
	                }
	                else
	                {
	                	mComboMixerGain.setEnabled( false );
	                	mComboMixerGain.setSelectedItem( gain.getMixerGain() );

	                	mComboLNAGain.setEnabled( false );
	                	mComboLNAGain.setSelectedItem( gain.getLNAGain() );

	                	mComboEnhanceGain.setEnabled( false );
	                	mComboEnhanceGain.setSelectedItem( gain.getEnhanceGain() );
	                }
	                
	                mSelectedConfig.setMasterGain( gain );
	                save();
                }
                catch ( UsbException e )
                {
                	JOptionPane.showMessageDialog( 
                			E4KTunerConfigurationPanel.this, 
                			"E4K Tuner Controller - couldn't apply the gain "
                			+ "setting - " + e.getLocalizedMessage() );  
                	
                	mLog.error( "E4K Tuner Controller - couldn't apply "
                			+ "gain setting", e );
                }
            }
        } );
        mComboMasterGain.setToolTipText( "<html>Select <b>AUTOMATIC</b> for auto "
        		+ "gain, <b>MANUAL</b> to enable<br> independent control of "
        		+ "<i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>"
        		+ "settings, or one of the individual gain settings for<br>"
        		+ "semi-manual gain control</html>" );
        gainPanel.add( new JLabel( "Master" ) );
        gainPanel.add( mComboMasterGain );

        /* Mixer Gain Control */
        mComboMixerGain = new JComboBox<E4KMixerGain>( E4KMixerGain.values() );
        mComboMixerGain.setSelectedItem( mSelectedConfig.getMixerGain() );
        mComboMixerGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				EventQueue.invokeLater( new Runnable() 
				{
					@Override
                    public void run()
                    {
						try
		                {
							E4KMixerGain mixerGain = 
									(E4KMixerGain)mComboMixerGain.getSelectedItem();
							
							if( mComboMixerGain.isEnabled() )
							{
								mController.setMixerGain( mixerGain, true );
							}

							mSelectedConfig.setMixerGain( mixerGain );
			                save();
		                }
		                catch ( UsbException e )
		                {
		                	JOptionPane.showMessageDialog( 
		                			E4KTunerConfigurationPanel.this, 
		                			"E4K Tuner Controller - couldn't apply the mixer "
		                			+ "gain setting - " + e.getLocalizedMessage() );  
		                	
		                	mLog.error( "E4K Tuner Controller - couldn't apply mixer "
		                			+ "gain setting", e );
		                }
                    }
				} );
            }
        } );
        mComboMixerGain.setToolTipText( "<html>Mixer Gain.  Set master gain "
        		+ "to <b>MASTER</b> to enable adjustment</html>" );
        mComboMixerGain.setEnabled( false );
        gainPanel.add( new JLabel( "Mixer" ) );
        gainPanel.add( mComboMixerGain, "wrap" );

        /* LNA Gain Control */
        mComboLNAGain = new JComboBox<E4KLNAGain>( E4KLNAGain.values() );
        mComboLNAGain.setSelectedItem( mSelectedConfig.getLNAGain() );
        mComboLNAGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
					E4KLNAGain lnaGain = (E4KLNAGain)mComboLNAGain.getSelectedItem();
					
					if( mComboLNAGain.isEnabled() )
					{
						mController.setLNAGain( lnaGain, true );
					}

					mSelectedConfig.setLNAGain( lnaGain );
	                save();
                }
                catch ( UsbException e )
                {
                	JOptionPane.showMessageDialog( 
                			E4KTunerConfigurationPanel.this, 
                			"E4K Tuner Controller - couldn't apply the LNA "
                			+ "gain setting - " + e.getLocalizedMessage() );  
                	
                	mLog.error( "E4K Tuner Controller - couldn't apply LNA "
                			+ "gain setting - ", e );
                }
            }
        } );
        mComboLNAGain.setToolTipText( "<html>LNA Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        mComboLNAGain.setEnabled( false );
        gainPanel.add( new JLabel( "LNA" ) );
        gainPanel.add( mComboLNAGain );

        /* Enhance Gain Control */
        mComboEnhanceGain = new JComboBox<E4KEnhanceGain>( E4KEnhanceGain.values() );
        mComboEnhanceGain.setSelectedItem( mSelectedConfig.getEnhanceGain() );
        mComboEnhanceGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
					E4KEnhanceGain enhanceGain = 
							(E4KEnhanceGain)mComboEnhanceGain.getSelectedItem();

					if( mComboEnhanceGain.isEnabled() )
					{
						mController.setEnhanceGain( enhanceGain, true );
					}
					
					mSelectedConfig.setEnhanceGain( enhanceGain );
	                save();
                }
                catch ( UsbException e )
                {
                	JOptionPane.showMessageDialog( 
                			E4KTunerConfigurationPanel.this, 
                			"E4K Tuner Controller - couldn't apply the enhance "
                			+ "gain setting - " + e.getLocalizedMessage() );  
                	
                	mLog.error( "E4K Tuner Controller - couldn't apply enhance "
                			+ "gain setting", e );
                }
            }
        } );
        mComboEnhanceGain.setToolTipText( "<html>Enhance Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        mComboEnhanceGain.setEnabled( false );
        gainPanel.add( new JLabel( "Enhance" ) );
        gainPanel.add( mComboEnhanceGain, "wrap" );

        add( gainPanel, "span,grow,push" );

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
									TunerType.ELONICS_E4000, 
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
				E4KTunerConfiguration selected = 
						(E4KTunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					int n = JOptionPane.showConfirmDialog(
						    E4KTunerConfigurationPanel.this,
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
    private void update( E4KTunerConfiguration config )
    {
    	mSelectedConfig = config;
    	
		try
        {
	        mController.apply( config );
	        
	        mName.setText( config.getName() );
	        mFrequencyCorrection.setValue( config.getFrequencyCorrection() );

	        mComboMixerGain.setSelectedItem( mSelectedConfig.getMixerGain() );
	        mComboLNAGain.setSelectedItem( mSelectedConfig.getLNAGain() );
	        mComboEnhanceGain.setSelectedItem( mSelectedConfig.getEnhanceGain() );

	        /* Apply master gain last so that the Mixer, LNA, and Enhance gain
	         * settings are updated with the master setting where necessary */
	        mComboMasterGain.setSelectedItem( mSelectedConfig.getMasterGain() );

	        mComboSampleRate.setSelectedItem( mSelectedConfig.getSampleRate() );

	        mSettingsManager.setSelectedTunerConfiguration( 
    			TunerType.ELONICS_E4000, mController.getUniqueID(), config );
        }
        catch ( SourceException e1 )
        {
        	JOptionPane.showMessageDialog( 
        			E4KTunerConfigurationPanel.this, 
        			"E4K Tuner Controller - couldn't "
        			+ "apply the tuner configuration settings - " + 
        					e1.getLocalizedMessage() );  
        	
        	mLog.error( "E4K Tuner Controller - couldn't apply "
        			+ "config [" + config.getName() + "]", e1 ); 
        }
    }
    
    /**
     * Constructs a combo box model for the tuner configuration combo component
     * by assembling a list of the tuner configurations appropriate for this
     * specific tuner
     */
    private ComboBoxModel<E4KTunerConfiguration> getModel()
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager
    			.getTunerConfigurations( TunerType.ELONICS_E4000 );
    	
    	DefaultComboBoxModel<E4KTunerConfiguration> model = 
    			new DefaultComboBoxModel<E4KTunerConfiguration>();
    	
    	for( TunerConfiguration config: configs )
    	{
    		model.addElement( (E4KTunerConfiguration)config );
    	}
    	
    	return model;
    }

    /**
     * Retrieves a specific (named) tuner configuration
     */
    private E4KTunerConfiguration getNamedConfiguration( String name )
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager
    			.getTunerConfigurations( TunerType.ELONICS_E4000 );
    	
    	for( TunerConfiguration config: configs )
    	{
    		if( config.getName().contentEquals( name ) )
    		{
    			return (E4KTunerConfiguration)config;
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
