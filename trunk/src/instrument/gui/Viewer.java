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
package instrument.gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.Source;
import source.wave.ComplexWaveSource;
import source.wave.FloatWaveSource;
import source.wave.WaveSource;

public class Viewer
{
	private final static Logger mLog = LoggerFactory.getLogger( Viewer.class );

	private JFrame mFrame;
	private JDesktopPane mDesktop;
	
	private Source mSource;
	
	public Viewer()
	{
		initGUI();
	}
	
	public void setVisible( boolean visible )
	{
		mFrame.setVisible( visible );
	}
	
    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
    	/* Setup main JFrame window */
    	mFrame = new JFrame();
    	mFrame.setTitle( "SDRTrunk - Sample File Viewer" );
    	mFrame.setBounds( 100, 100, 800, 600 );
    	mFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        /* Multi-window desktop */
        mDesktop = new JDesktopPane();
        mFrame.add( mDesktop );
        
//		JInternalFrame frame = 
//		new JInternalFrame( "New", true, true, true, true );
//		frame.add( new JLabel( "This is a test" ) );
//		frame.pack();
//		mDesktop.add( frame );
//		frame.setVisible( true );

		/* Menu items */
        JMenuBar menuBar = new JMenuBar();
        mFrame.setJMenuBar( menuBar );
        
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );
        
        fileMenu.add( new SourceFileItem() );
        fileMenu.add( new JSeparator() );
        
        JMenuItem exitMenu = new JMenuItem( "Exit" );
        exitMenu.addActionListener( new ActionListener() 
		{
			public void actionPerformed( ActionEvent event )
			{
				System.exit( 0 );
			}
		} );
        
        fileMenu.add( exitMenu );
        
//        JMenuItem windowItem = new JMenuItem( "New Window" );
//        windowItem.addActionListener( new ActionListener() 
//		{
//			@Override
//			public void actionPerformed( ActionEvent arg0 )
//			{
//				JInternalFrame frame = 
//		        		new JInternalFrame( "New", true, true, true, true );
//
//		        frame.add( new JLabel( "This is a test" ) );
//		        frame.pack();
//		        
//		        mDesktop.add( frame );
//		        
//		        frame.setVisible( true );
//			}
//		} );
//        menuBar.add( windowItem );
    }
    
    private void setSourceFile( File file )
    {
    	mLog.info( "File selected [" + file.getAbsolutePath() + "]");

    	WaveSource source = null;
    	
    	/* Attempt to open file as a 1-channel float source */
    	try
    	{
    		FloatWaveSource floatSource = new FloatWaveSource( file, false );
    		floatSource.open();
    		
    		source = floatSource;
    		
    		mLog.info( "File opened as float wave file" );
    		
    		AudioSourceFrame sourcePanel = 
					new AudioSourceFrame( source, mDesktop );
    		sourcePanel.setVisible( true );
    		mDesktop.add( sourcePanel );
    	}
    	catch( Exception e )
    	{
    		mLog.error( "Couldn't open file as float single-channel source" );
    	}
    	
    	if( source == null )
    	{
        	/* Attempt to open file as a 2-channel complex source */
        	try
        	{
        		ComplexWaveSource complex = new ComplexWaveSource( file, false );
        		complex.open();
        		
        		source = complex;

        		mLog.info( "File opened as complex wave file" );
        		
        		BasebandSourceFrame sourcePanel = 
        				new BasebandSourceFrame( complex, mDesktop );
        		
        		sourcePanel.setVisible( true );
        		
        		mDesktop.add( sourcePanel );
        	}
        	catch( Exception e )
        	{
        		mLog.error( "Couldn't open file as float source", e);
        	}
    	}
    	
    	if( source != null )
    	{
    		mSource = source;

    	}
    }
	
	public static void main( String[] args )
	{
		final Viewer viewer = new Viewer();
		
        EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
                try
                {
                	viewer.setVisible( true );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        } );
		
	}
	
	public class SourceFileItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;

		public SourceFileItem()
		{
			super( "Open" );
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent arg0 )
				{
					final JFileChooser fc = new JFileChooser();
					fc.setFileFilter( new FileFilter() 
					{
						@Override
                        public boolean accept( File file )
                        {
	                        return file.isDirectory() || 
	                        	   file.getName().endsWith( ".wav" );
                        }

						@Override
                        public String getDescription()
                        {
	                        return "Float or Complex 16-bit Wave Files";
                        }
					} );
					
					int val = fc.showOpenDialog( mFrame );

					if( val == JFileChooser.APPROVE_OPTION )
					{
						final File file = fc.getSelectedFile();
						
						EventQueue.invokeLater( new Runnable()
						{
							@Override
                            public void run()
                            {
								setSourceFile( file );
                            }
						} );
					}
				}
			} );
		}
	}
}
