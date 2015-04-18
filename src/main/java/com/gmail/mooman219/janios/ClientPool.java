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
            client = new Client(this);
        }
        client.redeem(socket);
        return client;
    }

    /**
     * Adds the client back into the pool so it can be reused. The user is
     * responsible for cleaning up any resources before returning the client to
     * the pool.
     *
     * @param client the client to return to the pool
     */
    protected void redeem(Client client) {
        if (pool.size() > capacity) {
            return; // Let the GC handle the client.
        }
        pool.add(client);
    }
}
