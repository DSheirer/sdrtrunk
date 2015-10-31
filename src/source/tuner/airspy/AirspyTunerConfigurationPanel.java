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
package source.tuner.airspy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;
import controller.ResourceManager;

public class AirspyTunerConfigurationPanel extends JPanel
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AirspyTunerConfigurationPanel.class );

	private static final long serialVersionUID = 1L;
    
    private ResourceManager mResourceManager;
    private AirspyTunerController mController;
    private AirspyTunerConfiguration mSelectedConfig;

    private JButton mNewConfiguration;
    private JButton mDeleteConfiguration;
    private JComboBox<AirspyTunerConfiguration> mComboConfigurations;

    private JTextField mName;

    private JComboBox<AirspySampleRate> mSampleRateCombo;
    private JComboBox mDecimationCombo;
    private JLabel mEffectiveRate;
    
    private JLabel mIFGainLabel;
    private JSlider mIFGain;
    private JLabel mLNAGainLabel;
    private JSlider mLNAGain;
    private JLabel mMixerGainLabel;
    private JSlider mMixerGain;
    
    private JCheckBox mMixerAGC;
    private JCheckBox mLNAAGC;
    
    public AirspyTunerConfigurationPanel( ResourceManager resourceManager,
    								      AirspyTunerController controller )
    {
    	mResourceManager = resourceManager;
    	mController = controller;
    	
    	init();
    }
    
    /**
     * Initializes the gui components
     */
    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 4", "[left][grow][grow][right]", 
				"[][][][][][][grow]" ) );

        /**
         * Tuner configuration selector
         */
        mComboConfigurations = new JComboBox<AirspyTunerConfiguration>();

        mComboConfigurations.setModel( getModel() );

        /* Determine which tuner configuration should be selected/displayed */
        TunerConfigurationAssignment savedConfig = null;
        String serial = null;

        serial = mController.getDeviceInfo().getSerialNumber();
        
        if( serial != null )
        {
            savedConfig = mResourceManager
            		.getSettingsManager().getSelectedTunerConfiguration( 
    				TunerType.AIRSPY_R820T, serial );
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
            	mResourceManager.getSettingsManager()
        		.setSelectedTunerConfiguration( TunerType.AIRSPY_R820T, 
    				serial, mSelectedConfig );
        	}
        }

        mComboConfigurations.setSelectedItem( mSelectedConfig );

		mComboConfigurations.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				AirspyTunerConfiguration selected = 
						(AirspyTunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					update( selected );
				}
           }
		});

		add( new JLabel( "Config:" ) );
		add( mComboConfigurations, "span,growx,push" );
		
		add( new JSeparator(), "span,growx,push" );
		

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
		add( mName, "span,growx,push" );
		
		/**
		 * Sample Rate
		 */
		add( new JLabel( "Sample Rate:" ) );
		
		List<AirspySampleRate> rates = mController.getSampleRates();
		
		mSampleRateCombo = new JComboBox<AirspySampleRate>( 
			new DefaultComboBoxModel<AirspySampleRate>( rates.toArray( 
					new AirspySampleRate[ rates.size() ] ) ) );
		
		mSampleRateCombo.setSelectedItem( mController.getSampleRate() );
		
		mSampleRateCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				AirspySampleRate rate = (AirspySampleRate)mSampleRateCombo.getSelectedItem();

				try
				{
					mController.setSampleRate( rate );
					mSelectedConfig.setSampleRate( rate.getRate() );
					save();
					
					mEffectiveRate.setText( rate.getLabel() );
				} 
				catch ( LibUsbException | UsbException e1 )
				{
					JOptionPane.showMessageDialog( AirspyTunerConfigurationPanel.this, 
						"Couldn't set sample rate to " + rate.getLabel() );
					
					mLog.error( "Error setting airspy sample rate", e1 );
				} 
			}
		} );
		
		add( mSampleRateCombo, "span 3,growx" );

		/**
		 * Decimation
		 */
		add( new JLabel( "Decimation:" ) );
		mDecimationCombo = new JComboBox();
		add( mDecimationCombo, "span 3,growx" );
		mDecimationCombo.setEnabled( false );
		
		/**
		 * Effective Rate
		 */
		add( new JLabel( "Effective Rate:" ) );
		mEffectiveRate = new JLabel( "10,000,000" );
		add( mEffectiveRate, "span 3,growx" );
		
		add( new JSeparator(), "span,growx,push" );

		/**
		 *  IF/VGA Gain 
		 */
		add( new JLabel( "IF Gain" ) );
		add( new JLabel( "" ), "span 2" );
		
		mIFGainLabel = new JLabel( String.valueOf( mSelectedConfig.getIFGain() ) );
		add( mIFGainLabel, "wrap" );
		
		mIFGain = new JSlider( JSlider.HORIZONTAL, 
								AirspyTunerController.IF_GAIN_MIN, 
								AirspyTunerController.IF_GAIN_MAX,
								mSelectedConfig.getIFGain() );
		
		mIFGain.setMajorTickSpacing( 1 );
		mIFGain.setPaintTicks( true );
		
		mIFGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				int gain = mIFGain.getValue();

				try
				{
					mController.setIFGain( gain );
					mSelectedConfig.setIFGain( gain );
					save();
					mIFGainLabel.setText( String.valueOf( gain ) );
				} 
				catch ( Exception e )
				{
					mLog.error( "Couldn't set airspy IF gain to:" + gain, e );
					
					JOptionPane.showMessageDialog( mIFGain, 
							"Couldn't set IF gain value to " + gain );
					
					mIFGain.setValue( mSelectedConfig.getIFGain() );
				}
			}
		} );

		add( mIFGain, "span,grow" );
		
		add( new JLabel( "Mixer Gain" ) );
		
		mMixerAGC = new JCheckBox( "AGC" );
		
		mMixerAGC.setSelected( mSelectedConfig.isMixerAGC() );
		
		mMixerAGC.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					mController.setMixerAGC( !mSelectedConfig.isMixerAGC() );
					mSelectedConfig.setMixerAGC( !mSelectedConfig.isMixerAGC() );
					save();
				} 
				catch ( Exception e1 )
				{
					mLog.error( "Error setting Mixer AGC Enabled:" + !mSelectedConfig.isMixerAGC() );
					mMixerAGC.setSelected( mSelectedConfig.isMixerAGC() );
				}
			}
		} );
		
		
		add( mMixerAGC, "span 2,center" );
		
		mMixerGainLabel = new JLabel( String.valueOf( mSelectedConfig.getMixerGain() ) );
		add( mMixerGainLabel, "wrap" );
		
		mMixerGain = new JSlider( JSlider.HORIZONTAL, 
								AirspyTunerController.MIXER_GAIN_MIN, 
								AirspyTunerController.MIXER_GAIN_MAX,
								mSelectedConfig.getMixerGain() );
		
		mMixerGain.setMajorTickSpacing( 1 );
		mMixerGain.setPaintTicks( true );
		
		mMixerGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				int gain = mMixerGain.getValue();

				try
				{
					mController.setMixerGain( gain );
					mSelectedConfig.setMixerGain( gain );
					save();
					mMixerGainLabel.setText( String.valueOf( gain ) );
				} 
				catch ( Exception e )
				{
					mLog.error( "Couldn't set airspy Mixer gain to:" + gain, e );
					
					JOptionPane.showMessageDialog( mIFGain, "Couldn't set Mixer gain value to " + gain );
					
					mMixerGain.setValue( mSelectedConfig.getMixerGain() );
				}
			}
		} );

		add( mMixerGain, "span,grow" );
		
		add( new JLabel( "LNA Gain" ) );
		
		mLNAAGC = new JCheckBox( "AGC" );

		mLNAAGC.setSelected( mSelectedConfig.isLNAAGC() );

		mLNAAGC.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					mController.setLNAAGC( !mSelectedConfig.isLNAAGC() );
					mSelectedConfig.setLNAAGC( !mSelectedConfig.isLNAAGC() );
					save();
				} 
				catch ( Exception e1 )
				{
					mLog.error( "Error setting LNA AGC Enabled:" + !mSelectedConfig.isLNAAGC() );
					mLNAAGC.setSelected( mSelectedConfig.isLNAAGC() );
				}
				
			}
		} );
		
		add( mLNAAGC, "span 2,center" );
		
		mLNAGainLabel = new JLabel( String.valueOf( mSelectedConfig.getLNAGain() ) );
		add( mLNAGainLabel, "wrap" );
		
		mLNAGain = new JSlider( JSlider.HORIZONTAL, 
								AirspyTunerController.LNA_GAIN_MIN, 
								AirspyTunerController.LNA_GAIN_MAX,
								mSelectedConfig.getLNAGain() );
		
		mLNAGain.setMajorTickSpacing( 1 );
		mLNAGain.setPaintTicks( true );
		
		mLNAGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				int gain = mLNAGain.getValue();

				try
				{
					mController.setLNAGain( gain );
					mSelectedConfig.setLNAGain( gain );
					save();
					mLNAGainLabel.setText( String.valueOf( gain ) );
				} 
				catch ( Exception e )
				{
					mLog.error( "Couldn't set airspy LNA gain to:" + gain, e );
					
					JOptionPane.showMessageDialog( mIFGain, "Couldn't set LNA gain value to " + gain );
					
					mLNAGain.setValue( mSelectedConfig.getLNAGain() );
				}
			}
		} );

		add( mLNAGain, "span,grow" );
		
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
						mResourceManager.getSettingsManager()
							.addNewTunerConfiguration( 
									TunerType.AIRSPY_R820T, 
									"New Configuration" );
				
				mComboConfigurations.setModel( getModel() );
				
				mComboConfigurations.setSelectedItem( config );

				repaint();
            }
		} );
		
		add( mNewConfiguration, "span 2,growx,push" );

		/**
		 * Delete the currently selected configuration
		 */
		mDeleteConfiguration = new JButton( "Delete" );
		
		mDeleteConfiguration.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				AirspyTunerConfiguration selected = 
						(AirspyTunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					int n = JOptionPane.showConfirmDialog(
						    AirspyTunerConfigurationPanel.this,
						    "Are you sure you want to delete '"
					    		+ selected.getName() + "'?",
						    "Are you sure?",
						    JOptionPane.YES_NO_OPTION );

					if( n == JOptionPane.YES_OPTION )
					{
						mResourceManager.getSettingsManager()
							.deleteTunerConfiguration( selected );

						mComboConfigurations.setModel( getModel() );
						
						mComboConfigurations.setSelectedIndex( 0 );
						
						repaint();
					}
				}
            }
		} );

		add( mDeleteConfiguration, "span 2,growx,push" );
    }

    /**
     * Updates gui controls with the values from the tuner configuration
     * @param config - tuner configuration
     */
    private void update( AirspyTunerConfiguration config )
    {
    	mSelectedConfig = config;
    	
		try
        {
	        mController.apply( config );
	        
	        mName.setText( config.getName() );

	        AirspySampleRate rate = mController.getSampleRate( 
	        		mSelectedConfig.getSampleRate() );
	        
	        if( rate != null )
	        {
	        	mSampleRateCombo.setSelectedItem( rate );
	        }
	        
	        //TODO: set decimation and effective rate here
	        
	        mIFGain.setValue( mSelectedConfig.getIFGain() );
	        mMixerGain.setValue( mSelectedConfig.getMixerGain() );
	        mLNAGain.setValue( mSelectedConfig.getLNAGain() );
	        
	        mMixerAGC.setSelected( mSelectedConfig.isMixerAGC() );
	        mLNAAGC.setSelected( mSelectedConfig.isLNAAGC() );
	        
	        mResourceManager.getSettingsManager().setSelectedTunerConfiguration( 
			TunerType.AIRSPY_R820T, mController.getDeviceInfo().getSerialNumber(), config );
	        
	        save();
        }
        catch ( /*UsbException |*/ SourceException e1 )
        {
        	JOptionPane.showMessageDialog( 
        			AirspyTunerConfigurationPanel.this, 
        			"Airspy Tuner Controller - couldn't "
        			+ "apply the tuner configuration settings - " + 
        					e1.getLocalizedMessage() );  
        	
        	mLog.error( "Airspy Tuner Controller - couldn't apply "
        			+ "config [" + config.getName() + "]", e1 );
        }
    }
    
    /**
     * Constructs a combo box model for the tuner configuration combo component
     * by assembling a list of the tuner configurations appropriate for this
     * specific tuner
     */
    private ComboBoxModel<AirspyTunerConfiguration> getModel()
    {
    	ArrayList<TunerConfiguration> configs = 
    			mResourceManager.getSettingsManager()
    			.getTunerConfigurations( TunerType.AIRSPY_R820T );
    	
    	DefaultComboBoxModel<AirspyTunerConfiguration> model = 
    			new DefaultComboBoxModel<AirspyTunerConfiguration>();
    	
    	for( TunerConfiguration config: configs )
    	{
    		model.addElement( (AirspyTunerConfiguration)config );
    	}
    	
    	return model;
    }

    /**
     * Retrieves a specific (named) tuner configuration
     */
    private AirspyTunerConfiguration getNamedConfiguration( String name )
    {
    	ArrayList<TunerConfiguration> configs = 
    			mResourceManager.getSettingsManager()
    			.getTunerConfigurations( TunerType.AIRSPY_R820T );
    	
    	for( TunerConfiguration config: configs )
    	{
    		if( config.getName().contentEquals( name ) )
    		{
    			return (AirspyTunerConfiguration)config;
    		}
    	}

    	return null;
    }
    
    /**
     * Saves current tuner configuration settings
     */
    private void save()
    {
    	mResourceManager.getSettingsManager().save();
    }
}
