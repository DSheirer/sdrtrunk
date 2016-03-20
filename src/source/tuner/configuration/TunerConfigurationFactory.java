package source.tuner.configuration;

import gui.editor.Editor;
import gui.editor.EmptyEditor;
import source.tuner.Tuner;
import source.tuner.TunerType;
import source.tuner.airspy.AirspyTuner;
import source.tuner.airspy.AirspyTunerConfiguration;
import source.tuner.airspy.AirspyTunerEditor;
import source.tuner.fcd.proV1.FCD1TunerConfiguration;
import source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import source.tuner.hackrf.HackRFTunerConfiguration;
import source.tuner.rtl.RTL2832Tuner;
import source.tuner.rtl.e4k.E4KTunerConfiguration;
import source.tuner.rtl.e4k.E4KTunerEditor;
import source.tuner.rtl.r820t.R820TTunerConfiguration;
import source.tuner.rtl.r820t.R820TTunerEditor;

public class TunerConfigurationFactory
{
	/**
	 * Creates a tuner configuration for the specified tuner type, unique ID and name
	 */
	public static TunerConfiguration getTunerConfiguration( TunerType type, 
			String uniqueID, String name )
	{
		switch( type )
		{
			case AIRSPY_R820T:
				return new AirspyTunerConfiguration( uniqueID, name );
			case ELONICS_E4000:
				return new E4KTunerConfiguration( uniqueID, name );
			case FUNCUBE_DONGLE_PRO:
				return new FCD1TunerConfiguration( uniqueID, name );
			case FUNCUBE_DONGLE_PRO_PLUS:
				return new FCD2TunerConfiguration( uniqueID, name );
			case HACKRF:
				return new HackRFTunerConfiguration( uniqueID, name );
			case RAFAELMICRO_R820T:
				return new R820TTunerConfiguration( uniqueID, name );
			default:
				throw new IllegalArgumentException( "Unrecognized tuner type ["
					+ type.name() + "] - can't create named [" + name + "] tuner"
					+ " configuration" );
		}
	}
	
	/**
	 * Creates a tuner editor gui for the specified tuner
	 */
    public static Editor<TunerConfiguration> getEditor( Tuner tuner, 
    		TunerConfigurationModel model )
    {
    	switch( tuner.getTunerType() )
    	{
			case AIRSPY_R820T:
				return new AirspyTunerEditor( model, (AirspyTuner)tuner );
			case ELONICS_E4000:
				return new E4KTunerEditor( model, (RTL2832Tuner)tuner );
			case FUNCUBE_DONGLE_PRO:
		    	return new EmptyEditor<TunerConfiguration>( "a tuner" );
			case FUNCUBE_DONGLE_PRO_PLUS:
		    	return new EmptyEditor<TunerConfiguration>( "a tuner" );
			case HACKRF:
		    	return new EmptyEditor<TunerConfiguration>( "a tuner" );
			case RAFAELMICRO_R820T:
				return new R820TTunerEditor( model, (RTL2832Tuner)tuner );
			case UNKNOWN:
			default:
				throw new IllegalArgumentException( "Unrecognized Tuner: " + tuner.getName() );
    	}
    }
}
