package com.iyxan23.zipalignjava;

// A ByteBuffer wrapper for working with unsigned values

import java.nio.ByteBuffer;

class UnsignedByteBufferWrapper {
    private ByteBuffer inner;

    UnsignedByteBufferWrapper(ByteBuffer inner) {
        this.inner = inner;
    }

    public int getUShort() {
        return inner.getShort() & 0xffff;
    }

    public long getUInt() {
        return inner.getInt() & 0xffffffffL;
    }

    public int getUShort(int index) {
        return inner.getShort(index) & 0xffff;
    }

    public long getUInt(int index) {
        return inner.getInt(index) & 0xffffffffL;
    }
}
