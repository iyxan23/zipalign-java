<h1 align=center><pre>zipalign-java</pre></h1>

[Zipalign](https://developer.android.com/studio/command-line/zipalign) implemented in java without any dependencies.
[Read my post about it :>](https://nurihsanalghifari.my.id/posts/Zipalign-java/)

## Using

Pass in a `RandomAccessFile` to the function `ZipAlign#alignZip` to provide ZipAlign with a seekable file for more performant aligning.

```java
import com.iyxan23.zipalignjava.ZipAlign;

RandomAccessFile zipIn = new RandomAccessFile(zipPath, "r");
FileOutputStream zipOut = ...;

ZipAlign.alignZip(zipIn, zipOut);
```

Additionaly, you could use a regular `InputStream` (caution: terribly slow)

```java
import com.iyxan23.zipalignjava.ZipAlign;

FileInputStream zipIn = ...;
FileOutputStream zipOut = ...;

ZipAlign.alignZip(zipIn, zipOut);
```

> [!WARNING]
> The `InputStream` API is deprecated and is not supported anymore, it does not support aligning
> `.so` files and will not be.

Aligning .so files to 16KiB page boundaries is enabled by default, pass in a boolean to opt out:

```java
// only if zipIn is a RandomAccessFile
ZipAlign.alignZip(zipIn, zipOut, false);
```

Prefer to align .so files in a 4KiB page boundaries?

```java
// only if zipIn is a RandomAccessFile
ZipAlign.alignZip(zipIn, zipOut, 4, 4096);
```

## Importing

This library is published in [jitpack](https://jitpack.io), you can add it as your dependency it with:

```gradle
allprojects {
    repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}

dependencies {
    implementation 'com.github.iyxan23:zipalign-java:1.2.0'
}
```

You could also use [prebuilt jars by the CI](https://github.com/Iyxan23/zipalign-java/actions) or in the [releases section](https://github.com/Iyxan23/zipalign-java/releases).

The jar contains a simple CLI that allows you to use it in the command line.

```
$ java -jar zipalign-java-1.2.0.jar input.zip output.zip
```

## Benchmarks

Benchmarks are ran on an AMD Ryzen 5-5500U CPU with IntelliJ IDEA's runner and Temurin JDK 18.

```
Function                                             Avg elapsed time   Input

ZipAlign#alignZip(RandomAccessFile, OutputStream)    28ms               File from https://github.com/Iyxan23/zipalign-java/issues/1#issue-1236875761 (270K)
ZipAlign#alignZip(InputStream, OutputStream)         497ms              -- same file --
```
