package com.iyxan23.zipalignjava;

import java.io.ByteArrayOutputStream;
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
        ByteBuffer inBuffer = ByteBuffer.wrap(readAll(zipIn));
        // zip's file format is little endian
        inBuffer.order(ByteOrder.LITTLE_ENDIAN);

        LittleEndianOutputStream outStream = new LittleEndianOutputStream(zipOut);
        ArrayList<Integer> fileOffsets = new ArrayList<>();

        // todo: handle zip64 asdjlkajdoijdlkasjd

        // source: https://en.wikipedia.org/wiki/ZIP_(file_format)#Structure
        // better source: https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
        // the real source: https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT

        int header = inBuffer.getInt();

        // starts with local file header signature
        while (header == 0x04034b50) {
            fileOffsets.add(outStream.bytesWritten());
            outStream.writeInt(0x04034b50);

            passBytes(inBuffer, outStream, 2);

            short generalPurposeFlag = inBuffer.getShort();
            outStream.writeShort(generalPurposeFlag);

            // data descriptor is used if the 3rd bit is 1
            boolean hasDataDescriptor = (generalPurposeFlag & 0x8) == 0x8;

            short compressionMethod = inBuffer.getShort();
            outStream.writeShort(compressionMethod);
            // 0 is when there is no compression done
            boolean shouldAlign = compressionMethod == 0;

            passBytes(inBuffer, outStream, 8);

            int compressedSize = inBuffer.getInt();
            outStream.writeInt(compressedSize);
            passBytes(inBuffer, outStream, 4);

            short fileNameLen = inBuffer.getShort();
            outStream.writeShort(fileNameLen);

            short extraFieldLen = inBuffer.getShort();

            // we're going to extend this extra field (if the data is uncompressed) so that the data will align into
            // the specified alignment boundaries (usually 4 bytes)
            int dataStartPoint = outStream.bytesWritten() + 2 + fileNameLen + extraFieldLen;
            int wrongOffset = dataStartPoint % alignment;
            int paddingSize = wrongOffset == 0 ? 0 : alignment - wrongOffset;

            if (shouldAlign) {
                outStream.writeShort(extraFieldLen + paddingSize);
            } else {
                outStream.writeShort(extraFieldLen);
            }

            passBytes(inBuffer, outStream, fileNameLen);
            passBytes(inBuffer, outStream, extraFieldLen);

            if (shouldAlign && paddingSize != 0) {
                // pad the extra field with null bytes
                byte[] padding = new byte[paddingSize];
                outStream.write(padding);
            }

            // if there isn't any data descriptor we can just pass the data right away
            if (!hasDataDescriptor) {
                passBytes(inBuffer, outStream, compressedSize);

                outStream.flush();
                header = inBuffer.getInt();

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
                cur = inBuffer.get();
                outStream.write(cur);

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
            passBytes(inBuffer, outStream, 12);

            outStream.flush();

            // todo: zip64
            // if (zip64) passBytes(inBuffer, outStream, 20);

            // next should be a new header
            header = inBuffer.getInt();
        }

        int centralDirectoryPosition = outStream.bytesWritten();
        int fileOffsetIndex = 0;

        // we're at the central directory
        while (header == 0x02014b50) {
            outStream.writeInt(0x02014b50);
            int fileOffset = fileOffsets.get(fileOffsetIndex);

            passBytes(inBuffer, outStream, 24);

            short fileNameLen = inBuffer.getShort();
            outStream.writeShort(fileNameLen);

            short extraFieldLen = inBuffer.getShort();
            outStream.writeShort(extraFieldLen);

            short fileCommentLen = inBuffer.getShort();
            outStream.writeShort(fileCommentLen);

            passBytes(inBuffer, outStream, 8);

            // offset of local header
            inBuffer.getInt();
            outStream.writeInt(fileOffset);

            byte[] fileName = new byte[fileNameLen];

            inBuffer.get(fileName);
            outStream.write(fileName);

            passBytes(inBuffer, outStream, extraFieldLen);
            passBytes(inBuffer, outStream, fileCommentLen);

            outStream.flush();
            fileOffsetIndex++;

            header = inBuffer.getInt();
        }

        if (header != 0x06054b50)
            throw new IOException("No end of central directory record header, there is something wrong");

        // end of central directory record
        outStream.writeInt(0x06054b50);
        passBytes(inBuffer, outStream, 12);

        // offset of where central directory starts
        inBuffer.getInt();
        outStream.writeInt(centralDirectoryPosition);

        short commentLen = inBuffer.getShort();
        outStream.writeShort(commentLen);

        passBytes(inBuffer, outStream, commentLen);
    }

    /**
     * Passes a specified length of bytes from a ByteBuffer to an output stream
     * @param in The byte buffer
     * @param out The output stream
     * @param len The length of how many bytes to be passed
     */
    private static void passBytes(ByteBuffer in, OutputStream out, int len) throws IOException {
        byte[] data = new byte[len];
        in.get(data);
        out.write(data);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        while (in.read(buffer) != -1) {
            output.write(buffer);
        }

        return output.toByteArray();
    }
}
