package com.gmail.mooman219.janios;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Joseph Cumbo (mooman219)
 */
public final class Client {

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

    public boolean isClosed() {
        return closed;
    }

    /**
     * Appends the data in the given buffer to the client's internal buffer. The
     * data is then parsed.
     *
     * @param read the data to append
     * @return true if there were errors when parsing the data, false otherwise.
     * If there were errors, the clients internal buffer is cleared.
     * @throws IllegalStateException if the client is closed
     */
    public boolean read(ByteBuffer read) {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        if (read.limit() > buffer.remaining()) {
            System.out.println("Overflow");
            buffer.clear();
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

    /**
     * Writes anything on the client's queue to the socket.
     *
     * @return true if all data on the client's queue was written, false if
     * there's still pending data to be written
     * @throws IOException if some other I/O error occurs
     * @throws IllegalStateException if the client is closed
     */
    public boolean write() throws IOException {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        ByteBuffer buf;
        while ((buf = queue.peek()) != null) {
            socket.write(buf);
            if (!buf.hasRemaining()) {
                queue.poll();
            } else {
                return false;
            }
        }
        return true;
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
