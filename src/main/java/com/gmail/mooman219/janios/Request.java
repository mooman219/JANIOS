package com.gmail.mooman219.janios;

import java.nio.ByteBuffer;

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

    public static Request parse(ByteBuffer buffer) {
        /**
         * Parse the request type
         */
        RequestType requestType = RequestType.getRequestType(buffer);
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

        if (buffer.limit() < position) {
            return INCOMPLETE_REQUEST;
        } else if (buffer.get(position) != 0x20) { // ASCII Space
            return ERRONEOUS_REQUEST;
        }
        position = start;
        for (; position < buffer.limit(); position++) {
            if (buffer.get(position) == 0x20) { // ASCII Space
                end = position;
                break;
            } else if (buffer.get(position) < 0x020) { // Unsupported characters
                return ERRONEOUS_REQUEST;
            }
        }
        if (end == 0) {
            return INCOMPLETE_REQUEST;
        }
        String requestURL = BufferHelper.toString(buffer, start, end - start);

        /**
         * Parse the http version.
         */
        start = ++position;
        end = 0;
        for (; position < buffer.limit(); position++) {
            if (buffer.get(position) == 0x0D || buffer.get(position) == 0x0A) { // ASCII New Line or ASCII Carriage Return
                end = position;
                break;
            } else if (buffer.get(position) < 0x021) { // Unsupported characters
                return ERRONEOUS_REQUEST;
            }
        }
        if (end == 0) {
            return INCOMPLETE_REQUEST;
        }
        String httpVersion = BufferHelper.toString(buffer, start, end - start);

        return new Request(requestType, requestURL, httpVersion);
    }
}
