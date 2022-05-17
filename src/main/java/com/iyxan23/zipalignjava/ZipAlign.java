package com.iyxan23.zipalignjava;

import com.macfaq.io.LittleEndianInputStream;
import com.macfaq.io.LittleEndianOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class ZipAlign {
    /**
     * Aligns the zip from the given input stream and outputs it to the given output stream with 4 byte alignment.<br/>
     * <br/>
     * <b>NOTE: This function assumes that the given input stream is a valid zip file and skims manually through bytes.
     * It is advised to first verify the zip before passing it to this function.</b><br/>
     * <br/>
     * Example usage:
     * <pre>
     *     FileInputStream zipIn = ...;
     *     FileOutputStream zipOut = ...;
     *
     *     ZipAlign.alignZip(zipIn, zipOut);
     * </pre>
     *
     * @param zipIn The zip input stream
     * @param zipOut The zip output stream
     * @see ZipAlign#alignZip(InputStream, OutputStream, int)
     */
    public static void alignZip(InputStream zipIn, OutputStream zipOut) throws IOException {
        alignZip(zipIn, zipOut, 4);
    }

    /**
     * Aligns the zip from the given input stream and outputs it to the given output stream.<br/>
     * <br/>
     * <b>NOTE: This function assumes that the given input stream is a valid zip file and skims manually through bytes.
     * It is advised to first verify the zip before passing it to this function.</b><br/>
     * <br/>
     * Example usage:
     * <pre>
     *     FileInputStream zipIn = ...;
     *     FileOutputStream zipOut = ...;
     *
     *     ZipAlign.alignZip(zipIn, zipOut, 4);
     * </pre>
     *
     * @param zipIn The zip input stream
     * @param zipOut The zip output stream
     * @param alignment Alignment in bytes, usually 4
     * @see ZipAlign#alignZip(InputStream, OutputStream)
     */
    public static void alignZip(InputStream zipIn, OutputStream zipOut, int alignment) throws IOException {
        LittleEndianInputStream in = new LittleEndianInputStream(zipIn);
        LittleEndianOutputStream out = new LittleEndianOutputStream(zipOut);
        ArrayList<Integer> fileOffsets = new ArrayList<>();

        // todo: handle zip64 asdjlkajdoijdlkasjd

        // source: https://en.wikipedia.org/wiki/ZIP_(file_format)#Structure
        // better source: https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        // the real source: https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT

        int header = in.readInt();

        // starts with local file header signature
        while (header == 0x04034b50) {
            fileOffsets.add(out.bytesWritten());
            out.writeInt(0x04034b50);

            passBytes(in, out, 2);

            short generalPurposeFlag = in.readShort();
            out.writeShort(generalPurposeFlag);

            // data descriptor is used if the 3rd bit is 1
            boolean hasDataDescriptor = (generalPurposeFlag & 0x8) == 0x8;

            short compressionMethod = in.readShort();
            out.writeShort(compressionMethod);
            // 0 is when there is no compression done
            boolean shouldAlign = compressionMethod == 0;

            passBytes(in, out, 8);

            int compressedSize = in.readInt();
            out.writeInt(compressedSize);
            passBytes(in, out, 4);

            short fileNameLen = in.readShort();
            out.writeShort(fileNameLen);

            short extraFieldLen = in.readShort();

            // we're going to extend this extra field (if the data is uncompressed) so that the data will align into
            // the specified alignment boundaries (usually 4 bytes)
            int dataStartPoint = out.bytesWritten() + 2 + fileNameLen + extraFieldLen;
            int wrongOffset = dataStartPoint % alignment;
            int paddingSize = wrongOffset == 0 ? 0 : alignment - wrongOffset;

            if (shouldAlign) {
                out.writeShort(extraFieldLen + paddingSize);
            } else {
                out.writeShort(extraFieldLen);
            }

            passBytes(in, out, fileNameLen);
            passBytes(in, out, extraFieldLen);

            if (shouldAlign && paddingSize != 0) {
                // pad the extra field with null bytes
                byte[] padding = new byte[paddingSize];
                out.write(padding);
            }

            // if there isn't any data descriptor we can just pass the data right away
            if (!hasDataDescriptor) {
                passBytes(in, out, compressedSize);

                out.flush();
                header = in.readInt();

                continue;
            }

            // we have a data descriptor

            // fixme: pkware's spec 4.3.9.3 - although crazy rare, it is possible for data descriptors to not have
            //        a header before it. It's very tricky to implement it so I prefer to do it later.

            byte[] buffer = new byte[4];
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // loop until we stumble upon 0x08074b50
            byte cur;
            do {
                cur = in.readByte();
                out.write(cur);

                if (byteBuffer.position() == buffer.length - 1) {
                    // the buffer is full, so we shift all of it to the left
                    buffer[0] = buffer[1];
                    buffer[1] = buffer[2];
                    buffer[2] = buffer[3];
                    // then put our byte on the last index
                    byteBuffer.put(buffer.length - 1, cur);
                } else {
                    byteBuffer.put(cur);
                }
            } while (byteBuffer.getInt(0) != 0x08074b50);

            // we skip all the data descriptor lol, we don't need it
            passBytes(in, out, 12);

            out.flush();

            // todo: zip64
            // if (zip64) passBytes(in, outStream, 20);

            // next should be a new header
            header = in.readInt();
        }

        int centralDirectoryPosition = out.bytesWritten();
        int fileOffsetIndex = 0;

        // we're at the central directory
        while (header == 0x02014b50) {
            out.writeInt(0x02014b50);
            int fileOffset = fileOffsets.get(fileOffsetIndex);

            passBytes(in, out, 24);

            short fileNameLen = in.readShort();
            out.writeShort(fileNameLen);

            short extraFieldLen = in.readShort();
            out.writeShort(extraFieldLen);

            short fileCommentLen = in.readShort();
            out.writeShort(fileCommentLen);

            passBytes(in, out, 8);

            // offset of local header
            in.readInt();
            out.writeInt(fileOffset);

            passBytes(in, out, fileNameLen);
            passBytes(in, out, extraFieldLen);
            passBytes(in, out, fileCommentLen);

            out.flush();
            fileOffsetIndex++;

            header = in.readInt();
        }

        if (header != 0x06054b50)
            throw new IOException("No end of central directory record header, there is something wrong");

        // end of central directory record
        out.writeInt(0x06054b50);
        passBytes(in, out, 12);

        // offset of where central directory starts
        in.readInt();
        out.writeInt(centralDirectoryPosition);

        short commentLen = in.readShort();
        out.writeShort(commentLen);

        passBytes(in, out, commentLen);
    }

    /**
     * Passes a specified length of bytes from an {@link InputStream} to an {@link OutputStream}
     * @param in The input stream
     * @param out The output stream
     * @param len The length of how many bytes to be passed
     */
    private static void passBytes(InputStream in, OutputStream out, int len) throws IOException {
        byte[] data = new byte[len];
        if (in.read(data) == -1) throw new IOException("Reached EOF when passing bytes");
        out.write(data);
    }
}
