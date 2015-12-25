# Introduction on element-wise XML revision control #

Before beginning you need to make sure that `fida` is included in PATH environment variable. The [installation guide](QuickInstallGuide.md) shows how to do this.

## Simple usage ##

### Command: init ###
The first thing to do is to create a new repository to the current working directory. A new repository is created by
```
fida init
```

Next, use a text editor to create an XML document `test.xml` with very simple contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root">
  <title>My test document</title>
</myroot>
```

The attribute `@id` is considered to be the "name" of the root node.  _NOTE: document's root element must always have an `@id` attribute_.

### Command: add ###
Next, the `fida` is commanded to start tracking this particular XML document. The file is "added" to the tracking:
```
fida add test.xml
```

Contrary to typical source code revision control systems such as Subversion or Mercurial, this is just a proof-of-concept program, and it does not include a "staging area". Instead, _every action modifying the repository is committed immediately_, and therefore the program does not have a separate "`fida commit`" instruction.

The forementioned "`fida add`" command should finish by outputting
```
[...]
Processing file test.xml
Committed revision 1
```

When the addition of an XML file was committed, the file was processed and modified by `fida`. Lets re-open the file in a text editor to see what happened. The contents of the XML document `test.xml` should now be
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root" rev="1">
  <title>My test document</title>
</myroot>
```

The root element with `@id`="root" has a new attribute called `@rev`, and its value has been set to "1". Within a tracked XML document, _each XML element with an `@id` attribute is tracked separately!_

Lets modify the contents of the `<title>` element. Change the title, for instance, to "My revised test document". Now the contents of the file `test.xml` should be
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root" rev="1">
  <title>My revised test document</title>
</myroot>
```

### Command: status ###

After saving the modified XML document, it would be nice to get some sort of status display of the tracked files out of `fida`. Similarly to typical revision control systems, execute the command
```
fida status
```
to see the status of the tracked files.

The command should output:
```
r1     M    test.xml
Current revision 1
```
where the flag "`M`" means that the file has been modified since the last time its state was committed. The revision number when the latest commit of the file has been made is expressed by "`r1`", meaning revision one.

### Command: update ###

Lets commit the current state of the tracked files. Execute the command
```
fida update
```

The output of the command should be:
```
[...]
Processing file test.xml
Committed revision 2
```

Now re-open the XML document `test.xml` to see its current contents. The contents of the file should be
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root" rev="2">
  <title>My revised test document</title>
</myroot>
```

The attribute `@rev` of the identified XML element has been updated to reflect the revision number of the element´s contents.

## More advanced usage ##

Lets complicate the XML document a bit. Re-open the file `test.xml` again in a text editor, and append its contents so that the resulting file is:
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root" rev="2">
  <title>My revised test document</title>
  
  <abstract id="abstract">
    <p>The purpose of this document is to demonstrate "fida".</p>
    <p>Fida is an element-wise XML document revision control system.</p>
  </abstract>
  
  <section id="sect_intro">
    <p>This section will introduce the basic principles of fida.</p>
    <subsection id="subsection_download">
      <p>First, you need to download fida.</p>
    </subsection>
  </section>
</myroot>
```

Save the file, and run `update`:
```
fida update
```

The output of the command should be:
```
[...]
Processing file test.xml
Committed revision 3
```

The current contents of the file `test.xml` should now be:
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root" rev="3">
  <title>My revised test document</title>
  
  <abstract id="abstract" rev="3">
    <p>The purpose of this document is to demonstrate "fida".</p>
    <p>Fida is an element-wise XML document revision control system.</p>
  </abstract>
  
  <section id="sect_intro" rev="3">
    <p>This section will introduce the basic principles of fida.</p>
    <subsection id="subsection_download" rev="3">
      <p>First, you need to download fida.</p>
    </subsection>
  </section>
</myroot>
```

Lets modify the abstract a little bit. Lets remove first `<p>` element, so that there is only one paragraph left. These are the contents of the updated `<abstract>` element:
```
  <abstract id="abstract" rev="3">
    <p>Fida is an element-wise XML document revision control system.</p>
  </abstract>
```

Save the modified file, and run update:
```
fida update
```

Re-open the file, and now the contents are:
```
<?xml version="1.0" encoding="UTF-8"?>
<myroot id="root" rev="4">
  <title>My revised test document</title>
  
  <abstract id="abstract" rev="4">
    <p>Fida is an element-wise XML document revision control system.</p>
  </abstract>
  
  <section id="sect_intro" rev="3">
    <p>This section will introduce the basic principles of fida.</p>
    <subsection id="subsection_download" rev="3">
      <p>First, you need to download fida.</p>
    </subsection>
  </section>
</myroot>
```

The revision number of the element `<abstract>` has been updated, and consequently the revision number of `<myroot>` got updated too. This is because the element `<abstract>` belongs to the contents of `<myroot>` element. _Every element (except the root) belongs to the contents of its parent element_. The contents of `<section>` and `<subsection>` elements were not modified, and consequently their revision numbers weren't updated.

### Command: lifelines ###

At this point there is some interesting data available in the repository. For instance, one might be interested in seeing how the various XML elements have evolved. To see the "lifelines" of each identified XML element ever ingested by `fida`, run
```
fida lifelines
```

The output of the `lifelines` command should be
```
1     root:1 > r2 > r3 > r4
2     abstract:3 > r4
3     subsection_download:3
4     sect_intro:3
```

the number at the beginning of each row identifies the particular lifeline. The second piece of data is the element's `@id` attribute combined with the revision number. This revision number tells in which commit the element appeared for the first time. For example, "abstract:3" means that the element with `@id="abstract"` appeared for the first time in the third revision. After the element's initial revision, there is a sequence of further revision separated with `>` characters. These are revision numbers in which the element was modified. So, for instance, "`abstract:3 > r4`" means that after the element was introduced at revision three, it was modified at revision four.

### Command: commitinfo ###

The repository database was created and initialized with "`fida init`" command. This command created the file `fida.xml` which is the repository itself. Deleting that file will lose all the revision tracking information.

The repository database can be considered to be simply a list of commit objects. A _commit object_ includes all the information that were appended to the repository database as a result of a repository-modifying action. Information which is already included in the repository is never deleted or modified. Consequently, a commit object is there forever frozen once it has been included in the repository.

There is a command, which enables to see what information were included in each individual commit object. Lets see what information were included in the last commit set:
```
fida commitinfo 4
```
where the number four is the number of commit object, ie. the revision number.

The output of the command should be:
```
Nearest match is xid=#commit!4b0da4d1:4
Commit details
   Commit xid:       #commit!4b0da4d1:4
   Date:             2013/02/16 15:37:38
   Author:           hautamaki
   Layout:           1 files
   Nodes:            2 nodes

   File #1           r4    test.xml

   Node #1           abstract:4
   Node #2           root:4
```

The most interesting information is probably the list of files which were updated and which nodes (= identified XML elements) within those files received a new revision. The example output tells that only the file `test.xml` was updated, receiving a revision four ([r4](https://code.google.com/p/xml-snippets/source/detail?r=4)), and XML elements with `@id="abstract"` and `@id="root"` received new revisions of their contents.

### Command: fileinfo ###

To dig a little deeper into a commit object, there is another command which retrieves more detailed information regarding a specified file. Lets see more detailed information about the file `test.xml` in the fourth commit object. This can be done with the command
```
fida fileinfo 4 test.xml
```

This command should result in output:
```
Nearest match is xid=#file!80bb25ff:4
File details
   File xid:         #file!80bb25ff:4
   Path:             test.xml
   Digest:           md5:8bc656890168b2a7d0de241875f79f9e
   Unexpands:        0
   Previous xid:     #file!8f325460:3
   Next xid:         <no next>
   Commit xid:       #commit!4b0da4d1:4
   Commit date:      2013/02/16 15:37:38
   Commit author:    hautamaki
   Earliest rev:     #file!e1542356:1
   Latest rev:       <this>
```

These are various details regarding the file itself within the fourth commit object. If you would like to see the whole repository database model, just take a look at the source code file "`xml-snippets/src/xmlsnippets/fida/Fida.java`". This file contains all data structures as very simple Java classes. The file should be rather easy to follow.

### Command: output ###

Naturally, the program can resolve a specified `id:rev` pair, called _xid_, to the corresponding XML element. Lets resolve the element with `@id="abstract"` and `@rev="4"` into its contents:
```
fida output abstract:4
```

Here, the string "`abstract:4`" is called the _xid_ of the element. Xids are simply a combination of the `@id` attribute with the `@rev` attribute, separated by a colon '`:`'.

The output of the program should tell that the xid "`abstract:4`" corresponds to the XML element
```
<abstract id="abstract" rev="4">
  <p>Fida is an element-wise XML document revision control system.</p>
</abstract>
```

If you resolve the xid "`root:4`" it should return the contents of the file `test.xml`. Try it out!

### Command: output2 ###

The previously introduced command, `fida output`, does something which is called _rebuilding_. The command _rebuilds_ the specified xid. The verb "rebuild" should point to the idea that the data related to the specified xid is not actually stored as such into the repository database, but instead the result is somehow a product of a building process.

To see what the raw data present in the repository related to a specified xid is, use a different command. Lets see what is the raw data stored within a commit set about the xid `root:4`:
```
fida output2 root:4
```

Now the output should be:
```
<myroot id="root" rev="4">
  <title>My revised test document</title>
  <abstract ref_xid="abstract:4" link_xid="#link!8ce96c8a:4" />
  <section ref_xid="sect_intro:3" link_xid="#link!0854f411:4" />
</myroot>
```

This is rather different from the results of `fida output root:4`! The command begins to reveal the internals of the element-wise XML revisioning. What can be seen from the output is that _normalization_  has been performed to the identified XML elements.

The element `<section>` contains an attribute `@ref_xid="sect_intro:3"`. This means that the element should be replaced with the contents of the xid `sect_intro:3`. Lets see how that xid looks like:
```
fida output2 sect_intro:3
```

The result should be:
```
<section id="sect_intro" rev="3">
  <p>This section will introduce the basic principles of fida.</p>
  <subsection ref_xid="subsection_download:3" link_xid="#link!15c8bab7:3" />
</section>
```

This shows again the fact that _(x)identified XML elements have been normalized_. The unidentified XML element `<p>` appears as such within the element, but the (x)identified child XML element `<subsection>` is represented indirectly through a link.

## Conclusion ##

This page demonstrated the basic usage of `fida`, the proof-of-concept program for element-wise XML revision control. This tutorial has probably given rise to many interesting questions regarding many different scenarios. The reader is encouraged to play with the tool to see how it works in different scenarios.

## Next tutorial ##

The next tutorial is about [duplicating and migrating XML elements](DuplicateMigrateTutorial.md). In that tutorial copies of the XML files are ingested into the repository, and duplicated content is modified.