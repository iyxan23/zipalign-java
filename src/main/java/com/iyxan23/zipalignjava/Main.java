package com.iyxan23.zipalignjava;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println(
                "Usage:\n\t<exec> <input zip> <output zip>\n\nExample:\n\tjava -jar zipalign.jar input.zip output.zip"
            );
            System.exit(1);
        }

        File inZip = new File(args[0]);
        File outZip = new File(args[1]);

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

        try (FileInputStream in = new FileInputStream(inZip)) {
            try (FileOutputStream out = new FileOutputStream(outZip)) {
                System.out.println("Aligning zip");
                ZipAlign.alignZip(in, out);
                System.out.println("Zip successfully aligned");
            }
        }
    }
}
