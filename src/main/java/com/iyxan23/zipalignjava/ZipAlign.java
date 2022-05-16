package com.iyxan23.zipalignjava;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ZipAlign {
    /**
     * Aligns the zip as how zipalign would from the specified input stream into the specified output stream.
     * @param zipIn The zip input stream
     * @param zipOut The zip output stream
     */
    public static void alignZip(InputStream zipIn, OutputStream zipOut) throws IOException {
        ByteBuffer inBuffer = ByteBuffer.wrap(readAll(zipIn));
        // zip's file format is little endian
        inBuffer.order(ByteOrder.LITTLE_ENDIAN);
        LittleEndianOutputStream outStream = new LittleEndianOutputStream(zipOut);
        ArrayList<Integer> fileOffsets = new ArrayList<>();

        // todo: handle zip64 asdjlkajdoijdlkasjd
        // todo: test!?

        // source: https://en.wikipedia.org/wiki/ZIP_(file_format)#Structure
        // better source: https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html

        // starts with local header signature
        while (inBuffer.getInt() == 0x04034b50) {
            fileOffsets.add(outStream.bytesWritten());
            outStream.writeInt(0x04034b50);

            passBytes(inBuffer, outStream, 4);

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
            // 4-byte boundaries
            int dataStartPoint = outStream.bytesWritten() + 2 + fileNameLen + extraFieldLen;
            int wrongOffset = dataStartPoint % 4;
            int paddingSize = wrongOffset == 0 ? 0 : 4 - wrongOffset;

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

            // pass all the data
            passBytes(inBuffer, outStream, compressedSize);

            outStream.flush();
        }

        int centralDirectoryPosition = outStream.bytesWritten();
        int fileOffsetIndex = 0;

        // we're at the central directory
        do {
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

            passBytes(inBuffer, outStream, fileNameLen);
            passBytes(inBuffer, outStream, extraFieldLen);
            passBytes(inBuffer, outStream, fileCommentLen);

            outStream.flush();
            fileOffsetIndex++;

        } while (inBuffer.getInt() == 0x02014b50);

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
