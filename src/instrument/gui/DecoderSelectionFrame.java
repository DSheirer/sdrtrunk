package instrument.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import module.decode.DecodeComponentEditor;
import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import source.IControllableFileSource;
import controller.ResourceManager;

public class DecoderSelectionFrame extends JInternalFrame
{
	private static final long serialVersionUID = 1L;

	private DecodeComponentEditor mDecodeEditor = new DecodeComponentEditor();
	private ResourceManager mResourceManager = new ResourceManager();
	
	private IControllableFileSource mSource;
	private JDesktopPane mDesktop;
	
	public DecoderSelectionFrame( JDesktopPane desktop, 
								  IControllableFileSource source )
	{
		mDesktop = desktop;
		mSource = source;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setTitle( "Decoders" );
		setPreferredSize( new Dimension( 700, 150 ) );
		setSize( 700, 150 );

		setResizable( true );
		setClosable( true ); 
		setIconifiable( true );
		setMaximizable( false );

		add( mDecodeEditor );
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
								mResourceManager, 
								config,
								null,
								null,
								null,
								mSource );

						decoderFrame.setVisible( true );
						
						mDesktop.add( decoderFrame );
					}
				}
			} );
		}
	}
	
}
