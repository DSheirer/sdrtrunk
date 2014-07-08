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
import java.text.SimpleDateFormat;
import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;

import sample.Broadcaster;
import sample.Listener;

public class CallEventModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;
    
    private static final int DEFAULT_ICON_HEIGHT = 14;
    
    private static final int sTIME = 0;
    private static final int sEVENT = 1;
    private static final int sFROM_ID = 2;
    private static final int sFROM_ALIAS = 3;
    private static final int sTO_ID = 4;
    private static final int sTO_ALIAS =5;
    private static final int sCHANNEL = 6;
    private static final int sFREQUENCY = 7;
    private static final int sDETAILS = 8;

	protected int mMaxMessages = 500;

	protected LinkedList<CallEvent> mEvents = new LinkedList<CallEvent>();

	protected String[] mHeaders = new String[] { "Time",
												 "Event",
												 "From",
												 "From Alias",
												 "To",
												 "To Alias",
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
		mEvents.clear();
		mBroadcaster.dispose();
	}
	
	public void flush()
	{
		/* Flush all events to listeners ( normally the call event logger ) */
		for( CallEvent event: mEvents )
		{
			mBroadcaster.receive( event );
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
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				mEvents.addFirst( event );

				CallEventModel.this.fireTableRowsInserted( 0, 0 );

				prune();
            }
		} );
	}
	
	private void prune()
	{
		while( mEvents.size() > mMaxMessages )
		{
			CallEvent event = mEvents.removeLast();
			
			mBroadcaster.receive( event );
			
			super.fireTableRowsDeleted( mEvents.size(), mEvents.size() );
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
		switch( columnIndex )
		{
			case sTIME:
				return mSDFTime.format( mEvents.get( rowIndex ).getEventTime() );
			case sEVENT:
				return mEvents.get( rowIndex ).getCallEventType();
			case sFROM_ID:
				return mEvents.get( rowIndex ).getFromID();
			case sFROM_ALIAS:
				return mEvents.get( rowIndex ).getFromIDAlias();
			case sTO_ID:
				return mEvents.get( rowIndex ).getToID();
			case sTO_ALIAS:
				return mEvents.get( rowIndex ).getToIDAlias();
			case sCHANNEL:
				return mEvents.get( rowIndex ).getChannel();
			case sFREQUENCY:
				return mEvents.get( rowIndex ).getFrequency();
			case sDETAILS:
				return mEvents.get( rowIndex ).getDetails();
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
