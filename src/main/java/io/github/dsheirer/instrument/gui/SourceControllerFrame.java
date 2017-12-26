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
package io.github.dsheirer.instrument.gui;

import io.github.dsheirer.source.IControllableFileSource;
import io.github.dsheirer.source.IFrameLocationListener;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;

public class SourceControllerFrame extends JInternalFrame 
				implements IFrameLocationListener
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( SourceControllerFrame.class );

	private IControllableFileSource mSource;
	private JDesktopPane mDesktop;
	
	private JLabel mCurrentPosition = new JLabel( "0" );
	
	public SourceControllerFrame( IControllableFileSource source, 
						     JDesktopPane desktop )
	{
		mSource = source;
		mSource.setListener( this );
		
		mDesktop = desktop;
		
		initGui();
	}
	
	private void initGui()
	{
		setTitle( "Source [" + mSource.getFile().getAbsolutePath() + "]" );
		setPreferredSize( new Dimension( 670, 70 ) );
		setSize( 670, 70 );

		setResizable( true );
		setClosable( true );
		setIconifiable( true );
		setMaximizable( false );

		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout() );

		JButton decoderButton = new JButton( "Decoders" );
		decoderButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				EventQueue.invokeLater( new Runnable() 
				{
					@Override
					public void run()
					{
						DecoderSelectionFrame frame = 
								new DecoderSelectionFrame( mDesktop, mSource );
						
						frame.setVisible( true );
						
						mDesktop.add( frame );
					}
				} );
			}
		} );
		
		panel.add( decoderButton );

		panel.add( new JLabel( "Skip:" ) );
		panel.add( new SkipFramesField( mSource ) );
		panel.add( new NextFrameButton( mSource, "> 1", 1 ) );
		panel.add( new NextFrameButton( mSource, "> 10", 10 ) );
		panel.add( new NextFrameButton( mSource, "> 100", 100 ) );
		panel.add( new NextFrameButton( mSource, "> 1000", 1000 ) );
		panel.add( new JLabel( "Posn:" ) );
		panel.add( mCurrentPosition, "wrap" );
		
		add( panel );
	}
	
	public class NextFrameButton extends JButton
	{
		private static final long serialVersionUID = 1L;
		
		private IControllableFileSource mSource;
		private int mFrames;
		
		public NextFrameButton( IControllableFileSource source, 
				String label, int frames )
		{
			super( label );
			mSource = source;
			mFrames = frames;
			
			addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent arg0 )
                {
					EventQueue.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							try
		                    {
			                    mSource.next( mFrames );
		                    }
		                    catch ( IOException e )
		                    {
		                    	mLog.error( "Viewer - reading frames [" + mFrames + 
		                    			"] from source", e );

		                    	JOptionPane.showMessageDialog( SourceControllerFrame.this,
		                    		    "Can't read " + mFrames + " more frames [" + 
		                    		    		e.getLocalizedMessage() + "]",
		                    		    "Wave File Error",
		                    		    JOptionPane.ERROR_MESSAGE );                    	
		                    }
						}
					} );
                }
			} );
		}
	}

	public class SkipFramesField extends JTextField
	{
		private static final long serialVersionUID = 1L;
		
		private IControllableFileSource mSource;
		
		public SkipFramesField( IControllableFileSource source )
		{
			super( "0" );
			
			mSource = source;
			
			setMinimumSize( new Dimension( 100, getHeight() ) );

			addFocusListener( new FocusListener() 
			{
				@Override
                public void focusGained( FocusEvent arg0 ) {}

				@Override
                public void focusLost( FocusEvent arg0 )
                {
					try
					{
						int framesToSkip = Integer.parseInt( getText() );
						
						mSource.next( framesToSkip, false );
					}
					catch( Exception e )
					{
						mLog.error( "WaveSourceFrame - error skipping frames", e );
						
                    	JOptionPane.showMessageDialog( SourceControllerFrame.this,
                    		    "Can't skip [" + getText() + "] frames.",
                    		    "Error",
                    		    JOptionPane.ERROR_MESSAGE );                    	
					}
                }
			} );
		}
	}

	
	@Override
	public void frameLocationUpdated( final int location )
	{
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
			public void run()
			{
				mCurrentPosition.setText( String.valueOf( location ) );
			}
		} );
	}

	@Override
	public void frameLocationReset()
	{
		// TODO Auto-generated method stub
		
	}
}
