/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.module.decode.mpt1327;

public enum MPTMessageType
{
    GTC_GO_TO_CHANNEL,

    ALH_ALOHA,
    ALHS_ALOHA_STANDARD_DATA_EXCLUDED,
    ALHD_ALOHA_SIMPLE_CALLS_EXCLUDED,
    ALHE_ALOHA_EMERGENCY_CALLS_ONLY,
    ALHR_ALOHA_EMERGENCY_OR_REGISTRATION,
    ALHX_ALOHA_REGISTRATION_EXCLUDED,
    ALHF_ALOHA_FALLBACK_MODE,

    ACK_ACKNOWLEDGE,
    ACKI_ACKNOWLEDGE_MORE_TO_FOLLOW,
    ACKQ_ACKNOWLEDGE_CALL_QUEUED,
    ACKX_ACKNOWLEDGE_MESSAGE_REJECTED,
    ACKV_ACKNOWLEDGE_CALLED_UNIT_UNAVAILABLE,
    ACKE_ACKNOWLEDGE_EMERGENCY,
    ACKT_ACKNOWLEDGE_TRY_ON_GIVEN_ADDRESS,
    ACKB_ACKNOWLEDGE_CALL_BACK_NEGATIVE_ACKNOWLEDGE,

    AHY_AHOY_GENERAL_AVAILABILITY_CHECK,
    AHYX_AHOY_CANCEL_ALERT_OR_WAITING_STATUS,
    AHYP_AHOY_CALLED_UNIT_PRESENCE_MONITORING,
    AHYQ_AHOY_STATUS_MESSAGE,
    AHYC_AHOY_SHORT_DATA_MESSAGE,

    MARK_CONTROL_CHANNEL_MARKER,

    MAINT_CALL_MAINTENANCE_MESSAGE,
    CLEAR_DOWN_FROM_ALLOCATED_CHANNEL,
    MOVE_TO_SPECIFIED_CHANNEL,
    BCAST_BROADCAST_SYSTEM_PARAMETERS,

    SAMO_OUTBOUND_SINGLE_ADDRESS_MESSAGE,
    SAMIS_INBOUND_SINGLE_ADDRESS_MESSAGE_SOLICITED,
    SAMIU_INBOUND_SINGLE_ADDRESS_MESSAGE_UNSOLICITED,

    HEAD_PLUS1_1_DATA_CODEWORD,
    HEAD_PLUS1_2_DATA_CODEWORD,
    HEAD_PLUS1_3_DATA_CODEWORD,
    HEAD_PLUS1_4_DATA_CODEWORD,

    GTT_GO_TO_TRANSACTION,

    SACK_STANDARD_DATA_SELECTIVE_ACK_HEADER,

    DACK_DATA_ACKNOWLEDGE,
    DALG_DATA_ALOHA_LIMIT_GROUP,
    DALN_DATA_ALOHA_LIMIT_NON_URGENT,

    DACK_DATA_ACKNOWLEDGE_GO_FRAGMENT_TRANSMIT_INVITATION,
    DACKZ_DATA_ACKNOWLEDGE_EXPEDITED_DATA,
    DACKZ_DATA_ACKNOWLEDGE_STANDARD_DATA,

    DAHY_DATA_AHOY_STANDARD,
    DRQG_DATA_REPEAT_GROUP_MESSAGE,
    DRQZ_DATA_REQUEST_CONTAINING_EXPEDITED_DATA,
    DAHYZ_DATA_AHOY_EXPEDITED,
    DAHYX_DATA_AHOY_STANDARD_DATA_FOR_CLOSING_TRANS,
    DRQX_DATA_REQUEST_TO_CLOSE_A_TRANSACTION,

    RLA_REPEAT_LAST_ACKNOWLEDGE,

    SITH_STANDARD_DATA_ADDRESS_CODEWORD_DATA_ITEM,

    UNKNOWN;

    public String getDescription()
    {
        return "";
    }

    public static MPTMessageType fromNumber(int number)
    {
        if(number < 256)
        {
            return GTC_GO_TO_CHANNEL;
        }

        switch(number)
        {
//            case 256:
//                return ALH_ALOHA;
//            case 257:
//                return ALHS;
//            case 258:
//                return ALHD;
//            case 259:
//                return ALHE;
//            case 260:
//                return ALHR;
//            case 261:
//                return ALHX;
//            case 262:
//                return ALHF;
//            case 264:
//                return ACK;
//            case 265:
//                return ACKI;
//            case 266:
//                return ACKQ;
//            case 267:
//                return ACKX;
//            case 268:
//                return ACKV;
//            case 269:
//                return ACKE;
//            case 270:
//                return ACKT;
//            case 271:
//                return ACKB;
//            case 272:
//                return AHOY;
//            case 274:
//                return AHYX;
//            case 277:
//                return AHYP;
//            case 278:
//                return AHYQ;
//            case 279:
//                return AHYC;
//            case 280:
//                return MARK;
//            case 281:
//                return MAINT;
//            case 282:
//                return CLEAR;
//            case 283:
//                return MOVE;
//            case 284:
//                return BCAST;
//            case 288:
//            case 289:
//            case 290:
//            case 291:
//            case 292:
//            case 293:
//            case 294:
//            case 295:
//            case 296:
//            case 297:
//            case 298:
//            case 299:
//            case 300:
//            case 301:
//            case 302:
//            case 303:
//                return SAMO;
//            case 304:
//            case 305:
//            case 306:
//            case 307:
//                return HEAD_PLUS1;
//            case 308:
//            case 309:
//            case 310:
//            case 311:
//                return HEAD_PLUS2;
//            case 312:
//            case 313:
//            case 314:
//            case 315:
//                return HEAD_PLUS3;
//            case 316:
//            case 317:
//            case 318:
//            case 319:
//                return HEAD_PLUS4;
//            case 320:
//            case 321:
//            case 322:
//            case 323:
//            case 324:
//            case 325:
//            case 326:
//            case 327:
//            case 328:
//            case 329:
//            case 330:
//            case 331:
//            case 332:
//            case 333:
//            case 334:
//            case 335:
//                return GTT;
//            case 416:
//                return DACK_DAL;
//            case 417:
//                return DACK_DALG;
//            case 418:
//                return DACK_DALN;
//            case 419:
//                return DACK_GO;
//            case 420:
//                return DACKZ;
//            case 421:
//                return DACKD;
//            case 424:
//                return DAHY;
//            case 426:
//                return DRQG;
//            case 428:
//                return DAHYZ;
//            case 430:
//                return DAHYX;
//            case 440:
//            case 441:
//            case 442:
//            case 443:
//                return SITH;
            default:
                return UNKNOWN;
        }
    }
}
