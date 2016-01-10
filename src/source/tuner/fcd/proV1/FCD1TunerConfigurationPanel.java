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
package source.tuner.fcd.proV1;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

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
import javax.usb.UsbClaimException;
import javax.usb.UsbException;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import source.tuner.fcd.proV1.FCD1TunerController.MixerGain;

import com.jidesoft.swing.JideTabbedPane;

public class FCD1TunerConfigurationPanel extends JPanel
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FCD1TunerConfigurationPanel.class );

	private static final long serialVersionUID = 1L;
    private SettingsManager mSettingsManager;
    private FCD1TunerController mController;

    private FCD1TunerConfiguration mSelectedConfig;
    private JButton mNewConfiguration;
    private JButton mDeleteConfiguration;
    private JComboBox<FCD1TunerConfiguration> mComboConfigurations;
    private JComboBox<LNAGain> mComboLNAGain;
    private JComboBox<LNAEnhance> mComboLNAEnhance;
    private JComboBox<MixerGain> mComboMixerGain;
    private JTextField mName;
    private JSpinner mCorrectionFrequency;
    private CorrectionSpinner mCorrectionDCI;
    private CorrectionSpinner mCorrectionDCQ;
    private CorrectionSpinner mCorrectionGain;
    private CorrectionSpinner mCorrectionPhase;
    
    public FCD1TunerConfigurationPanel( SettingsManager settingsManager,
    									FCD1TunerController controller )
    {
    	mSettingsManager = settingsManager;
    	mController = controller;
    	mSelectedConfig = controller.getTunerConfiguration();
    	
    	init();
    }
    
    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][grow]" ) );

		/**
         * Tuner configurations combo
         */
        mComboConfigurations = new JComboBox<FCD1TunerConfiguration>();
        mComboConfigurations.setToolTipText( "Select a tuner configuration.  "
        		+ "Create a separate tuner configuration for each of your "
        		+ "funcube dongles by using the NEW button and typing a "
        		+ "descriptive name" );

        mComboConfigurations.setModel( getModel() );
        
        mComboConfigurations.setSelectedItem( mSelectedConfig );

		mComboConfigurations.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				FCD1TunerConfiguration selected = 
						(FCD1TunerConfiguration)mComboConfigurations
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
		 * Tuner Configuration - Name
		 */
		mName = new JTextField( mSelectedConfig.getName() );
		mName.setToolTipText( "Name for the selected tuner configuration" );
		
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
		
		JideTabbedPane tabs = new JideTabbedPane();
        tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );
		add( tabs, "span,grow,push" );
		
        JPanel gainPanel = new JPanel();
        gainPanel.setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

        /**
         * LNA Gain
         */
        mComboLNAGain = new JComboBox<LNAGain>( LNAGain.values() );
        
        mComboLNAGain.setToolTipText( "Adjust the low noise amplifier gain "
        		+ "setting.  Default value is 20db" );
        
        mComboLNAGain.setSelectedItem( mSelectedConfig.getLNAGain() );

        mComboLNAGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				LNAGain gain = (LNAGain)mComboLNAGain.getSelectedItem();
				
				mSelectedConfig.setLNAGain( gain );
				
				try
                {
					mController.setLNAGain( gain );
                }
                catch ( UsbClaimException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - cannot claim FCD "
                			+ "Controller to apply LNA gain [" + gain.toString() + "]" );  
                	
                	mLog.error( "FuncubeDonglePro Controller - usb claim "
                			+ "exception while applying LNA gain value [" + 
                			gain.toString() + "]", e1 );
                }
                catch ( UsbException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - FCD Controller "
                			+ "cannot apply LNA gain [" + gain.toString() + "]" );  
                	
                	mLog.error( "FuncubeDonglePro Controller - "
                			+ "exception while applying LNA gain value [" + 
                			gain.toString() + "]", e1 );
                }
				
				save();
            }
       	
        } );
        
        gainPanel.add( new JLabel( "LNA:" ) );
        gainPanel.add( mComboLNAGain, "growx,push" );
        
        /**
         * LNA Enhance
         */
        mComboLNAEnhance = new JComboBox<LNAEnhance>( LNAEnhance.values() );
        mComboLNAEnhance.setToolTipText( "Adjust the LNA enhance setting.  "
        		+ "Default value is OFF" );
        
        mComboLNAEnhance.setSelectedItem( mSelectedConfig.getLNAEnhance() );

        mComboLNAEnhance.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				LNAEnhance enhance = (LNAEnhance)mComboLNAEnhance.getSelectedItem();
				
				mSelectedConfig.setLNAEnhance( enhance );
				
				try
                {
					mController.setLNAEnhance( enhance );
                }
                catch ( UsbClaimException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - cannot claim FCD "
                			+ "Controller to apply LNA enhance [" + enhance.toString() + "]" );  
                	
                	mLog.error( "FuncubeDonglePro Controller - usb claim "
                			+ "exception while applying LNA enhance value [" + 
                			enhance.toString() + "]", e1 );
                }
                catch ( UsbException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - FCD Controller "
                			+ "cannot apply LNA enhance [" + enhance.toString() + "]" );  
                	
                	mLog.error( "FuncubeDonglePro Controller - "
                			+ "exception while applying LNA enhance value [" + 
                			enhance.toString() + "]", e1 );
                }
				
				save();
            }
       	
        } );
        
        gainPanel.add( new JLabel( "LNA Enhance:" ) );
        gainPanel.add( mComboLNAEnhance, "growx,push" );
        
        /**
         * Mixer Gain
         */
        mComboMixerGain = new JComboBox<MixerGain>( MixerGain.values() );
        mComboMixerGain.setToolTipText( "Adjust mixer gain setting" );
        
        mComboMixerGain.setSelectedItem( mSelectedConfig.getMixerGain() );

        mComboMixerGain.addActionListener( new ActionListener() 
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				MixerGain gain = (MixerGain)mComboMixerGain.getSelectedItem();
				
				mSelectedConfig.setMixerGain( gain );
				
				try
                {
					mController.setMixerGain( gain );
                }
                catch ( UsbClaimException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - cannot claim FCD "
                			+ "Controller to apply Mixer gain [" + gain.toString() + "]" );  
                	
                	mLog.error( "FuncubeDonglePro Controller - usb claim "
                			+ "exception while applying Mixer gain value [" + 
                			gain.toString() + "]", e1 );
                }
                catch ( UsbException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - FCD Controller "
                			+ "cannot apply Mixer gain [" + gain.toString() + "]" );  
                	
                	mLog.error( "FuncubeDonglePro Controller - "
                			+ "exception while applying Mixer gain value [" + 
                			gain.toString() + "]", e1 ); 
                }
				
				save();
            }
       	
        } );
        
        gainPanel.add( new JLabel( "Mixer:" ) );
        gainPanel.add( mComboMixerGain, "growx,push" );
        
        tabs.add( "Gain", gainPanel );
        
        JPanel correctionPanel = new JPanel();
        correctionPanel.setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][][grow]" ) );

        /**
         * Frequency Correction
         */
        SpinnerModel model =
                new SpinnerNumberModel( 0.0, -1000.0, 1000.0, 0.1 ); //initial,min,max,step

        mCorrectionFrequency = new JSpinner( model );

        mCorrectionFrequency.setToolTipText( "Frequency Correction: adjust "
        		+ "value +/- to align the displayed frequency label with the "
        		+ "actual signals" );

        JSpinner.NumberEditor editor = 
        		(JSpinner.NumberEditor)mCorrectionFrequency.getEditor();  

        DecimalFormat format = editor.getFormat();  
        format.setMinimumFractionDigits( 1 );  
        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );          
        
        mCorrectionFrequency.setValue( mSelectedConfig.getFrequencyCorrection() );

        mCorrectionFrequency.addChangeListener( new ChangeListener() 
        {
			@Override
            public void stateChanged( ChangeEvent e )
            {
				double value = ((SpinnerNumberModel)mCorrectionFrequency
						.getModel()).getNumber().doubleValue();

				try
                {
					mSelectedConfig.setFrequencyCorrection( value );
	                mController.setFrequencyCorrection( value );
	                save();
                }
                catch ( SourceException e1 )
                {
                	JOptionPane.showMessageDialog( 
                			FCD1TunerConfigurationPanel.this, 
                			"FCD Pro Tuner Controller - couldn't "
                			+ "apply frequency correction value: " + value + 
                					e1.getLocalizedMessage() );  
                	
                	mLog.error( "FuncubeDonglePro Controller - couldn't apply "
                			+ "frequency correction value: " + value, e1 ); 
                }
            }
        } );
        
        correctionPanel.add( new JLabel( "Frequency PPM:" ) );
        correctionPanel.add( mCorrectionFrequency, "growx,push" );

        /**
         * Inphase DC Correction
         */
        mCorrectionDCI = new CorrectionSpinner( Correction.DC_INPHASE, 
        		mSelectedConfig.getInphaseDCCorrection(), 0.00001, 5 );
        mCorrectionDCI.setToolTipText( "DC Bias Correction/Inphase "
        		+ "Component: valid values are -1.0 to 1.0 (default: 0.0)" );
        correctionPanel.add( new JLabel( "DC Inphase:" ) );
        correctionPanel.add( mCorrectionDCI, "growx,push" );

        /**
         * Quadrature DC Correction
         */
        mCorrectionDCQ = new CorrectionSpinner( Correction.DC_QUADRATURE, 
        		mSelectedConfig.getQuadratureDCCorrection(), 0.00001, 5 );
        mCorrectionDCQ.setToolTipText( "DC Bias Correction/Quadrature "
        		+ "Component: valid values are -1.0 to 1.0 (default: 0.0)" );
        correctionPanel.add( new JLabel( "DC Quadrature:" ) );
        correctionPanel.add( mCorrectionDCQ, "growx,push" );

        /**
         * Gain Correction
         */
        mCorrectionGain = new CorrectionSpinner( Correction.GAIN, 
        		mSelectedConfig.getGainCorrection(), 0.00001, 5 );
        mCorrectionGain.setToolTipText( "Gain Correction: valid values are "
        		+ "-1.0 to 1.0 (default: 0.0)" );
        correctionPanel.add( new JLabel( "Gain:" ) );
        correctionPanel.add( mCorrectionGain, "growx,push" );

        /**
         * Phase Correction
         */
        mCorrectionPhase = new CorrectionSpinner( Correction.PHASE, 
        		mSelectedConfig.getPhaseCorrection(), 0.00001, 5 );
        mCorrectionPhase.setToolTipText( "Phase Correction: valid values are "
        		+ "-1.0 to 1.0 (default: 0.0)" );
        correctionPanel.add( new JLabel( "Phase:" ) );
        correctionPanel.add( mCorrectionPhase, "growx,push" );

        tabs.add( "Correction", correctionPanel );

        /**
         * Create a new configuration
         */
        mNewConfiguration = new JButton( "New" );
        mNewConfiguration.setToolTipText( "Create a new tuner configuration "
        		+ "for each of your dongles to keep track of the specific "
        		+ "settings for each dongle" );

		mNewConfiguration.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				TunerConfiguration config = 
						mSettingsManager.addNewTunerConfiguration( 
									TunerType.FUNCUBE_DONGLE_PRO, 
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
		mDeleteConfiguration.setToolTipText( "Deletes the currently selected "
				+ "tuner configuration.  Note: the DEFAULT tuner "
				+ "configuration will be recreated if you delete it, "
				+ "after you restart the application" );
		
		mDeleteConfiguration.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				FCD1TunerConfiguration selected = 
						(FCD1TunerConfiguration)mComboConfigurations
						.getItemAt( mComboConfigurations.getSelectedIndex() );

				if( selected != null )
				{
					int n = JOptionPane.showConfirmDialog(
						    FCD1TunerConfigurationPanel.this,
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

		add( mDeleteConfiguration, "growx,push" );
    }

    private void update( FCD1TunerConfiguration config )
    {
    	mSelectedConfig = config;
    	
		try
        {
	        mController.apply( config );
	        
	        mName.setText( config.getName() );
	        
	        mComboLNAGain.setSelectedItem( mSelectedConfig.getLNAGain() );
	        mComboLNAEnhance.setSelectedItem( mSelectedConfig.getLNAEnhance() );
	        mComboMixerGain.setSelectedItem( mSelectedConfig.getMixerGain() );
	       
	        mCorrectionFrequency.setValue( config.getFrequencyCorrection() );
	        mCorrectionDCI.setValue( config.getInphaseDCCorrection() );
	        mCorrectionDCQ.setValue( config.getQuadratureDCCorrection() );
	        mCorrectionGain.setValue( config.getGainCorrection() );
	        mCorrectionPhase.setValue( config.getPhaseCorrection() );
	        
	        mSettingsManager.setSelectedTunerConfiguration( 
	        			TunerType.FUNCUBE_DONGLE_PRO, 
	        			mController.getUSBAddress(), config );
        }
        catch ( SourceException e1 )
        {
        	JOptionPane.showMessageDialog( 
        			FCD1TunerConfigurationPanel.this, 
        			"FCD Pro Tuner Controller - couldn't "
        			+ "apply the tuner configuration settings - " + 
        					e1.getLocalizedMessage() );  
        	
        	mLog.error( "FuncubeDonglePro Controller - couldn't apply "
        			+ "config [" + config.getName() + "]", e1 );
        }
    }
    
    private ComboBoxModel<FCD1TunerConfiguration> getModel()
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager.getTunerConfigurations( TunerType.FUNCUBE_DONGLE_PRO );
    	
    	DefaultComboBoxModel<FCD1TunerConfiguration> model = 
    			new DefaultComboBoxModel<FCD1TunerConfiguration>();
    	
    	for( TunerConfiguration config: configs )
    	{
    		model.addElement( (FCD1TunerConfiguration)config );
    	}
    	
    	return model;
    }
    
    @SuppressWarnings( "unused" )
    private FCD1TunerConfiguration getNamedConfiguration( String name )
    {
    	ArrayList<TunerConfiguration> configs = 
    			mSettingsManager.getTunerConfigurations( TunerType.FUNCUBE_DONGLE_PRO );
    	
    	for( TunerConfiguration config: configs )
    	{
    		if( config.getName().contentEquals( name ) )
    		{
    			return (FCD1TunerConfiguration)config;
    		}
    	}

    	return null;
    }
    
    private void save()
    {
    	mSettingsManager.save();
    }
    
    public enum Correction { GAIN, PHASE, DC_INPHASE, DC_QUADRATURE };
    
    public class CorrectionSpinner extends JSpinner
    {
        private static final long serialVersionUID = 1L;
        private static final double sMIN_VALUE = -1.0d;
        private static final double sMAX_VALUE = 1.0d;
        
        private Correction mCorrectionComponent;

		public CorrectionSpinner( Correction component,
								  double initialValue,
								  double step,
								  int decimalPlaces )
    	{
			mCorrectionComponent = component;
			
	        SpinnerModel model = new SpinnerNumberModel( initialValue, 
	        					sMIN_VALUE, sMAX_VALUE, step );

	        setModel( model );

	        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)getEditor();  
	        
	        DecimalFormat format = editor.getFormat();  
	        format.setMinimumFractionDigits( decimalPlaces );  
	        
	        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );          

	        addChangeListener( new ChangeListener() 
	        {
				@Override
	            public void stateChanged( ChangeEvent e )
	            {
					double value = 
						((SpinnerNumberModel)getModel()).getNumber().doubleValue();

					try
	                {
						switch( mCorrectionComponent )
						{
							case DC_INPHASE:
								mSelectedConfig.setInphaseDCCorrection( value );
								mController.setDCCorrectionInPhase( value );
								break;
							case DC_QUADRATURE:
								mSelectedConfig.setQuadratureDCCorrection( value );
								mController.setDCCorrectionQuadrature( value );
								break;
							case GAIN:
								mSelectedConfig.setGainCorrection( value );
								mController.setGainCorrection( value );
								break;
							case PHASE:
								mSelectedConfig.setPhaseCorrection( value );
								mController.setPhaseCorrection( value );
								break;
						}
						
						//Save the change(s) to the selected tuner config
		                save();
	                }
                    catch ( UsbClaimException e1 )
                    {
	                	JOptionPane.showMessageDialog( 
	                			FCD1TunerConfigurationPanel.this, 
	                			"FCD Pro Tuner Controller - cannot claim FCD "
	                			+ "Controller to apply " 
	                			+ mCorrectionComponent.toString() + 
	                			" correction value [" + value + "]" );  
	                	
	                	mLog.error( "FuncubeDonglePro Controller - usb claim "
	                			+ "exception while applying "
	                			+ mCorrectionComponent.toString() 
	                			+ " correction value [" + value + "]", e1 );
                    }
                    catch ( UsbException e1 )
                    {
	                	JOptionPane.showMessageDialog( 
	                			FCD1TunerConfigurationPanel.this, 
	                			"FCD Pro Tuner Controller - USB error from FCD "
	                			+ "Controller while applying " 
	                			+ mCorrectionComponent.toString() + 
	                			" correction value [" + value + "]" );  
	                	
	                	mLog.error( "FuncubeDonglePro Controller - usb "
	                			+ "exception while applying "
	                			+ mCorrectionComponent.toString() 
	                			+ " correction value [" + value + "]", e1 );
                    }
	            }
	        } );
    	}
    }
}
