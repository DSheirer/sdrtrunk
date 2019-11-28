/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.controller.channel.map;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelRangeModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;

	private static final int START = 0;
    private static final int STOP = 1;
    private static final int BASE = 2;
    private static final int SIZE = 3;
    private static final int ERROR = 4;

	protected int[] mColumnWidths = { 110, 110, 110, -1, 80 };

	protected String[] mHeaders = new String[] { "Begin",
												 "End",
												 "Base",
												 "Size",
												 "Error" };

	private List<ChannelRange> mRanges = new CopyOnWriteArrayList<>();
	private ChannelRangeEventListener mListener;
	
	public ChannelRangeModel()
	{
	}
	
	public void setListener( ChannelRangeEventListener listener )
	{
		mListener = listener;
	}
	
	private void broadcastChange()
	{
		if( mListener != null )
		{
			mListener.channelRangesChanged();
		}
	}

	public void clear()
	{
		for( ChannelRange range: mRanges )
		{
			removeRange( range );
		}
		
		broadcastChange();
	}
	
	public List<ChannelRange> getChannelRanges()
	{
		return mRanges;
	}
	
	public void addRanges( List<ChannelRange> ranges )
	{
		for( ChannelRange range: ranges )
		{
			addRange( range );
		}
		
		broadcastChange();
	}
	
	public void addRange( ChannelRange range )
	{
		if( range != null && !mRanges.contains( range ) )
		{
			mRanges.add( range );

			int index = mRanges.indexOf( range );

			fireTableRowsInserted( index, index );
		}
		
		validate();
		
		broadcastChange();
	}
	
	public void removeRange( ChannelRange range )
	{
		if( range != null && mRanges.contains( range ) )
		{
			int index = mRanges.indexOf( range );
			
			mRanges.remove( range );
			
			fireTableRowsDeleted( index, index );
		}
		
		validate();

		broadcastChange();
	}
	
	public int[] getColumnWidths()
	{
		return mColumnWidths;
	}
	
	public void setColumnWidths( int[] widths )
	{
		if( widths.length != mHeaders.length )
		{
			throw new IllegalArgumentException( "ChannelMapRangeModel - "
					+ "column widths array should have " + 
					mHeaders.length + " elements" );
		}
		else
		{
			mColumnWidths = widths;
		}
	}
	
	@Override
    public int getRowCount()
    {
		return mRanges.size();
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
	
	public ChannelRange getChannelRange( int index )
	{
		if( index < mRanges.size() )
		{
			return mRanges.get( index );
		}
		
		return null;
	}
	
	@Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
		ChannelRange range = getChannelRange( rowIndex );

		if( range != null )
		{
			switch( columnIndex )
			{
				case START:
					return range.getFirstChannelNumber();
				case STOP:
					return range.getLastChannelNumber();
				case BASE:
					return range.getBaseFrequency();
				case SIZE:
					return range.getStepSize();
				case ERROR:
					StringBuilder sb = new StringBuilder();
					
					if( !range.isValid() )
					{
						sb.append( "Sequence" );
					}
					
					if( range.isOverlapping() )
					{
						if( sb.length() > 0 )
						{
							sb.append( "," );
						}
						
						sb.append( "Overlap" );
					}
					
					return sb.toString();
				default:
					break;
			}
		}
		
		return null;
    }

	/**
	 * Validates each of the channel ranges for overlap
	 */
	private void validate()
	{
		for( int x = 0; x < mRanges.size(); x++ )
		{
			mRanges.get( x ).setOverlapping( false );
			fireTableCellUpdated( x, ERROR );
		}
		
		for( int x = 0; x < mRanges.size(); x++ )
		{
			for( int y = x + 1; y <  mRanges.size(); y++ )
			{
				if( mRanges.get( x ).overlaps( mRanges.get( y ) ) )
				{
					mRanges.get( x ).setOverlapping( true );
					fireTableCellUpdated( x, ERROR );
					
					mRanges.get( y ).setOverlapping( true );
					fireTableCellUpdated( y, ERROR );
				}
			}
		}
	}
	
	public boolean isCellEditable( int row, int column )
	{
		return row < mRanges.size() && column != ERROR;
	}
	
	public void setValueAt( Object value, int row, int column )
	{
		int parsedValue = -1;
		
		try
		{
			parsedValue = Integer.parseInt( (String)value );
		}
		catch( Exception e )
		{
			//Do nothing ... we'll use 0
		}

		if( parsedValue >= 0 )
		{
			ChannelRange range = getChannelRange( row );
			
			if( range != null )
			{
				switch( column )
				{
					case START:
						range.setFirstChannelNumber( parsedValue );
						break;
					case STOP:
						range.setLastChannelNumber( parsedValue );
						break;
					case BASE:
						range.setBaseFrequency( parsedValue );
						break;
					case SIZE:
						range.setStepSize( parsedValue );
						break;
				}
				
				validate();

				broadcastChange();
			}
			else
			{
				throw new IllegalArgumentException( "Invalid channel range row index" );
			}
		}
	}
	
	public interface ChannelRangeEventListener
	{
		public void channelRangesChanged();
	}
}
