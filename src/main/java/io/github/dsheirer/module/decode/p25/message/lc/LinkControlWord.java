package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.List;

/**
 * APCO 25 Link Control Word.  This message word is contained in Logical Link Data Unit 1 and Terminator with
 * Link Control messages.
 */
public abstract class LinkControlWord
{
    private static final int ENCRYPTION_FLAG = 0;
    private static final int STANDARD_VENDOR_ID_FLAG = 1;
    private static final int[] OPCODE = {2, 3, 4, 5, 6, 7};
    private static final int[] VENDOR = {8, 9, 10, 11, 12, 13, 14, 15};

    private BinaryMessage mMessage;
    private boolean mValid;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LinkControlWord(BinaryMessage message)
    {
        mMessage = message;
    }

    /**
     * Binary message sequence for this LCW
     */
    protected BinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Sets the valid flag for this message to mark a flag as invalid (false) or the default value is true.
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Indicates if this link control word is valid and has passed all error detection and correction routines.
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Indicates if this is an encrypted LCW
     */
    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTION_FLAG);
    }

    /**
     * Indicates if this is a standard vendor format LCW
     */
    public boolean isStandardVendorFormat()
    {
        return isStandardVendorFormat(getMessage());
    }

    /**
     * Indicates if the link control word message has standard vendor format.
     */
    public static boolean isStandardVendorFormat(BinaryMessage binaryMessage)
    {
        return binaryMessage.get(STANDARD_VENDOR_ID_FLAG);
    }

    /**
     * Vendor format for this link control word.
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Lookup the Vendor format for the specified LCW
     */
    public static Vendor getVendor(BinaryMessage binaryMessage)
    {
        if(isStandardVendorFormat(binaryMessage))
        {
            return Vendor.STANDARD;
        }
        else
        {
            return Vendor.fromValue(binaryMessage.getInt(VENDOR));
        }
    }

    /**
     * Opcode for this LCW
     */
    public LinkControlOpcode getOpcode()
    {
        return getOpcode(getMessage());
    }

    /**
     * Identifies the link control word opcode from the binary message.
     */
    public static LinkControlOpcode getOpcode(BinaryMessage binaryMessage)
    {
        return LinkControlOpcode.fromValue(binaryMessage.getInt(OPCODE));
    }

    /**
     * List of identifiers provided by the message
     */
    public abstract List<IIdentifier> getIdentifiers();

    /**
     * Creates a string with the basic Link Control Word information
     */
    public String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append(getOpcode().getLabel());

            if(isEncrypted())
            {
                sb.append(" ENCRYPTED");
            }
            else
            {
                if(!isStandardVendorFormat())
                {
                    sb.append(" VENDOR:").append(getVendor().getLabel());
                }
            }
        }
        else
        {
            sb.append("**CRC-FAILED**");
        }

        return sb.toString();
    }
}
