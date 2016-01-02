/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package spectrum;

import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;
import playlist.PlaylistManager;
import settings.SettingsManager;
import source.tuner.Tuner;
import controller.ConfigurationControllerModel;
import controller.ResourceManager;
import controller.channel.ChannelModel;

public class SpectrumFrame extends JFrame implements WindowListener
{
    private static final long serialVersionUID = 1L;

    private ResourceManager mResourceManager;
    private SpectralDisplayPanel mSpectralDisplayPanel;
	
	public SpectrumFrame( ChannelModel channelModel,
						  ConfigurationControllerModel controller,
						  PlaylistManager playlistManager,
						  SettingsManager settingsManager,
						  Tuner tuner )
	{
    	setTitle( "SDRTRunk [" + tuner.getName() + "]" );
    	setBounds( 100, 100, 1280, 600 );
    	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

    	setLayout( new MigLayout( "insets 0 0 0 0", "[grow]", "[grow]") );
    	
		mSpectralDisplayPanel = new SpectralDisplayPanel( channelModel, 
				controller, playlistManager, settingsManager );
		
		mSpectralDisplayPanel.tunerSelected( tuner );
		add( mSpectralDisplayPanel, "grow" );

		/* Register a shutdown listener */
		this.addWindowListener( this );
		
		EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
            	setVisible( true );
            }
        } );
	}

	@Override
    public void windowClosed( WindowEvent arg0 )
    {
		mSpectralDisplayPanel.dispose();
    }

	@Override
    public void windowActivated( WindowEvent arg0 ) {}
	@Override
    public void windowClosing( WindowEvent arg0 ) {}
	@Override
    public void windowDeactivated( WindowEvent arg0 ) {}
	@Override
    public void windowDeiconified( WindowEvent arg0 ) {}
	@Override
    public void windowIconified( WindowEvent arg0 ) {}
	@Override
    public void windowOpened( WindowEvent arg0 ) {}
}
