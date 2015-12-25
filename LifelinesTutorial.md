# Intertwined lifelines #

## Preparations ##

Start by creating a new repository:
```
fida init
```

Next, create a simple XML file called "`test.xml`":
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc">
  <title>Example document</title>
</root>
```

Save the file, and then start tracking the file by issuing a command
```
fida add test.xml
```

Run "`fida lifelines`" to see that a lifeline was created for
the root element with `@id="doc"`:
```
fida lifelines
```

Output:
```
1     doc:1
```

Introducing an XML element with a defined `@id` but without a `@rev`
attribute is considered as a request to allocate and start
a new _lifeline_.

Lets allocate and start another new lifeline in our repository.
Add a new `<section>` child element with an `@id` to the root element
so that the contents of the file are:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" rev="1">
  <title>Example document</title>
  <section id="sect_a">
    <p>lorem</p>
  </section>
</root>
```

Save the file, and run update:
```
fida update
```

Verify that a new lifeline was created by running the lifelines command
again:
```
fida lifelines
```

Output:
```
1     doc:1 > r2
2     sect_a:2
```

The output shows that now there are two lifelines. The first one
begins with xid "`doc:1`" and continues to xid "`doc:2`". The second
begins with xid "`sect_a:2`".

Revise the element `<section>` by appending its contents, that is,
by appending its `<p>` child element into:
```
    <p>lorem ipsum</p>
```

Save the modifications, and run "`fida update`".

Lets view the lifelines once again:
```
fida lifelines
```

Output:
```
1     doc:1 > r2 > r3
2     sect_a:2 > r3
```

Next, the root element is revised. Lets do that by modifying the
the `<title>` element into `<title>untitled</title>`.

Run "`fida update`". Now the contents of the file should be:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" rev="4">
  <title>untitled</title>
  <section id="sect_a" rev="3">
    <p>lorem ipsum</p>
  </section>
</root>
```

Running "`fida lifelines`" should give the following output:
```
1     doc:1 > r2 > r3 > r4
2     sect_a:2 > r3
```

## Creating yet another lifeline? ##

Now I can ask an interesting: should it be possible to request
a new lifeline (that would be third) for an XML element
with an `@id="sect\_a"? Lets make the question more concrete by
presenting an example of the file's possible contents:

```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" rev="4">
  <title>untitled</title>
  <section id="sect_a" rev="3">
    <p>lorem ipsum</p>
  </section>
  
  <anothersection id="sect_a">hmm???</anothersection>
</root>
```

The element `<anothersection>` would receive initial revision of `5`,
and consequently, the third lifeline would begin with the xid "`sect_a:5`",
which has not been assigned to any XML element yet.

If this is possible, then the xids "`sect_a:3`" and "`sect_a:5`" would
represent differing lifelines, even though by looking at their `@id`
values it is easy to mistake them to be the same XML element at different
points in time.

Well, should it be possible? There actually isn't any technical
restrictions for making it possible. However, it has been a design
decision not to allow it. You can test it out by modifying the file
into what was shown above, and then executing

```
fida update
```

which will result in an error message:
```
The element /root/anothersection with xid=sect_a:5 cannot be added, because
it would hide an older, but still active xid=sect_a:3 (#node!dda36d41:3)
```

Allowing it would immediately "freeze" the older XML element with xid
"`sect_a:3`" to its current state. No further modification could be
allowed, because then the two lifelines would become intertwined,
sort of, and possibly even clashing at some point if they were
modified simultaneously prior to `update`.

## Messing with the lifelines ##

Lets continue the example further. Lifelines can be "ended" implicitly,
if the xid representing the tip of a lifeline is not no longer present in
the files belonging to the repository's head. Here, the meaning of
"ending" a lifeline shouldn't be taken too strictly, because are
no technical restrictions why the tip of a lifeline couldn't be continued
with a new xid using a different `@id`.

This so-called "ending" of a lifeline is now demonstrated. Lets
modify the file by removing `<section>` element completely. The file
should now look like this:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" rev="4">
  <title>untitled</title>
</root>
```

Commit to the new state of the file by executing
```
fida update
```

Now, the xids "`sect_a:2`" and "`sect_a:3`" are not present in the
head revision of the repository. Only xid that exists in the repository
head is just "`doc:4`" which is the root element. By glancing the
layout of the repository's head there just aren't any references
to any XML element with an `@id="sect_a"`.

Next, a new lifeline is requested to be allocated and started by
introducing a new XML element into the document. The file's contents
are modified into:
```
<root id="doc" rev="4">
  <title>untitled</title>
  <subsection id="sect_a">
    <h2>header</h2>
  </subsection>
</root>
```

Save the file, and run "`fida update`". The command succeeds.

Looking at the updated contents of the file will show that the newly
introduced XML has received `@rev="6"`. So, is this a new revision
of the XML element which was previously known with xid "`sect\_a:3"?

To answer to the question, run
```
fida lifelines
```

which will show:
```
1     doc:1 > r2 > r3 > r4 > r5 > r6
2     sect_a:2 > r3
3     sect_a:6
```

There are now two different lifelines, which have used the same
`@id` value at some point in the past. To make things still little
more confusing, it is actually possible to have XML elements
from two different lifelines designated by the same "@id" even in
the same document.

Lets dig out what the older "`sect_a:3`" was. This is done by
rebuilding the element:

```
fida output sect_a:3
```

The program outputs:
```
<section id="sect_a" rev="3">
  <p>lorem ipsum</p>
</section>
```

Lets copy-paste this into the current file somewhere. For instance,
the contents could be modified into:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" rev="6">
  <title>untitled</title>
  
  <subsection id="sect_a" rev="6">
    <h2>header</h2>
  </subsection>

  <section id="sect_a" rev="3">
    <p>lorem ipsum</p>
  </section>
</root>
```

Save, and run "`fida update`" as usual. The root element will receive
a new revision number, `@rev="6"`, but nothing else changes. Now the
document has two XML elements belonging into different lifelines,
but both having the same id. Admittedly, this might be quite confusing
to anyone, because the human mind will almost immediately consider
these two XML elements to be two different revisions of a single
XML element.

To some extent, the situation is similar to what occurs sometimes with
source code revision control systems when a file "`Adapter.java`" is first created, then deleted, and finally a new "`Adapter.java`" is created many revisions later. However, the situation above is
different, because it was possible to "resurrect" a forgotten revision
of the file into the repository's head layout, and both revisions of
the same file can exists simultaneously in the head.

## Further messing up with the lifelines ##

Modifying the xid `"sect_a:6"` isn't really a new scenario. It will produce a new xid to the head of that lifeline. However, modifying the xid "`sect_a:3`" is more interesting case.

Modify the element, and then run "`fida update`". This will end with an error message:
```
The element /root/section with xid=sect_a:3 cannot be revisioned, 
because it would hide a newer and active xid=sect_a:6 (#node!ce945dc0:6)
```

So apparently it is not possible. Well, at least not directly.

However, the previous trick about "ending" a lifeline can be applied again. Remove the xid "`sect_a:6`" from the file completely. The file should now look like
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="doc" rev="7">
  <title>untitled</title>

  <section id="sect_a" rev="3">
    <p>lorem ipsum</p>
  </section>
</root>
```

Run `fida update`. Then, modify the contents of the xid "`sect_a:3`". For instance, change the `<p>` element into `<p>xyz</p>`. Save, and then run "`fida update´" again.

Little surprisingly, the command succeeds this time. Looking at the file reveals that the `"@id="sect_a"` element has received a new revision, `rev="9"`.

What has happened with the lifelines? Lets execute "`fida lifelines`"
which results in:
```
1     doc:1 > r2 > r3 > r4 > r5 > r6 > r7 > r8 > r9
2     sect_a:2 > r3 > r9
3     sect_a:6
```

There are now two different lifelines that alternate their usage of the
`@id="sect_a"`. This is definitely somewhat confusing. To make it even more confusing, it is actually possible to repeat the procedure.

First, forget "`sect_a:9`" from and resurrect "`sect_a:6`" to the current head of the repository. Then, modify the resurrected "`sect_a:6`" element. Now, looking at "`fida lifelines`" gives you
```
1     doc:1 > r2 > r3 > r4 > r5 > r6 > r7 > r8 > r9 > r10 > r11
2     sect_a:2 > r3 > r9
3     sect_a:6 > r11
```

Depicting this information more graphically reveals how the lifelines are now intertwined:
```
                    lifeline-2
                  ______________     
                 /              \   /      
sect_a   *----->*       *-       >*-      >*
         r2   r3       r6 \      r9      / r11
                           '------------' 
                             lifeline-3
```

This is probably the most complicated scenario that is possible to create.

If there was a feature allowing the `@id` attributes to be renamed, then the following scenario would be equally possible:

```
       r1     r2       r3       r4
sect_a *------>*------->*--\ /-->*
                            X
sect_b         *------->*--/ \-->*
```

This is a scenario that is possible with source code revision control systems. Two source code files have been renamed into each others (by using a third file name). The result is that the same `@id` is again pointing to different lifelines at different times. _An `@id` does not identify a lifeline uniquely_.

## Conceptual framework ##

It is time to sum up and conclude this tutorial with a conceptual framework.

Lifeline is an abstract concept that does not have static id. Instead, a lifeline is identified indirectly at every time point by the element's `@id` attribute, which may change over time.

Each possible value for `@id` is called a _lifeline designator_.

When an element at a given time point is using a certain lifeline designator, then that lifeline designator is said to be _leased_.

The adjective "leased" should reflect the idea that the lifeline designator, ie. the value of the `@id`, is only borrowed to designate a certain lifeline. The lease might end, and then the lifeline designator might be leased to another lifeline later on. This is exactly what was seen in the preceding examples: the same lifeline designator, `@id="sect_a"`, was leased to different lifelines depending on the particular time point in question. However, at every time point, there was at most one lifeline leasing the given lifeline designator.

The is the rule: at every time point, there is at most one leaser for a given lifeline designator. If this invariant is not respected, then a xid, such as "sect\_a:6", would be ambiguous. It might refer to two or more lifelines at the same time indirectly by the XML elements of those lifelines to which it refers to.

## Next tutorial ##

The next tutorial is about [handling XML files with already-existing revision numbers](ForcingRevsTutorial.md).