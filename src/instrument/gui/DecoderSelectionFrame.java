package instrument.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import module.decode.DecodeComponentEditor;
import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import playlist.PlaylistManager;
import source.IControllableFileSource;
import alias.AliasModel;
import controller.ThreadPoolManager;
import controller.channel.ChannelModel;

public class DecoderSelectionFrame extends JInternalFrame
{
	private static final long serialVersionUID = 1L;

	private DecodeComponentEditor mDecodeEditor = new DecodeComponentEditor( null );
	private PlaylistManager mPlaylistManager;
	
	private IControllableFileSource mSource;
	private JDesktopPane mDesktop;
	
	public DecoderSelectionFrame( JDesktopPane desktop, 
								  IControllableFileSource source )
	{
		AliasModel aliasModel = new AliasModel();
		ChannelModel channelModel = new ChannelModel();
		
		ThreadPoolManager tpm = new ThreadPoolManager();
		
		mPlaylistManager = new PlaylistManager( tpm, aliasModel, channelModel );

		mDesktop = desktop;
		mSource = source;
		
		initGUI();
	}
	
	private void initGUI()
	{
        setLayout( new MigLayout( "", "[grow,fill]", "[][][grow,fill]" ) );

		setTitle( "Decoders" );
		setPreferredSize( new Dimension( 700, 450 ) );
		setSize( 700, 450 );

		setResizable( true );
		setClosable( true ); 
		setIconifiable( true );
		setMaximizable( false );

		add( mDecodeEditor, "wrap" );
		
		add( new AddDecoderButton(), "span" );
		
	}
	
	public class AddDecoderButton extends JButton
	{
		private static final long serialVersionUID = 1L;

		public AddDecoderButton()
		{
			super( "Add" );
			
			addActionListener( new ActionListener() 
			{
				@Override
				public void actionPerformed( ActionEvent arg0 )
				{
					DecodeConfiguration config = mDecodeEditor.getDecodeConfig();
					
					if( config != null )
					{
						DecoderViewFrame decoderFrame = new DecoderViewFrame( 
								mPlaylistManager, null, mSource );

						decoderFrame.setVisible( true );
						
						mDesktop.add( decoderFrame );
					}
				}
			} );
		}
	}
	
}
