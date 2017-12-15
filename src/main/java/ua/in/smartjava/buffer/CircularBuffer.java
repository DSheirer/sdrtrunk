package ua.in.smartjava.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ua.in.smartjava.sample.Listener;

public class CircularBuffer<T> implements Listener<T>
{
	private ArrayList<T> mElements;
	private Listener<T> mListener;
	private int mPointer;
	private int mSize;


	/**
	 * Circular Buffer - stores elements in an array list.  Once the ua.in.smartjava.buffer is
	 * full, new samples replace older samples and older samples are broadcast
	 * to an optional listener.
	 * 
	 * @param size - size of ua.in.smartjava.buffer
	 */
	public CircularBuffer( int size )
	{
		mSize = size;
		mElements = new ArrayList<T>();
	}

	/**
	 * Returns the element stored at index.  Note: the index argument refers to
	 * the internal storage index and does not indicate a temporal relationship
	 * among elements.  For example, if you get indexes 1, 2, and 3, you may be
	 * getting ( oldest - 1, oldest, newest ) elements, if the internal pointer
	 * is currently pointing to and will insert the next element at index 4.
	 * 
	 * @param index - element index
	 * 
	 * @return element or null
	 */
	public T get( int index )
	{
		if( index < mElements.size() )
		{
			return mElements.get( index );
		}
		
		return null;
	}
	
	public int getSize()
	{
		return mSize;
	}
	
	public List<T> getElements()
	{
		return Collections.unmodifiableList( mElements );
	}
	
	@Override
	public void receive( T element )
	{
		T previous = null;
		
		if( mElements.size() > mPointer )
		{
			previous = mElements.get( mPointer );
			
			mElements.set( mPointer, element );
		}
		else
		{
			mElements.add( element );
		}
		
		mPointer++;
		
		if( mPointer >= mSize )
		{
			mPointer = 0;
		}
		
		if( previous != null && mListener != null )
		{
			mListener.receive( previous );
		}
	}

	public void setListener( Listener<T> listener )
	{
		mListener = listener;
	}
}
