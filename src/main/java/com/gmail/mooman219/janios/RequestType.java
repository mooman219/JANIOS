package com.gmail.mooman219.janios;

/**
 * @author Joseph Cumbo (mooman219)
 */
public enum RequestType {

    UNKNOWN,
    CONNECT,
    DELETE,
    GET {
                @Override
                public boolean hasTerminated(byte[] builder, int limit) {
                    return limit > 1
                            ? builder[limit - 1] == 0x0A
                            && builder[limit - 2] == 0x0D
                            && builder[limit - 3] == 0x0A
                            && builder[limit - 4] == 0x0D
                            : false;
                }
            },
    HEAD,
    OPTIONS,
    PATCH,
    POST,
    PUT,
    TRACE;

    public boolean hasTerminated(byte[] builder, int limit) {
        throw new UnsupportedOperationException("RequestType" + this.name() + " does not implement this method.");
    }

    public static RequestType getRequestType(byte[] builder, int limit) {
        if (limit < 1) {
            return UNKNOWN;
        }
        switch (builder[0]) {
            case 0x43: // ASCII C
                return CONNECT;
            case 0x44: // ASCII D
                return DELETE;
            case 0x47: // ASCII G
                return GET;
            case 0x48: // ASCII H
                return HEAD;
            case 0x4F: // ASCII O
                return OPTIONS;
            case 0x50: // ASCII P
                if (limit >= 2) {
                    switch (builder[1]) {
                        case 0x41: // ASCII A
                            return PATCH;
                        case 0x4F: // ASCII O
                            return POST;
                        case 0x55: // ASCII U
                            return PUT;
                    }
                }
                return UNKNOWN;
            case 0x54: // ASCII T
                return TRACE;
            default:
                return UNKNOWN;
        }
    }
}
