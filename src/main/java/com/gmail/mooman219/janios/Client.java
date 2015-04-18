package com.gmail.mooman219.janios;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * @author Joseph Cumbo (mooman219)
 */
public final class Client {

    private final LinkedList<ByteBuffer> queue = new LinkedList<>();
    private final ByteBuffer buffer = ByteBuffer.allocate(Server.BUFFER_SIZE);
    private final ClientPool pool;
    private SocketChannel socket;
    private boolean closed = false;

    protected Client(ClientPool pool) {
        this.pool = pool;
    }

    /**
     * Resets the client state to that of a new client.
     *
     * @param socket the socket to assign the new client
     */
    protected void redeem(SocketChannel socket) {
        this.socket = socket;
        this.closed = false;
    }

    /**
     * Checks if the client has been closed.
     *
     * @return true if the client has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    public void queue(ByteBuffer buffer) {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        queue.add(buffer);
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
        if (read.remaining() > buffer.remaining()) {
            System.out.println("Overflow");
            return true;
        }
        buffer.put(read);
        buffer.flip();
        Request request = Request.parse(buffer);
        System.out.print("\n" + request.toString() + "\n");
        switch (request.getRequestType()) {
            case INCOMPLETE:
                System.out.println("Incomplete data");
                break;
            case ERRONEOUS:
                System.out.println("Erroneous data");
                return true;
            default:
                System.out.println(BufferHelper.toString(buffer, 0, buffer.limit()));
        }
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
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

    /**
     * Closes the underlying socket and nullifies the state of the client. The
     * client should not be used or held reference to after close has been
     * called as it can be reused by another connection.
     */
    public void close() {
        if (closed) {
            throw new IllegalStateException("Client closed");
        }
        closed = true;
        buffer.clear();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = null;
        pool.redeem(this);
    }
}
