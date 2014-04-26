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

import message.Message;
import sample.Listener;

public class MessageActivityModel extends AbstractTableModel
								  implements Listener<Message>
{
    private static final long serialVersionUID = 1L;
    private static final int sTIME = 0;
    private static final int sPROTOCOL = 1;
    private static final int sERROR_STATUS = 2;
    private static final int sMESSAGE = 3;
    private static final int sMESSAGE_BITS = 4;

	protected int mMaxMessages = 500;
	protected LinkedList<Message> mMessages = new LinkedList<Message>();

	protected int[] mColumnWidths = { 110, 110, 110, -1, -1 };

	protected String[] mHeaders = new String[] { "Time",
												 "Protocol",
												 "Error Check",
												 "Message",
												 "Binary" };

	private SimpleDateFormat mSDFTime = new SimpleDateFormat( "HH:mm:ss" );
	
	private boolean mNewMessagesFirst = true;
	
	public MessageActivityModel()
	{
	}
	
	public void dispose()
	{
		mMessages.clear();
	}
	
	public int[] getColumnWidths()
	{
		return mColumnWidths;
	}
	
	public void setColumnWidths( int[] widths )
	{
		if( widths.length != 5 )
		{
			throw new IllegalArgumentException( "MessageActivityModel - "
					+ "column widths array should have 5 elements" );
		}
		else
		{
			mColumnWidths = widths;
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
	
	public void receive( final Message message )
	{
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				mMessages.addFirst( message );

				MessageActivityModel.this.fireTableRowsInserted( 0, 0 );

				prune();
            }
		} );
	}
	
	private void prune()
	{
		while( mMessages.size() > mMaxMessages )
		{
			mMessages.removeLast();
			
			super.fireTableRowsDeleted( mMessages.size() - 1, mMessages.size() - 1 );
		}
	}

	@Override
    public int getRowCount()
    {
		return mMessages.size();
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
		Message message = mMessages.get( rowIndex );
		
		switch( columnIndex )
		{
			case sTIME:
				return mSDFTime.format( message.getDateReceived() );
			case sPROTOCOL:
				return message.getProtocol();
			case sERROR_STATUS:
				return message.getErrorStatus();
			case sMESSAGE:
				return message.getMessage();
			case sMESSAGE_BITS:
				return message.getBinaryMessage();
		}
		
		return null;
    }
}
