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
package gui;


import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import log.Log;
import net.miginfocom.swing.MigLayout;
import properties.SystemProperties;
import source.tuner.TunerSelectionListener;
import spectrum.SpectralDisplayPanel;
import util.TimeStamp;

import com.jidesoft.swing.JideSplitPane;

import controller.ControllerPanel;
import controller.ResourceManager;

public class SDRTrunk
{
    private ResourceManager mResourceManager;
	private ControllerPanel mControllerPanel;
	private SpectralDisplayPanel mSpectralPanel;
	private JFrame mMainGui = new JFrame();
    
    public SDRTrunk() 
    {
    	//Setup the application home directory
    	Path home = getHomePath();

    	//Load properties file
    	if( home != null )
    	{
    		loadProperties( home );
    	}

    	//Start the logger
    	Log.init();
		
		//Log current properties setting
		SystemProperties.getInstance().logCurrentSettings();

		/** 
		 * Construct the resource manager now, so that it can use the system
		 * properties that were just loaded 
		 */
		mResourceManager = new ResourceManager();
		
		//Initialize the GUI
        initGUI();

        Log.info( "Starting the main gui" );
        //Start the gui
        EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
                try
                {
                	mMainGui.setVisible( true );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        } );
    }

    /**
     * Launch the application.
     */
    public static void main( String[] args )
    {
        new SDRTrunk();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
    	mMainGui.setLayout( new MigLayout( "insets 0 0 0 0 ", 
    									   "[grow,fill]", 
    									   "[grow,fill]") );
    	
    	mControllerPanel = new ControllerPanel( mResourceManager );
    	
    	mSpectralPanel = new SpectralDisplayPanel( mResourceManager );
    	
    	//Register spectral panel to receive tuner selection events
    	mControllerPanel.getController()
    		.addListener( (TunerSelectionListener)mSpectralPanel );

    	//Add spectrum panel to receive channel change events
    	mResourceManager.getChannelManager().addListener( mSpectralPanel );

    	//init() the controller to load tuners and playlists
    	mControllerPanel.getController().init();

    	//Fire first tuner selection so it is displayed in the spectrum/waterfall
    	mControllerPanel.getController().fireFirstTunerSelected();

    	/**
    	 * Setup main JFrame window
    	 */
    	mMainGui.setTitle( "SDRTrunk" );
    	mMainGui.setBounds( 100, 100, 1280, 800 );
    	mMainGui.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

    	mSpectralPanel.setPreferredSize( new Dimension( 1280, 400 ) );
    	
    	JideSplitPane splitPane = new JideSplitPane( JideSplitPane.VERTICAL_SPLIT );
    	splitPane.setDividerSize( 5 );
    	splitPane.add( "Denny", mSpectralPanel );
    	splitPane.add( "Denny2", mControllerPanel );

    	mMainGui.add( splitPane, "cell 0 0,span,grow");
    	
        /**
         * Menu items
         */
        JMenuBar menuBar = new JMenuBar();
        mMainGui.setJMenuBar( menuBar );
        
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );
        
        JMenuItem settingsMenu = new JMenuItem( "Icon Manager" );
        settingsMenu.addActionListener( new ActionListener()
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				mControllerPanel.getController().showIconManager();
            }
        } );
        fileMenu.add( settingsMenu );

        fileMenu.add( new JSeparator() );
        
        JMenuItem exitMenu = new JMenuItem( "Exit" );
        exitMenu.addActionListener( 
        		new ActionListener() 
        		{
        			public void actionPerformed( ActionEvent event )
        			{
        				System.exit( 0 );
        			}
        		}
        );
        
        fileMenu.add( exitMenu );

        JMenuItem screenCaptureItem = new JMenuItem( "Screen Capture" );
        screenCaptureItem.addActionListener( new ActionListener()
        {
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				try
                {
	                Robot robot = new Robot();
	                
	                final BufferedImage image = 
	                		robot.createScreenCapture( mMainGui.getBounds() );
	                
	            	SystemProperties props = SystemProperties.getInstance();
	            	
	            	Path capturePath = props.getApplicationFolder( "screen_captures" );
	            	
	            	if( !Files.exists( capturePath ) )
	            	{
	            		try
	                    {
	        	            Files.createDirectory( capturePath );
	                    }
	                    catch ( IOException e )
	                    {
	                    	Log.error( "Couldn't create 'screen_captures' "
	                    			+ "subdirectory in the " +
	                    			"SDRTrunk application directory" );
	                    }
	            	}
	            	
	            	String filename = TimeStamp.getTimeStamp( "_" ) + 
	            			"_screen_capture.png";

	            	final Path captureFile = capturePath.resolve( filename );

	            	EventQueue.invokeLater( new Runnable() 
	                {
						@Override
                        public void run()
                        {
							try
                            {
	                            ImageIO.write( image, "png", 
	                            		captureFile.toFile() );
                            }
                            catch ( IOException e )
                            {
                            	Log.error( "Couldn't write screen capture to "
                        			+ "file [" + captureFile.toString() + "]" );
                            }
                        }} );
                }
                catch ( AWTException e )
                {
	                Log.error( "Exception while taking screen capture - " + 
	                			e.getLocalizedMessage() );
                }
            }
        } );
        
        menuBar.add( screenCaptureItem );
    }
    
    /**
     * Loads the application properties file from the user's home directory,
     * creating the properties file for the first-time, if necessary
     */
    private void loadProperties( Path homePath )
    {
		Path propsPath = homePath.resolve( "SDRTrunk.properties" );

		if( !Files.exists( propsPath ) )
		{
			try
            {
				Log.info( "SDRTrunk - creating application properties file [" + 
						propsPath.toAbsolutePath() + "]" );

				Files.createFile( propsPath );
            }
            catch ( IOException e )
            {
	            Log.error( "SDRTrunk - couldn't create application properties "
	            		+ "file [" + propsPath.toAbsolutePath() );
            }
		}
		
		if( Files.exists( propsPath ) )
		{
			SystemProperties.getInstance().load( propsPath );
		}
		else
		{
			Log.error( "SDRTrunk - couldn't find or recreate the SDRTrunk " +
					"application properties file" );
		}
    }

    /**
     * Gets (or creates) the SDRTRunk application home directory.  
     * 
     * Note: the user can change this setting to allow log files and other 
     * files to reside elsewhere on the file system.
     */
    private Path getHomePath()
    {
		Path homePath = FileSystems.getDefault()
				.getPath( System.getProperty( "user.home" ), "SDRTrunk" );

		if( !Files.exists( homePath ) )
		{
			try
	        {
	            Files.createDirectory( homePath );
	            
	            Log.info( "SDRTrunk - created application home directory [" + 
	            				homePath.toString() + "]" );
	        }
	        catch ( Exception e )
	        {
	        	homePath = null;
	        	
	        	Log.error( "SDRTrunk: exception while creating SDRTrunk home " +
	        			"directory in the user's home directory" );
	        }
		}

		return homePath;
    }
}
