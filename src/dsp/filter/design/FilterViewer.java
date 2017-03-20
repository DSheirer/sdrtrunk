package dsp.filter.design;

import dsp.filter.FilterFactory;
import dsp.filter.Window;
import dsp.filter.fir.FIRFilterSpecification;
import dsp.filter.fir.remez.PolyphaseChannelizerFilterFactory;
import dsp.filter.fir.remez.RemezFIRFilterDesigner;
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
//        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
//            .sampleRate(10000)
//            .gridDensity(16)
//            .passBandAmplitude(1.0)
//            .passBandCutoff(4000)
//            .passBandRipple(0.01)
//            .stopBandAmplitude(0.0)
//            .stopBandRipple(0.001)
//            .stopBandStart(5000)
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

        float[] taps = null;

//        int tapCount = FilterFactory.getTapCount(10000000, 12500, 18750, 80);
//		taps = FilterFactory.getLowPass( 10000000, 12500, 5600, Window.WindowType.BLACKMAN_HARRIS_7 );

//        int channels = 10;
//        int channelRate = 12500;
//        int cutoff = 8000;
//        int sampleRate = channelRate * channels;
//        int filterLength = channels * 7;
//
//        taps = FilterFactory.getLowPass(sampleRate, cutoff, filterLength, Window.WindowType.BLACKMAN_HARRIS_7);
//
//        double response = FilterFactory.evaluate(taps, (double)channelRate / (double)sampleRate );
//
//        mLog.debug("Taps:" + taps.length + " With response at [" + channelRate + "]: " + response);

        int channelBandwidth = 12500;
        int channels = 4;
        int tapsPerChannel = 20;

        taps = FilterFactory.getChannelizer(channelBandwidth, channels, tapsPerChannel, Window.WindowType.BLACKMAN_HARRIS_4);

        double sampleRate = channelBandwidth * channels * 2;

        for(int x = 0; x < 25000; x += 100)
        {
            double toEval = (double)x / sampleRate;
            mLog.debug("Response at [" + x + "] is:" + FilterFactory.evaluate(taps, toEval));
        }

        return taps;
    }
}
