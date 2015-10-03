package audio;

import java.awt.Color;
import java.util.List;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;
import audio.output.AudioOutput;

public class AudioChannelsPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	public AudioChannelsPanel( SettingsManager settingsManager, 
							   IAudioController controller )
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[]0[]") );
		
		setBackground( Color.BLACK );

		List<AudioOutput> outputs = controller.getAudioOutputs();
		
		for( AudioOutput audioOutput: controller.getAudioOutputs() )
		{
			add( new AudioChannelPanel( settingsManager, audioOutput ), "wrap" );
		}

		/* Add an empty channel panel so that the panel is sized appropriately
		 * for either a single channel or two channels */
		if( outputs.size() == 1 )
		{
			add( new AudioChannelPanel( settingsManager, null ), "wrap" );
		}
	}
}
