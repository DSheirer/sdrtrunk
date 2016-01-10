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

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;
import source.tuner.airspy.AirspyTunerController.Gain;
import source.tuner.airspy.AirspyTunerController.GainMode;

public class AirspyTunerConfigurationPanel extends JPanel
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AirspyTunerConfigurationPanel.class );

	private static final long serialVersionUID = 1L;
    
    private SettingsManager mSettingsManager;
    private AirspyTunerController mController;
    private AirspyTunerConfiguration mSelectedConfig;

    private JButton mNewConfiguration;
    private JButton mDeleteConfiguration;
    private JComboBox<AirspyTunerConfiguration> mComboConfigurations;

    private JTextField mName;

    private JComboBox<AirspySampleRate> mSampleRateCombo;
    
    private JComboBox<GainMode> mGainModeCombo;
    private JLabel mGainLabel;
    private JLabel mGainFillerLabel;
    private JLabel mGainValueLabel;
    private JSlider mGain;

    private JLabel mIFGainLabel;
    private JLabel mIFGainFillerLabel;
    private JLabel mIFGainValueLabel;
    private JSlider mIFGain;

    private JLabel mLNAGainLabel;
    private JCheckBox mLNAAGC;
    private JLabel mLNAGainValueLabel;
    private JSlider mLNAGain;

    private JLabel mMixerGainValueLabel;
    private JCheckBox mMixerAGC;
    private JLabel mMixerGainLabel;
    private JSlider mMixerGain;
    
    
    public AirspyTunerConfigurationPanel( SettingsManager settingsManager,
    								      AirspyTunerController controller )
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
            savedConfig = mSettingsManager.getSelectedTunerConfiguration( 
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
            	mSettingsManager.setSelectedTunerConfiguration( TunerType.AIRSPY_R820T, 
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

		add( new JSeparator(), "span,growx,push" );

		/**
		 * Gain Mode
		 */
		add( new JLabel( "Gain Mode:" ) );
		mGainModeCombo = new JComboBox<AirspyTunerController.GainMode>( GainMode.values() );
		mGainModeCombo.setSelectedItem( Gain.getGainMode( mSelectedConfig.getGain() ) );
		mGainModeCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
				updateGainComponents( mode );
				
				switch( mode )
				{
					case LINEARITY:
						mGain.setValue( AirspyTunerController.LINEARITY_GAIN_DEFAULT.getValue() );
						mSelectedConfig.setGain( AirspyTunerController.LINEARITY_GAIN_DEFAULT );
						break;
					case SENSITIVITY:
						mGain.setValue( AirspyTunerController.SENSITIVITY_GAIN_DEFAULT.getValue() );
						mSelectedConfig.setGain( AirspyTunerController.SENSITIVITY_GAIN_DEFAULT );
						break;
					case CUSTOM:
					default:
						mLNAGain.setValue( mSelectedConfig.getLNAGain() );
						mMixerGain.setValue( mSelectedConfig.getMixerGain() );
						mIFGain.setValue( mSelectedConfig.getIFGain() );
						mSelectedConfig.setGain( Gain.CUSTOM );
						break;
				}
				
				save();
			}
		} );
		
		add( mGainModeCombo, "wrap" );

		/**
		 * Gain
		 */
		mGainLabel = new JLabel( "Gain" );
		add( mGainLabel );
		mGainFillerLabel = new JLabel( "" );
		add( mGainFillerLabel, "span 2" );
		mGainValueLabel = new JLabel( String.valueOf( mSelectedConfig.getGain().getValue() ) );
		add( mGainValueLabel, "wrap" );
		mGain = new JSlider( JSlider.HORIZONTAL, 
				AirspyTunerController.GAIN_MIN, 
				AirspyTunerController.GAIN_MAX,
				mSelectedConfig.getGain().getValue() );
		mGain.setMajorTickSpacing( 1 );
		mGain.setPaintTicks( true );
		
		mGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
				int value = mGain.getValue();
				Gain gain = Gain.getGain( mode, value );

				try
				{
					mController.setGain( gain );
					mSelectedConfig.setGain( gain );
					save();
					mGainValueLabel.setText( String.valueOf( gain.getValue() ) );
				} 
				catch ( Exception e )
				{
					mLog.error( "Couldn't set airspy gain to:" + gain.name(), e );
					
					JOptionPane.showMessageDialog( mGain, 
							"Couldn't set gain value to " + gain.getValue() );
					
					mGain.setValue( mSelectedConfig.getGain().getValue() );
				}
			}
		} );

		add( mGain, "span,grow" );
		
		/**
		 *  IF/VGA Gain 
		 */
		mIFGainLabel = new JLabel( "IF Gain" );
		add( mIFGainLabel );
		mIFGainFillerLabel = new JLabel( "" );
		add( mIFGainFillerLabel, "span 2" );
		mIFGainValueLabel = new JLabel( String.valueOf( mSelectedConfig.getIFGain() ) );
		add( mIFGainValueLabel, "wrap" );
		
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
					mIFGainValueLabel.setText( String.valueOf( gain ) );
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
		
		/**
		 *  Mixer Gain 
		 */
		mMixerGainLabel = new JLabel( "Mixer Gain" );
		add( mMixerGainLabel );
		
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
		
		mMixerGainValueLabel = new JLabel( String.valueOf( mSelectedConfig.getMixerGain() ) );
		add( mMixerGainValueLabel, "wrap" );
		
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
					mMixerGainValueLabel.setText( String.valueOf( gain ) );
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

		/**
		 *  LNA Gain 
		 */
		mLNAGainLabel = new JLabel( "LNA Gain" );
		add( mLNAGainLabel );
		
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
		
		mLNAGainValueLabel = new JLabel( String.valueOf( mSelectedConfig.getLNAGain() ) );
		add( mLNAGainValueLabel, "wrap" );
		
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
					mLNAGainValueLabel.setText( String.valueOf( gain ) );
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

		//Set the enabled state of each of the gain controls
		updateGainComponents( (GainMode)mGainModeCombo.getSelectedItem() );
		
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
						mSettingsManager.addNewTunerConfiguration( 
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
						mSettingsManager.deleteTunerConfiguration( selected );

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
     * Updates the enabled state of each of the gain controls according to the
     * specified gain mode.  The overall gain controls are enabled for linearity
     * and sensitivity and the individual gain controls are disabled, and 
     * vice-versa for custom mode.
     */
    private void updateGainComponents( GainMode mode )
    {
    	boolean isCustom = ( mode == GainMode.CUSTOM );
    	
		mGainLabel.setEnabled( !isCustom );
		mGainFillerLabel.setEnabled( !isCustom );
		mGainValueLabel.setEnabled( !isCustom );
		mGain.setEnabled( !isCustom );
		mIFGainLabel.setEnabled( isCustom );
	    mIFGainFillerLabel.setEnabled( isCustom );
	    mIFGainValueLabel.setEnabled( isCustom );
	    mIFGain.setEnabled( isCustom );
	    mLNAGainLabel.setEnabled( isCustom );
	    mLNAAGC.setEnabled( isCustom );
	    mLNAGainValueLabel.setEnabled( isCustom );
	    mLNAGain.setEnabled( isCustom );
	    mMixerGainValueLabel.setEnabled( isCustom );
	    mMixerAGC.setEnabled( isCustom );
	    mMixerGainLabel.setEnabled( isCustom );
	    mMixerGain.setEnabled( isCustom );
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

	        mIFGain.setValue( mSelectedConfig.getIFGain() );
	        mMixerGain.setValue( mSelectedConfig.getMixerGain() );
	        mLNAGain.setValue( mSelectedConfig.getLNAGain() );
	        
	        mMixerAGC.setSelected( mSelectedConfig.isMixerAGC() );
	        mLNAAGC.setSelected( mSelectedConfig.isLNAAGC() );
	        
	        Gain gain = mSelectedConfig.getGain();
        	mGainModeCombo.setSelectedItem( Gain.getGainMode( gain ) );
	        mGain.setValue( gain.getValue() );
	        
	        mSettingsManager.setSelectedTunerConfiguration( 
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
			mSettingsManager.getTunerConfigurations( TunerType.AIRSPY_R820T );
    	
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
    			mSettingsManager.getTunerConfigurations( TunerType.AIRSPY_R820T );
    	
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
    	mSettingsManager.save();
    }
}
