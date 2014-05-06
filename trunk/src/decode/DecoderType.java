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
package decode;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Enumeration of decoder types
 */
public enum DecoderType
{
    FLEETSYNC2( "Fleetsync II", "Fsync2", "images/fm", 12500 ),
    LTR_STANDARD( "LTR-Standard", "LTR", "images/ltr_standard", 10000 ),
    LTR_NET( "LTR-Net", "LTR-Net", "images/ltr_net", 10000 ),
    MDC1200( "MDC1200", "MDC1200", "images/fm", 12500 ),
    MPT1327( "MPT1327", "MPT1327", "images/mpt1327", 12500 ),
    NBFM( "NBFM", "NBFM", "images/fm", 12500 ),
    PASSPORT( "Passport", "Passport", "images/passport", 12500 );
    
    private String mDisplayString;
    private String mShortDisplayString;
    private String mIconPrefix;
    private int mChannelBandwidth;
    
    DecoderType( String displayString, String shortDisplayString, 
    			 String iconPrefix, int bandwidth )
    {
        mDisplayString = displayString;
        mShortDisplayString = shortDisplayString;
        mIconPrefix = iconPrefix;
        mChannelBandwidth = bandwidth;
    }

    /**
     * Primary decoders
     */
    public static EnumSet<DecoderType> getAvailableDecoders()
    {
    	return EnumSet.of( DecoderType.LTR_NET,
    					   DecoderType.LTR_STANDARD,
    					   DecoderType.MPT1327,
    					   DecoderType.NBFM,
    					   DecoderType.PASSPORT );
    }

    /**
     * Decoders that can be used in the viewer application
     */
    public static EnumSet<DecoderType> getInstrumentableDecoders()
    {
    	return EnumSet.of( DecoderType.FLEETSYNC2,
    	                   DecoderType.LTR_NET,
    					   DecoderType.MDC1200,
    					   DecoderType.MPT1327,
    					   DecoderType.PASSPORT );
    }
    
    /**
     * Returns listing of auxiliary decoders.
     */
    public static ArrayList<DecoderType> getAuxDecoders()
    {
    	ArrayList<DecoderType> decoders = new ArrayList<DecoderType>();
    	
    	decoders.add( DecoderType.FLEETSYNC2 );
    	decoders.add( DecoderType.MDC1200 );
    	
    	return decoders;
    }
    
    public String getDisplayString()
    {
        return mDisplayString;
    }
    
    public String getShortDisplayString()
    {
    	return mShortDisplayString;
    }
    
    public String getIconPrefix()
    {
    	return mIconPrefix;
    }
    
    public int getChannelBandwidth()
    {
    	return mChannelBandwidth;
    }
    
    @Override
    public String toString()
    {
    	return mDisplayString;
    }
}
