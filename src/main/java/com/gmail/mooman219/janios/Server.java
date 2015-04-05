package com.gmail.mooman219.janios;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Server {

    private static final int SIZE = 3 * 1024;
    private static final ConcurrentHashMap<SocketChannel, Client> clients = new ConcurrentHashMap<>();
    private static final ByteBuffer readBuffer = ByteBuffer.allocateDirect(SIZE);

    public static class Client {

        private final ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
        private final ByteBuffer buffer = ByteBuffer.allocate(SIZE);

        public ConcurrentLinkedQueue<ByteBuffer> getQueue() {
            return queue;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public boolean process(ByteBuffer read) {
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
        clients.put(sc, new Client());
        System.out.println("Connected to " + sc.getRemoteAddress().toString());
    }

    public static void read(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        readBuffer.clear();
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
        if (clients.get(socket).process(readBuffer)) {
            System.out.println("Closing erronous connection to " + socket.getRemoteAddress().toString());
            clients.remove(socket);
            socket.close();
            return;
        }

//        byte[] message = generateResponse(ResponseType.OK, "<!DOCTYPE html>\n"
//                + "<html>\n"
//                + "<body>\n"
//                + "<form action=\".\\\" method=\"POST\">\n"
//                + "First name:<br>\n"
//                + "<input type=\"text\" name=\"firstname\" value=\"Mickey\">\n"
//                + "<br>\n"
//                + "Last name:<br>\n"
//                + "<input type=\"text\" name=\"lastname\" value=\"Mouse\">\n"
//                + "<br><br>\n"
//                + "<input type=\"submit\" value=\"Submit\">\n"
//                + "</form> \n"
//                + "</body>\n"
//                + "</html>");
//        ByteBuffer writeBuffer = ByteBuffer.allocate(message.length);
//        writeBuffer.put(message);
//        writeBuffer.flip();
//        clients.get(socket).getQueue().add(writeBuffer);
//        socket.register(key.selector(), SelectionKey.OP_WRITE);
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

    /**
     * Searches the base array for an occurrence of the term array. Performance
     * is okayish, it tries to jump as far as it can after a mismatch.
     *
     * @param base the array to search in
     * @param baseOffset the offset to start searching in the base array
     * @param baseLimit the limit to search to in the base array
     * @param term the array to search for
     * @return the index of where the term array begins, -1 if the term array
     * could not be found in the base array
     */
    public static int indexOf(byte[] base, int baseOffset, int baseLimit, byte[] term) {
        int f;
        for (int i = baseOffset; i < baseLimit; i++) {
            f = 0;
            for (int j = 0; j < term.length; j++) {
                if (base[j + i] == term[j]) { // Match?
                    if (j == term.length - 1) { // Finished searching?
                        return i;
                    } else if (f == 0 && term[0] == term[j]) {
                        f = j;
                    }
                } else { // Missmatch
                    if (j > 0) {
                        if (f > 0) {
                            i += f - 1;
                            break;
                        }
                        i += j - 1;
                    }
                    break;
                }
            }
        }
        return -1;
    }
}
