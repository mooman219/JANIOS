package com.gmail.mooman219.janios;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class BufferHelper {

    /**
     * Prints the given buffer in hex format from the 0 position to the limit.
     *
     * @param buffer the buffer to print
     */
    public static void printBuffer(ByteBuffer buffer) {
        for (int i = 0; i < buffer.limit(); i++) {
            System.out.format("%x ", buffer.get(i));
        }
    }

    /**
     * Constructs a new String by decoding the specified subarray of bytes using
     * the ASCII charset.
     *
     * @param buffer the buffer to be decoded into characters offset
     * @param offset the index of the first byte to decode length
     * @param length the number of bytes to decode
     * @return a string representing the region defined by the given start and
     * length
     */
    public static String toString(ByteBuffer buffer, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > buffer.limit()) {
            throw new IndexOutOfBoundsException();
        }
        int position = buffer.position();
        int limit = buffer.limit();
        buffer.position(offset);
        buffer.limit(offset + length);
        String result = StandardCharsets.US_ASCII.decode(buffer).toString();
        buffer.position(position);
        buffer.limit(limit);
        return result;
    }

    public static ByteBuffer generateResponse(ResponseType responceType, String page) {
        return ByteBuffer.wrap(("HTTP/1.0 " + responceType.getStatus() + "\r\n"
                + "Content-Type: text/html\r\n"
                + "Server: JANIOS\r\n\r\n"
                + page).getBytes(StandardCharsets.US_ASCII));
    }
}
