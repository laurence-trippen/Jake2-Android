/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

// Created on 25.01.2004 by RST.

package jake2.qcommon.network.messages.client;

import jake2.qcommon.sys.Sys;

class CRC
{

	private final static short CRC_INIT_VALUE= (short) 0xffff;

	private static int[] crctable = { 0x0000, 0x1021, 0x2042, 0x3063, 0x4084,
            0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c,
            0xd1ad, 0xe1ce, 0xf1ef, 0x1231, 0x0210, 0x3273, 0x2252, 0x52b5,
            0x4294, 0x72f7, 0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd,
            0xc39c, 0xf3ff, 0xe3de, 0x2462, 0x3443, 0x0420, 0x1401, 0x64e6,
            0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee,
            0xf5cf, 0xc5ac, 0xd58d, 0x3653, 0x2672, 0x1611, 0x0630, 0x76d7,
            0x66f6, 0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df,
            0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840,
            0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948,
            0x9969, 0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71,
            0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79,
            0x8b58, 0xbb3b, 0xab1a, 0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22,
            0x3c03, 0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a,
            0xbd0b, 0x8d68, 0x9d49, 0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13,
            0x2e32, 0x1e51, 0x0e70, 0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b,
            0xaf3a, 0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c,
            0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004,
            0x4025, 0x7046, 0x6067, 0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d,
            0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235,
            0x5214, 0x6277, 0x7256, 0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e,
            0xe54f, 0xd52c, 0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466,
            0x6447, 0x5424, 0x4405, 0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f,
            0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657,
            0x7676, 0x4615, 0x5634, 0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8,
            0x89e9, 0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827, 0x18c0,
            0x08e1, 0x3882, 0x28a3, 0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9,
            0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1,
            0x1ad0, 0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa,
            0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2,
            0x2c83, 0x1ce0, 0x0cc1, 0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b,
            0xbfba, 0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93,
            0x3eb2, 0x0ed1, 0x1ef0 };

	private static byte[] chktbl = { (byte) 0x84, (byte) 0x47, (byte) 0x51, (byte) 0xc1,
			(byte) 0x93, (byte) 0x22, (byte) 0x21, (byte) 0x24, (byte) 0x2f,
			(byte) 0x66, (byte) 0x60, (byte) 0x4d, (byte) 0xb0, (byte) 0x7c,
			(byte) 0xda, (byte) 0x88, (byte) 0x54, (byte) 0x15, (byte) 0x2b,
			(byte) 0xc6, (byte) 0x6c, (byte) 0x89, (byte) 0xc5, (byte) 0x9d,
			(byte) 0x48, (byte) 0xee, (byte) 0xe6, (byte) 0x8a, (byte) 0xb5,
			(byte) 0xf4, (byte) 0xcb, (byte) 0xfb, (byte) 0xf1, (byte) 0x0c,
			(byte) 0x2e, (byte) 0xa0, (byte) 0xd7, (byte) 0xc9, (byte) 0x1f,
			(byte) 0xd6, (byte) 0x06, (byte) 0x9a, (byte) 0x09, (byte) 0x41,
			(byte) 0x54, (byte) 0x67, (byte) 0x46, (byte) 0xc7, (byte) 0x74,
			(byte) 0xe3, (byte) 0xc8, (byte) 0xb6, (byte) 0x5d, (byte) 0xa6,
			(byte) 0x36, (byte) 0xc4, (byte) 0xab, (byte) 0x2c, (byte) 0x7e,
			(byte) 0x85, (byte) 0xa8, (byte) 0xa4, (byte) 0xa6, (byte) 0x4d,
			(byte) 0x96, (byte) 0x19, (byte) 0x19, (byte) 0x9a, (byte) 0xcc,
			(byte) 0xd8, (byte) 0xac, (byte) 0x39, (byte) 0x5e, (byte) 0x3c,
			(byte) 0xf2, (byte) 0xf5, (byte) 0x5a, (byte) 0x72, (byte) 0xe5,
			(byte) 0xa9, (byte) 0xd1, (byte) 0xb3, (byte) 0x23, (byte) 0x82,
			(byte) 0x6f, (byte) 0x29, (byte) 0xcb, (byte) 0xd1, (byte) 0xcc,
			(byte) 0x71, (byte) 0xfb, (byte) 0xea, (byte) 0x92, (byte) 0xeb,
			(byte) 0x1c, (byte) 0xca, (byte) 0x4c, (byte) 0x70, (byte) 0xfe,
			(byte) 0x4d, (byte) 0xc9, (byte) 0x67, (byte) 0x43, (byte) 0x47,
			(byte) 0x94, (byte) 0xb9, (byte) 0x47, (byte) 0xbc, (byte) 0x3f,
			(byte) 0x01, (byte) 0xab, (byte) 0x7b, (byte) 0xa6, (byte) 0xe2,
			(byte) 0x76, (byte) 0xef, (byte) 0x5a, (byte) 0x7a, (byte) 0x29,
			(byte) 0x0b, (byte) 0x51, (byte) 0x54, (byte) 0x67, (byte) 0xd8,
			(byte) 0x1c, (byte) 0x14, (byte) 0x3e, (byte) 0x29, (byte) 0xec,
			(byte) 0xe9, (byte) 0x2d, (byte) 0x48, (byte) 0x67, (byte) 0xff,
			(byte) 0xed, (byte) 0x54, (byte) 0x4f, (byte) 0x48, (byte) 0xc0,
			(byte) 0xaa, (byte) 0x61, (byte) 0xf7, (byte) 0x78, (byte) 0x12,
			(byte) 0x03, (byte) 0x7a, (byte) 0x9e, (byte) 0x8b, (byte) 0xcf,
			(byte) 0x83, (byte) 0x7b, (byte) 0xae, (byte) 0xca, (byte) 0x7b,
			(byte) 0xd9, (byte) 0xe9, (byte) 0x53, (byte) 0x2a, (byte) 0xeb,
			(byte) 0xd2, (byte) 0xd8, (byte) 0xcd, (byte) 0xa3, (byte) 0x10,
			(byte) 0x25, (byte) 0x78, (byte) 0x5a, (byte) 0xb5, (byte) 0x23,
			(byte) 0x06, (byte) 0x93, (byte) 0xb7, (byte) 0x84, (byte) 0xd2,
			(byte) 0xbd, (byte) 0x96, (byte) 0x75, (byte) 0xa5, (byte) 0x5e,
			(byte) 0xcf, (byte) 0x4e, (byte) 0xe9, (byte) 0x50, (byte) 0xa1,
			(byte) 0xe6, (byte) 0x9d, (byte) 0xb1, (byte) 0xe3, (byte) 0x85,
			(byte) 0x66, (byte) 0x28, (byte) 0x4e, (byte) 0x43, (byte) 0xdc,
			(byte) 0x6e, (byte) 0xbb, (byte) 0x33, (byte) 0x9e, (byte) 0xf3,
			(byte) 0x0d, (byte) 0x00, (byte) 0xc1, (byte) 0xcf, (byte) 0x67,
			(byte) 0x34, (byte) 0x06, (byte) 0x7c, (byte) 0x71, (byte) 0xe3,
			(byte) 0x63, (byte) 0xb7, (byte) 0xb7, (byte) 0xdf, (byte) 0x92,
			(byte) 0xc4, (byte) 0xc2, (byte) 0x25, (byte) 0x5c, (byte) 0xff,
			(byte) 0xc3, (byte) 0x6e, (byte) 0xfc, (byte) 0xaa, (byte) 0x1e,
			(byte) 0x2a, (byte) 0x48, (byte) 0x11, (byte) 0x1c, (byte) 0x36,
			(byte) 0x68, (byte) 0x78, (byte) 0x86, (byte) 0x79, (byte) 0x30,
			(byte) 0xc3, (byte) 0xd6, (byte) 0xde, (byte) 0xbc, (byte) 0x3a,
			(byte) 0x2a, (byte) 0x6d, (byte) 0x1e, (byte) 0x46, (byte) 0xdd,
			(byte) 0xe0, (byte) 0x80, (byte) 0x1e, (byte) 0x44, (byte) 0x3b,
			(byte) 0x6f, (byte) 0xaf, (byte) 0x31, (byte) 0xda, (byte) 0xa2,
			(byte) 0xbd, (byte) 0x77, (byte) 0x06, (byte) 0x56, (byte) 0xc0,
			(byte) 0xb7, (byte) 0x92, (byte) 0x4b, (byte) 0x37, (byte) 0xc0,
			(byte) 0xfc, (byte) 0xc2, (byte) 0xd5, (byte) 0xfb, (byte) 0xa8,
			(byte) 0xda, (byte) 0xf5, (byte) 0x57, (byte) 0xa8, (byte) 0x18,
			(byte) 0xc0, (byte) 0xdf, (byte) 0xe7, (byte) 0xaa, (byte) 0x2a,
			(byte) 0xe0, (byte) 0x7c, (byte) 0x6f, (byte) 0x77, (byte) 0xb1,
			(byte) 0x26, (byte) 0xba, (byte) 0xf9, (byte) 0x2e, (byte) 0x1d,
			(byte) 0x16, (byte) 0xcb, (byte) 0xb8, (byte) 0xa2, (byte) 0x44,
			(byte) 0xd5, (byte) 0x2f, (byte) 0x1a, (byte) 0x79, (byte) 0x74,
			(byte) 0x87, (byte) 0x4b, (byte) 0x00, (byte) 0xc9, (byte) 0x4a,
			(byte) 0x3a, (byte) 0x65, (byte) 0x8f, (byte) 0xe6, (byte) 0x5d,
			(byte) 0xe5, (byte) 0x0a, (byte) 0x77, (byte) 0xd8, (byte) 0x1a,
			(byte) 0x14, (byte) 0x41, (byte) 0x75, (byte) 0xb1, (byte) 0xe2,
			(byte) 0x50, (byte) 0x2c, (byte) 0x93, (byte) 0x38, (byte) 0x2b,
			(byte) 0x6d, (byte) 0xf3, (byte) 0xf6, (byte) 0xdb, (byte) 0x1f,
			(byte) 0xcd, (byte) 0xff, (byte) 0x14, (byte) 0x70, (byte) 0xe7,
			(byte) 0x16, (byte) 0xe8, (byte) 0x3d, (byte) 0xf0, (byte) 0xe3,
			(byte) 0xbc, (byte) 0x5e, (byte) 0xb6, (byte) 0x3f, (byte) 0xcc,
			(byte) 0x81, (byte) 0x24, (byte) 0x67, (byte) 0xf3, (byte) 0x97,
			(byte) 0x3b, (byte) 0xfe, (byte) 0x3a, (byte) 0x96, (byte) 0x85,
			(byte) 0xdf, (byte) 0xe4, (byte) 0x6e, (byte) 0x3c, (byte) 0x85,
			(byte) 0x05, (byte) 0x0e, (byte) 0xa3, (byte) 0x2b, (byte) 0x07,
			(byte) 0xc8, (byte) 0xbf, (byte) 0xe5, (byte) 0x13, (byte) 0x82,
			(byte) 0x62, (byte) 0x08, (byte) 0x61, (byte) 0x69, (byte) 0x4b,
			(byte) 0x47, (byte) 0x62, (byte) 0x73, (byte) 0x44, (byte) 0x64,
			(byte) 0x8e, (byte) 0xe2, (byte) 0x91, (byte) 0xa6, (byte) 0x9a,
			(byte) 0xb7, (byte) 0xe9, (byte) 0x04, (byte) 0xb6, (byte) 0x54,
			(byte) 0x0c, (byte) 0xc5, (byte) 0xa9, (byte) 0x47, (byte) 0xa6,
			(byte) 0xc9, (byte) 0x08, (byte) 0xfe, (byte) 0x4e, (byte) 0xa6,
			(byte) 0xcc, (byte) 0x8a, (byte) 0x5b, (byte) 0x90, (byte) 0x6f,
			(byte) 0x2b, (byte) 0x3f, (byte) 0xb6, (byte) 0x0a, (byte) 0x96,
			(byte) 0xc0, (byte) 0x78, (byte) 0x58, (byte) 0x3c, (byte) 0x76,
			(byte) 0x6d, (byte) 0x94, (byte) 0x1a, (byte) 0xe4, (byte) 0x4e,
			(byte) 0xb8, (byte) 0x38, (byte) 0xbb, (byte) 0xf5, (byte) 0xeb,
			(byte) 0x29, (byte) 0xd8, (byte) 0xb0, (byte) 0xf3, (byte) 0x15,
			(byte) 0x1e, (byte) 0x99, (byte) 0x96, (byte) 0x3c, (byte) 0x5d,
			(byte) 0x63, (byte) 0xd5, (byte) 0xb1, (byte) 0xad, (byte) 0x52,
			(byte) 0xb8, (byte) 0x55, (byte) 0x70, (byte) 0x75, (byte) 0x3e,
			(byte) 0x1a, (byte) 0xd5, (byte) 0xda, (byte) 0xf6, (byte) 0x7a,
			(byte) 0x48, (byte) 0x7d, (byte) 0x44, (byte) 0x41, (byte) 0xf9,
			(byte) 0x11, (byte) 0xce, (byte) 0xd7, (byte) 0xca, (byte) 0xa5,
			(byte) 0x3d, (byte) 0x7a, (byte) 0x79, (byte) 0x7e, (byte) 0x7d,
			(byte) 0x25, (byte) 0x1b, (byte) 0x77, (byte) 0xbc, (byte) 0xf7,
			(byte) 0xc7, (byte) 0x0f, (byte) 0x84, (byte) 0x95, (byte) 0x10,
			(byte) 0x92, (byte) 0x67, (byte) 0x15, (byte) 0x11, (byte) 0x5a,
			(byte) 0x5e, (byte) 0x41, (byte) 0x66, (byte) 0x0f, (byte) 0x38,
			(byte) 0x03, (byte) 0xb2, (byte) 0xf1, (byte) 0x5d, (byte) 0xf8,
			(byte) 0xab, (byte) 0xc0, (byte) 0x02, (byte) 0x76, (byte) 0x84,
			(byte) 0x28, (byte) 0xf4, (byte) 0x9d, (byte) 0x56, (byte) 0x46,
			(byte) 0x60, (byte) 0x20, (byte) 0xdb, (byte) 0x68, (byte) 0xa7,
			(byte) 0xbb, (byte) 0xee, (byte) 0xac, (byte) 0x15, (byte) 0x01,
			(byte) 0x2f, (byte) 0x20, (byte) 0x09, (byte) 0xdb, (byte) 0xc0,
			(byte) 0x16, (byte) 0xa1, (byte) 0x89, (byte) 0xf9, (byte) 0x94,
			(byte) 0x59, (byte) 0x00, (byte) 0xc1, (byte) 0x76, (byte) 0xbf,
			(byte) 0xc1, (byte) 0x4d, (byte) 0x5d, (byte) 0x2d, (byte) 0xa9,
			(byte) 0x85, (byte) 0x2c, (byte) 0xd6, (byte) 0xd3, (byte) 0x14,
			(byte) 0xcc, (byte) 0x02, (byte) 0xc3, (byte) 0xc2, (byte) 0xfa,
			(byte) 0x6b, (byte) 0xb7, (byte) 0xa6, (byte) 0xef, (byte) 0xdd,
			(byte) 0x12, (byte) 0x26, (byte) 0xa4, (byte) 0x63, (byte) 0xe3,
			(byte) 0x62, (byte) 0xbd, (byte) 0x56, (byte) 0x8a, (byte) 0x52,
			(byte) 0x2b, (byte) 0xb9, (byte) 0xdf, (byte) 0x09, (byte) 0xbc,
			(byte) 0x0e, (byte) 0x97, (byte) 0xa9, (byte) 0xb0, (byte) 0x82,
			(byte) 0x46, (byte) 0x08, (byte) 0xd5, (byte) 0x1a, (byte) 0x8e,
			(byte) 0x1b, (byte) 0xa7, (byte) 0x90, (byte) 0x98, (byte) 0xb9,
			(byte) 0xbb, (byte) 0x3c, (byte) 0x17, (byte) 0x9a, (byte) 0xf2,
			(byte) 0x82, (byte) 0xba, (byte) 0x64, (byte) 0x0a, (byte) 0x7f,
			(byte) 0xca, (byte) 0x5a, (byte) 0x8c, (byte) 0x7c, (byte) 0xd3,
			(byte) 0x79, (byte) 0x09, (byte) 0x5b, (byte) 0x26, (byte) 0xbb,
			(byte) 0xbd, (byte) 0x25, (byte) 0xdf, (byte) 0x3d, (byte) 0x6f,
			(byte) 0x9a, (byte) 0x8f, (byte) 0xee, (byte) 0x21, (byte) 0x66,
			(byte) 0xb0, (byte) 0x8d, (byte) 0x84, (byte) 0x4c, (byte) 0x91,
			(byte) 0x45, (byte) 0xd4, (byte) 0x77, (byte) 0x4f, (byte) 0xb3,
			(byte) 0x8c, (byte) 0xbc, (byte) 0xa8, (byte) 0x99, (byte) 0xaa,
			(byte) 0x19, (byte) 0x53, (byte) 0x7c, (byte) 0x02, (byte) 0x87,
			(byte) 0xbb, (byte) 0x0b, (byte) 0x7c, (byte) 0x1a, (byte) 0x2d,
			(byte) 0xdf, (byte) 0x48, (byte) 0x44, (byte) 0x06, (byte) 0xd6,
			(byte) 0x7d, (byte) 0x0c, (byte) 0x2d, (byte) 0x35, (byte) 0x76,
			(byte) 0xae, (byte) 0xc4, (byte) 0x5f, (byte) 0x71, (byte) 0x85,
			(byte) 0x97, (byte) 0xc4, (byte) 0x3d, (byte) 0xef, (byte) 0x52,
			(byte) 0xbe, (byte) 0x00, (byte) 0xe4, (byte) 0xcd, (byte) 0x49,
			(byte) 0xd1, (byte) 0xd1, (byte) 0x1c, (byte) 0x3c, (byte) 0xd0,
			(byte) 0x1c, (byte) 0x42, (byte) 0xaf, (byte) 0xd4, (byte) 0xbd,
			(byte) 0x58, (byte) 0x34, (byte) 0x07, (byte) 0x32, (byte) 0xee,
			(byte) 0xb9, (byte) 0xb5, (byte) 0xea, (byte) 0xff, (byte) 0xd7,
			(byte) 0x8c, (byte) 0x0d, (byte) 0x2e, (byte) 0x2f, (byte) 0xaf,
			(byte) 0x87, (byte) 0xbb, (byte) 0xe6, (byte) 0x52, (byte) 0x71,
			(byte) 0x22, (byte) 0xf5, (byte) 0x25, (byte) 0x17, (byte) 0xa1,
			(byte) 0x82, (byte) 0x04, (byte) 0xc2, (byte) 0x4a, (byte) 0xbd,
			(byte) 0x57, (byte) 0xc6, (byte) 0xab, (byte) 0xc8, (byte) 0x35,
			(byte) 0x0c, (byte) 0x3c, (byte) 0xd9, (byte) 0xc2, (byte) 0x43,
			(byte) 0xdb, (byte) 0x27, (byte) 0x92, (byte) 0xcf, (byte) 0xb8,
			(byte) 0x25, (byte) 0x60, (byte) 0xfa, (byte) 0x21, (byte) 0x3b,
			(byte) 0x04, (byte) 0x52, (byte) 0xc8, (byte) 0x96, (byte) 0xba,
			(byte) 0x74, (byte) 0xe3, (byte) 0x67, (byte) 0x3e, (byte) 0x8e,
			(byte) 0x8d, (byte) 0x61, (byte) 0x90, (byte) 0x92, (byte) 0x59,
			(byte) 0xb6, (byte) 0x1a, (byte) 0x1c, (byte) 0x5e, (byte) 0x21,
			(byte) 0xc1, (byte) 0x65, (byte) 0xe5, (byte) 0xa6, (byte) 0x34,
			(byte) 0x05, (byte) 0x6f, (byte) 0xc5, (byte) 0x60, (byte) 0xb1,
			(byte) 0x83, (byte) 0xc1, (byte) 0xd5, (byte) 0xd5, (byte) 0xed,
			(byte) 0xd9, (byte) 0xc7, (byte) 0x11, (byte) 0x7b, (byte) 0x49,
			(byte) 0x7a, (byte) 0xf9, (byte) 0xf9, (byte) 0x84, (byte) 0x47,
			(byte) 0x9b, (byte) 0xe2, (byte) 0xa5, (byte) 0x82, (byte) 0xe0,
			(byte) 0xc2, (byte) 0x88, (byte) 0xd0, (byte) 0xb2, (byte) 0x58,
			(byte) 0x88, (byte) 0x7f, (byte) 0x45, (byte) 0x09, (byte) 0x67,
			(byte) 0x74, (byte) 0x61, (byte) 0xbf, (byte) 0xe6, (byte) 0x40,
			(byte) 0xe2, (byte) 0x9d, (byte) 0xc2, (byte) 0x47, (byte) 0x05,
			(byte) 0x89, (byte) 0xed, (byte) 0xcb, (byte) 0xbb, (byte) 0xb7,
			(byte) 0x27, (byte) 0xe7, (byte) 0xdc, (byte) 0x7a, (byte) 0xfd,
			(byte) 0xbf, (byte) 0xa8, (byte) 0xd0, (byte) 0xaa, (byte) 0x10,
			(byte) 0x39, (byte) 0x3c, (byte) 0x20, (byte) 0xf0, (byte) 0xd3,
			(byte) 0x6e, (byte) 0xb1, (byte) 0x72, (byte) 0xf8, (byte) 0xe6,
			(byte) 0x0f, (byte) 0xef, (byte) 0x37, (byte) 0xe5, (byte) 0x09,
			(byte) 0x33, (byte) 0x5a, (byte) 0x83, (byte) 0x43, (byte) 0x80,
			(byte) 0x4f, (byte) 0x65, (byte) 0x2f, (byte) 0x7c, (byte) 0x8c,
			(byte) 0x6a, (byte) 0xa0, (byte) 0x82, (byte) 0x0c, (byte) 0xd4,
			(byte) 0xd4, (byte) 0xfa, (byte) 0x81, (byte) 0x60, (byte) 0x3d,
			(byte) 0xdf, (byte) 0x06, (byte) 0xf1, (byte) 0x5f, (byte) 0x08,
			(byte) 0x0d, (byte) 0x6d, (byte) 0x43, (byte) 0xf2, (byte) 0xe3,
			(byte) 0x11, (byte) 0x7d, (byte) 0x80, (byte) 0x32, (byte) 0xc5,
			(byte) 0xfb, (byte) 0xc5, (byte) 0xd9, (byte) 0x27, (byte) 0xec,
			(byte) 0xc6, (byte) 0x4e, (byte) 0x65, (byte) 0x27, (byte) 0x76,
			(byte) 0x87, (byte) 0xa6, (byte) 0xee, (byte) 0xee, (byte) 0xd7,
			(byte) 0x8b, (byte) 0xd1, (byte) 0xa0, (byte) 0x5c, (byte) 0xb0,
			(byte) 0x42, (byte) 0x13, (byte) 0x0e, (byte) 0x95, (byte) 0x4a,
			(byte) 0xf2, (byte) 0x06, (byte) 0xc6, (byte) 0x43, (byte) 0x33,
			(byte) 0xf4, (byte) 0xc7, (byte) 0xf8, (byte) 0xe7, (byte) 0x1f,
			(byte) 0xdd, (byte) 0xe4, (byte) 0x46, (byte) 0x4a, (byte) 0x70,
			(byte) 0x39, (byte) 0x6c, (byte) 0xd0, (byte) 0xed, (byte) 0xca,
			(byte) 0xbe, (byte) 0x60, (byte) 0x3b, (byte) 0xd1, (byte) 0x7b,
			(byte) 0x57, (byte) 0x48, (byte) 0xe5, (byte) 0x3a, (byte) 0x79,
			(byte) 0xc1, (byte) 0x69, (byte) 0x33, (byte) 0x53, (byte) 0x1b,
			(byte) 0x80, (byte) 0xb8, (byte) 0x91, (byte) 0x7d, (byte) 0xb4,
			(byte) 0xf6, (byte) 0x17, (byte) 0x1a, (byte) 0x1d, (byte) 0x5a,
			(byte) 0x32, (byte) 0xd6, (byte) 0xcc, (byte) 0x71, (byte) 0x29,
			(byte) 0x3f, (byte) 0x28, (byte) 0xbb, (byte) 0xf3, (byte) 0x5e,
			(byte) 0x71, (byte) 0xb8, (byte) 0x43, (byte) 0xaf, (byte) 0xf8,
			(byte) 0xb9, (byte) 0x64, (byte) 0xef, (byte) 0xc4, (byte) 0xa5,
			(byte) 0x6c, (byte) 0x08, (byte) 0x53, (byte) 0xc7, (byte) 0x00,
			(byte) 0x10, (byte) 0x39, (byte) 0x4f, (byte) 0xdd, (byte) 0xe4,
			(byte) 0xb6, (byte) 0x19, (byte) 0x27, (byte) 0xfb, (byte) 0xb8,
			(byte) 0xf5, (byte) 0x32, (byte) 0x73, (byte) 0xe5, (byte) 0xcb,
			(byte) 0x32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0 };

	static int CRC_Block(byte start[], int count)
	{
		short crc= CRC_INIT_VALUE;

		int ndx= 0;

		while (count-- > 0)
			crc= (short) ((crc << 8) ^ crctable[0xff & ((crc >> 8) ^ start[ndx++])]);

		// unsigned short
		return crc & 0xFFFF;
	}

	/**
	 * Calculates a crc checksum-sequence over an array.
	 */
	static byte BlockSequenceCRCByte(byte base[], int offset, int length, int sequence) {
		byte[] chkb = new byte [60 + 4];

		if (sequence < 0)
			Sys.Error("sequence < 0, this shouldn't happen\n");

		//p_ndx = (sequence % (sizeof(chktbl) - 4));
		int p_ndx = (sequence % (1024 - 4));

		//memcpy(chkb, base, length);
		length = Math.min(60, length);
		System.arraycopy(base, offset , chkb, 0, length);

		chkb[length] = chktbl[p_ndx + 0];
		chkb[length + 1] = chktbl[p_ndx + 1];
		chkb[length + 2] = chktbl[p_ndx + 2];
		chkb[length + 3] = chktbl[p_ndx + 3];

		length += 4;

		// unsigned short
		int crc = CRC_Block(chkb, length);

		int x = 0;
		for (int n=0; n < length; n++)
			x += chkb[n] & 0xFF;

		crc ^= x;

		return (byte)(crc & 0xFF);
	}
}
