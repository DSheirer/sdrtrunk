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
package controller.channel;

import javax.swing.table.AbstractTableModel;

import log.Log;

public class ChannelMapModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;
    private static final int sSTART = 0;
    private static final int sSTOP = 1;
    private static final int sBASE = 2;
    private static final int sSIZE = 3;

	protected int[] mColumnWidths = { 110, 110, 110, -1 };

	protected String[] mHeaders = new String[] { "Begin",
												 "End",
												 "Base",
												 "Size" };

	private ChannelMap mChannelMap;
	
	public ChannelMapModel( ChannelMap map )
	{
		mChannelMap = map;
	}
	
	public ChannelMap getChannelMap()
	{
		return mChannelMap;
	}
	
	public void setChannelMap( ChannelMap map )
	{
		mChannelMap = map;
		
		fireTableDataChanged();
	}
	
	public void addRange( ChannelRange range )
	{
		mChannelMap.addRange( range );
		
		int index = mChannelMap.getRanges().size() - 1;
		
		fireTableRowsInserted( index, index );
	}
	
	public void removeRange( int rangeIndex )
	{
		if( rangeIndex < mChannelMap.getRanges().size() )
		{
			mChannelMap.getRanges().remove( rangeIndex );
		}
		
		fireTableRowsDeleted( rangeIndex, rangeIndex );
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
		return mChannelMap.getRanges().size();
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
		ChannelRange range = mChannelMap.getRanges().get( rowIndex );
		
		switch( columnIndex )
		{
			case sSTART:
				return range.getFirstChannelNumber();
			case sSTOP:
				return range.getLastChannelNumber();
			case sBASE:
				return range.getBase();
			case sSIZE:
				return range.getSize();
		}
		
		return null;
    }
	
	public boolean isCellEditable( int row, int column )
	{
		return true;
	}
	
	public void setValueAt( Object value, int row, int column )
	{
		try
		{
			int parsedValue = Integer.parseInt( (String)value );
			
			ChannelRange range = mChannelMap.getRanges().get( row );
			
			switch( column )
			{
				case sSTART:
					range.setFirstChannelNumber( parsedValue );
					break;
				case sSTOP:
					range.setLastChannelNumber( parsedValue );
					break;
				case sBASE:
					range.setBase( parsedValue );
					break;
				case sSIZE:
					range.setSize( parsedValue );
					break;
			}
		}
		catch( Exception e )
		{
			Log.info( "ChannelMapModel - couldn't parse integer value "
					+ "for [" + value + "] - " + e.getLocalizedMessage() );
		}
	}
}
