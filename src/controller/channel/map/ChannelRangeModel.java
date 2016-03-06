/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package controller.channel.map;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelRangeModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( ChannelRangeModel.class );

	private static final int START = 0;
    private static final int STOP = 1;
    private static final int BASE = 2;
    private static final int SIZE = 3;

	protected int[] mColumnWidths = { 110, 110, 110, -1 };

	protected String[] mHeaders = new String[] { "Begin",
												 "End",
												 "Base",
												 "Size" };

	private ChannelMap mChannelMap;
	
	public ChannelRangeModel()
	{
	}
	
	public void setChannelMap( ChannelMap channelMap )
	{
		mChannelMap = channelMap;
		fireTableDataChanged();
	}
	
	public void addRange( ChannelRange range )
	{
		if( mChannelMap != null )
		{
			mChannelMap.addRange( range );
			
			int index = mChannelMap.getRanges().size() - 1;
			
			fireTableRowsInserted( index, index );
		}
	}
	
	public void removeRange( int rangeIndex )
	{
		if( mChannelMap != null )
		{
			if( rangeIndex < mChannelMap.getRanges().size() )
			{
				mChannelMap.getRanges().remove( rangeIndex );
			}
			
			fireTableRowsDeleted( rangeIndex, rangeIndex );
		}
	}
	
	public int[] getColumnWidths()
	{
		return mColumnWidths;
	}
	
	public void setColumnWidths( int[] widths )
	{
		if( widths.length != 4 )
		{
			throw new IllegalArgumentException( "ChannelMapRangeModel - "
					+ "column widths array should have 5 elements" );
		}
		else
		{
			mColumnWidths = widths;
		}
	}
	
	@Override
    public int getRowCount()
    {
		if( mChannelMap != null )
		{
			return mChannelMap.getRanges().size();
		}
		
		return 0;
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
		if( mChannelMap != null )
		{
			ChannelRange range = mChannelMap.getRanges().get( rowIndex );
			
			switch( columnIndex )
			{
				case START:
					return range.getFirstChannelNumber();
				case STOP:
					return range.getLastChannelNumber();
				case BASE:
					return range.getBase();
				case SIZE:
					return range.getSize();
			}
		}
		
		return null;
    }
	
	public boolean isCellEditable( int row, int column )
	{
		return mChannelMap != null;
	}
	
	public void setValueAt( Object value, int row, int column )
	{
		if( mChannelMap != null )
		{
			try
			{
				int parsedValue = Integer.parseInt( (String)value );
				
				ChannelRange range = mChannelMap.getRanges().get( row );
				
				switch( column )
				{
					case START:
						range.setFirstChannelNumber( parsedValue );
						break;
					case STOP:
						range.setLastChannelNumber( parsedValue );
						break;
					case BASE:
						range.setBase( parsedValue );
						break;
					case SIZE:
						range.setSize( parsedValue );
						break;
				}
			}
			catch( Exception e )
			{
				mLog.error( "ChannelMapModel - couldn't parse integer value "
						+ "for [" + value + "]", e );
			}
		}
	}
}
