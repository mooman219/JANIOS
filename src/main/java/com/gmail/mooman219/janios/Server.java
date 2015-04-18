package com.gmail.mooman219.janios;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Server {

    public static final String ADDRESS = "localhost";
    public static final int PORT = 8081;
    public static final Charset ASCII = StandardCharsets.US_ASCII;
    public static final int BUFFER_SIZE = 3 * 1024;

    static {
        System.out.println("Found charset " + ASCII.displayName());
        System.out.println("Buffer size: " + BUFFER_SIZE);
        System.out.println("Server address: " + ADDRESS + ":" + PORT);
    }

    private final ConcurrentHashMap<SocketChannel, Client> clients = new ConcurrentHashMap<>();
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final InetSocketAddress address;

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

    public static String toString(ByteBuffer buffer, int start, int length) {
        int position = buffer.position();
        int limit = buffer.limit();
        buffer.position(start);
        buffer.limit(start + length);
        String result = Server.ASCII.decode(buffer).toString();
        buffer.position(position);
        buffer.limit(limit);
        return result;
    }

    public static byte[] generateResponse(ResponseType responceType, String page) {
        return ("HTTP/1.0 " + responceType.getStatus() + "\r\n"
                + "Content-Type: text/html\r\n"
                + "Server: JANIOS\r\n\r\n"
                + page).getBytes(ASCII);
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(new InetSocketAddress(ADDRESS, PORT));
        server.start();
    }

    public Server(InetSocketAddress address) {
        this.address = address;
    }

    public void start() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(address);
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

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ);
        clients.put(sc, Client.CLIENT_POOL.get(sc));
        System.out.println("Connected to " + sc.getRemoteAddress().toString());
    }

    private void read(SelectionKey key) throws IOException {
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
            clients.remove(socket);
            client.close();
            return;
        }

        readBuffer.flip();
        if (client.read(readBuffer)) {
            System.out.println("Closing erronous connection to " + socket.getRemoteAddress().toString());
            clients.remove(socket);
            client.close();
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

    private void write(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        Client client = clients.get(socket);
        if (!client.write()) {
            return;
        }
        System.out.println("Closed connection to " + socket.getRemoteAddress().toString());
        socket.close();
        clients.remove(socket);
    }
}
