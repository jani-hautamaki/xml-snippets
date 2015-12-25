# From revisions to versions #

This tutorial shows how the repository is switched from using
revision numbers to use three-level version numbers, ie. version
numbers such as "x.y.z".

## Initializing the repository ##

This time the preparation is the key. The repository is created
first in the usual way by exeucting
```
fida init
```

At this point the tutorial diverges from the typical scenario.
The repository is now initialized to a three-level version number,
by using the following command:
```
fida setversion 1.0
```

Now the repository is at "revision" number 1.0.1.

## The version numbering scheme ##

The version numbering scheme what is used in `fida` is as follows.
The version numbers have three digits separated by a dot. That is,
they have the format
```
<a>.<b>.<r>
```

here `<a>` and `<b>` are arbitrary digits decided by the user,
and `<r>` is the familiar revision number. The revision number `<r>`
is always increased automatically - just the same way as earlier.
However, the user controls how the major version number `<a>` and
minor version number `<b>` are advanced.

## Adding some data ##

As always, XML file is needed for the demonstration. Lets create
a file called `sample.xml` with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc">
  <title>my title</title>
    
  <section id="sect_a">
    <p>lorem ipsum</p>
  </section>

  <section id="sect_b">
    <p>dolor</p>
  </section>
</root>
```

Start tracking on that file by executing
```
fida add sample.xml
```

Lets look at the updated file. The file's contents are now:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" version="1.0.2">
  <title>my title</title>
    
  <section id="sect_a" version="1.0.2">
    <p>lorem ipsum</p>
  </section>

  <section id="sect_b" version="1.0.2">
    <p>dolor</p>
  </section>
</root>
```

Notice that `fida` is now using `@version` attribute instead of `@rev`
attribute to record the version numbers. Also, notice that the revision
number is still there, disguised as the last digit in the version number.

Lets modify the last `<p>` element by append few words to it:
```
    <p>dolor sit amet</p>
```

As usual, commit to the modifications by running
```
fida upate
```

Inspecting the file after the update reveals that the last `<section>`
element has received a new revision number into its `@version` attribute.

## Updating the version number ##

Imagine that now the version "1.0" is ready for freezing. The version
is frozen by advancing to the next version. This time just the minor
version number is increased. Running the command
```
fida incversion minor
```

increases the minor version number by one. Note: the major version number
can be increased by one similarly, just change the last parameter from
"minor" to "major". Also, the version can be explicitly set at any time
with the "`setversion`" command. This enables the version number to
be able to warp just as the user wishes.

It is time to modify the file again. Now it is turn for the first
`<section>` element to be modified. Its contents are changed into:
```
    <p>lorem ipsum dolor sit amet</p>
```

Save the file, and commit to the modifications by executing
```
fida update
```

Reopen the file, it should be looking now as this:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" version="1.1.5">
  <title>my title</title>
    
  <section id="sect_a" version="1.1.5">
    <p>lorem ipsum dolor sit amet</p>
  </section>

  <section id="sect_b" version="1.0.3">
    <p>dolor sit amet</p>
  </section>
</root>
```

## Technical notes ##

The version numbering scheme introduced above makes it possible to
keep using the revision number as the time point identifier. Consequently,
from the program's point of view, the major and minor version numbers
are just ballast / payload. It really doesn't do anything useful with
them.

This is rather clearly demonstrated that you search for any major/minor
version number combination of an element as long as the revision number
is sane, and you get the element corresponding to it. More concretely,
all of the following commands return the same XML element:

```
fida output sect_a:2
fida output sect_a:1.0.2
fida output sect_a:2.99.2
```

The program does not limit how the major and minor version numbers
propagate in time. It is therefore possible to warp "backwards", for
instance from 2.0 to 1.5. However, this is something that is probably
not very desirable in production-quality programs.

## Next tutorial ##

The version number issues are now put aside. The next tutorial changes to the focus to XML namespaces, and how they are handled. Specifically, the tutorial explains the concept of [bubbling XML namespace declarations](NamespaceBubblingTutorial.md).

