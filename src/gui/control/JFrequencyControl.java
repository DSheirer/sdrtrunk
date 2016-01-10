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
package gui.control;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeListener;
import source.tuner.frequency.IFrequencyChangeProcessor;

public class JFrequencyControl extends JPanel implements IFrequencyChangeProcessor
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( JFrequencyControl.class );

	private List<IFrequencyChangeProcessor> mProcessors = new ArrayList<>();
    
    private Color mHighlightColor = Color.YELLOW;
    
	private long mFrequency;
	
	private Cursor mBlankCursor;
	
	private HashMap<Integer,Digit> mDigits = new HashMap<Integer,Digit>();
	
	public JFrequencyControl()
	{
		init();
	}
	
	public JFrequencyControl( long value )
	{
		this();
		mFrequency = value;
	}
	
	private void init()
	{
		setLayout( new MigLayout( "", "[]0[]", "" ) );
		
		Font font = new Font( Font.MONOSPACED, Font.BOLD, 30 );
		
		for( int x = 9; x >= 0; x-- )
		{
			Digit digit = null;

			try
            {
	            digit = new Digit( x );
            }
            catch ( ParseException e )
            {
            	mLog.error( "JFrequencyControl - parse exception "
            			+ "constructing a digit - " + e );
            }

			if( digit != null )
			{
				mDigits.put( x, digit );
				
				add( digit );
				
				digit.setFont( font );

				if( x == 6 )
				{
					JLabel period = new JLabel( "." );
					
					add( period );
				}
			}
		}
		
		add( new JLabel( " MHz" ) );

		/**
		 * Create a blank cursor to use when the mouse is over the digits
		 */
		//Create an empty byte array  
		byte[] imageByte = new byte[ 0 ];  
		  
		//Create image for cursor using empty array  
		Image cursorImage = Toolkit.getDefaultToolkit().createImage( imageByte );  
		  
		mBlankCursor = Toolkit.getDefaultToolkit()
				.createCustomCursor( cursorImage, new Point( 0, 0 ), "cursor" );
		
		revalidate();
	}

	/**
	 * Receives a frequency change event invoked by another control.  We don't
	 * rebroadcast this event, just set the control to indicate the new frequency.
	 */
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		if( event.getEvent() == Event.NOTIFICATION_FREQUENCY_CHANGE )
		{
			setFrequency( event.getValue().longValue(), false );
		}
    }
	
	/**
	 * Loads the frequency into the display and optionally fires a change event
	 * to all registered listeners
	 */
	public void setFrequency( long frequency, boolean fireChangeEvent )
	{
		mFrequency = frequency;
		
		for( Digit digit: mDigits.values() )
		{
			digit.setFrequency( frequency, fireChangeEvent );
		}
	}
	
	public long getFrequency()
	{
		return mFrequency;
	}
	
	private void updateFrequency()
	{
		long frequency = 0;
		
		for( Digit digit: mDigits.values() )
		{
			frequency += digit.getFrequency();
		}
		
		mFrequency = frequency;
	}
	
	private void fireFrequencyChanged()
	{
		updateFrequency();
		
		Iterator<IFrequencyChangeProcessor> it = mProcessors.iterator();
		
		FrequencyChangeEvent event = 
				new FrequencyChangeEvent( Event.NOTIFICATION_FREQUENCY_CHANGE, mFrequency );
		
		while( it.hasNext() )
		{
			it.next().frequencyChanged( event );
		}
	}
	
	public void addListener( IFrequencyChangeProcessor processor )
	{
		mProcessors.add( processor );
	}
	
	public void removeListener( IFrequencyChangeProcessor processor )
	{
		mProcessors.remove( processor );
	}
	
    public class Digit extends JTextField
	{
        private static final long serialVersionUID = 1L;
        private int mPower = 0;
        private long mValue = 0;
        
        private Digit( int position ) throws ParseException
        {
        	super( "0" );

        	mPower = position;

            Listener listener = new Listener();

        	this.addKeyListener( listener );
            this.addMouseListener( listener );
        	this.addMouseWheelListener( listener );
        }
        
        /**
         * Sets this digit to the value of the column in frequency that corresponds
         * to the power (ie column) set for this digit.  Optionally, fires a 
         * value change event to all listeners.
         */
        public void setFrequency( long frequency, boolean fireChangeEvent )
        {
        	//Strip the digits higher than this one
        	long lower = frequency % (long)( Math.pow( 10, mPower + 1) );

        	//Set the value to int value of dividing by 10 to this power
        	long value = (long)( lower / (long)( Math.pow( 10, mPower ) ) );
        	
        	set( value, fireChangeEvent );
        }
        
        public long getFrequency()
        {
        	return mValue * (long)Math.pow( 10, mPower );
        }

        public void increment()
        {
        	increment( true );
        }

        public void increment( boolean fireChangeEvent )
        {
        	set( mValue + 1, fireChangeEvent );
        }

        public void decrement()
        {
        	decrement( true );
        }
        
        public void decrement( boolean fireChangeEvent )
        {
        	set( mValue - 1, fireChangeEvent );
        }

        /**
         * Convenience wrapper to change amount and fire change event
         */
        private void set( long amount )
        {
        	set( amount, true );
        }

        /**
         * Changes the value and optionally fires change event to listeners
         */
        private void set( long amount, boolean fireChangeEvent )
        {
        	mValue = amount;

        	while( mValue < 0 )
        	{
        		mValue += 10;

        		Digit nextHigherDigit = mDigits.get( mPower + 1 );
        		
        		if( nextHigherDigit != null )
        		{
        			nextHigherDigit.decrement( false );
        		}
        		
        	}

        	while( mValue > 9 )
        	{
        		mValue -= 10;

        		Digit nextHigherDigit = mDigits.get( mPower + 1 );
        		
        		if( nextHigherDigit != null )
        		{
        			nextHigherDigit.increment( false );
        		}
        	}

        	setText( String.valueOf( mValue ) );

        	repaint();
        	
        	if( fireChangeEvent )
        	{
            	fireFrequencyChanged();
        	}
        	
        }

        private class Listener implements KeyListener,
        							      MouseListener, 
        								  MouseWheelListener
        {
			@Override
            public void keyReleased( KeyEvent e )
            {
				int key = e.getKeyCode();

				switch( key )
				{
					case KeyEvent.VK_0:
					case KeyEvent.VK_NUMPAD0:
						set( 0 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_1:
					case KeyEvent.VK_NUMPAD1:
						set( 1 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_2:
					case KeyEvent.VK_NUMPAD2:
						set( 2 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_3:
					case KeyEvent.VK_NUMPAD3:
						set( 3 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_4:
					case KeyEvent.VK_NUMPAD4:
						set( 4 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_5:
					case KeyEvent.VK_NUMPAD5:
						set( 5 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_6:
					case KeyEvent.VK_NUMPAD6:
						set( 6 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_7:
					case KeyEvent.VK_NUMPAD7:
						set( 7 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_8:
					case KeyEvent.VK_NUMPAD8:
						set( 8 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_9:
					case KeyEvent.VK_NUMPAD9:
						set( 9 );
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_LEFT:
						Digit.this.transferFocusBackward();
						break;
					case KeyEvent.VK_RIGHT:
						Digit.this.transferFocus();
						break;
					case KeyEvent.VK_UP:
						increment();
						break;
					case KeyEvent.VK_DOWN:
						decrement();
						break;
					case KeyEvent.VK_TAB:
						break;
					default:
						set( mValue );
						break;
				}
				
				repaint();
            }
			
			@Override
            public void keyPressed( KeyEvent e ) {}
			@Override
            public void keyTyped( KeyEvent e ) {}

			@Override
            public void mouseClicked( MouseEvent e )
            {
				switch( e.getButton() )
				{
					case MouseEvent.BUTTON1:
						increment();
						break;
					case MouseEvent.BUTTON2:
						break;
					case MouseEvent.BUTTON3:
						decrement();
						break;
				}
            }

            public void mousePressed( MouseEvent e ) {}
            public void mouseReleased( MouseEvent e ) {}

			@Override
            public void mouseEntered( MouseEvent e )
            {
				Digit.this.setBackground( mHighlightColor );

				setCursor( mBlankCursor );
				
				repaint();
            }

			@Override
            public void mouseExited( MouseEvent e )
            {
				Digit.this.setBackground( Color.WHITE );
				
				setCursor( Cursor.getDefaultCursor() );

				repaint();
            }

			@Override
            public void mouseWheelMoved( MouseWheelEvent e )
            {
				set( mValue - e.getWheelRotation() );
            }
        }
	}
}
