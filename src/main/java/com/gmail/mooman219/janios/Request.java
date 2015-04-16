package com.gmail.mooman219.janios;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Request {

    private final static Request INCOMPLETE_REQUEST = new Request(RequestType.INCOMPLETE, null, null);
    private final static Request ERRONEOUS_REQUEST = new Request(RequestType.ERRONEOUS, null, null);
    private final RequestType requestType;
    private final String requestURL;
    private final String httpVersion;

    private Request(RequestType requestType, String requestURL, String httpVersion) {
        this.requestType = requestType;
        this.requestURL = requestURL;
        this.httpVersion = httpVersion;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    @Override
    public String toString() {
        return "(Request: \"" + requestType.name() + "\", \"" + (requestURL == null ? "*" : requestURL) + "\", \"" + (httpVersion == null ? "*" : httpVersion) + "\")";
    }

    public static Request parse(byte[] base, int baseLimit) {
        /**
         * Parse the request type
         */
        RequestType requestType = RequestType.getRequestType(base, baseLimit);
        switch (requestType) {
            case INCOMPLETE:
                return INCOMPLETE_REQUEST;
            case ERRONEOUS:
                return ERRONEOUS_REQUEST;
        }

        /**
         * Parse the request url
         */
        int position = requestType.length();
        int start = position + 1;
        int end = 0;

        if (baseLimit < position) {
            return INCOMPLETE_REQUEST;
        } else if (base[position] != 0x20) { // ASCII Space
            return ERRONEOUS_REQUEST;
        }
        position = start;
        for (; position < baseLimit; position++) {
            if (base[position] == 0x20) { // ASCII Space
                end = position;
                break;
            } else if (base[position] < 0x020) { // Unsupported characters
                return ERRONEOUS_REQUEST;
            }
        }
        if (end == 0) {
            return INCOMPLETE_REQUEST;
        }
        String requestURL = new String(base, start, end - start);

        /**
         * Parse the http version.
         */
        start = ++position;
        end = 0;
        for (; position < baseLimit; position++) {
            if (base[position] == 0x0D || base[position] == 0x0A) { // ASCII New Line or ASCII Carriage Return
                end = position;
                break;
            } else if (base[position] < 0x021) { // Unsupported characters
                return ERRONEOUS_REQUEST;
            }
        }
        if (end == 0) {
            return INCOMPLETE_REQUEST;
        }
        String httpVersion = new String(base, start, end - start);

        return new Request(requestType, requestURL, httpVersion);
    }
}
