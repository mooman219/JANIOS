package com.gmail.mooman219.janios;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Client {

    private final ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private final SocketChannel socket;
    private ByteBuffer buffer;
    private boolean closed = false;

    public Client(SocketChannel socket) {
        this.buffer = Server.BUFFER_POOL.getByteBuffer();
        this.socket = socket;
    }

    public ConcurrentLinkedQueue<ByteBuffer> getQueue() {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        return queue;
    }

    public ByteBuffer getBuffer() {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        return buffer;
    }

    public boolean process(ByteBuffer read) {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        if (read.limit() > buffer.remaining()) {
            System.out.println("Overflow");
            return true;
        }
        buffer.put(read);
        Request request = Request.parse(buffer.array(), buffer.position());
        System.out.print("\n" + request.toString() + "\n");
        switch (request.getRequestType()) {
            case INCOMPLETE:
                System.out.println("Incomplete data");
                break;
            case ERRONEOUS:
                System.out.println("Erroneous data");
                buffer.clear();
                return true;
            default:
                System.out.println(new String(buffer.array(), 0, buffer.position()));
        }
        return false;
    }

    public void close() throws IOException {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        closed = true;
        Server.BUFFER_POOL.returnByteBuffer(buffer);
        buffer = null;
        socket.close();
    }
}
