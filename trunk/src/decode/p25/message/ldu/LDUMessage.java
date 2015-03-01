package decode.p25.message.ldu;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.P25Interleave;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import edac.CRC;
import edac.Galois24;

public class LDUMessage extends P25Message
{
	public static final int IMBE_FRAME_1 = 64;
	public static final int IMBE_FRAME_2 = 208;
	public static final int IMBE_FRAME_3 = 392;
	public static final int IMBE_FRAME_4 = 576;
	public static final int IMBE_FRAME_5 = 760;
	public static final int IMBE_FRAME_6 = 944;
	public static final int IMBE_FRAME_7 = 1128;
	public static final int IMBE_FRAME_8 = 1312;
	public static final int IMBE_FRAME_9 = 1488;
	
	public static final int[] PN_SEED_1 = { 64,65,66,67,68,69,70,71,72,73,74,75 };
	public static final int[] PN_SEED_2 = { 208,209,210,211,212,213,214,215,216,
		217,218,219 };
	public static final int[] PN_SEED_3 = { 392,393,394,395,396,397,398,399,400,
		401,402,403 };
	public static final int[] PN_SEED_4 = { 576,577,578,579,580,581,582,583,584,
		585,586,587 };
	public static final int[] PN_SEED_5 = { 760,761,762,763,764,765,766,767,768,
		769,770,771 };
	public static final int[] PN_SEED_6 = { 944,945,946,947,948,949,950,951,952,
		953,954,955 };
	public static final int[] PN_SEED_7 = { 1128,1129,1130,1131,1132,1133,1134,
		1135,1136,1137,1138,1139 };
	public static final int[] PN_SEED_8 = { 1312,1313,1314,1315,1316,1317,1318,
		1319,1320,1321,1322,1323 };
	public static final int[] PN_SEED_9 = { 1488,1489,1490,1491,1492,1493,1494,
		1495,1496,1497,1498,1499 };
	
	public static final int[] FRAME1_1= { 64,65,66,67,68,69,70,71 };
	public static final int[] FRAME1_2= { 72,73,74,75,87,88,89,90 };
	public static final int[] FRAME1_3= { 91,92,93,94,95,96,97,98 };
	public static final int[] FRAME1_4= { 110,111,112,113,114,115,116,117 };
	public static final int[] FRAME1_5= { 118,119,120,121,133,134,135,136 };
	public static final int[] FRAME1_6= { 137,138,139,140,141,142,143,144 };
	public static final int[] FRAME1_7= { 156,157,158,159,160,161,162,163 };
	public static final int[] FRAME1_8= { 164,165,166,171,172,173,174,175 };
	public static final int[] FRAME1_9= { 176,177,178,179,180,181,186,187 };
	public static final int[] FRAME1_10= { 188,189,190,191,192,193,194,195 };
	public static final int[] FRAME1_11= { 196,201,202,203,204,205,206,207 };
	
	public static final int[] FRAME2_1= { 208,209,210,211,212,213,214,215 };
	public static final int[] FRAME2_2= { 216,217,218,219,231,232,233,234 };
	public static final int[] FRAME2_3= { 235,236,237,238,239,240,241,242 };
	public static final int[] FRAME2_4= { 254,255,256,257,258,259,260,261 };
	public static final int[] FRAME2_5= { 262,263,264,265,277,278,279,280 };
	public static final int[] FRAME2_6= { 281,282,283,284,285,286,287,288 };
	public static final int[] FRAME2_7= { 300,301,302,303,304,305,306,307 };
	public static final int[] FRAME2_8= { 308,309,310,315,316,317,318,319 };
	public static final int[] FRAME2_9= { 320,321,322,323,324,325,330,331 };
	public static final int[] FRAME2_10= { 332,333,334,335,336,337,338,339 };
	public static final int[] FRAME2_11= { 340,345,346,347,348,349,350,351 };

	public static final int[] FRAME3_1= { 392,393,394,395,396,397,398,399 };
	public static final int[] FRAME3_2= { 400,401,402,403,415,416,417,418 };
	public static final int[] FRAME3_3= { 419,420,421,422,423,424,425,426 };
	public static final int[] FRAME3_4= { 438,439,440,441,442,443,444,445 };
	public static final int[] FRAME3_5= { 446,447,448,449,461,462,463,464 };
	public static final int[] FRAME3_6= { 465,466,467,468,469,470,471,472 };
	public static final int[] FRAME3_7= { 484,485,486,487,488,489,490,491 };
	public static final int[] FRAME3_8= { 492,493,494,499,500,501,502,503 };
	public static final int[] FRAME3_9= { 504,505,506,507,508,509,514,515 };
	public static final int[] FRAME3_10= { 516,517,518,519,520,521,522,523 };
	public static final int[] FRAME3_11= { 524,529,530,531,532,533,534,535 };

	public static final int[] FRAME4_1= { 576,577,578,579,580,581,582,583 };
	public static final int[] FRAME4_2= { 584,585,586,587,599,600,601,602 };
	public static final int[] FRAME4_3= { 603,604,605,606,607,608,609,610 };
	public static final int[] FRAME4_4= { 622,623,624,625,626,627,628,629 };
	public static final int[] FRAME4_5= { 630,631,632,633,645,646,647,648 };
	public static final int[] FRAME4_6= { 649,650,651,652,653,654,655,656 };
	public static final int[] FRAME4_7= { 668,669,670,671,672,673,674,675 };
	public static final int[] FRAME4_8= { 676,677,678,683,684,685,686,687 };
	public static final int[] FRAME4_9= { 688,689,690,691,692,693,698,699 };
	public static final int[] FRAME4_10= { 700,701,702,703,704,705,706,707 };
	public static final int[] FRAME4_11= { 708,713,714,715,716,717,718,719 };

	public static final int[] FRAME5_1= { 760,761,762,763,764,765,766,767 };
	public static final int[] FRAME5_2= { 768,769,770,771,783,784,785,786 };
	public static final int[] FRAME5_3= { 787,788,789,790,791,792,793,794 };
	public static final int[] FRAME5_4= { 806,807,808,809,810,811,812,813 };
	public static final int[] FRAME5_5= { 814,815,816,817,829,830,831,832 };
	public static final int[] FRAME5_6= { 833,834,835,836,837,838,839,840 };
	public static final int[] FRAME5_7= { 852,853,854,855,856,857,858,859 };
	public static final int[] FRAME5_8= { 860,861,862,867,868,869,870,871 };
	public static final int[] FRAME5_9= { 872,873,874,875,876,877,882,883 };
	public static final int[] FRAME5_10= { 884,885,886,887,888,889,890,891 };
	public static final int[] FRAME5_11= { 892,897,898,899,900,901,902,903 };

	public static final int[] FRAME6_1= { 944,945,946,947,948,949,950,951 };
	public static final int[] FRAME6_2= { 952,953,954,955,967,968,969,970 };
	public static final int[] FRAME6_3= { 971,972,973,974,975,976,977,978 };
	public static final int[] FRAME6_4= { 990,991,992,993,994,995,996,997 };
	public static final int[] FRAME6_5= { 998,999,1000,1001,1013,1014,1015,1016 };
	public static final int[] FRAME6_6= { 1017,1018,1019,1020,1021,1022,1023,1024 };
	public static final int[] FRAME6_7= { 1036,1037,1038,1039,1040,1041,1042,1043 };
	public static final int[] FRAME6_8= { 1044,1045,1046,1051,1052,1053,1054,1055 };
	public static final int[] FRAME6_9= { 1056,1057,1058,1059,1060,1061,1066,1067 };
	public static final int[] FRAME6_10= { 1068,1069,1070,1071,1072,1073,1074,1075 };
	public static final int[] FRAME6_11= { 1076,1081,1082,1083,1084,1085,1086,1087 };

	public static final int[] FRAME7_1= { 1128,1129,1130,1131,1132,1133,1134,1135 };
	public static final int[] FRAME7_2= { 1136,1137,1138,1139,1151,1152,1153,1154 };
	public static final int[] FRAME7_3= { 1155,1156,1157,1158,1159,1160,1161,1162 };
	public static final int[] FRAME7_4= { 1174,1175,1176,1177,1178,1179,1180,1181 };
	public static final int[] FRAME7_5= { 1182,1183,1184,1185,1197,1198,1199,1200 };
	public static final int[] FRAME7_6= { 1201,1202,1203,1204,1205,1206,1207,1208 };
	public static final int[] FRAME7_7= { 1220,1221,1222,1223,1224,1225,1226,1227 };
	public static final int[] FRAME7_8= { 1228,1229,1230,1235,1236,1237,1238,1239 };
	public static final int[] FRAME7_9= { 1240,1241,1242,1243,1244,1245,1250,1251 };
	public static final int[] FRAME7_10= { 1252,1253,1254,1255,1256,1257,1258,1259 };
	public static final int[] FRAME7_11= { 1260,1265,1266,1267,1268,1269,1270,1271 };

	public static final int[] FRAME8_1= { 1312,1313,1314,1315,1316,1317,1318,1319 };
	public static final int[] FRAME8_2= { 1320,1321,1322,1323,1335,1336,1337,1338 };
	public static final int[] FRAME8_3= { 1339,1340,1341,1342,1343,1344,1345,1346 };
	public static final int[] FRAME8_4= { 1358,1359,1360,1361,1362,1363,1364,1365 };
	public static final int[] FRAME8_5= { 1366,1367,1368,1369,1381,1382,1383,1384 };
	public static final int[] FRAME8_6= { 1385,1386,1387,1388,1389,1390,1391,1392 };
	public static final int[] FRAME8_7= { 1404,1405,1406,1407,1408,1409,1410,1411 };
	public static final int[] FRAME8_8= { 1412,1413,1414,1419,1420,1421,1422,1423 };
	public static final int[] FRAME8_9= { 1424,1425,1426,1427,1428,1429,1434,1435 };
	public static final int[] FRAME8_10= { 1436,1437,1438,1439,1440,1441,1442,1443 };
	public static final int[] FRAME8_11= { 1444,1449,1450,1451,1452,1453,1454,1455 };

	public static final int[] FRAME9_1= { 1488,1489,1490,1491,1492,1493,1494,1495 };
	public static final int[] FRAME9_2= { 1496,1497,1498,1499,1511,1512,1513,1514 };
	public static final int[] FRAME9_3= { 1515,1516,1517,1518,1519,1520,1521,1522 };
	public static final int[] FRAME9_4= { 1534,1535,1536,1537,1538,1539,1540,1541 };
	public static final int[] FRAME9_5= { 1542,1543,1544,1545,1557,1558,1559,1560 };
	public static final int[] FRAME9_6= { 1561,1562,1563,1564,1565,1566,1567,1568 };
	public static final int[] FRAME9_7= { 1580,1581,1582,1583,1584,1585,1586,1587 };
	public static final int[] FRAME9_8= { 1588,1589,1590,1595,1596,1597,1598,1599 };
	public static final int[] FRAME9_9= { 1600,1601,1602,1603,1604,1605,1610,1611 };
	public static final int[] FRAME9_10= { 1612,1613,1614,1615,1616,1617,1618,1619 };
	public static final int[] FRAME9_11= { 1620,1625,1626,1627,1628,1629,1630,1631 };
	
	public static final int[] LOW_SPEED_DATA = { 1456,1457,1458,1459,1460,1461,
		1462,1463,1472,1473,1474,1475,1476,1477,1478,1479 };

	public LDUMessage( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    deinterleave();

	    /* Perform error detection & correction on coset zero of each frame */
	    errorCorrectCosetZero();
	    
	    /* Remove randomizer from coset words c1 - c6 */
	    derandomize();

	    /* Perform edac against remaining info vectors */
	    errorCorrectInformationVectors();

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */	    
	    
//TODO: update crc status for voice frames
	    mCRC = new CRC[ 2 ];
	    mCRC[ 0 ] = CRC.PASSED;
    }
	
	@Override
    public String getMessage()
    {
	    return getMessageStub();
    }
	
    public String getMessageStub()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		sb.append( " VOICE LSD:" );
		sb.append( getLowSpeedData() );
		sb.append( " " );
		
		return sb.toString();
    }
	
	public String getLowSpeedData()
	{
		return mMessage.getHex( LOW_SPEED_DATA, 4 );
	}

	/**
	 * Deinterleaves 144 message bits of each of the 9 imbe voice frames
	 */
	private void deinterleave()
	{
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_1, IMBE_FRAME_1 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_2, IMBE_FRAME_2 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_3, IMBE_FRAME_3 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_4, IMBE_FRAME_4 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_5, IMBE_FRAME_5 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_6, IMBE_FRAME_6 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_7, IMBE_FRAME_7 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_8, IMBE_FRAME_8 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_FRAME_9, IMBE_FRAME_9 + 144 );
	}


	/**
	 * Performs error detection and correction on coset work zero of each of the
	 * 9 imbe voice frames.
	 */
	private void errorCorrectCosetZero()
	{
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_1 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_2 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_3 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_4 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_5 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_6 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_7 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_8 );
	    Galois24.checkAndCorrect( mMessage, IMBE_FRAME_9 );
	}

	/**
	 * Generates a pseudo-random noise randomizer mask and applies (xor) the 
	 * mask against coset words c1 through c6 of each of the 9 imbe voice frames
	 */
	private void derandomize()
	{
		derandomize( PN_SEED_1 );
		derandomize( PN_SEED_2 );
		derandomize( PN_SEED_3 );
		derandomize( PN_SEED_4 );
		derandomize( PN_SEED_5 );
		derandomize( PN_SEED_6 );
		derandomize( PN_SEED_7 );
		derandomize( PN_SEED_8 );
		derandomize( PN_SEED_9 );
	}

	/**
	 * Perform error detection and correction against information vectors 1 - 6
	 */
	private void errorCorrectInformationVectors()
	{
		errorCorrectInformationVectors( IMBE_FRAME_1 );
		errorCorrectInformationVectors( IMBE_FRAME_2 );
		errorCorrectInformationVectors( IMBE_FRAME_3 );
		errorCorrectInformationVectors( IMBE_FRAME_4 );
		errorCorrectInformationVectors( IMBE_FRAME_5 );
		errorCorrectInformationVectors( IMBE_FRAME_6 );
		errorCorrectInformationVectors( IMBE_FRAME_7 );
		errorCorrectInformationVectors( IMBE_FRAME_8 );
		errorCorrectInformationVectors( IMBE_FRAME_9 );
	}
	
	/**
	 * Performs Golay(23,12,7) error detection and correction against information
	 * vectors 1 - 3 and Hamming(15,11,3) against vectors 4 - 6.
	 * 
	 * @param frameStart - bit position of the imbe frame start
	 */
	private void errorCorrectInformationVectors( int frameStart )
	{
	    Galois24.checkAndCorrect( mMessage, frameStart + 23 );
	    Galois24.checkAndCorrect( mMessage, frameStart + 46 );
	    Galois24.checkAndCorrect( mMessage, frameStart + 69 );

	    //TODO: hamming edac for iv 4,5,6 ...
	}
	
	
	/* Temporary override */
	public boolean isValid()
	{
		return true;
	}

	/**
	 * Removes randomizer by generating a pseudo-random noise sequence from the 
	 * first 12 bits of coset word c0 and applies (xor) that sequence against 
	 * message coset words c1 through c6.
	 * 
	 * @param seedBits - first 12 bit indexes of coset word c0
	 */
	public void derandomize( int[] seedBits )
	{
		/* Set the offset to the first seed bit plus 23 to point to coset c1 */
		int offset = seedBits[ 0 ] + 23;
		
		/* Get seed value from first 12 bits of coset c0 */
		int seed = mMessage.getInt( seedBits );
		
		/* Left shift 4 places (multiply by 16) */
		seed <<= 4;
		
		for( int x = 0; x < 114; x++ )
		{
			seed = ( ( 173 * seed ) + 13849 ) & 65536;

			/* If seed bit 15 is set, toggle the corresponding message bit */
			if( ( seed & 32768 ) == 32768 )
			{
				mMessage.flip( x + offset );
			}
		}
	}
	
	/**
	 * Extracts 9 IMBE voice frames of 11-bytes (88-bit) each into a 99 byte array
	 */
	public byte[] getIMBEFrameData()
	{
		byte[] data = new byte[ 99 ];
		
		data[ 0 ] = mMessage.getByte( FRAME1_1 );
		data[ 1 ] = mMessage.getByte( FRAME1_2 );
		data[ 2 ] = mMessage.getByte( FRAME1_3 );
		data[ 3 ] = mMessage.getByte( FRAME1_4 );
		data[ 4 ] = mMessage.getByte( FRAME1_5 );
		data[ 5 ] = mMessage.getByte( FRAME1_6 );
		data[ 6 ] = mMessage.getByte( FRAME1_7 );
		data[ 7 ] = mMessage.getByte( FRAME1_8 );
		data[ 8 ] = mMessage.getByte( FRAME1_9 );
		data[ 9 ] = mMessage.getByte( FRAME1_10 );
		data[ 10 ] = mMessage.getByte( FRAME1_11 );
		
		data[ 11 ] = mMessage.getByte( FRAME2_1 );
		data[ 12 ] = mMessage.getByte( FRAME2_2 );
		data[ 13 ] = mMessage.getByte( FRAME2_3 );
		data[ 14 ] = mMessage.getByte( FRAME2_4 );
		data[ 15 ] = mMessage.getByte( FRAME2_5 );
		data[ 16 ] = mMessage.getByte( FRAME2_6 );
		data[ 17 ] = mMessage.getByte( FRAME2_7 );
		data[ 18 ] = mMessage.getByte( FRAME2_8 );
		data[ 19 ] = mMessage.getByte( FRAME2_9 );
		data[ 20 ] = mMessage.getByte( FRAME2_10 );
		data[ 21 ] = mMessage.getByte( FRAME2_11 );
		
		data[ 22 ] = mMessage.getByte( FRAME3_1 );
		data[ 23 ] = mMessage.getByte( FRAME3_2 );
		data[ 24 ] = mMessage.getByte( FRAME3_3 );
		data[ 25 ] = mMessage.getByte( FRAME3_4 );
		data[ 26 ] = mMessage.getByte( FRAME3_5 );
		data[ 27 ] = mMessage.getByte( FRAME3_6 );
		data[ 28 ] = mMessage.getByte( FRAME3_7 );
		data[ 29 ] = mMessage.getByte( FRAME3_8 );
		data[ 30 ] = mMessage.getByte( FRAME3_9 );
		data[ 31 ] = mMessage.getByte( FRAME3_10 );
		data[ 32 ] = mMessage.getByte( FRAME3_11 );
		
		data[ 33 ] = mMessage.getByte( FRAME4_1 );
		data[ 34 ] = mMessage.getByte( FRAME4_2 );
		data[ 35 ] = mMessage.getByte( FRAME4_3 );
		data[ 36 ] = mMessage.getByte( FRAME4_4 );
		data[ 37 ] = mMessage.getByte( FRAME4_5 );
		data[ 38 ] = mMessage.getByte( FRAME4_6 );
		data[ 39 ] = mMessage.getByte( FRAME4_7 );
		data[ 40 ] = mMessage.getByte( FRAME4_8 );
		data[ 41 ] = mMessage.getByte( FRAME4_9 );
		data[ 42 ] = mMessage.getByte( FRAME4_10 );
		data[ 43 ] = mMessage.getByte( FRAME4_11 );
		
		data[ 44 ] = mMessage.getByte( FRAME5_1 );
		data[ 45 ] = mMessage.getByte( FRAME5_2 );
		data[ 46 ] = mMessage.getByte( FRAME5_3 );
		data[ 47 ] = mMessage.getByte( FRAME5_4 );
		data[ 48 ] = mMessage.getByte( FRAME5_5 );
		data[ 49 ] = mMessage.getByte( FRAME5_6 );
		data[ 50 ] = mMessage.getByte( FRAME5_7 );
		data[ 51 ] = mMessage.getByte( FRAME5_8 );
		data[ 52 ] = mMessage.getByte( FRAME5_9 );
		data[ 53 ] = mMessage.getByte( FRAME5_10 );
		data[ 54 ] = mMessage.getByte( FRAME5_11 );
		
		data[ 55 ] = mMessage.getByte( FRAME6_1 );
		data[ 56 ] = mMessage.getByte( FRAME6_2 );
		data[ 57 ] = mMessage.getByte( FRAME6_3 );
		data[ 58 ] = mMessage.getByte( FRAME6_4 );
		data[ 59 ] = mMessage.getByte( FRAME6_5 );
		data[ 60 ] = mMessage.getByte( FRAME6_6 );
		data[ 61 ] = mMessage.getByte( FRAME6_7 );
		data[ 62 ] = mMessage.getByte( FRAME6_8 );
		data[ 63 ] = mMessage.getByte( FRAME6_9 );
		data[ 64 ] = mMessage.getByte( FRAME6_10 );
		data[ 65 ] = mMessage.getByte( FRAME6_11 );
		
		data[ 66 ] = mMessage.getByte( FRAME7_1 );
		data[ 67 ] = mMessage.getByte( FRAME7_2 );
		data[ 68 ] = mMessage.getByte( FRAME7_3 );
		data[ 69 ] = mMessage.getByte( FRAME7_4 );
		data[ 70 ] = mMessage.getByte( FRAME7_5 );
		data[ 71 ] = mMessage.getByte( FRAME7_6 );
		data[ 72 ] = mMessage.getByte( FRAME7_7 );
		data[ 73 ] = mMessage.getByte( FRAME7_8 );
		data[ 74 ] = mMessage.getByte( FRAME7_9 );
		data[ 75 ] = mMessage.getByte( FRAME7_10 );
		data[ 76 ] = mMessage.getByte( FRAME7_11 );
		
		data[ 77 ] = mMessage.getByte( FRAME8_1 );
		data[ 78 ] = mMessage.getByte( FRAME8_2 );
		data[ 79 ] = mMessage.getByte( FRAME8_3 );
		data[ 80 ] = mMessage.getByte( FRAME8_4 );
		data[ 81 ] = mMessage.getByte( FRAME8_5 );
		data[ 82 ] = mMessage.getByte( FRAME8_6 );
		data[ 83 ] = mMessage.getByte( FRAME8_7 );
		data[ 84 ] = mMessage.getByte( FRAME8_8 );
		data[ 85 ] = mMessage.getByte( FRAME8_9 );
		data[ 86 ] = mMessage.getByte( FRAME8_10 );
		data[ 87 ] = mMessage.getByte( FRAME8_11 );
		
		data[ 88 ] = mMessage.getByte( FRAME9_1 );
		data[ 89 ] = mMessage.getByte( FRAME9_2 );
		data[ 90 ] = mMessage.getByte( FRAME9_3 );
		data[ 91 ] = mMessage.getByte( FRAME9_4 );
		data[ 92 ] = mMessage.getByte( FRAME9_5 );
		data[ 93 ] = mMessage.getByte( FRAME9_6 );
		data[ 94 ] = mMessage.getByte( FRAME9_7 );
		data[ 95 ] = mMessage.getByte( FRAME9_8 );
		data[ 96 ] = mMessage.getByte( FRAME9_9 );
		data[ 97 ] = mMessage.getByte( FRAME9_10 );
		data[ 98 ] = mMessage.getByte( FRAME9_11 );
		
		return data;
	}
}
