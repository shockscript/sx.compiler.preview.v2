package org.hydroper.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ByteArray
{
    private ByteBuffer _buffer;
    private int _len;

    public ByteArray(ByteBuffer buffer, int length)
    {
        _buffer = buffer;
        _len = length;
        _buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public ByteArray(ByteBuffer buffer)
    {
        this(buffer, buffer.array().length);
    }

    public ByteArray(byte[] octets)
    {
        this(ByteBuffer.wrap(octets));
    }

    public ByteArray()
    {
        this(ByteBuffer.allocate(8), 0);
    }

    public ByteBuffer back()
    {
        return _buffer;
    }

    public ByteOrder order()
    {
        return _buffer.order();
    }

    public void setOrder(ByteOrder order)
    {
        _buffer.order(order);
    }

    public int position()
    {
        return _buffer.position();
    }

    public void setPosition(int pos)
    {
        if (pos < 0 || pos >= _len)
        {
            return;
        }
        _buffer.position(pos);
    }

    public int length()
    {
        return _len;
    }

    public void setLength(int length)
    {
        var k = _len;
        _len = length;
        while (_buffer.array().length < length)
        {
            var k2 = _buffer.array();
            _buffer = ByteBuffer.allocate(k2.length * 2);
            _buffer.put(k2, 0, k2.length);
        }
    }

    public int bytesAvailable()
    {
        return _len - _buffer.position();
    }

    private void _grow(int length)
    {
        var plus = _buffer.position() + length;
        if (plus < _len)
            return;
        if (plus >= _buffer.array().length())
        {
            var l = _buffer.array().length + length;
            while (_buffer.array().length < l)
            {
                var k = _buffer.array();
                _buffer = ByteBuffer.allocate(k.length * 2);
                _buffer.put(k, 0, k.length);
            }
        }
        _len += length;
    }

    public boolean readBoolean()
    {
        return readByte() == 1;
    }

    public void writeBoolean(boolean value)
    {
        writeByte((byte) (value ? 1 : 0));
    }

    public byte readByte()
    {
        return _buffer.get();
    }

    public void writeByte(byte value)
    {
        _grow(1);
        _buffer.put(value);
    }

    public float readFloat()
    {
        return _buffer.getFloat();
    }

    public void writeFloat(float value)
    {
        _grow(4);
        _buffer.putFloat(value);
    }

    public double readDouble()
    {
        return _buffer.getDouble();
    }

    public void writeDouble(double value)
    {
        _grow(8);
        _buffer.putDouble(value);
    }

    public int readInt()
    {
        return _buffer.getInt();
    }

    public void writeInt(int value)
    {
        _grow(4);
        _buffer.putInt(value);
    }

    public void readBytes(ByteArray bytes)
    {
        readBytes(bytes, 0);
    }

    public void readBytes(ByteArray bytes, int offset)
    {
        readBytes(bytes, offset, 0);
    }

    public void readBytes(ByteArray bytes, int offset, int length)
    {
        length = length == 0 ? _len - _buffer.position() : length;
        boolean outOfBounds = (offset + length) >= bytes.length();
        outOfBounds = outOfBounds || (_buffer.position() + length) >= this.length();
        if (outOfBounds)
            throw new RuntimeException("Out of bounds.");
        var k = bytes._buffer.position();
        bytes._buffer.put(_buffer.array(), offset, length);
        bytes._buffer.position(k);
    }

    public void writeBytes(ByteArray bytes)
    {
        writeBytes(bytes, 0);
    }

    public void writeBytes(ByteArray bytes, int offset)
    {
        writeBytes(bytes, offset, 0);
    }

    public void writeBytes(ByteArray bytes, int offset, int length)
    {
        length = length == 0 ? bytes.length() : length;
        if ((offset + length) >= bytes.length())
            throw new RuntimeException("Out of bounds.");
        _grow(length);
        _buffer.put(bytes.back().array(), offset, length);
    }

    public int writeUTF(String str)
    {
        return writeUTF(str, true);
    }

    public int writeUTF(String str, boolean prependLength)
    {
        var ba = new ByteArray(str.getBytes(StandardCharsets.UTF_8));
        if (prependLength)
            writeInt(ba.length());
        var pos = length();
        writeBytes(ba);
        return pos;
    }
}