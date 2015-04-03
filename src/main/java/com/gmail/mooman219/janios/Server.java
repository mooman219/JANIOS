package com.gmail.mooman219.janios;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Server {

    /*  */
    private static final int SIZE = 2 * 1024;
    private static final ConcurrentHashMap<SocketChannel, Client> clients = new ConcurrentHashMap<>();
    private static final ByteBuffer readBuffer = ByteBuffer.allocateDirect(SIZE);

    public static class Client {

        private final SocketChannel socket;
        private final ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
        private final ByteBuffer buffer = ByteBuffer.allocate(SIZE);

        public Client(SocketChannel socket) {
            this.socket = socket;
        }

        public ConcurrentLinkedQueue<ByteBuffer> getQueue() {
            return queue;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public boolean process(ByteBuffer read) throws IOException {
            if (read.limit() > buffer.remaining()) {
                socket.close();
                buffer.clear();
                return true;
            }
            buffer.put(read);
            RequestType request = RequestType.getRequestType(buffer.array(), buffer.position());
            System.out.print("\n[" + request.name() + " | " + request.hasTerminated(buffer.array(), buffer.position()) + "]\n");
            System.out.println(new String(buffer.array(), 0, buffer.position()));
            if (request.hasTerminated(buffer.array(), buffer.position())) {
                buffer.clear();
            }
            return false;
        }
    }

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

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress("localhost", 8081));
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select();
            for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext();) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        }
    }

    public static void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ);
        clients.put(sc, new Client(sc));
        System.out.println("Connected to " + sc.getRemoteAddress().toString());
    }

    public static void read(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        int read = -1;
        try {
            read = socket.read(readBuffer);
        } catch (IOException e) {
            /**
             * If there was an error, read will be -1 and we'll close the
             * connection anyway.
             */
        }
        if (read == -1) {
            System.out.println("Lost connection to " + socket.getRemoteAddress().toString());
            clients.remove(socket);
            socket.close();
            return;
        }

        readBuffer.flip();
        clients.get(socket).process(readBuffer);
        readBuffer.clear();

        byte[] message = generateResponse(ResponseType.OK, "<h1>JANIOS welcomes " + socket.getRemoteAddress().toString() + " at this hour of " + Calendar.getInstance().get(Calendar.HOUR) + "</h1>");
        ByteBuffer writeBuffer = ByteBuffer.allocate(message.length);
        writeBuffer.put(message);
        writeBuffer.flip();
        clients.get(socket).getQueue().add(writeBuffer);
        socket.register(key.selector(), SelectionKey.OP_WRITE);
    }

    public static void write(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        Queue<ByteBuffer> queue = clients.get(socket).getQueue();
        ByteBuffer buf;
        while ((buf = queue.peek()) != null) {
            socket.write(buf);
            if (!buf.hasRemaining()) {
                queue.poll();
            } else {
                return;
            }
        }
        System.out.println("Closed connection to " + socket.getRemoteAddress().toString());
        socket.close();
        clients.remove(socket);
    }

    public static byte[] generateResponse(ResponseType responceType, String page) {
        return ("HTTP/1.0 " + responceType.getStatus() + "\n"
                + "Content-Type: text/html\n"
                + "Server: JANIOS\n\n"
                + page).getBytes();
    }
}
