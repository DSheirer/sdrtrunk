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

import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;

public class FCD2TunerConfigurationPanel extends JPanel
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FCD2TunerConfigurationPanel.class );

	private static final long serialVersionUID = 1L;
    private SettingsManager mSettingsManager;
    private FCD2TunerController mController;
    private FCD2TunerConfiguration mSelectedConfig;
    private JButton mNewConfiguration;
    private JButton mDeleteConfiguration;
    private JComboBox<FCD2TunerConfiguration> mComboConfigurations;
    private JTextField mName;
    private JCheckBox mLNAGain;
    private JCheckBox mMixerGain;
    private JSpinner mFrequencyCorrection;
    
    public FCD2TunerConfigurationPanel( SettingsManager settingsManager,
    									FCD2TunerController controller )
    {
    	mSettingsManager = settingsManager;
    	mController = controller;
    	
    	init();
    }
    
    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][][grow]" ) );

        mComboConfigurations = new JComboBox<FCD2TunerConfiguration>();

        mComboConfigurations.setModel( getModel() );

		mComboConfigurations.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				FCD2TunerConfiguration selected = 
						(FCD2TunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					update( selected );
				}
           }
		});

		add( new JLabel( "Config:" ) );
		add( mComboConfigurations, "growx,push" );

		mName = new JTextField();
		
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
		
		add( new JLabel( "Name" ) );
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

        mFrequencyCorrection.addChangeListener( new ChangeListener() 
        {
			@Override
            public void stateChanged( ChangeEvent e )
            {
				double value = ((SpinnerNumberModel)mFrequencyCorrection
						.getModel()).getNumber().doubleValue();

				try
                {
	                mController.setFrequencyCorrection( value );
	                mSelectedConfig.setFrequencyCorrection( value );
	                save();
                }
                catch ( SourceException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD2TunerConfigurationPanel.this, 
                			"FCD Pro Plus Tuner Controller - couldn't "
                			+ "apply frequency correction value: " + value + 
                					e1.getLocalizedMessage() );  
                	
                	mLog.error( "FuncubeDongleProPlus Controller - couldn't apply "
                			+ "frequency correction value: " + value, e1 );
                }
            }
        } );
        
        add( new JLabel( "Correction PPM:" ) );
        add( mFrequencyCorrection, "growx,push" );
        
        /**
         * LNA Gain
         */
        mLNAGain = new JCheckBox( "LNA Gain:" );
        mLNAGain.setComponentOrientation( ComponentOrientation.RIGHT_TO_LEFT );
        
        mLNAGain.addActionListener( new ActionListener() 
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
            	mSelectedConfig.setGainLNA( mLNAGain.isSelected() );

            	save();
            	
            	update( mSelectedConfig );
            }
        } );

        add( mLNAGain );

        /**
         * Mixer Gain
         */
        mMixerGain = new JCheckBox( "Mixer Gain:" );
        mMixerGain.setComponentOrientation( ComponentOrientation.RIGHT_TO_LEFT );
        
        mMixerGain.addActionListener( new ActionListener() 
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
            	mSelectedConfig.setGainMixer( mMixerGain.isSelected() );

            	save();
            	
            	update( mSelectedConfig );
            }
        } );

        add( mMixerGain );

		/**
		 * Lookup the save config and apply that config to update all of the
		 * controls
		 */
        TunerConfigurationAssignment savedConfig = mSettingsManager
        		.getSelectedTunerConfiguration( 
        				TunerType.FUNCUBE_DONGLE_PRO_PLUS, 
        								mController.getUSBAddress() );
        
        if( savedConfig != null )
        {
        	mSelectedConfig = getNamedConfiguration( 
        			savedConfig.getTunerConfigurationName() );
        }

        if( mSelectedConfig == null )
        {
        	mSelectedConfig = mComboConfigurations.getItemAt( 0 );

        	//Store this config as the default for this tuner at this address
        	mSettingsManager
        		.setSelectedTunerConfiguration( 
        				TunerType.FUNCUBE_DONGLE_PRO_PLUS, 
        				mController.getUSBAddress(), mSelectedConfig );
        }

        mComboConfigurations.setSelectedItem( mSelectedConfig );

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
									TunerType.FUNCUBE_DONGLE_PRO_PLUS, 
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
				FCD2TunerConfiguration selected = 
						(FCD2TunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					int n = JOptionPane.showConfirmDialog(
						    FCD2TunerConfigurationPanel.this,
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

		add( mDeleteConfiguration, "growx,push" );
    }

    private void update( FCD2TunerConfiguration config )
    {
    	mSelectedConfig = config;
    	
		try
        {
	        mController.apply( config );
	        
	        mName.setText( config.getName() );
	        mLNAGain.setSelected( config.getGainLNA() );
	        mMixerGain.setSelected( config.getGainMixer() );
	        mFrequencyCorrection.setValue( config.getFrequencyCorrection() );
	        
	        mSettingsManager.setSelectedTunerConfiguration( 
        			TunerType.FUNCUBE_DONGLE_PRO_PLUS, 
        			mController.getUSBAddress(), config );
        }
        catch ( SourceException e1 )
        {
        	JOptionPane.showMessageDialog( 
        			FCD2TunerConfigurationPanel.this, 
        			"FCD Pro Plus Tuner Controller - couldn't "
        			+ "apply the tuner configuration settings - " + 
        					e1.getLocalizedMessage() );  
        	
        	mLog.error( "FuncubeDongleProPlus Controller - couldn't apply "
        			+ "config [" + config.getName() + "]", e1 );
        }
    }
    
    private ComboBoxModel<FCD2TunerConfiguration> getModel()
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager
    			.getTunerConfigurations( TunerType.FUNCUBE_DONGLE_PRO_PLUS );
    	
    	DefaultComboBoxModel<FCD2TunerConfiguration> model = 
    			new DefaultComboBoxModel<FCD2TunerConfiguration>();
    	
    	for( TunerConfiguration config: configs )
    	{
    		model.addElement( (FCD2TunerConfiguration)config );
    	}
    	
    	return model;
    }
    
    private FCD2TunerConfiguration getNamedConfiguration( String name )
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager
    			.getTunerConfigurations( TunerType.FUNCUBE_DONGLE_PRO_PLUS );
    	
    	for( TunerConfiguration config: configs )
    	{
    		if( config.getName().contentEquals( name ) )
    		{
    			return (FCD2TunerConfiguration)config;
    		}
    	}

    	return null;
    }
    
    private void save()
    {
    	mSettingsManager.save();
    }
}
