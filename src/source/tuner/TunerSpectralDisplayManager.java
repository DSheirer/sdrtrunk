package source.tuner;

import java.awt.EventQueue;

import sample.Listener;
import settings.SettingsManager;
import spectrum.SpectralDisplayPanel;
import spectrum.SpectrumFrame;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;

public class TunerSpectralDisplayManager implements Listener<TunerEvent>
{
	private SpectralDisplayPanel mSpectralDisplayPanel;
	private ChannelModel mChannelModel;
	private ChannelProcessingManager mChannelProcessingManager;
	private SettingsManager mSettingsManager;

	public TunerSpectralDisplayManager( SpectralDisplayPanel panel,
										ChannelModel channelModel,
										ChannelProcessingManager channelProcessingManager,
										SettingsManager settingsManager )
	{
		mSpectralDisplayPanel = panel;
		mChannelModel = channelModel;
		mChannelProcessingManager = channelProcessingManager;
		mSettingsManager = settingsManager;
	}
	
	@Override
	public void receive( TunerEvent event )
	{
		switch( event.getEvent() )
		{
			case REQUEST_MAIN_SPECTRAL_DISPLAY:
				mSpectralDisplayPanel.showTuner( event.getTuner() );
				break;
			case REQUEST_NEW_SPECTRAL_DISPLAY:
				final SpectrumFrame frame = new SpectrumFrame( mChannelModel,
					mChannelProcessingManager, mSettingsManager, event.getTuner() );	
				
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						frame.setVisible( true );
					}
				} );
				break;
			default:
				break;
		}
	}
}
