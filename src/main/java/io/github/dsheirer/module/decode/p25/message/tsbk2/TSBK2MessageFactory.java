package io.github.dsheirer.module.decode.p25.message.tsbk2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.AuthenticationQuery;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.CallAlertRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.CancelServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.EmergencyAlarmRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.ExtendedFunctionResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.GroupVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.IndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.TelephoneInterconnectAnswerResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.TelephoneInterconnectPstnRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.UnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.UnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.UnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp.UnknownISPMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.GroupDataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.GroupDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.GroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.IndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.TelephoneInterconnectVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.UnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.UnitToUnitVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.UnitToUnitVoiceServiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp.UnknownOSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

/**
 *
 */
public class TSBK2MessageFactory
{
    public static TSBK2Message create(Direction direction, DataUnitID dataUnitID, CorrectedBinaryMessage message,
                                      int nac, long timestamp)
    {
        Vendor vendor = TSBK2Message.getVendor(message);

        switch(vendor)
        {
            case STANDARD:
                return createStandard(direction, dataUnitID, message, nac, timestamp);
            default:
                return createStandard(direction, dataUnitID, message, nac, timestamp);
        }
    }

    private static TSBK2Message createStandard(Direction direction, DataUnitID dataUnitID,
                                               CorrectedBinaryMessage message, int nac, long timestamp)
    {
        Opcode opcode = TSBK2Message.getOpcode(message, direction);

        switch(opcode)
        {
            case ISP_AUTHENTICATION_QUERY:
                return new AuthenticationQuery(dataUnitID, message, nac, timestamp);
            case ISP_CALL_ALERT_REQUEST:
                return new CallAlertRequest(dataUnitID, message, nac, timestamp);
            case ISP_CANCEL_SERVICE_REQUEST:
                return new CancelServiceRequest(dataUnitID, message, nac, timestamp);
            case ISP_EMERGENCY_ALARM_REQUEST:
                return new EmergencyAlarmRequest(dataUnitID, message, nac, timestamp);
            case ISP_EXTENDED_FUNCTION_RESPONSE:
                return new ExtendedFunctionResponse(dataUnitID, message, nac, timestamp);
            case ISP_GROUP_VOICE_SERVICE_REQUEST:
                return new GroupVoiceServiceRequest(dataUnitID, message, nac, timestamp);
            case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                return new IndividualDataServiceRequest(dataUnitID, message, nac, timestamp);
            case ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                return new TelephoneInterconnectAnswerResponse(dataUnitID, message, nac, timestamp);
            case ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST:
                return new TelephoneInterconnectPstnRequest(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                return new UnitAcknowledgeResponse(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                return new UnitToUnitVoiceServiceAnswerResponse(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                return new UnitToUnitVoiceServiceRequest(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                return new GroupDataChannelAnnouncement(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                return new GroupDataChannelAnnouncementExplicit(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                return new GroupDataChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                return new GroupVoiceChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                return new GroupVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                return new GroupVoiceChannelGrantUpdateExplicit(dataUnitID, message, nac, timestamp);
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                return new IndividualDataChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                return new TelephoneInterconnectAnswerRequest(dataUnitID, message, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                return new TelephoneInterconnectVoiceChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                return new TelephoneInterconnectVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                return new UnitToUnitAnswerRequest(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                return new UnitToUnitVoiceServiceChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                return new UnitToUnitVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
            default:
                if(direction == Direction.INBOUND)
                {
                    return new UnknownISPMessage(dataUnitID, message, nac, timestamp);
                }
                else
                {
                    return new UnknownOSPMessage(dataUnitID, message, nac, timestamp);
                }
        }
    }
}
