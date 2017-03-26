package dsp.filter.design;

import dsp.filter.FilterFactory;
import dsp.filter.Window;
import dsp.filter.fir.FIRFilterSpecification;
import dsp.filter.fir.remez.PolyphaseChannelizerFilterFactory;
import dsp.filter.fir.remez.RemezFIRFilterDesigner;
import dsp.filter.fir.remez.RemezFIRFilterDesigner2;
import dsp.filter.fir.remez.RemezFIRFilterDesignerWithLagrange;
import filter.Filter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class FilterViewer extends Application
{
    private final static Logger mLog = LoggerFactory.getLogger(FilterViewer.class);

    /**
     * Developer tool to visualize filter designs
     */
    public FilterViewer()
    {
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        float[] taps = getFilter();

        if(taps != null)
        {
            Scene scene = new Scene(new FilterView(taps));

            primaryStage.setTitle("Filter Viewer");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
        else
        {
            throw new Exception("Couldn't start application - filter error");
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    /**
     * Provides the filter to visualize.  Modify this method to visualize your filter design.
     */
    private float[] getFilter()
    {
        float[] taps = null;

//        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
//            .sampleRate(1000)
//            .gridDensity(16)
//            .passBandAmplitude(1.0)
//            .passBandCutoff(100)
//            .passBandRipple(0.01)
//            .stopBandAmplitude(0.0)
//            .stopBandRipple(0.0001)
//            .stopBandStart(150)
//            .build();

//		FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
//		        .sampleRate( 24000 )
//		        .stopBandCutoff( 800 )
//		        .stopBandAmplitude( 0.0 )
//		        .stopBandRipple( 0.03 )
//		        .passBandStart( 1000 )
//		        .passBandAmplitude( 1.0 )
//		        .passBandRipple( 0.08 )
//		        .build();

//		FIRFilterSpecification specification = FIRFilterSpecification.bandPassBuilder()
//	        .sampleRate( 48000 )
//	        .stopFrequency1( 200 )
//	        .passFrequencyBegin( 400 )
//	        .passFrequencyEnd( 3200 )
//	        .stopFrequency2( 3400 )
//	        .stopRipple( 0.0003 )
//	        .passRipple( 0.008)
//	        .build();

//        try
//        {
//            RemezFIRFilterDesignerWithLagrange remez = new RemezFIRFilterDesignerWithLagrange(specification);
//            taps = remez.getImpulseResponse();
//        }
//        catch(Exception e)
//        {
//            mLog.error("Error designing filter", e);
//        }

//        taps = FilterFactory.getSinc(0.1, 51, Window.WindowType.BLACKMAN_HARRIS_7);
        try
        {
            taps = FilterFactory.getChannelizer(12500, 50, 14,
                Window.WindowType.BLACKMAN_HARRIS_7, true);
        }
        catch(Exception e)
        {
            mLog.error("Couldn't design filter", e);
        }

        return taps;
    }
}
