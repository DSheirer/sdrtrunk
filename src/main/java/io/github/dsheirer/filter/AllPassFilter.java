package io.github.dsheirer.filter;

import java.util.Collections;
import java.util.List;

/**
 * Implements a message filter that passes (true) all messages
 */
@SuppressWarnings( "rawtypes" )
public class AllPassFilter<T> extends Filter<T>
{
	public AllPassFilter()
	{
		super( "Allow All Messages" );
	}
	
	@Override
    public boolean passes( T t )
    {
	    return true;
    }

	@Override
    public boolean canProcess( T t )
    {
	    return true;
    }

    @Override
	@SuppressWarnings( "unchecked" )
    public List getFilterElements()
    {
		return Collections.EMPTY_LIST;
    }
}
