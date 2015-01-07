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
	ACARS( "ACARS", "ACARS", "images/acars.png", 12500 ),
	AIS( "AIS", "AIS", "images/ais.png", 12500 ),
    AM( "AM", "AM", "images/am.png", 10000 ),
    APRS( "APRS", "APRS", "images/aprs.png", 12500 ),
    DMR( "DMR", "DMR", "images/dmr.png", 12500 ),
    FLEETSYNC2( "Fleetsync II", "Fsync2", "images/fm.png", 12500 ),
    LJ_1200( "LJ1200 173.075", "LJ1200", "images/lj1200.png", 12500 ),
    LTR_STANDARD( "LTR-Standard", "LTR", "images/ltr.png", 10000 ),
    LTR_NET( "LTR-Net", "LTR-Net", "images/ltr_net.png", 10000 ),
    MDC1200( "MDC1200", "MDC1200", "images/fm.png", 12500 ),
    MOTOROLA_TYPE_2( "MOTO T2", "MOTO T2", "images/motorola_type_2.png", 15000 ),
    MPT1327( "MPT1327", "MPT1327", "images/mpt1327.png", 12500 ),
    NXDN( "NXDN", "NXDN", "images/nxdn.png", 8000 ),
    NBFM( "NBFM", "NBFM", "images/fm.png", 12500 ),
    PASSPORT( "Passport", "Passport", "images/passport.png", 12500 ),
    P25_PHASE1( "P25 Phase I", "P25-1", "images/p25_1.png", 12500 ),
    P25_PHASE2( "P25 Phase II", "P25-2", "images/p25_2.png", 15000 ),
    TAIT_1200( "Tait 1200", "Tait 1200", "images/tait.png", 12500 );
    
    private String mDisplayString;
    private String mShortDisplayString;
    private String mIconFilename;
    private int mChannelBandwidth;
    
    DecoderType( String displayString, String shortDisplayString, 
    			 String iconFilename, int bandwidth )
    {
        mDisplayString = displayString;
        mShortDisplayString = shortDisplayString;
        mIconFilename = iconFilename;
        mChannelBandwidth = bandwidth;
    }

    /**
     * Primary decoders
     */
    public static EnumSet<DecoderType> getAvailableDecoders()
    {
    	return EnumSet.of( DecoderType.AM,
    	                   DecoderType.LTR_NET,
    					   DecoderType.LTR_STANDARD,
    					   DecoderType.MPT1327,
    					   DecoderType.NBFM,
//    					   DecoderType.P25_PHASE1,
    					   DecoderType.PASSPORT );
    }

    /**
     * Decoders that can be used in the viewer application
     */
    public static EnumSet<DecoderType> getInstrumentableDecoders()
    {
    	return EnumSet.of( DecoderType.FLEETSYNC2,
    				 	   DecoderType.LJ_1200,
    	                   DecoderType.LTR_NET,
    					   DecoderType.MDC1200,
    					   DecoderType.MPT1327,
    					   DecoderType.PASSPORT,
    					   DecoderType.P25_PHASE1,
    					   DecoderType.TAIT_1200 );
    }
    
    /**
     * Available auxiliary decoders.
     */
    public static ArrayList<DecoderType> getAuxDecoders()
    {
    	ArrayList<DecoderType> decoders = new ArrayList<DecoderType>();
    	
    	decoders.add( DecoderType.FLEETSYNC2 );
    	decoders.add( DecoderType.LJ_1200 );
    	decoders.add( DecoderType.MDC1200 );
    	decoders.add( DecoderType.TAIT_1200 );
    	
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
    
    public String getIconFilename()
    {
    	return mIconFilename;
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
