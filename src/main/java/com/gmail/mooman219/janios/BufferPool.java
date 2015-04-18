package com.gmail.mooman219.janios;

import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class BufferPool {

    private final LinkedList<ByteBuffer> pool = new LinkedList<>();
    private final int bufferSize;
    private final int capacity;

    public BufferPool(int bufferSize, int capacity) {
        this.bufferSize = bufferSize;
        this.capacity = capacity;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getCapacity() {
        return capacity;
    }

    public ByteBuffer get() {
        ByteBuffer buffer = pool.poll();
        if (buffer == null) {
            buffer = ByteBuffer.allocate(bufferSize);
        } else {
            buffer.clear();
        }
        return buffer;
    }

    public void redeem(ByteBuffer buffer) {
        if (buffer.capacity() != bufferSize) {
            throw new IllegalArgumentException("Expected buffer size of : " + bufferSize + " got: " + buffer.capacity());
        } else if (pool.size() > capacity) {
            return; // Let the GC handle the buffer.
        }
        pool.add(buffer);
    }
}
