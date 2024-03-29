
==============================================================================
      XML Processing Snippets - https://code.google.com/p/xml-snippets/
==============================================================================


Introduction to the xml-snippets project
========================================

    The xml-snippets package was originally meant to be a collection of 
    Java snippets for manipulating XML documents (just like in the good old 
    days there were Pascal and C snippets collections in BBSes). The purpose 
    was to demonstrate how to do certain things with XML documents like 
    equivalence relation or 'normalization'. However, during the short 
    development sprint, which happened in 2 weeks during November 2012, 
    it became apparent that the main objective for the author was to 
    provide a proof-of-concept program for XML revisioning system, 
    which included a referencing mechanism.


fida - The XML revision control system
======================================

    The main 'snippet' in the package is the XML revision control system 
    called 'fida'. This program provides an XML-specific revision control
    system similar to other command-line SCM/RCS software.

    To build and install 'fida', see the quick install guide at
    http://code.google.com/p/xml-snippets/wiki/QuickInstallGuide

    To begin the use, please see the introduction tutorial at
    http://code.google.com/p/xml-snippets/wiki/IntroductionTutorial


Documentation
=============

    Tutorials are available on the project's Google Code wiki. See
    http://code.google.com/p/xml-snippets/wiki/WikiTableOfContents

    The source code includes javadocs comments here and there.
    Javadocs documentation can be generated by executing "ant javadocs".


Licensing
=========

    This software is licensed under the terms you may find in the file 
    named "LICENSE.txt" in this directory.

    This distribution includes 3rd party software components. 
    Licenses and legal notices regarding these components you may find
    in the file named "NOTICES.txt" in this directory.


The files in this packages
==========================

    bin/                 Scripts to be included in the PATH
    build/               The redistributable jar package 
    build.xml            Ant build file
    lib/                 Third-party libraries
    LICENSE.txt          License text
    NOTICES.txt          Legal notices for 3rd party components
    README.txt           This text file
    src/                 Java source code
    templ/               Script templates
    text/                Texts for your entertainment.


Building
========
    
    Generally, the following software is required for building:
    
        - ant (tested 1.8.2)
        - Java JDK (tested 1.6 and 1.7)
    
    On Linux debian/ubuntu the following packages are required:
    
        - openjdk-7-jdk
        - ant

    After the required software has been installed, 
    building happens in the conventional way. Execute ant:

        ant

    which will build the default target "dev" (see the header
    comments in "build.xml" for other targets). 


Cloning the repository
======================

    Generally, the following SCM software is required:
    
        - hg (Mercurial)
        
    On Linux ubuntu/debian the following package is required:
    
        - mercurial
        
    After the required software has been installed, a local copy
    of the xml-snippets repository can be cloned with this command:
    
        hg clone https://code.google.com/p/xml-snippets/ 

    which exactly the same command as instructed in 
    the Google Code web page.


Information sources
===================

Web site:                http://code.google.com/p/xml-snippets/
Project maintainer:      Jani Hautamaki <jani.hautamaki@hotmail.com>


