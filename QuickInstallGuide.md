# Quick installation guide #

This page provides quick instructions on how to install `fida`,
the XML revision control tool.

The package can be installed in two different ways:

  1. By downloading the binary distribution
  1. By building from the sources

Binary distribution includes pre-built JAR files, so no compiling or building is necessary even though all the necessary source code files are included in the package. The binary distribution is probably the easiest way to install the program.

## Steps 0–3 (Method 1: The binary distribution) ##

**Step 0.** Prerequisites

You need to have the following software installed to your system:

  * Java JRE (>v1.6)

Java JRE is used for executing the Java program.

**Step 1.** Download the latest binary package

The latest binary distribution package can be downloaded from

  * [https://sourceforge.net/projects/fida/files/](https://sourceforge.net/projects/fida/files/)

**Step 2.** Extract the files

On Windows: you should use whatever tool is available to you.

On Linux: use `'unzip'`.

**Step 3.** Change your working directory to the uncompressed folder

Execute the following command:
```
cd xmlsnippets-x.y.z
```
(You have to replace `'x.y.z'` with the actual version numbers)

Note: if you are using Windows and you used the graphical user interface for decompressing the downloaded package, then you need to open "Command Prompt" first, and change your current working directory to the same directory where the package was downloaded.

From here on the instructions are equivalent regardless of the installation method used. Continue to [Step 4](#Step_4._Appending_%27bin%27_to_%27PATH%27_..md) below.

## Steps 0–3 (Method 2: Build from the sources) ##

**Step 0.** Prerequisites

You need to have the following software installed to your system:

  * Java JDK (>v1.6): provides `'javac'`, and `'java'`
  * Ant (>1.8.2): provides `'ant'`
  * Mercurial (>2.0.1): provides `'hg'`

Java JDK is used for compiling and executing the Java program. Ant is used to orchestrate the build process. Mercurial is used to clone the source codes from the Google Code's repository to your local system.

**Step 1.** Clone the sources

Execute the following command:
```
hg clone https://code.google.com/p/xml-snippets
```

**Step 2.** Change your working directory to the repository root

Execute the following command:
```
cd xml-snippets
```

**Step 3.** Build the binaries

Execute the following command:
```
ant
```

From here on the instructions are equivalent regardless of the installation method used. Continue to [Step 4](#Step_4._Appending_%27bin%27_to_%27PATH%27_..md) below.

## Step 4. Appending `'bin'` to `'PATH'`. ##

In this step, the sub-directory`'bin'` is appended to the environment variable `'PATH'` which specifies the set of directories where executable programs are located.

Your current working directory should be either

  * Method 1: the repository root (ie. `'xml-snippets'`)
  * Method 2: the uncompressed folder (ie. `'xmlsnippets-x.y.z'`)

You know you are in the right directory if the command `'dir'` (on Windows) or `'ls'` (on Linux) shows the directory contains a file named `build.xml` file and a sub-directory named `bin`.

Being in this particular directory simplifies the next commands greatly.

On windows:
```
set PATH=%CD%\bin;%PATH% 
```

On Linux in `bash` shell:
```
export PATH=`pwd`/bin:${PATH}
hash -r
```

On Linux in `tcsh` shell:
```
setenv PATH `pwd`/bin:${PATH}
rehash
```

## Step 5. Getting started. ##

Now the program should be ready to use. Verify he installation by test running the program. Execute the following command in the shell / command prompt:
```
fida
```

This should output a message `No arguments` to the console. To get an understanding of what arguments you can pass to `fida`, execute the command
```
fida help
```

**NOTE**: The arguments passed to `fida` are **case-sensitive**!

Next steps in getting started are covered in the [introduction tutorial](IntroductionTutorial.md).