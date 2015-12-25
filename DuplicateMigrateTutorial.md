# Duplicating and Migrating XML elements #

## The usual stuff ##

Start by creating a new repository:
```
fida init
```

This creates a new file called "`fida.xml`" which is the actual repository.

Next, create a simple XML file using a text editor:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="exampledoc">
  <title>Example document</title>
    
  <section id="sect_a">
    <p>this is the first section, section A.</p>
  </section>
    
  <section id="sect_b">
    <p>this is the second section, section B.</p>
  </section>
</root>
```

Save this into a file named "`sample.xml`". After saving the file, make
`fida` aware of the file that should be tracked by issuing an "`add`"
command for the file:
```
fida add sample.xml
```

Now the contents of the "`sample.xml`" should look like:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="exampledoc" rev="1">
  <title>Example document</title>
    
  <section id="sect_a" rev="1">
    <p>this is the first section, section A.</p>
  </section>
    
  <section id="sect_b" rev="1">
    <p>this is the second section, section B.</p>
  </section>
</root>
```

So far, everything should be similar from the introduction tutorial.
Lets make a small modification to the section B. The contents of
the element `<p>` are changed to:
```
    <p>this is still the second section, section B.</p>
```

The word "still" was inserted. Okay, lets "commit" to the modified
version of the file. This is done by executing the "`update`" command:
```
fida update
```

Now, the contents of the file "`sample.xml`" should be
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="exampledoc" rev="2">
  <title>Example document</title>
    
  <section id="sect_a" rev="1">
    <p>this is the first section, section A.</p>
  </section>
    
  <section id="sect_b" rev="2">
    <p>this is still the second section, section B.</p>
  </section>
</root>
```

## Creating a copy ##

What would happen if a copy was made of the file and the copy
was to be added? Lets find out! Make a copy of the file "`sample.xml`"
first:
```
copy sample.xml sample2.xml
```

and then start tracking the file:
```
fida add sample2.xml
```

The program will say that "`Commited revision 3`", but you can't find
revision number 3 from neither file. This happens because _the contents
of identified elements have not been modified_, and consequently there
is no need to revise the identified elements. However, it is a different
story for the repository: in the previous state there was only one file
being tracked, namely "`sample.xml`", but now there are two. In conclusion,
the state of the repository was changed, but not the state of any
committed XML element with an identity.

This is an interesting situation: it seems that each of the identified
XML elements are stored twice into the repository somehow. In reality,
this is not the case. What was recorded into the repository was
only the knowledge that file "sample2.xml" is a _manifestation_ of
a commited XML element known as "exampledoc:2" (remember, this is a xid,
ie. an id with a revision number).

## Modify the copy ##

Now it gets more interesting. Modify the XML element with `@id="sect_b"`
in the original file "`sample.xml`" by setting the contents of
the element `<p>` to:
```
    <p>this is the edited first section in sample.xml</p>
```

Save the file, and run "`fida status`" just to see what it tells you.
It should tell that the file "`sample.xml`" has been modified by
displaying flag "`M`", and it should also say that the file "`sample2.xml`"
has not been modified by displaying"`OK`" there.

Commit to these files by executing:
```
fida update
```

Examine the contents of the original file `"sample.xml". They should be:
```
<?xml version="1.0" encoding="UTF-8"?>
<root id="exampledoc" rev="4">
  <title>Example document</title>
    
  <section id="sect_a" rev="4">
    <p>this is the edited first section in sample.xml</p>
  </section>
    
  <section id="sect_b" rev="2">
    <p>this is still the second section, section B.</p>
  </section>
</root>
```

Examine also the contents of the copied file "`sample2.xml`". Contents
should be the as they were when the file was copied. More importantly,
note that root elements of the files are "exampledoc:4" and "exampledoc:2"
for "`sample.xml" and for "`sample2.xml`", respectively.

## You cannot create branches ##

What this means is that the copy "`sample2.xml`" has an older revision
of the XML element known as "exampledoc" than the other file.

```
                      (sample2.xml)  (sample.xml)
             r1            r2            r4
exampledoc:   *------------>*------------>*
```

Modifying the `<title>` element in the file `sample2.xml` would
basically mean _branching_ for the root element.

```
                                     (sample.xml)
             r1            r2            r4
exampledoc:   *------------>*------------>*
                            \
                             `-------------------->*
                                                  r5??
                                             (sample2.xml)
```

This is a bit of a problem, if the revisions were to be numbered
sequentially. Should the new revision be numbered as rev="5",
it would get confusing after modifying the "[r4](https://code.google.com/p/xml-snippets/source/detail?r=4)" of the element.
And even worse, what should happen when the both were to be modified
simultaneously? In the worst case there would be two different
XML elements to be revisioned with the same new revision number.
That would simply contradict the idea that the XML elements are
uniquely identified by their xids (ie. by the tuples (id,rev)).

One way to work around this issue would be to assign a new, unique id
to the branched revision, but that doesn't sound very tempting.
As this is just a simple proof-of-concept program, the branching
has been made illegal. You can try to modify the file "`sample2.xml`"
somehow and then command "`fida update`". This will always result
in the message
```
The modified element /root constitutes a branch of xid=exampledoc:2
```

and the commit has been aborted.

## Instead, you can migrate ##

In these kinds of situations the manifestation containing older
revisions of identified XML elements can be brought to up-to-date
by a procedure called _migration_. Lets migrate all elements,
which have never revisions to their newest revisions. Execute
```
fida migrate
```

(Execute also "`fida update`" after the migration just
as the program asks you to do)

NOTE: This migration procedure is just a simple (=dummy) migration
procedure, and it is not implemented in a proper way, but it works
in the simplest scenarios.

The command outputs information regarding what migration procedures
were carried out:
```
sample2.xml: 1 migrations
                exampledoc:2 -> exampledoc:4
```

This simply tells that in the file "`sample2.xml`" only 1 migration
was carried out, and it was replacing the manifestation of "exampledoc:2"
with a manifestation "exampledoc:4".

It is _**a**_ manifestation. This becomes rather clear when comparing
the files side-by-side. The files are not identical, even though they
seem to have same contents.

## Theoretically... ##

Theoretically, an XML element can be thought as _an equivalence class_,
and a manifestation of an XML element is simply _a member_ of this
equivalence class. Therefore, a single XML element may have many
superficially different manifestations.

Modify the migrated file "`sample2.xml`" by inserting additional
newlines. Make it look identical to the other file. Now, running
"`fida status`" clearly indicates that the file "`sample2.xml`" has
been modified. Execute "`fida update`" to commit to this modified
version. The program outputs, that it just "Commited revision` `6".

Reopen the file "`sample2.xml`" to see what has happened. It actually
hasn't been touched at all. This is because the manifestations of
the XML elements in that file still belong to the same equivalence
classes as they did before inserting those additional newlines.
Consequently, the program doesn't consider them as modified.

## Next tutorial ##

The next tutorial is about [lifelines in detail](LifelinesTutorial.md). In that tutorial, the lifelines are certainly mixed up to the point of confusion.