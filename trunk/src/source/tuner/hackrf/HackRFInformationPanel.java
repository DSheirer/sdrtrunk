package source.tuner.hackrf;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.hackrf.HackRFTunerController.Serial;

public class HackRFInformationPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

	private final static Logger mLog = 
			LoggerFactory.getLogger( HackRFInformationPanel.class );

	private HackRFTunerController mController;

	/**
	 * Displays HackRF firmware, serial number and part identifier 
	 */
	public HackRFInformationPanel( HackRFTunerController controller )
	{
		mController = controller;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", 
				  				  "[right,grow][grow]", 
								  "[][][][grow]" ) );

		Serial serial = null;
		
		try
		{
			serial = mController.getSerial();
		}
		catch( Exception e )
		{
			mLog.error( "couldn't read HackRF serial number", e );
		}
		
		if( serial != null )
		{
			add( new JLabel( "Serial:" ) );
			add( new JLabel( serial.getSerialNumber() ) );
			add( new JLabel( "Part:" ) );
			add( new JLabel( serial.getPartID() ) );
		}
		else
		{
			add( new JLabel( "Serial:" ) );
			add( new JLabel( "Unknown" ) );
			add( new JLabel( "Part:" ) );
			add( new JLabel( "Unknown" ) );
		}
		
		String firmware = null;
		
		try
		{
			firmware = mController.getFirmwareVersion();
		}
		catch( Exception e )
		{
			mLog.error( "couldn't read HackRF firmware version", e );
		}
		
		add( new JLabel( "Firmware:" ) );
		
		if( firmware != null )
		{
			add( new JLabel( firmware ) );
		}
		else
		{
			add( new JLabel( "Unknown" ) );
		}
	}
}
