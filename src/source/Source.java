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
package source;

import module.Module;
import sample.SampleType;

/**
 * Abstract class to define the minimum functionality of a sample data provider.
 */
public abstract class Source extends Module
{
    protected SampleType mSampleType;

    public Source( SampleType sampleType )
    {
        mSampleType = sampleType;
    }
    
    public abstract int getSampleRate() throws SourceException;
    
    public abstract long getFrequency() throws SourceException;
    
    public abstract void dispose();
    
    public SampleType getSampleType()
    {
    	return mSampleType;
    }
    
    public void setSampleType( SampleType sampleType )
    {
    	mSampleType = sampleType;
    }
}
