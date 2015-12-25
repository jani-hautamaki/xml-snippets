# Handling pre-existing revision numbers #

This tutorial will show how to deal with revision numbers
that already exist in an XML document.

## Preparations ##

Lets begin the preparations as usual by initializing a new repository,
and creating some test data.

Create a new repository:
```
fida init
```

Since in this tutorial existing revision numbers are discussed,
the sample XML must include some pre-existing revision numbers.
Create a new file, called "`data.xml`", and insert the following
contents to it:
```
<?xml version="1.0" encoding="UTF-8"?>
<csvfile id="survey_xyz" rev="101">
  <author id="author" rev="23">random number generator</author>
  <row>1,,,4,,,2,9</row>
</csvfile>
```

At this time we would usually say "`fida add data.xml`". Well, lets try it out, and see what happens:
```
fida add data.xml
```

This will output an error message:
```
Processing file data.xml
The element /csvfile/author has an unknown xid=author:23
```

The error message indicates that the program has encountered a xid unknown to it. There are two different routes that can be taken now.

## Automatic removal of unknown revisions: `-unrev` ##

One way forward is to use, for instance, a text editor´s search-and-replace or UNIX command-line tool `sed` to remove all `@rev` attributes from the file. However, because this is not so nice to do every time with a separate tool, there is a command-line option/switch which can be used to perform this "unrevisioning" of the XML elements on-the-flu.

To unrevision automatically all unknown xids encountered, execute
```
fida add data.xml -unrev
```

Inspecting the file shows that revision numbers have been reset as expected. The contents of the file should be
```
<?xml version="1.0" encoding="UTF-8"?>
<csvfile id="survey_xyz" rev="1">
  <author id="author" rev="1">random number generator</author>
  <row>1,,,4,,,2,9</row>
</csvfile>
```

Okay, lets then see what the second option has to offer.

## Forcing unknown revisions: `-force` ##

In order to be able to demonstrate the second method, the data file has to be populated with some new XML elements. This is how the file looked like some stuff was added::
```
<?xml version="1.0" encoding="UTF-8"?>
<csvfile id="survey_xyz" rev="1">
  <author id="author" rev="1">random number generator</author>
  <author id="coauthor" rev="7">lucky number seven</author>
  <software id="software" rev="9">/dev/random/</software>
  <format id="format" rev="5">extended csv-xml</format>
  <row>1,,,4,,,2,9</row>
</csvfile>
```

Make a note the repository is now in the revision number `1`, and this file contains revision numers `7`, `9` and `5`, in that order. Trying to run "`fida update`" results in a similar error message as in the first attempt.

The second method is to really force the revision numbers into the repository. For this method to work, the repository's own revision number must be jumped more or less abruptly to the highest number encountered during the ingest. Also, the method won't work if the adding of such a "forced" xid will hide another xid with a newer revision number.

In other words, this option has lot of warnings, but its very nice feature to have. Also, this way the revision numbers are not lost.

Thats enough for warnings. Its time to force the revision numbers into the repository:
```
fida update -force
```

Inspecting the file reveals that only the revision number of the root was updated. The new revision number is the highest revision number encountered during the ingest, ie. `@rev="9"`. Practically the repository warped from revision "1" to revision "9".

Lets see how are the lifelines this time. Executing "`fida lifelines`" gives:
```
1     author:1
2     survey_xyz:1 > r9
3     coauthor:7
4     software:9
5     format:5
```

which is quite reasonable from our perspective, even though the existing revision numbers in the data usually suggest that there have been other revisions of them, but the repository doesn't know anything about them.

These are probably the two most reasonable actions that can be taken to handle already-existing revision numbers in XML files.

## Next tutorial ##

The next tutorial is about how to switch the repository from using revision numbers to use [3-level version numbers](UsingVersionTutorial.md).