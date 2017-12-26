package io.github.dsheirer.source.tuner;

import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.spectrum.SpectrumFrame;

import java.awt.*;

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
