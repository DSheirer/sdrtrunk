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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import properties.SystemProperties;
import source.tuner.Tuner;
import source.tuner.TunerSelectionListener;
import spectrum.SpectralDisplayPanel;
import util.TimeStamp;

import com.jidesoft.swing.JideSplitPane;

import controller.ControllerPanel;
import controller.ResourceManager;

public class SDRTrunk
{
	private final static Logger mLog = LoggerFactory.getLogger( SDRTrunk.class );

	private ResourceManager mResourceManager;
	private ControllerPanel mControllerPanel;
	private SpectralDisplayPanel mSpectralPanel;
	private JFrame mMainGui = new JFrame();
    
    public SDRTrunk() 
    {
    	mLog.info( "" );
    	mLog.info( "" );
    	mLog.info( "*******************************************************************" );
    	mLog.info( "**** SDRTrunk: a trunked radio and digital decoding application ***" );
    	mLog.info( "****  website: https://code.google.com/p/sdrtrunk               ***" );
    	mLog.info( "*******************************************************************" );
    	mLog.info( "" );
    	mLog.info( "" );
    	
    	//Setup the application home directory
    	Path home = getHomePath();
    	
    	mLog.info( "Home path: " + home.toString() );

    	//Load properties file
    	if( home != null )
    	{
    		loadProperties( home );
    	}
		
		//Log current properties setting
		SystemProperties.getInstance().logCurrentSettings();

		/** 
		 * Construct the resource manager now, so that it can use the system
		 * properties that were just loaded 
		 */
		mResourceManager = new ResourceManager();
		
		//Initialize the GUI
        initGUI();

    	mLog.info( "starting main application gui" );

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

    	/**
    	 * Setup main JFrame window
    	 */
    	mMainGui.setTitle( "SDRTrunk" );
    	mMainGui.setBounds( 100, 100, 1280, 800 );
    	mMainGui.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    	
    	mControllerPanel.getController().addListener( new TunerSelectionListener() 
    	{
			@Override
            public void tunerSelected( Tuner tuner )
            {
				if( tuner != null )
				{
					mMainGui.setTitle( "SDRTrunk - " + tuner.getName() );
				}
				else
				{
			    	mMainGui.setTitle( "SDRTrunk" );
				}
            }
    	} );

    	//Fire first tuner selection so it is displayed in the spectrum/waterfall
    	mControllerPanel.getController().fireFirstTunerSelected();

    	mSpectralPanel.setPreferredSize( new Dimension( 1280, 400 ) );
    	
    	JideSplitPane splitPane = new JideSplitPane( JideSplitPane.VERTICAL_SPLIT );
    	splitPane.setDividerSize( 5 );
    	splitPane.add( mSpectralPanel );
    	splitPane.add( mControllerPanel );

    	mMainGui.add( splitPane, "cell 0 0,span,grow");
    	
        /**
         * Menu items
         */
        JMenuBar menuBar = new JMenuBar();
        mMainGui.setJMenuBar( menuBar );
        
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        JMenuItem logFilesMenu = new JMenuItem( "Logs & Recordings" );
        logFilesMenu.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent arg0 )
			{
				try
				{
					Desktop.getDesktop().open( getHomePath().toFile() );				
				}
				catch( Exception e )
				{
					mLog.error( "Couldn't open file explorer" );
					
					JOptionPane.showMessageDialog( mMainGui, 
						"Can't launch file explorer - files are located at: " + 
										getHomePath().toString(),
						"Can't launch file explorer",
						JOptionPane.ERROR_MESSAGE );
				}
			}
		} );
        fileMenu.add( logFilesMenu );
        
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
	                    	mLog.error( "Couldn't create 'screen_captures' "
	                    			+ "subdirectory in the " +
	                    			"SDRTrunk application directory", e );
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
                            	mLog.error( "Couldn't write screen capture to "
                    			+ "file [" + captureFile.toString() + "]", e );
                            }
                        }} );
                }
                catch ( AWTException e )
                {
                	mLog.error( "Exception while taking screen capture", e );
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
				mLog.info( "SDRTrunk - creating application properties file [" + 
						propsPath.toAbsolutePath() + "]" );

				Files.createFile( propsPath );
            }
            catch ( IOException e )
            {
            	mLog.error( "SDRTrunk - couldn't create application properties "
	            		+ "file [" + propsPath.toAbsolutePath(), e );
            }
		}
		
		if( Files.exists( propsPath ) )
		{
			SystemProperties.getInstance().load( propsPath );
		}
		else
		{
			mLog.error( "SDRTrunk - couldn't find or recreate the SDRTrunk " +
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
	            
	            mLog.info( "SDRTrunk - created application home directory [" + 
	            				homePath.toString() + "]" );
	        }
	        catch ( Exception e )
	        {
	        	homePath = null;
	        	
	        	mLog.error( "SDRTrunk: exception while creating SDRTrunk home " +
	        			"directory in the user's home directory", e );
	        }
		}

		return homePath;
    }
}
