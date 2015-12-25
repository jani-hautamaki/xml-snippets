# XML namespaces and bubbling the declarations #

## Introduction ##

This tutorial shows how the program deals with XML namespaces.
Admittedly, there are many different strategies for handling XML
namespaces and their declarations from the revision tracking point
view.

The twist in handling XML namespaces, and especially XML namespace
declarations, comes from the fact that repository shreds the hierarchical
input XML document into a flat list of independent XML elements.
Because the XML elements are independent of each others, at least
to a certain extent, each XML element must independently record
the information about the XML namespaces included.

The program relies heavily on the workings of jdom v1.1.3 library,
and the program doesn't really spend any additional effort in
detecting XML namespace declarations or uses within the document.

What this all means in practice is that after an XML document
has been ingested, that is, turned from an hierarchy into a flat
list, the program no longer knows where each XML namespace
declaration originally was.

## Bubbling namespace declarations ##

Lets see how all this looks in practice. First, the repository
is created in the usual manner:
```
fida init
```

Next, an XML document is needed. The source XML document needs to
have few XML namespace declarations that are suitably chosen for
the demonstration purposes. Create a file called "demo1.xml"
with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc">

  <ns1:a id="a1a" xmlns:ns1="namespace1">
    <ns1:b id="a1b">
      <ns1:c id="a1c">
        <ns1:d id="a1d">
          <p>data int a1d</p>
        </ns1:d>
      </ns1:c>
    </ns1:b>
  </ns1:a>

  <a id="a2a" xmlns:ns2="namespace2">
    <b id="a2b">
      <c id="a2c">
        <d id="a2d">
          <ns2:p>data in a2d</ns2:p>
        </d>
      </c>
    </b>
  </a>

</root>
```

Lets make two important observations about the placing of
the namespace declarations and locations of usage. In the first
part of the file the namespace "ns1" is declared at the first
level, and used in all sub-levels. In the second part of the file
the namespace "ns2" is also declared at the first level. However,
it is used is only at the deepest sub-level.

Lets ingest the file:
```
fida add demo1.xml
```

Reopening the file does not contain any suprises, the revision numbers
have been added just as one would have expected. The namespace
declarations haven't been touched at all.

However, rebuilding the elements individually from the command-line
will show some suprises. Lets rebuild the root element
from the command-line:
```
fida output doc:1
```

The output is (after inserting few additional newlines just
for the clarity):
```
<root xmlns:ns1="namespace1" xmlns:ns2="namespace2" id="doc" rev="1">

  <ns1:a id="a1a" rev="1">
    <ns1:b id="a1b" rev="1">
      <ns1:c id="a1c" rev="1">
        <ns1:d id="a1d" rev="1">
          <p>data int a1d</p>
        </ns1:d>
      </ns1:c>
    </ns1:b>
  </ns1:a>
  
  <a id="a2a" rev="1">
    <b id="a2b" rev="1">
      <c id="a2c" rev="1">
        <d id="a2d" rev="1">
          <ns2:p>data in a2d</ns2:p>
        </d>
      </c>
    </b>
  </a>
  
</root>
```

What has changed compared to the original file is that BOTH namespace
declarations have been leveled up in the hierarchy, all the way up to
the root element itself. Moving the namespace declarations from
the inner elements towards the outer elements is called _bubbling_.

Bubbling is performed to all XML output by default.

## Self-contained XML elements ##

According to the rebuild of the root element, the namespaces
are declared in the root element itself. Lets see how the things
are for the individual child elements. The XML element with xid
"`a1a:1`" was the XML element which had the namespace declaration
in the original source XML file. Rebuilding that child element first:
```
fida output a1a:1
```

The output is:
```
<ns1:a xmlns:ns1="namespace1" id="a1a" rev="1">
  <ns1:b id="a1b" rev="1">
    <ns1:c id="a1c" rev="1">
      <ns1:d id="a1d" rev="1">
        <p>data int a1d</p>
      </ns1:d>
    </ns1:c>
  </ns1:b>
</ns1:a>
```

Okay, the XML element has the namespace declaration as it did in
the original source XML file too. Rebuilding the next child in
this hierarchy:
```
fida output a1b:1
```

The output is:
```
<ns1:b xmlns:ns1="namespace1" id="a1b" rev="1">
  <ns1:c id="a1c" rev="1">
    <ns1:d id="a1d" rev="1">
      <p>data int a1d</p>
    </ns1:d>
  </ns1:c>
</ns1:b>
```

This is little surprising at first. Surely, the XML element with xid
"`a1b`" didn't contain that namespace declaration? That is the correct
observation. It didn't. However, if the output would have omitted
the namespace declaration, then the outputed XML segment wouldn't
have been valid XML at all, because the namespace prefix "ns1"
would have been undefined.

Continuing the rebuilding with the rest of the child elements left
in this hierarchy.

Executing
```
fida output a1c:1
```

gives the output:

```
<ns1:c xmlns:ns1="namespace1" id="a1c" rev="1">
  <ns1:d id="a1d" rev="1">
    <p>data int a1d</p>
  </ns1:d>
</ns1:c>
```

Executing
```
fida output a1d:1
```

gives the output:

```
<ns1:d xmlns:ns1="namespace1" id="a1d" rev="1">
  <p>data int a1d</p>
</ns1:d>
```

These few outputs and observations are emphasizing an important
principle: each outputted XML segment always contains the necessary
namespace declarations. In other words, the XML fragments are
_self-contained_.

## Disabling bubbling ##

In the second part of the file the namespace declaration was
defined lot earlier in the hiearachy than it was used. Lets inspect
how the namespace declaration behaves in the second part, when
the child elements are rebuilt from the command-line. Rebuilding
the first child element in the familiar manner:
```
fida output a2a:1
```

The output is:
```
<a xmlns:ns2="namespace2" id="a2a" rev="1">
  <b id="a2b" rev="1">
    <c id="a2c" rev="1">
      <d id="a2d" rev="1">
        <ns2:p>data in a2d</ns2:p>
      </d>
    </c>
  </b>
</a>
```

The namespace "ns2" is being declared at the root element of this
XML segment. Incidentally, this was the XML element in which the
namespace declaration was given in the original XML source file.

The namespace is not used until the XML element at the bottom
of this hierarchy is reached. In other words, the namespace declaration
is being introduced earlier than it is necessary, and it is what
the bubbling in action means.

The program has a command-line option which allows the user to turn
off the namespace declaration bubbling. Lets see how the same xid
looks when the namespace bubbling is turned off:
```
fida output a2a:1 -nobubble
```

The output is:
```
<a id="a2a" rev="1">
  <b id="a2b" rev="1">
    <c id="a2c" rev="1">
      <d id="a2d" rev="1">
        <ns2:p xmlns:ns2="namespace2">data in a2d</ns2:p>
      </d>
    </c>
  </b>
</a>
```

Once the bubbling is turned off, declaring namespaces have been delayed
up to the point where they are absolutely necessary.

## Using namespace declarations economically ##

Turning off the namespace bubbling resulted in delaying the namespace
declarations until they became necessary to produce valid XML.
This kind of "economical" behaviour, where nothing which isn't absolutely
needed isn't done, might feel desirable. However, in practice the XML files
mixing multiple namespaces are such that they use a certain namespace at
a certain depth. With such files the behaviour which at first felt
economic is actually very uneconomic, because the XML output will
be littered with redundant XML namespace declarations.

Lets study more concretely what this means. First, scrap the previous
repository by initializing a new repository:
```
fida init
```

Then, a suitably crafted XML file is needed for the demonstration.
Create a file called "`demo2.xml`" with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<csv:file id="data" xmlns:csv="csvfile" xmlns:r="row" xmlns:c="column">
  <r:row id="row1">
    <c:column id="row1/sex">Male</c:column>
    <c:column id="row1/age">32</c:column>
    <c:column id="row1/born">1981</c:column>
  </r:row>
</csv:file>
```

What makes this XML file indistinguishable from a real XML file
with respect to the XML namespace declarations is that all namespace
declarations have been placed into the root element. That's the way
people usually do. Why? It is probably because they like to maintain
their namespace declaration in just one, easily accessible place.
What would more natural than the root element? Declare the namespace
there, and it is usable anywhere in the document.

Next, the initial ingestion of the file is performed:
```
fida add demo2.xml
```

Re-examining the contents of the file after the ingestion doesn't
reveal any surprises. The revision numbers are inserted as expected,
and the namespace declarations haven't been touched.

The purpose was to show why disabling namespace bubbling isn't really
as economic as it was first thought. To see this, rebuild the root
element with the namespace bubbling turned off:
```
fida output data:1 -nobubble
```

The result is:
```
<csv:file xmlns:csv="csvfile" id="data" rev="1">
  <r:row xmlns:r="row" id="row1" rev="1">
    <c:column xmlns:c="column" id="row1/sex" rev="1">Male</c:column>
    <c:column xmlns:c="column" id="row1/age" rev="1">32</c:column>
    <c:column xmlns:c="column" id="row1/born" rev="1">1981</c:column>
  </r:row>
</csv:file>
```

Observe that the namespace declarations have been delayed as long
as it has been possible before they have become necessary. That
has caused the namespace "c" to be declared in each `<c:column>`
element separately.

Imagine writing that by hand. It doesn't feel very useful to repeat
the same XML namespace declaration over and over again. Also, in
case of big XML files that are mixing many namespaces the redundant
XML namespace declarations are littered densely all over. In such
cases the namespace declarations have actually become "noise" for the
human reader, and they have a significantly degrading effect on
the readability of the file's contents.

Thus, the XML namespace declaration bubbling is a very welcome addition
to post-processing phase of rebuilds. In fact, because it was considered to be so important, a decision was made to perform the bubbling by default.

## Next tutorial ##

In this tutorial the namespace bubbling was introduced. The next
tutorial considers the [different bubbling strategies](BubblingStrategiesTutorial.md) which can be used in namespace bubbling when dealing with conflicting namespace declarations.