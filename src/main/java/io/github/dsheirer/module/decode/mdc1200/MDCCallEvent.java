/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package io.github.dsheirer.module.decode.mdc1200;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.CallEvent;

public class MDCCallEvent extends CallEvent
{
    public MDCCallEvent( CallEventType callEventType,
    					 AliasList aliasList, 
    					 String fromID, 
    					 String toID, 
    					 String details ) 
    {
        super( DecoderType.MDC1200, callEventType,
        		aliasList, fromID, toID, details );
    }

    private Alias getAlias( String ident )
    {
        if( hasAliasList() )
        {
            return getAliasList().getMDC1200Alias( ident );
        }
        
        return null;
    }
    
    @Override
    public Alias getFromIDAlias()
    {
        return getAlias( getFromID() );
    }

    @Override
    public Alias getToIDAlias()
    {
        return getAlias( getToID() );
    }

    @Override
    public String getChannel()
    {
        return null;
    }

    @Override
    public long getFrequency()
    {
        return 0;
    }

    public static class Builder
    {
        /* Required parameters */
        private CallEventType mCallEventType;

        /* Optional parameters */
        private AliasList mAliasList;
        private String mFromID;
        private String mToID;
        private String mDetails;

        public Builder( CallEventType callEventType )
        {
            mCallEventType = callEventType;
        }
        
        public Builder aliasList( AliasList aliasList )
        {
            mAliasList = aliasList;
            return this;
        }
        
        public Builder from( String val )
        {
            mFromID = val;
            return this;
        }

        public Builder details( String details )
        {
            mDetails = details;
            return this;
        }
        
        public Builder to( String toID )
        {
            mToID = toID;
            return this;
        }

        public MDCCallEvent build()
        {
            return new MDCCallEvent( this );
        }
    }

    /**
     * Private constructor for the builder
     */
    private MDCCallEvent( Builder builder )
    {
        this( builder.mCallEventType,
              builder.mAliasList, 
              builder.mFromID,
              builder.mToID,
              builder.mDetails );
    }
    
    public static MDCCallEvent 
                        getMDCCallEvent( MDCMessage message )
    {
        CallEventType type = CallEventType.UNKNOWN;
        StringBuilder sbDetails = new StringBuilder();
        sbDetails.append( "MDC-1200 OPCODE:" );
        sbDetails.append( String.valueOf( message.getOpcode() ) );
        
        switch( message.getMessageType() )
        {
            case ACKNOWLEDGE:
                type = CallEventType.RESPONSE;
                break;
            case ANI:
                type = CallEventType.ID_ANI;
                break;
            case EMERGENCY:
                type = CallEventType.EMERGENCY;
                break;
            case PAGING:
                type = CallEventType.PAGE;
                break;
			default:
				break;
        }
 
        return new MDCCallEvent.Builder( type )
				       	.details( sbDetails.toString() )
				        .from( message.getFromID() )
				        .to( message.getToID() )
				        .build();
    }
}
