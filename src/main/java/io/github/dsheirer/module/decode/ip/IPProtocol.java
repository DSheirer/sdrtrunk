/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip;

public enum IPProtocol
{
	HOPOPT( 0, "IPV6 HOP-BY-HOP" ),
	ICMP( 1, "ICMP" ),
	IGMP( 2, "IGMP" ),
	GGP( 3, "GGP" ),
	IPinIP( 4, "IPinIP" ),
	ST( 5, "ST" ),
	TCP( 6, "TCP" ),
	CBT( 7, "CBT" ),
	EGP( 8, "EGP" ),
	IGP( 9, "IGP" ),
	BBN( 10, "BBN" ),
	NVP( 11, "NVP" ),
	PUP( 12, "PUP" ),
	ARGUS( 13, "ARGUS" ),
	EMCON( 14, "EMCON" ),
	XNET( 15, "XNET" ),
	CHAOS( 16, "CHAOS" ),
	UDP( 17, "UDP" ),
	MUX( 18, "MUX" ),
	DCN( 19, "DCN" ),
	HMP( 20, "HMP" ),
	PRM( 21, "PRM" ),
	XNS_IDP( 22, "XNS-IDP" ),
	TRUNK_1( 23, "TRUNK-1" ),
	TRUNK_2( 24, "TRUNK-2" ),
	LEAF_1( 25, "LEAF-1" ),
	LEAF_2( 26, "LEAF-2" ),
	RDP( 27, "RDP" ),
	IRTP( 28, "IRTP" ),
	ISO_TP4( 29, "ISO-TP4" ),
	NETBLT( 30, "NETBLT" ),
	MFE_NSP( 31, "MFE-NSP" ),
	MERIT_INP( 32, "MERIT-INP" ),
	DCCP( 33, "DCCP" ),
	_3PC( 34, "3PC" ),
	IDPR( 35, "IDPR" ),
	XTP( 36, "XTP" ),
	DDP( 37, "DDP" ),
	IDPR_CMTP( 38, "IDPR-CMTP" ),
	TPPP( 39, "TP++" ),
	IL( 40, "IL" ),
	IPV6( 41, "IPV6" ),
	SDRP( 42, "SDRP" ),
	IPV6_ROUTE( 43, "IPV6-ROUTE" ),
	IPV6_FRAG( 44, "IPV6-FRAG" ),
	IDRP( 45, "IDRP" ),
	RSVP( 46, "RSVP" ),
	GRE( 47, "GRE" ),
	MHRP( 48, "MHRP" ),
	BNA( 49, "BNA" ),
	ESP( 50, "ESP" ),
	AH( 51, "AH" ),
	INLSP( 52, "I-NLSP" ),
	SWIPE( 53, "SWIPE" ),
	NARP( 54, "NARP" ),
	MOBILE( 55, "MOBILE" ),
	TLSP( 56, "TLSP" ),
	SKIP( 57, "SKIP" ),
	IPV6_ICMP( 58, "IPV6-ICMP" ),
	IPV6_NONXT( 59, "IPV6-NoNXT" ),
	IPV6_OPTS( 60, "IPV6-OPTS" ),
	ANY_HOST( 61, "ANY HOST" ),
	CFTP( 62, "CFTP" ),
	ANY_NETWORK( 63, "ANY NETWORK" ),
	SAT_EXPAK( 64, "SAT_EXPAK" ),
	KRYPTOLAN( 65, "KRYPTOLAN" ),
	RVD( 66, "RVD" ),
	IPPC( 67, "IPPC" ),
	ANY_DIST( 68, "ANY DISTRIBUTED" ),
	SAT_MON( 69, "SAT-MON" ),
	VISA( 70, "VISA" ),
	IPCU( 71, "IPCU" ),
	CPNX( 72, "CPNX" ),
	CPHB( 73, "CPHB" ),
	WSN( 74, "WSN" ),
	PVP( 75, "PVP" ),
	BR_SAT_MON( 76, "BR-SAT-MON" ),
	SUN_ND( 77, "SUN-ND" ),
	WB_MON( 78, "WB-MON" ),
	WB_EXPAK( 79, "WB-EXPAK" ),
	ISO_IP( 80, "ISO-IP" ),
	VMTP( 81, "VMTP" ),
	SECURE_VMTP( 82, "SECURE-VMTP" ),
	VINES( 83, "VINES" ),
	TTP( 84, "TTP" ),
	NSFNET_IGP( 85, "NSFNET-IGP" ),
	DGP( 86, "DGP" ),
	TCF( 87, "TCF" ),
	EIGRP( 88, "EIGRP" ),
	OSPF( 89, "OSPF" ),
	SPRITE_RPC( 90, "SPRITE-RPC" ),
	LARP( 91, "LARP" ),
	MTP( 92, "MTP" ),
	AX25( 93, "AX-25" ),
	IPIP( 94, "IPIP" ),
	MICP( 95, "MICP" ),
	SCC_SP( 96, "SCC-SP" ),
	ETHERIP( 97, "ETHER-IP" ),
	ENCAP( 98, "ENCAP" ),
	ANY_PRIVATE( 99, "ANY PRIVATE" ),
	GMTP( 100, "GMTP" ),
	IFMP( 101, "IFMP" ),
	PNNI( 102, "PNNI" ),
	PIM( 103, "PIM" ),
	ARIS( 104, "ARIS" ),
	SCPS( 105, "SCPS" ),
	QNX( 106, "QNX" ),
	A_N( 107, "A-N" ),
	IPCOMP( 108, "IPCOMP" ),
	SNP( 109, "SNP" ),
	COMPAQ_PEER( 110, "COMPAQ-PEER" ),
	IPXinIP( 111, "IPX-in-IP" ),
	VRRP( 112, "VRRP" ),
	PGM( 113, "PGM" ),
	ANY_0_HOP( 114, "ANY 0-HOP" ),
	L2TP( 115, "L2TP" ),
	DDX( 116, "DDX" ),
	IATP( 117, "IATP" ),
	STP( 118, "STP" ),
	SRP( 119, "SRP" ),
	UTI( 120, "UTI" ),
	SMP( 121, "SMP" ),
	SM( 122, "SM" ),
	PTP( 123, "PTP" ),
	ISIS( 124, "IS-IS OVER IPV4" ),
	FIRE( 125, "FIRE" ),
	CRTP( 126, "CRTP" ),
	CRUDP( 127, "CRUDP" ),
	SSCOPMCE( 128, "SSCOPMCE" ),
	IPLT( 129, "IPLT" ),
	SPS( 130, "SPS" ),
	PIPE( 131, "PIPE" ),
	SCTP( 132, "SCTP" ),
	FC( 133, "FC" ),
	RSVP_E2E( 134, "RSVP E2E" ),
	MOBILITY( 135, "MOBILITY HDR" ),
	UDPLITE( 136, "UDP LITE" ),
	MPLSinIP( 137, "MPLS-IN-IP" ),
	MANET( 138, "MANET" ),
	HIP( 139, "HIP" ),
	SHIM6( 140, "SHIM6" ),
	WESP( 141, "WESP" ),
	ROHC( 142, "ROHC" ),
	UNKNOWN( -1, "UNKNOWN" );
	
	private int mValue;
	private String mLabel;
	
	private IPProtocol( int value, String label )
	{
		mValue = value;
		mLabel = label;
	}
	
	public int getValue()
	{
		return mValue;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public static IPProtocol fromValue( int value )
	{
		if( 0 <= value && value <= 142 )
		{
			return values()[ value ];
		}
		
		return IPProtocol.UNKNOWN;
	}
}
