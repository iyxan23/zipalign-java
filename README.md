<h1 align=center><pre>zipalign-java</pre></h1>

[Zipalign](https://developer.android.com/studio/command-line/zipalign) implemented in java with 0 dependencies.

> **Note: [zip64](https://en.wikipedia.org/wiki/ZIP_(file_format)#ZIP64) is currently unsupported, this may not work on apks with the size bigger than 4GB.**

## Using

It is highly recommended to use a `RandomAccessFile` as the zip input.
```java
import com.iyxan23.zipalignjava.ZipAlign;

RandomAccessFile zipIn = new RandomAccessFile(zipPath, "r");
FileOutputStream zipOut = ...;

ZipAlign.alignZip(zipIn, zipOut);
```

or if you really need it, you can read from an `InputStream`. Do note that this is substantially slower than using `RandomAccessFile`.
```java
import com.iyxan23.zipalignjava.ZipAlign;

FileInputStream zipIn = ...;
FileOutputStream zipOut = ...;

ZipAlign.alignZip(zipIn, zipOut);
```

## Importing

You can use this library by importing this as a gradle module to your project, building a jar, or just by copying the classes.

### Building a jar

To build a jar, run:
```console
$ ./gradlew jar
```
and you'll have a built jar in the `build/libs` directory.

This jar will contain a very simple cli that allows you to run it in your command line.
```
$ java -jar zipalign-java-0.1-dev.jar input.zip output.zip
```

## Benchmarks

These benchmarks are ran on an AMD Ryzen 5-5500U CPU.

```
Function                                             Op/s    Avg elapsed time   Input

ZipAlign#alignZip(RandomAccessFile, OutputStream)    12      81ms               File from https://github.com/Iyxan23/zipalign-java/issues/1#issue-1236875761 (270K)
ZipAlign#alignZip(InputStream, OutputStream)         2       641ms              -- same file --
```

[zipalign_code]: https://cs.android.com/android/platform/superproject/+/master:build/make/tools/zipalign/ZipAlign.cpp;l=45
