package dsp.filter.design;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dsp.filter.fir.FIRFilterSpecification;
import dsp.filter.fir.remez.RemezFIRFilterDesigner;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FilterViewer extends Application
{
	private final static Logger mLog = LoggerFactory.getLogger( FilterViewer.class );

	/**
	 * Developer tool to visualize filter designs
	 */
	public FilterViewer()
	{
	}
	
	@Override
	public void start( Stage primaryStage ) throws Exception
	{
		Scene scene = new Scene( new FilterView( getFilter() ) );
		
		primaryStage.setTitle( "Filter Viewer" );
		primaryStage.setScene( scene );
		primaryStage.show();
	}
	
	public static void main( String[] args )
	{
		launch( args );
	}

	/**
	 * Provides the filter to visualize.  Modify this method to visualize your filter design.
	 */
	private float[] getFilter()
	{
		FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
		        .sampleRate( 48000 )
		        .gridDensity( 16 )
                .passBandCutoff( 3000 )
		        .passBandAmplitude( 1.0 )
		        .passBandRipple( 0.01 )
		        .stopBandStart( 4000 )
		        .stopBandAmplitude( 0.0 )
		        .stopBandRipple( 0.027 )
		        .build();
		
//		FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
//		        .sampleRate( 8000 )
//		        .gridDensity( 16 )
//		        .stopBandCutoff( 2000 )
//		        .stopBandAmplitude( 0.0 )
//		        .stopBandRipple( 0.003 )
//		        .passBandStart( 3000 )
//		        .passBandAmplitude( 1.0 )
//		        .passBandRipple( 0.001 )
//		        .build();

//		FIRFilterSpecification specification = FIRFilterSpecification.bandPassBuilder()
//	        .sampleRate( 8000 )
//	        .order( 164 )
//	        .stopFrequency1( 1100 )
//	        .passFrequencyBegin( 1200 )
//	        .passFrequencyEnd( 2400 )
//	        .stopFrequency2( 2500 )
//	        .stopRipple( 0.01 )
//	        .passRipple( 0.01 )
//	        .build();

		float[] taps = null;

//		taps = FilterFactory.getLowPass( 16000, 2400, 73, WindowType.HANNING );

		try
		{
			RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner( specification );
			
			if( designer.isValid() )
			{
				taps = designer.getImpulseResponse();
			}
		}
		catch( FilterDesignException fde )
		{
		    mLog.error( "Filter design error", fde );
		}
		
		return taps;
	}
}
