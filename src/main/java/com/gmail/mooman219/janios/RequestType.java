package com.gmail.mooman219.janios;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

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
        byte[] ident;
        /**
         * Since the HTML standard is ASCII, we really need to be able support
         * that character set on this machine.
         */
        try {
            if (Charset.isSupported("ASCII")) {
                ident = identifier.getBytes("ASCII");
            } else {
                ident = identifier.getBytes("US-ASCII");
            }
        } catch (UnsupportedEncodingException ex) {
            throw new Error("System unable to encode to ASCII.");
        }
        this.identifier = ident;
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
     * @param base the base array to scan
     * @param baseLimit the length of usable data in the base array
     * @return the request type present in the base array. If one is is not
     * present, ERRONEOUS is returned. If there isn't enough data in the base
     * array to check, INCOMPLETE is returned.
     */
    public static RequestType getRequestType(byte[] base, int baseLimit) {
        if (baseLimit < 3) {
            return INCOMPLETE;
        }
        switch (base[0]) {
            case 0x43: // ASCII C
                return getStatus(base, baseLimit, CONNECT);
            case 0x44: // ASCII D
                return getStatus(base, baseLimit, DELETE);
            case 0x47: // ASCII G
                return getStatus(base, baseLimit, GET);
            case 0x48: // ASCII H
                return getStatus(base, baseLimit, HEAD);
            case 0x4F: // ASCII O
                return getStatus(base, baseLimit, OPTIONS);
            case 0x50: // ASCII P
                switch (base[1]) {
                    case 0x41: // ASCII A
                        return getStatus(base, baseLimit, PATCH);
                    case 0x4F: // ASCII O
                        return getStatus(base, baseLimit, POST);
                    case 0x55: // ASCII U
                        return getStatus(base, baseLimit, PUT);
                    default:
                        return ERRONEOUS;
                }
            case 0x54: // ASCII T
                return getStatus(base, baseLimit, TRACE);
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
    private static RequestType getStatus(byte[] base, int baseLimit, RequestType type) {
        if (baseLimit >= type.identifier.length) {
            for (int i = 0; i < type.identifier.length; i++) {
                if (base[i] != type.identifier[i]) {
                    return ERRONEOUS;
                }
            }
            return type;
        } else {
            return INCOMPLETE;
        }
    }
}
