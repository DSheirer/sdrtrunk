/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.reference;

import java.util.EnumSet;

public enum Vendor
{
	STANDARD( "STANDARD", "STANDARD", 0),
	STANDARD_V1( "STANDARD", "STANDARD_01", 1),
	V2( "VENDOR02", "V_02", 2),
	V3( "VENDOR03", "V_03", 3),
	V4( "VENDOR04", "V_04", 4),
	V5( "VENDOR05", "V_05", 5),
	V6( "VENDOR06", "V_06", 6),
	V7( "VENDOR07", "V_07", 7),
	V8( "VENDOR08", "V_08", 8),
	V9( "VENDOR09", "V_09", 9),
	V10( "VENDOR0A", "V_0A", 10),
	V11( "VENDOR0B", "V_0B", 11),
	V12( "VENDOR0C", "V_0C", 12),
	V13( "VENDOR0D", "V_0D", 13),
	V14( "VENDOR0E", "V_0E", 14),
	V15( "VENDOR0F", "V_0F", 15),
	RELM( "RELM BK ", "RELM/BK RADIO", 16),
	V17( "VENDOR11", "V_11", 17),
	V18( "VENDOR12", "V_12", 18),
	V19( "VENDOR13", "V_13", 19),
	V20( "VENDOR14", "V_14", 20),
	V21( "VENDOR15", "V_15", 21),
	V22( "VENDOR16", "V_16", 22),
	V23( "VENDOR17", "V_17", 23),
	V24( "VENDOR18", "V_18", 24),
	V25( "VENDOR19", "V_19", 25),
	V26( "VENDOR1A", "V_1A", 26),
	V27( "VENDOR1B", "V_1B", 27),
	V28( "VENDOR1C", "V_1C", 28),
	V29( "VENDOR1D", "V_1D", 29),
	V30( "VENDOR1E", "V_1E", 30),
	V31( "VENDOR1F", "V_1F", 31),
	CYCOMM( "CYCOMM  ", "CYCOMM", 32),
	V33( "VENDOR21", "V_21", 33),
	V34( "VENDOR22", "V_22", 34),
	V35( "VENDOR23", "V_23", 35),
	V36( "VENDOR24", "V_24", 36),
	V37( "VENDOR25", "V_25", 37),
	V38( "VENDOR26", "V_26", 38),
	V39( "VENDOR27", "V_27", 39),
	EFRATOM( "EFRATOM ", "EFRATOM", 40),
	V41( "VENDOR29", "V_29", 41),
	V42( "VENDOR2A", "V_2A", 42),
	V43( "VENDOR2B", "V_2B", 43),
	V44( "VENDOR2C", "V_2C", 44),
	V45( "VENDOR2D", "V_2D", 45),
	V46( "VENDOR2E", "V_2E", 46),
	V47( "VENDOR2F", "V_2F", 47),
	ERICSSON( "ERICSSON", "ERICSSON", 48),
	V49( "VENDOR31", "V_31", 49),
	V50( "VENDOR32", "V_32", 50),
	V51( "VENDOR33", "V_33", 51),
	V52( "VENDOR34", "V_34", 52),
	V53( "VENDOR35", "V_35", 53),
	V54( "VENDOR36", "V_36", 54),
	V55( "VENDOR37", "V_37", 55),
	DATRON( "DATRON  ", "DATRON", 56),
	V57( "VENDOR39", "V_39", 57),
	V58( "VENDOR3A", "V_3A", 58),
	V59( "VENDOR3B", "V_3B", 59),
	V60( "VENDOR3C", "V_3C", 60),
	V61( "VENDOR3D", "V_3D", 61),
	V62( "VENDOR3E", "V_3E", 62),
	V63( "VENDOR3F", "V_3F", 63),
	ICOM( "ICOM    ", "ICOM", 64),
	V65( "VENDOR41", "V_41", 65),
	V66( "VENDOR42", "V_42", 66),
	V67( "VENDOR43", "V_43", 67),
	V68( "VENDOR44", "V_44", 68),
	V69( "VENDOR45", "V_45", 69),
	V70( "VENDOR46", "V_46", 70),
	V71( "VENDOR47", "V_47", 71),
	GARMIN( "GARMIN  ", "GARMIN", 72),
	V73( "VENDOR49", "V_49", 73),
	V74( "VENDOR4A", "V_4A", 74),
	V75( "VENDOR4B", "V_4B", 75),
	V76( "VENDOR4C", "V_4C", 76),
	V77( "VENDOR4D", "V_4D", 77),
	V78( "VENDOR4E", "V_4E", 78),
	V79( "VENDOR4F", "V_4F", 79),
	GTE( "GTE     ", "GTE", 80),
	V81( "VENDOR51", "V_51", 81),
	V82( "VENDOR52", "V_52", 82),
	V83( "VENDOR53", "V_53", 83),
	V84( "VENDOR54", "V_54", 84),
	IFR_SYSTEMS( "IFR SYST", "IFR SYSTEMS", 85),
	V86( "VENDOR54", "V_56", 86),
	V87( "VENDOR57", "V_57", 87),
	V88( "VENDOR58", "V_58", 88),
	V89( "VENDOR59", "V_59", 89),
	V90( "VENDOR5A", "V_5A", 90),
	V91( "VENDOR5B", "V_5B", 91),
	V92( "VENDOR5C", "V_5C", 92),
	V93( "VENDOR5D", "V_5D", 93),
	V94( "VENDOR5E", "V_5E", 94),
	V95( "VENDOR5F", "V_5F", 95),
	GEC_MARCONI( "MARCONI ", "GEC-MARCONI", 96),
	V97( "VENDOR61", "V_61", 97),
	V98( "VENDOR62", "V_62", 98),
	V99( "VENDOR63", "V_63", 99),
	V100( "VENDOR64", "V_64", 100),
	V101( "VENDOR65", "V_65", 101),
	V102( "VENDOR66", "V_66", 102),
	V103( "VENDOR67", "V_67", 103),
	KENWOOD( "KENWOOD ", "KENWOOD", 104),
	V105( "VENDOR69", "V_69", 105),
	V106( "VENDOR6A", "V_6A", 106),
	V107( "VENDOR6B", "V_6B", 107),
	V108( "VENDOR6C", "V_6C", 108),
	V109( "VENDOR6D", "V_6D", 109),
	V110( "VENDOR6E", "V_6E", 110),
	V111( "VENDOR6F", "V_6F", 111),
	GLENAYRE( "GLENAYRE", "GLENAYRE ELECTRONICS", 112),
	V113( "VENDOR71", "V_71", 113),
	V114( "VENDOR72", "V_72", 114),
	V115( "VENDOR73", "V_73", 115),
	JAPAN_RADIO( "JAPRADCO", "JAPAN RADIO CO", 116),
	V117( "VENDOR75", "V_75", 117),
	V118( "VENDOR76", "V_76", 118),
	V119( "VENDOR77", "V_77", 119),
	KOKUSAI( "KOKUSAI ", "KOKUSAI", 120),
	V121( "VENDOR79", "V_79", 121),
	V122( "VENDOR7A", "V_7A", 122),
	V123( "VENDOR7B", "V_7B", 123),
	MAXON( "MAXON  ", "MAXON", 124),
	V125( "VENDOR7D", "V_7D", 125),
	V126( "VENDOR7E", "V_7E", 126),
	V127( "VENDOR7F", "V_7F", 127),
	MIDLAND( "MIDLAND ", "MIDLAND", 128),
	V129( "VENDOR81", "V_81", 129),
	V130( "VENDOR82", "V_82", 130),
	V131( "VENDOR83", "V_83", 131),
	V132( "VENDOR84", "V_84", 132),
	V133( "VENDOR85", "V_85", 133),
	DANIELS( "DANIELS ", "DANIELS ELECTRONICS", 134),
	V135( "VENDOR87", "V_87", 135),
	V136( "VENDOR88", "V_88", 136),
	V137( "VENDOR89", "V_89", 137),
	V138( "VENDOR8A", "V_8A", 138),
	V139( "VENDOR8B", "V_8B", 139),
	V140( "VENDOR8C", "V_8C", 140),
	V141( "VENDOR8D", "V_8D", 141),
	V142( "VENDOR8E", "V_8E", 142),
	V143( "VENDOR8F", "V_8F", 143),
	MOTOROLA( "MOTOROLA", "MOTOROLA", 144),
	V145( "VENDOR91", "V_91", 145),
	V146( "VENDOR92", "V_92", 146),
	V147( "VENDOR93", "V_93", 147),
	V148( "VENDOR94", "V_94", 148),
	V149( "VENDOR95", "V_95", 149),
	V150( "VENDOR96", "V_96", 150),
	V151( "VENDOR97", "V_97", 151),
	V152( "VENDOR98", "V_98", 152),
	V153( "VENDOR99", "V_99", 153),
	V154( "VENDOR9A", "V_9A", 154),
	V155( "VENDOR9B", "V_9B", 155),
	V156( "VENDOR9C", "V_9C", 156),
	V157( "VENDOR9D", "V_9D", 157),
	V158( "VENDOR9E", "V_9E", 158),
	V159( "VENDOR9F", "V_9F", 159),
	THALES( "THALES  ", "THALES", 160),
	V161( "VENDORA1", "V_A1", 161),
	V162( "VENDORA2", "V_A2", 162),
	V163( "VENDORA3", "V_A3", 163),
	HARRIS( "HARRIS", "HARRIS", 164),
	V165( "VENDORA5", "V_A5", 165),
	V166( "VENDORA6", "V_A6", 166),
	V167( "VENDORA7", "V_A7", 167),
	V168( "VENDORA8", "V_A8", 168),
	V169( "VENDORA9", "V_A9", 169),
	V170( "VENDORAA", "V_AA", 170),
	V171( "VENDORAB", "V_AB", 171),
	V172( "VENDORAC", "V_AC", 172),
	V173( "VENDORAD", "V_AD", 173),
	V174( "VENDORAE", "V_AE", 174),
	V175( "VENDORAF", "V_AF", 175),
	RATHEON( "RATHEON ", "RATHEON", 176),
	V177( "VENDORB1", "V_B1", 177),
	V178( "VENDORB2", "V_B2", 178),
	V179( "VENDORB3", "V_B3", 179),
	V180( "VENDORB4", "V_B4", 180),
	V181( "VENDORB5", "V_B5", 181),
	V182( "VENDORB6", "V_B6", 182),
	V183( "VENDORB7", "V_B7", 183),
	V184( "VENDORB8", "V_B8", 184),
	V185( "VENDORB9", "V_B9", 185),
	V186( "VENDORBA", "V_BA", 186),
	V187( "VENDORBB", "V_BB", 187),
	V188( "VENDORBC", "V_BC", 188),
	V189( "VENDORBD", "V_BD", 189),
	V190( "VENDORBE", "V_BE", 190),
	V191( "VENDORBF", "V_BF", 191),
	SEA( "SEA     ", "SEA", 192),
	V193( "VENDORC1", "V_C1", 193),
	V194( "VENDORC2", "V_C2", 194),
	V195( "VENDORC3", "V_C3", 195),
	V196( "VENDORC4", "V_C4", 196),
	V197( "VENDORC5", "V_C5", 197),
	V198( "VENDORC6", "V_C6", 198),
	V199( "VENDORC7", "V_C7", 199),
	SECURICOR( "SECURICO", "SECURICOR", 200),
	V201( "VENDORC9", "V_C9", 201),
	V202( "VENDORCA", "V_CA", 202),
	V203( "VENDORCB", "V_CB", 203),
	V204( "VENDORCC", "V_CC", 204),
	V205( "VENDORCD", "V_CD", 205),
	V206( "VENDORCE", "V_CE", 206),
	V207( "VENDORCF", "V_CF", 207),
	ADI( "ADI     ", "ADI", 208),
	V209( "VENDORD1", "V_D1", 209),
	V210( "VENDORD2", "V_D2", 210),
	V211( "VENDORD3", "V_D3", 211),
	V212( "VENDORD4", "V_D4", 212),
	V213( "VENDORD5", "V_D5", 213),
	V214( "VENDORD6", "V_D6", 214),
	V215( "VENDORD7", "V_D7", 215),
	TAIT( "TAIT    ", "TAIT", 216),
	V217( "VENDORD9", "V_D9", 217),
	V218( "VENDORDA", "V_DA", 218),
	V219( "VENDORDB", "V_DB", 219),
	V220( "VENDORDC", "V_DC", 220),
	V221( "VENDORDD", "V_DD", 221),
	V222( "VENDORDE", "V_DE", 222),
	V223( "VENDORDF", "V_DF", 223),
	TELETEC( "TELETEC ", "TELETEC", 224),
	V225( "VENDORE1", "V_E1", 225),
	V226( "VENDORE2", "V_E2", 226),
	V227( "VENDORE3", "V_E3", 227),
	V228( "VENDORE4", "V_E4", 228),
	V229( "VENDORE5", "V_E5", 229),
	V230( "VENDORE6", "V_E6", 230),
	V231( "VENDORE7", "V_E7", 231),
	V232( "VENDORE8", "V_E8", 232),
	V233( "VENDORE9", "V_E9", 233),
	V234( "VENDOREA", "V_EA", 234),
	V235( "VENDOREB", "V_EB", 235),
	V236( "VENDOREC", "V_EC", 236),
	V237( "VENDORED", "V_ED", 237),
	V238( "VENDOREE", "V_EE", 238),
	V239( "VENDOREF", "V_EF", 239),
	TRANSCRYPT( "TRANSCRPT", "TRANSCRYPT", 240),
	V241( "VENDORF1", "V_F1", 241),
	V242( "VENDORF2", "V_F2", 242),
	V243( "VENDORF3", "V_F3", 243),
	V244( "VENDORF4", "V_F4", 244),
	V245( "VENDORF5", "V_F5", 245),
	V246( "VENDORF6", "V_F6", 246),
	V247( "VENDORF7", "V_F7", 247),
	V248( "VENDORF8", "V_F8", 248),
	V249( "VENDORF9", "V_F9", 249),
	V250( "VENDORFA", "V_FA", 250),
	V251( "VENDORFB", "V_FB", 251),
	V252( "VENDORFC", "V_FC", 252),
	V253( "VENDORFD", "V_FD", 253),
	V254( "VENDORFE", "V_FE", 254),
	V255( "VENDORFF", "V_FF", 255),
	VUNK( "UNKNOWN ", "UNKN", -1 );
	
	private String mLabel;
	private String mDescription;
	private int mValue;

	/**
	 * Constructs an instance
	 * @param label for the vendor
	 * @param description of the vendor
	 * @param value for the vendor
	 */
	Vendor( String label, String description, int value )
	{
		mLabel = label;
		mDescription = description;
		mValue = value;
	}

	public static EnumSet<Vendor> LOGGABLE_VENDORS = EnumSet.of(STANDARD, MOTOROLA, HARRIS);
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public String getDescription()
	{
		return mDescription;
	}
	
	public int getValue()
	{
		return mValue;
	}

	/**
	 * Vendors where we log when new opcodes are encountered.
	 */
	public boolean isLoggable()
	{
		return LOGGABLE_VENDORS.contains(this);
	}

	public static Vendor fromValue( int value )
	{
		if( 0 <= value && value <= 255 )
		{
			return values()[ value ];
		}
		
		return VUNK;
	}
	
}
