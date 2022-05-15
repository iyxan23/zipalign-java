package com.iyxan23.zipalignjava;

import java.io.*;
import java.util.ArrayList;

public class ZipAlign {
    /**
     * Aligns the zip as how zipalign would from the specified input stream into the specified output stream.
     * @param zipIn The zip input stream
     * @param zipOut The zip output stream
     */
    public static void alignZip(InputStream zipIn, OutputStream zipOut) throws IOException {
        DataInputStream stream = new DataInputStream(zipIn);
        CountingWrapper outStream = new CountingWrapper(zipOut);
        ArrayList<Integer> fileOffsets = new ArrayList<>();

        // todo: handle zip64 asdjlkajdoijdlkasjd
        // todo: test!?

        // source: https://en.wikipedia.org/wiki/ZIP_(file_format)#Structure
        // better source: https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html

        // starts with local header signature
        while (stream.readInt() == 0x504b0304) {
            fileOffsets.add(outStream.bytesPosition);
            outStream.writeInt(0x504b0304);

            passBytes(stream, outStream, 4);

            short compressionMethod = stream.readShort();
            outStream.writeShort(compressionMethod);
            // 0 is when there is no compression done
            boolean shouldAlign = compressionMethod == 0;

            passBytes(stream, outStream, 8);

            int compressedSize = stream.readInt();
            outStream.writeInt(compressedSize);
            passBytes(stream, outStream, 4);

            short fileNameLen = stream.readShort();
            outStream.writeShort(fileNameLen);

            short extraFieldLen = stream.readShort();

            // we're going to extend this extra field (if the data is uncompressed) so that the data will align into
            // 4-byte boundaries
            int dataStartPoint = outStream.bytesPosition + 2 + fileNameLen + extraFieldLen;
            int paddingSize = dataStartPoint % 4;

            if (shouldAlign) {
                outStream.writeShort(extraFieldLen + paddingSize);
            } else {
                outStream.writeShort(extraFieldLen);
            }

            passBytes(stream, outStream, fileNameLen);
            passBytes(stream, outStream, extraFieldLen);

            if (shouldAlign && paddingSize != 0) {
                // pad the extra field with null bytes
                byte[] padding = new byte[paddingSize];
                outStream.write(padding);
            }

            // pass all the data
            passBytes(stream, outStream, compressedSize);
        }

        // we're at the central directory
        // todo
    }

    /**
     * Passes a specified length of bytes from the input to the output
     * @param in The input stream
     * @param out The output stream
     * @param len The length of how many bytes to be passed
     */
    private static void passBytes(InputStream in, OutputStream out, int len) throws IOException {
        byte[] data = new byte[len];
        if (in.read(data) == -1) throw new IOException("End of stream");
        out.write(data);
    }

    /**
     * A simple class that wraps around an `OutputStream` that will count how many bytes that has been written to the
     * stream.
     *
     * I don't think this is a good idea, but should work fine.
     */
    private static class CountingWrapper extends DataOutputStream {
        private int bytesPosition = 0;

        public CountingWrapper(OutputStream source) {
            super(source);
        }

        @Override
        public void write(int i) throws IOException {
            bytesPosition += 1;
            super.write(i);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            bytesPosition += bytes.length;
            super.write(bytes);
        }

        @Override
        public void write(byte[] bytes, int i, int i1) throws IOException {
            bytesPosition += bytes.length;
            super.write(bytes, i ,i1);
        }

        @Override
        public void flush() throws IOException {
            super.flush();
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }
}
