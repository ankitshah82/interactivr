package com.example.ankit2.controllerapp1;

/**
 * Created by Ankit on 2/12/2017.
 */

class PacketData {

    static byte SESSION_START_HEADER = 0x0A;
    static byte GESTURE_PACKET_HEADER = 0x0B;
    static byte SESSION_END_HEADER = 0x0C;

    static byte PACKET_FOOTER = 0x7F;
    static byte PROTOCOL_VERSION = 0x01;

    static byte GESTURE_TYPE_FLICK = 0x01;
    static byte GESTURE_TYPE_SWIPE = 0x02;
    static byte GESTURE_TYPE_TAP = 0x03;

    static byte PADDING_BYTE = 0x00;

}
