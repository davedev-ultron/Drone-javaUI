package com.odafa.cloudapp.utils;

import java.io.EOFException;
import java.io.InputStream;

public class NetworkFormatter {

    public static byte[] createNetworkMessage(byte[] msgBody) {
        // messages will have header that indicates number of bytes of the body
        // then we know when weve read everything
		byte[] head = new byte[4];
		int size = msgBody.length;

        // integer in java is represented by 4 bytes - 32 bits
        // here we go through all 4 bytes that our integer consists
        // and we are masking those eahc of those bytes
        // which means that we read the  single bytes value into the head
        // then we use right shift bitwise operator to shift 8 bits and read next byte
        // and we do that over and over again
        for (int i = head.length - 1; i >= 0; i--) {
			head[i] = (byte) (size & 0xff);
			size >>>= 8;
		}

        // this array contains the head and body
		byte[] result = new byte[head.length + msgBody.length];

        // now head contains encoded into 4 bytes size of message body
		for (int i = 0; i < head.length; i++) {
			result[i] = head[i];
		}
        // from head length to message body length and we write the bytes
		for (int i = head.length, j = 0; j < msgBody.length; i++, j++) {
			result[i] = msgBody[j];
		}

        // results contains 4 bytes for head and rest of bytes of message
        return result;
    }

    public static byte[] readNetworkMessage(InputStream in) throws Exception {
        // when reading from network we will read first 4 bytes and transform them into the
        // decimal value of the body size
        // and we will ready byte by byte until read the whole message
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
        // in these 4 bytes is encoded the size of the body that follows it
		if ((ch1 | ch2 | ch3 | ch4) < 0) {
			throw new EOFException();
		}

        // convert to decimal
		int msgSize = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

        // array that we will save data to
		final byte[] buffer = new byte[msgSize];
		int totalReadSize = 0;

        // once total read size is = msg size then it will exit the loop and return
        while (totalReadSize < msgSize) {
            // read byte and record into buffer
            // returns how many it read
            // if it did not read all then it will try to read it again
			int readSize = in.read(buffer, totalReadSize, msgSize - totalReadSize);
			if (readSize < 0) {
				throw new EOFException();
			}
			totalReadSize += readSize;
		}
		return buffer;
    }
}