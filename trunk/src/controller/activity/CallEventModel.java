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
package controller.activity;

import java.awt.EventQueue;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;

import log.Log;
import sample.Broadcaster;
import sample.Listener;
import controller.activity.CallEvent.CallEventType;

public class CallEventModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;
    
	private static DecimalFormat mFrequencyFormatter = 
			new DecimalFormat( "0.000000" );
    
    private static final int DEFAULT_ICON_HEIGHT = 14;
    
    public static final int TIME = 0;
    public static final int EVENT = 1;
    public static final int FROM_ID = 2;
    public static final int FROM_ALIAS = 3;
    public static final int TO_ID = 4;
    public static final int TO_ALIAS = 5;
    public static final int CHANNEL = 6;
    public static final int FREQUENCY = 7;
    public static final int DETAILS = 8;

	protected int mMaxMessages = 500;

	protected LinkedList<CallEvent> mEvents = new LinkedList<CallEvent>();

	protected String[] mHeaders = new String[] { "Time",
												 "Event",
												 "From",
												 "Alias",
												 "To",
												 "Alias",
												 "Channel",
												 "Frequency",
												 "Details" };

	private SimpleDateFormat mSDFTime = new SimpleDateFormat( "HH:mm:ss" );
	
	private Broadcaster<CallEvent> mBroadcaster = new Broadcaster<CallEvent>();
	private boolean mNewMessagesFirst = true;
	
	public CallEventModel()
	{
	}
	
	public void dispose()
	{
		synchronized( mEvents )
		{
			mEvents.clear();
		}
		
		mBroadcaster.dispose();
	}
	
	public void flush()
	{
		/* Flush all events to listeners ( normally the call event logger ) */
		synchronized( mEvents )
		{
			for( CallEvent event: mEvents )
			{
				mBroadcaster.receive( event );
			}
		}
	}
	
	public int getMaxMessageCount()
	{
		return mMaxMessages;
	}

	public void setMaxMessageCount( int count )
	{
		mMaxMessages = count;
	}
	
	public void add( final CallEvent event )
	{
		try
		{
			synchronized ( mEvents )
	        {
				mEvents.addFirst( event );
	        }

			fireTableRowsInserted( 0, 0 );

			prune();
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	public void remove( final CallEvent event )
	{
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				synchronized ( mEvents )
                {
					final int row = mEvents.indexOf( event );
					
					mEvents.remove( event );
					
					if( row != -1 )
					{
						fireTableRowsDeleted( row, row );
					}
                }
            }
		} );
	}
	
	public int indexOf( CallEvent event )
	{
		return mEvents.indexOf( event );
	}
	
	public void setEnd( final CallEvent event )
	{
		try
		{
			EventQueue.invokeAndWait( new Runnable()
			{
				@Override
				public void run()
				{
					if( event != null )
					{
						event.setEnd( System.currentTimeMillis() );

						synchronized ( mEvents )
	                    {
							int row = indexOf( event );

							if( row != -1 )
							{
								fireTableCellUpdated( row, TIME );
							}
	                    }
					}
					else 
					{
						Log.error( "CallEventModel - couldn't log end time - event was null" );
					}
				}
			} );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	public void setFromID( final CallEvent event, final String from )
	{
        EventQueue.invokeLater( new Runnable()
        {
        	@Override
        	public void run()
        	{
        		try
        		{
            		if( event != null )
            		{
            			event.setFromID( from );

            			synchronized ( mEvents )
                        {
                			int row = indexOf( event );

                			if( row != -1 )
                			{
                    			fireTableCellUpdated( row, FROM_ID );
                    			fireTableCellUpdated( row, FROM_ALIAS );
                			}
                			else
                			{
                				Log.error( "CallEventModel - tried to set from ID on "
            						+ "call event - couldn't find index of event" );
                			}
                        }
            		}
        		}
        		catch( Exception e )
        		{
        			e.printStackTrace();
        		}
        	}
        } );
	}
	
	private void prune()
	{
		synchronized( mEvents )
		{
			while( mEvents.size() > mMaxMessages )
			{
				CallEvent event = mEvents.removeLast();
				
				mBroadcaster.receive( event );
				
				fireTableRowsDeleted( mEvents.size(), mEvents.size() );
			}
		}
	}

	@Override
    public int getRowCount()
    {
		return mEvents.size();
    }

	@Override
    public int getColumnCount()
    {
	    return mHeaders.length;
    }

	public String getColumnName( int column ) 
	{
        return mHeaders[ column ];
    }
	
	@Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
		synchronized( mEvents )
		{
			switch( columnIndex )
			{
				case TIME:
					StringBuilder sb = new StringBuilder();
					
					sb.append( mSDFTime.format( 
							mEvents.get( rowIndex ).getEventStartTime() ) );
					
					if( mEvents.get( rowIndex ).getEventEndTime() != 0 )
					{
						sb.append( " - " );
						sb.append( mSDFTime.format( 
								mEvents.get( rowIndex ).getEventEndTime() ) );
					}
					else if( mEvents.get( rowIndex )
							.getCallEventType() == CallEventType.CALL )
					{
						sb.append( " - In Progress" );
					}
					
					return sb.toString();
				case EVENT:
					return mEvents.get( rowIndex ).getCallEventType();
				case FROM_ID:
					return mEvents.get( rowIndex ).getFromID();
				case FROM_ALIAS:
					return mEvents.get( rowIndex ).getFromIDAlias();
				case TO_ID:
					return mEvents.get( rowIndex ).getToID();
				case TO_ALIAS:
					return mEvents.get( rowIndex ).getToIDAlias();
				case CHANNEL:
					int channel = mEvents.get( rowIndex ).getChannel();
					
					if( channel != 0 )
					{
						return channel;
					}
					else
					{
						return null;
					}
				case FREQUENCY:
					long frequency = mEvents.get( rowIndex ).getFrequency();
					
					if( frequency != 0 )
					{
						return mFrequencyFormatter.format( (double)frequency / 1E6d );
					}
					else
					{
						return null;
					}
				case DETAILS:
					return mEvents.get( rowIndex ).getDetails();
			}
		}
		
		return null;
    }
	
	public void addListener( Listener<CallEvent> listener )
	{
		mBroadcaster.addListener( listener );
	}
	
	public void removeListener( Listener<CallEvent> listener )
	{
		mBroadcaster.removeListener( listener );
	}
}
