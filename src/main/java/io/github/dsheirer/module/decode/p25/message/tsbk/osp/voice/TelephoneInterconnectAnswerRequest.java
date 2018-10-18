package io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Digit;
import io.github.dsheirer.module.decode.p25.message.tsbk2.Opcode;

import java.util.ArrayList;
import java.util.List;

public class TelephoneInterconnectAnswerRequest extends TSBKMessage
{
    public static final int[] DIGIT_1 = { 80,81,82,83 };
    public static final int[] DIGIT_2 = { 84,85,86,87 };
    public static final int[] DIGIT_3 = { 88,89,90,91 };
    public static final int[] DIGIT_4 = { 92,93,94,95 };
    public static final int[] DIGIT_5 = { 96,97,98,99 };
    public static final int[] DIGIT_6 = { 100,101,102,103 };
    public static final int[] DIGIT_7 = { 104,105,106,107 };
    public static final int[] DIGIT_8 = { 108,109,110,111 };
    public static final int[] DIGIT_9 = { 112,113,114,115 };
    public static final int[] DIGIT_10 = { 116,117,118,119 };
    
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public TelephoneInterconnectAnswerRequest( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST.toString();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        sb.append( " TO:" );
        sb.append( getTargetAddress() );
        
        sb.append( " TEL:" );
        sb.append( getTelephoneNumber() );
        
        return sb.toString();
    }
    
    public String getTelephoneNumber()
    {
        List<Integer> digits = new ArrayList<Integer>();
        
        digits.add( mMessage.getInt( DIGIT_1 ) );
        digits.add( mMessage.getInt( DIGIT_2 ) );
        digits.add( mMessage.getInt( DIGIT_3 ) );
        digits.add( mMessage.getInt( DIGIT_4 ) );
        digits.add( mMessage.getInt( DIGIT_5 ) );
        digits.add( mMessage.getInt( DIGIT_6 ) );
        digits.add( mMessage.getInt( DIGIT_7 ) );
        digits.add( mMessage.getInt( DIGIT_8 ) );
        digits.add( mMessage.getInt( DIGIT_9 ) );
        digits.add( mMessage.getInt( DIGIT_10 ) );
        
        return Digit.decode( digits );
    }
    
    public String getTargetAddress()
    {
        return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
    
    @Override
    public String getToID()
    {
        return getTargetAddress();
    }
}
