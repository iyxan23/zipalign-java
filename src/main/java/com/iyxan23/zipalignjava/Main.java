package com.iyxan23.zipalignjava;

import java.io.*;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                "Usage:\n\t<exec> <input zip> <output zip> (old)\n\nExample(s):\n\t$ java -jar zipalign.jar input.zip output.zip\n\n\tTo use the old method, use the \"old\" parameter:\n\n\t$ java -jar zipalign.jar input.zip output.zip old"
            );
            System.exit(1);
        }

        File inZip = new File(args[0]);
        File outZip = new File(args[1]);
        boolean useOldMethod = args.length >= 3 && Objects.equals(args[2], "old");

        if (!inZip.exists()) {
            System.err.println("Input file doesn't exist: " + inZip.getPath());
            System.exit(1);
        }

        if (inZip.isDirectory()) {
            System.err.println("Input path must be of a file: " + inZip.getPath());
            System.exit(1);
        }

        if (!outZip.createNewFile()) {
            System.err.println("Output file already exists: " + outZip.getPath());
            System.exit(1);
        }

        System.out.println("Aligning zip " + inZip);
        long start = System.currentTimeMillis();

        if (useOldMethod) {
            try (FileInputStream in = new FileInputStream(inZip)) {
                try (FileOutputStream out = new FileOutputStream(outZip)) {
                    ZipAlign.alignZip(in, out);
                }
            }
        } else {
            try (RandomAccessFile raf = new RandomAccessFile(inZip, "r")) {
                try (FileOutputStream zipOut = new FileOutputStream(outZip)) {
                    ZipAlign.alignZip(raf, zipOut);
                } catch (InvalidZipException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        System.out.println("Zip aligned successfully, took " + (System.currentTimeMillis() - start) + "ms");
    }
}
