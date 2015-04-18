package com.gmail.mooman219.janios;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class ClientPool {

    private final LinkedList<Client> pool = new LinkedList<>();
    private final int capacity;

    public ClientPool(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public Client get(SocketChannel socket) {
        Client client = pool.poll();
        if (client == null) {
            client = new Client(socket);
        } else {
            client.redeem(socket);
        }
        return client;
    }

    public void redeem(Client client) {
        if (pool.size() > capacity) {
            return; // Let the GC handle the client.
        }
        pool.add(client);
    }
}
