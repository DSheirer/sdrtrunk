package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNEncryptionKey;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.type.CallControlOption;
import io.github.dsheirer.module.decode.nxdn.type.CallOption;
import io.github.dsheirer.module.decode.nxdn.type.CallType;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base call message
 */
public abstract class Call extends NXDNMessage
{
    private static final IntField CC_OPTION = IntField.length8(OCTET_1);
    private static final IntField CALL_TYPE = IntField.length3(OCTET_2);
    protected static final IntField CALL_OPTION = IntField.length5(OCTET_2 + 3);
    private static final IntField CIPHER_TYPE = IntField.length2(OCTET_7);
    private static final IntField KEY_ID = IntField.length6(OCTET_7 + 2);

    private CallControlOption mCallControlOption;
    private EncryptionKeyIdentifier mEncryptionKeyIdentifier;
    private NXDNRadioIdentifier mSourceIdentifier;
    private IntegerIdentifier mDestinationIdentifier;

    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     */
    public Call(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    /**
     * Call options for this call.
     */
    public abstract CallOption getCallOption();

    /**
     * Call control options for the call
     * @return options
     */
    public CallControlOption getCallControlOption()
    {
        if(mCallControlOption == null)
        {
            mCallControlOption = new CallControlOption(getMessage().getInt(CC_OPTION));
        }

        return mCallControlOption;
    }

    /**
     * Call type for this voice call.
     * @return type
     */
    public CallType getCallType()
    {
        return CallType.fromValue(getMessage().getInt(CALL_TYPE));
    }

    /**
     * Encryption algorithm and key ID.
     */
    public EncryptionKeyIdentifier getEncryptionKeyIdentifier()
    {
        if(mEncryptionKeyIdentifier == null)
        {
            mEncryptionKeyIdentifier = EncryptionKeyIdentifier.create(Protocol.NXDN,
                    NXDNEncryptionKey.create(getMessage().getInt(CIPHER_TYPE), getMessage().getInt(KEY_ID)));
        }

        return mEncryptionKeyIdentifier;
    }

    /**
     * Source radio ID
     * @return source identifier.
     */
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null)
        {
            mSourceIdentifier = NXDNRadioIdentifier.from(getMessage().getInt(SOURCE));
        }

        return mSourceIdentifier;
    }

    /**
     * Destination identifier, either talkgroup or radio.
     * @return destination identifier
     */
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            mDestinationIdentifier = switch (getCallType())
            {
                case GROUP_BROADCAST, GROUP_CONFERENCE -> NXDNTalkgroupIdentifier.to(getMessage().getInt(DESTINATION));
                default -> NXDNRadioIdentifier.to(getMessage().getInt(DESTINATION));
            };
        }

        return mDestinationIdentifier;
    }
}
