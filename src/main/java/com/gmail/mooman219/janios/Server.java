package com.gmail.mooman219.janios;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Server {

    public static final Charset ASCII;
    public static final int BUFFER_SIZE = 3 * 1024;
    public static final BufferPool BUFFER_POOL = new BufferPool(BUFFER_SIZE, 1024);

    static {
        Map<String, Charset> charsets = Charset.availableCharsets();
        if (charsets.containsKey("US-ASCII")) {
            ASCII = charsets.get("US-ASCII");
        } else if (charsets.containsKey("ASCII")) {
            ASCII = charsets.get("ASCII");
        } else {
            throw new Error("System unable to encode to ASCII.");
        }
        System.out.println("Found charset " + ASCII.displayName());
        System.out.println("Buffer size: " + BUFFER_SIZE);
        System.out.println("Buffer pool capacity: " + BUFFER_POOL.getCapacity());
    }

    private static final ConcurrentHashMap<SocketChannel, Client> clients = new ConcurrentHashMap<>();
    private static final ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

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
        Client client = clients.get(socket);
        if (read == -1) {
            System.out.println("Lost connection to " + socket.getRemoteAddress().toString());
            client.close();
            clients.remove(socket);
            return;
        }

        readBuffer.flip();
        if (clients.get(socket).process(readBuffer)) {
            System.out.println("Closing erronous connection to " + socket.getRemoteAddress().toString());
            client.close();
            clients.remove(socket);
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
        return ("HTTP/1.0 " + responceType.getStatus() + "\r\n"
                + "Content-Type: text/html\r\n"
                + "Server: JANIOS\r\n\r\n"
                + page).getBytes(ASCII);
    }
}
