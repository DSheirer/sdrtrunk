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
package sample;


public class Decimator<T> implements Listener<T>
{
    private int mDecimationFactor;
    private int mDecimationPointer = 0;
    private Listener<T> mListener;

    /**
     * Constructs the class to consume samples and decimate them by the
     * DECIMATIONFACTOR.
     * 
     * @param decimationFactor - number of samples to decimate.  Provides one 
     * sample to registered listeners and discards the next decimationFactor - 1 
     * samples.
     */
    public Decimator( int decimationFactor )
    {
        mDecimationFactor = decimationFactor;
    }

    /**
     * Receives and broadcasts a sample and then discards the next 
     * decimationFactor -1 samples
     */
    public void receive( T t )
    {
        if( mDecimationPointer == 0 )
        {
        	if( mListener != null )
        	{
        		mListener.receive( t );
        	}
        }
        
        mDecimationPointer++;

        //Reset the decimation pointer once it equals the decimation factor
        if( mDecimationPointer == mDecimationFactor )
        {
            mDecimationPointer = 0;
        }
    }

    /**
     * Adds a sample listener to receive samples
     */
    public void setListener( Listener<T> listener )
    {
        mListener = listener;
    }
}
