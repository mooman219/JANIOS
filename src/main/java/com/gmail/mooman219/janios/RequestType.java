package com.gmail.mooman219.janios;

import java.nio.ByteBuffer;

/**
 * @author Joseph Cumbo (mooman219)
 */
public enum RequestType {

    CONNECT("CONNECT"),
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    PATCH("PATCH"),
    POST("POST"),
    PUT("PUT"),
    TRACE("TRACE"),
    INCOMPLETE("*"),
    ERRONEOUS("*");

    private final byte[] identifier;

    private RequestType(String identifier) {
        this.identifier = identifier.getBytes(Server.ASCII);
    }

    /**
     * Gets the length of the request type's identifier.
     *
     * @return the length of the request type's identifier
     */
    public int length() {
        return identifier.length;
    }

    /**
     * Parses the base array for a request type if one is present.
     *
     * @param buffer the buffer to scan
     * @return the request type present in the base array. If one is is not
     * present, ERRONEOUS is returned. If there isn't enough data in the base
     * array to check, INCOMPLETE is returned.
     */
    public static RequestType getRequestType(ByteBuffer buffer) {
        if (buffer.limit() < 3) {
            return INCOMPLETE;
        }
        switch (buffer.get(0)) {
            case 0x43: // ASCII C
                return getStatus(buffer, CONNECT);
            case 0x44: // ASCII D
                return getStatus(buffer, DELETE);
            case 0x47: // ASCII G
                return getStatus(buffer, GET);
            case 0x48: // ASCII H
                return getStatus(buffer, HEAD);
            case 0x4F: // ASCII O
                return getStatus(buffer, OPTIONS);
            case 0x50: // ASCII P
                switch (buffer.get(1)) {
                    case 0x41: // ASCII A
                        return getStatus(buffer, PATCH);
                    case 0x4F: // ASCII O
                        return getStatus(buffer, POST);
                    case 0x55: // ASCII U
                        return getStatus(buffer, PUT);
                    default:
                        return ERRONEOUS;
                }
            case 0x54: // ASCII T
                return getStatus(buffer, TRACE);
            default:
                return ERRONEOUS;
        }
    }

    /**
     * Checks if the given type is in the front of the base.
     *
     * @param base the array of data
     * @param baseLimit the length of usable data in the base array
     * @param type the type to check for
     * @return if there isn't enough information then INCOMPLETE is returned,
     * else if the information doesn't match that of what's expected for the
     * given type then ERRONEOUS is returned. If there are no issues, then the
     * given type is returned
     */
    private static RequestType getStatus(ByteBuffer buffer, RequestType type) {
        if (buffer.limit() >= type.identifier.length) {
            for (int i = 0; i < type.identifier.length; i++) {
                if (buffer.get(i) != type.identifier[i]) {
                    return ERRONEOUS;
                }
            }
            return type;
        } else {
            return INCOMPLETE;
        }
    }
}
