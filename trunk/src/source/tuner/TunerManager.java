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
package source.tuner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;

import log.Log;
import source.Source;
import source.SourceException;
import source.config.SourceConfigTuner;
import source.mixer.MixerManager;
import source.tuner.ettus.B100Tuner;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.proV1.FCD1TunerController;
import source.tuner.fcd.proplusV2.FCD2TunerController;
import source.tuner.rtl.RTL2832Tuner;
import source.tuner.rtl.RTL2832TunerController;
import source.tuner.rtl.e4k.E4KTunerController;
import source.tuner.rtl.r820t.R820TTunerController;
import source.tuner.usb.USBTunerDevice;
import controller.ResourceManager;
import controller.channel.ProcessingChain;

public class TunerManager
{
	private static final String sLOADED = "\t[LOADED]\t";
	private static final String sNOT_LOADED = "\t[NOT LOADED]\t";

	private ResourceManager mResourceManager;

	private ArrayList<Tuner> mTuners = new ArrayList<Tuner>();

    public TunerManager( ResourceManager resourceManager )
	{
    	mResourceManager = resourceManager;
		loadTuners();
	}

    /**
     * Iterates current tuners to get a tuner channel source for the frequency
     * specified in the channel config's source config object
     */
    public Source getSource( ProcessingChain processingChain )
    {
    	TunerChannelSource retVal = null;

    	
    	if( processingChain.getChannel().getSourceConfiguration()
    						instanceof SourceConfigTuner )
    	{
    		TunerChannel tunerChannel = processingChain.getChannel().getTunerChannel();
			
			Iterator<Tuner> it = mTuners.iterator();
			
			Tuner tuner;
			
			while( it.hasNext() && retVal == null )
			{
				tuner = it.next();
				
				try
                {
                    retVal = tuner.getChannel( 
                		mResourceManager.getThreadPoolManager(), tunerChannel );
                }
				catch ( RejectedExecutionException ree )
				{
					Log.error( "TunerManager - couldn't provide tuner channel "
							+ "source - " + ree.getLocalizedMessage() );
				}
                catch ( SourceException e )
                {
                	Log.error( "TunerManager - error obtaining channel " +
                			"from tuner [" + tuner.getName() + "] - "
                			+ e.getLocalizedMessage() );
                }
			}
    	}
    	
    	return retVal;
    }

    /**
     * Get list of current tuners
     */
    public List<Tuner> getTuners()
    {
    	return mTuners;
    }

    /**
     * Loads all USB tuners and USB/Mixer tuner devices
     */
	private void loadTuners()
	{
		mTuners.clear();

		//Get list of attached USB tuner devices
		ArrayList<USBTunerDevice> devices = getUSBTunerDevices();

		StringBuilder sb = new StringBuilder();
		
		sb.append( "Tuner Manager - discovered [" + devices.size() + 
				"] USB tuner devices\n" );
		
		for( USBTunerDevice device: devices )
		{
			String status = sNOT_LOADED;
			String name = "Unknown";
			String reason = "No device driver class available";
			
			TunerClass tunerClass = device.getTunerClass();
			
			switch( tunerClass )
			{
				case FUNCUBE_DONGLE_PRO:
					//Locate the matching mixer tuner dataline
					MixerTunerDataLine fcd1Dataline = 
						getMixerTunerDataLine( tunerClass.getTunerClass() );
					if( fcd1Dataline != null )
					{
						FCD1TunerController fcd1Controller;

						try
                        {
	                        fcd1Controller = new FCD1TunerController( device );

							FCDTuner fcd1Tuner = 
									new FCDTuner( fcd1Dataline, fcd1Controller );
							
	                        TunerConfiguration config = 
	                        		getTunerConfiguration( fcd1Tuner );

							if( config != null )
	    	                {
								fcd1Tuner.apply( config );
	    	                }					

							mTuners.add( fcd1Tuner );

							name = fcd1Tuner.getName();
							status = sLOADED;
							reason = null;
                        }
                        catch ( SourceException e )
                        {
                        	reason = "Controller Source Exception: " + 
                        					e.getLocalizedMessage();
                        }
						
					}
					else
					{
						reason = "Couldn't locate matching mixer/sound card device";
					}
					break;
				case FUNCUBE_DONGLE_PRO_PLUS:
					//Locate the matching mixer tuner dataline
					MixerTunerDataLine fcd2Dataline = 
								getMixerTunerDataLine( tunerClass.getTunerClass() );

					if( fcd2Dataline != null )
					{
						FCD2TunerController fcd2Controller;

						try
                        {
	                        fcd2Controller = new FCD2TunerController( device );

							FCDTuner fcd2Tuner = 
								new FCDTuner( fcd2Dataline, fcd2Controller );

	                        TunerConfiguration config = 
	                        		getTunerConfiguration( fcd2Tuner );

							if( config != null )
	    	                {
								fcd2Tuner.apply( config );
	    	                }					

							mTuners.add( fcd2Tuner );

							status = sLOADED;
							name = fcd2Tuner.getName();
							reason = null;
                        }
                        catch ( SourceException e )
                        {
                        	reason = "Controller Source Exception: " + 
                        					e.getLocalizedMessage();
                        }
					}
					else
					{
						reason = "Couldn't locate matching mixer/sound card device";
					}
					break;
				case GENERIC_2832:
				case COMPRO_VIDEOMATE_U620F:
				case COMPRO_VIDEOMATE_U650F:
				case COMPRO_VIDEOMATE_U680F:
				case DEXATEK_5217_DVBT:
				case DEXATEK_DIGIVOX_MINI_II_REV3:
				case DEXATEK_LOGILINK_VG002A:
				case GENERIC_2838:
				case GIGABYTE_GTU7300:
				case GTEK_T803:
				case LIFEVIEW_LV5T_DELUXE:
				case MYGICA_TD312:
				case PEAK_102569AGPK:
				case PROLECTRIX_DV107669:
				case SVEON_STV20:
				case TERRATEC_CINERGY_T_REV1:
				case TERRATEC_CINERGY_T_REV3:
				case TERRATEC_NOXON_REV1_B3:
				case TERRATEC_NOXON_REV1_B4:
				case TERRATEC_NOXON_REV1_B7:
				case TERRATEC_NOXON_REV1_C6:
				case TERRATEC_NOXON_REV2:
				case TERRATEC_T_STICK_PLUS:
				case TWINTECH_UT40:
				case ZAAPA_ZTMINDVBZP:
					TunerType tunerType = tunerClass.getTunerClass();
					
					if( tunerType == TunerType.RTL2832_VARIOUS )
					{
						tunerType = RTL2832TunerController.identifyTunerType( device );
					}
					
					switch( tunerType )
					{
						case ELONICS_E4000:
							try
							{
								E4KTunerController controller = 
									new E4KTunerController( device );
								
								controller.init();
								
								RTL2832Tuner rtlTuner = 
									new RTL2832Tuner( tunerClass, controller );
								
		                        TunerConfiguration config = 
		                        		getTunerConfiguration( rtlTuner );

								if( config != null )
		    	                {
									rtlTuner.apply( config );
		    	                }					
								
								mTuners.add( rtlTuner );
								status = sLOADED;
								name = rtlTuner.getName();
								reason = null;
							}
							catch( SourceException se )
							{
								status = sNOT_LOADED;
								reason = "Error constructing E4K tuner "
									+ "controller - " + se.getLocalizedMessage();
							}
							break;
						case RAFAELMICRO_R820T:
							try
							{
								R820TTunerController controller = 
									new R820TTunerController( device );
								
								controller.init();
								
								RTL2832Tuner rtlTuner = 
									new RTL2832Tuner( tunerClass, controller );
								
								
		                        TunerConfiguration config = 
		                        		getTunerConfiguration( rtlTuner );

								if( config != null )
		    	                {
									rtlTuner.apply( config );
		    	                }					
								
								mTuners.add( rtlTuner );
								status = sLOADED;
								name = rtlTuner.getName();
								reason = null;
							}
							catch( SourceException se )
							{
								status = sNOT_LOADED;
								reason = "Error constructing R820T tuner "
									+ "controller - " + se.getLocalizedMessage();
							}
							break;
						case FITIPOWER_FC0012:
						case FITIPOWER_FC0013:
						case RAFAELMICRO_R828D:
						case UNKNOWN:
						default:
							status = sNOT_LOADED;
							reason = "SDRTRunk doesn't currently support RTL2832 "
									+ "Dongle with [" + tunerType.toString() + 
									"] tuner";
							break;
					}
					break;
				case ETTUS_USRP_B100:
					mTuners.add(  new B100Tuner( device ) );
					status = sLOADED;
					reason = null;
					break;
				case UNKNOWN:
				default:
					break;
			}

			/**
			 * Log the tuner loading status
			 */
			sb.append( status );
			sb.append( String.format( "%04X", 
					device.getDevice().getUsbDeviceDescriptor().idVendor() ) );
			sb.append( ":" );
			sb.append( String.format( "%04X", 
					device.getDevice().getUsbDeviceDescriptor().idProduct() ) );
			sb.append( " " );

			sb.append( name );
			
			if( reason != null )
			{
				sb.append( " - " );
				sb.append( reason );
			}
			
			sb.append( "\n" );
		}
		
		Log.header( "Configuring USB Tuners" );
		Log.info( sb.toString() );
	}
	
	/**
	 * Get all USB tuner devices
	 */
	private ArrayList<USBTunerDevice> getUSBTunerDevices()
	{
		StringBuilder sb = new StringBuilder();
		
		List<UsbDevice> devices = getUSBDevices();

		sb.append( "TunerManager - discovered [" + devices.size() + 
					"] attached usb devices\n" );
	    
		ArrayList<USBTunerDevice> tuners = new ArrayList<USBTunerDevice>();

        for (UsbDevice device : devices )
        {
        	String status = "\t[NOT RECOGNIZED]\t";
        	
            TunerClass type = TunerClass.valueOf( device.getUsbDeviceDescriptor() );

            if( type != TunerClass.UNKNOWN )
            {
            	status = "\t[RECOGNIZED]    \t";
                tuners.add( new USBTunerDevice( device, type) );
            }
           
            sb.append( status );
            sb.append( device.toString() );
            
            if( type != TunerClass.UNKNOWN )
            {
            	sb.append( " " );
            	sb.append( type.getVendorDescription() );
            	sb.append( " " );
            	sb.append( type.getDeviceDescription() );
            }

            sb.append( "\n" );
        }
        
		Log.header( "USB Device Discovery" );
        Log.info( sb.toString() );

        return tuners;
	}

	/**
	 * Gets all USB devices from the root hub downward, recursively, or 
	 * returns empty list if none are discovered
	 */
    public static List<UsbDevice> getUSBDevices()
    {
        ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();
        
        try
        {
            UsbServices services = UsbHostManager.getUsbServices();
            devices = getUsbChildDevices( services.getRootUsbHub() );
        }
        catch( SecurityException e )
        {
        	Log.error( "TunerManager - security exception while getting "
        			+ "USB devices" );
        }
        catch( UsbException e )
        {
        	Log.error( "TunerManager - usb exception while getting USB "
        			+ "devices - " + e.getLocalizedMessage() );
        }

        return devices;
    }
    
    /**
     * Get (recursively) all usb child devices below the usb hub
     */
    public static ArrayList<UsbDevice> getUsbChildDevices( UsbHub hub )
    {
        ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();

        for ( UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices() )
        {
            devices.add( device );
            
            if ( device.isUsbHub() )
            {
                devices.addAll( getUsbChildDevices( (UsbHub)device ) );
            }
        }
        
        return devices;
    }

    /**
     * Get a stored tuner configuration or create a default tuner configuration
     * for the tuner type, connected at the address.  
     * 
     * Note: a named tuner configuration will be stored for for each tuner type
     * and address combination.   
     * @param type - tuner type
     * @param address - current usb address/port 
     * @return - stored tuner configuration or default configuration
     */
    private TunerConfiguration getTunerConfiguration( Tuner tuner )
    {
    	TunerConfigurationAssignment selected = mResourceManager
			.getSettingsManager().getSelectedTunerConfiguration( 
					tuner.getTunerType(), tuner.getUniqueID() );

    	ArrayList<TunerConfiguration> configs = mResourceManager
			.getSettingsManager().getTunerConfigurations( tuner.getTunerType() );

    	TunerConfiguration config = null;
    	
    	if( selected != null )
    	{
    		for( TunerConfiguration tunerConfig: configs )
    		{
    			if( tunerConfig.getName().contentEquals( selected.getTunerConfigurationName() ) )
    			{
    				config = tunerConfig;
    			}
    		}
    	}
    	else
    	{
    		config = configs.get( 0 );
    	}
    	
    	//If we're still null at this point, create a default config
    	if( config == null )
    	{
    		config = mResourceManager.getSettingsManager()
				.addNewTunerConfiguration( tuner.getTunerType(), "Default" );
    	}
    	
    	//Persist the config as the selected tuner configuration.  The method
    	//auto-saves the setting
    	mResourceManager.getSettingsManager().setSelectedTunerConfiguration( 
    			tuner.getTunerType(), tuner.getUniqueID(), config );
    	
    	return config;
    }

    /**
     * Gets the first tuner mixer dataline that corresponds to the tuner class.
     * 
     * Note: this method is not currently able to align multiple tuner mixer
     * data lines of the same tuner type.  If you have multiple Funcube Dongle
     * tuners of the same TYPE, there is no guarantee that you will get the 
     * correct mixer.
     * 
     * @param tunerClass
     * @return
     */
    private MixerTunerDataLine getMixerTunerDataLine( TunerType tunerClass )
    {
    	Collection<MixerTunerDataLine> datalines = 
    			MixerManager.getInstance().getMixerTunerDataLines();

    	for( MixerTunerDataLine mixerTDL: datalines  )
		{
    		if( mixerTDL.getMixerTunerType().getTunerClass() == tunerClass )
    		{
    			return mixerTDL;
    		}
		}
    	
    	return null;
    }
}
