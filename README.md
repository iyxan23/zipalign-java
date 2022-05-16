<h1 align=center><pre>zipalign-java</pre></h1>

[Zipalign](https://developer.android.com/studio/command-line/zipalign) implemented in java with 0 dependencies.

> **Note: I haven't implemented Zip64 handling yet, this might not work on a few apks.**

## How to use it?

Either you build a jar, import the gradle module, or just copy the classes.

### Building a jar

To build a jar, run:
```console
$ ./gradlew jar
```
and you'll have the built jar in the `build/libs` directory.

#### Using it as a library

You can use the jar as a library and directly access the `ZipAlign` class.
```java
import com.iyxan23.zipalignjava.ZipAlign;

FileInputStream zipIn = ...;
FileOutputStream zipOut = ...;

ZipAlign.alignZip(zipIn, zipOut);
// hell yeah it's that easy!
```

#### Using it as a CLI program

or just run it as a cli program:
``` console
$ ls
unaligned.apk  zipalign-java-1.0.jar
$ java -jar zipalign-java-1.0.jar unaligned.apk aligned.apk
Aligning zip
Zip successfully aligned
$ ls
aligned.apk  unaligned.apk  zipalign-java-1.0.jar
```

## Proof?

![image](https://user-images.githubusercontent.com/31884435/168528933-92395c12-01ac-4f3e-a065-bf2abd97b191.png)

<!-- todo: write ## How!? -->

[zipalign_code]: https://cs.android.com/android/platform/superproject/+/master:build/make/tools/zipalign/ZipAlign.cpp;l=45
