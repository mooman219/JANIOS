package com.gmail.mooman219.janios;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Server {

    public static final int BUFFER_SIZE = 3 * 1024;

    private final ConcurrentHashMap<SocketChannel, Client> clients = new ConcurrentHashMap<>();
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final ClientPool clientPool;
    private final InetSocketAddress address;

    public static void main(String[] args) throws IOException {
        InetSocketAddress address = new InetSocketAddress("localhost", 8081);
        int clientPoolSize = 512;

        Server server = new Server(address, clientPoolSize);
        server.start();
    }

    public Server(InetSocketAddress address, int clientPoolSize) {
        this.address = address;
        this.clientPool = new ClientPool(clientPoolSize);
    }

    public void start() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(address);
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server Started");
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
        clients.put(sc, clientPool.get(sc));
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
