# Renaming, Branching, and Merging #

## Introduction ##

It is now possible to branch, merge and rename xidentified XML elements. This tutorial shows how these features can be used.

Branching XML elements is something that can described as branching _data_,
as opposed to branching _code_. Branching data is fundamentally somewhat different than branching source code, which is readily handled by software such as svn, hg, git etc. This tutorial will demonstrate how branching data differs from branching source code.

So far the author has not conceived any realistic use case for merging xidentified XML elements, even though the program makes it possible to do so.

## Basics as usual ##

Start by deleting any previously existing repository by manually removing the file "`fida.xml`". For instance, in Windows this is achieved with the command

```
del fida.xml
```

A new repository can now be created in the usual way,

```
fida init
```

## The document ##

Lets begin with a simple XML document. The document is a code list for some the basic colors.

```
<Codelist id="colors">
  <Color id="blue">Blue color</Color>
  <Color id="red">Red color</Color>
  <Color id="green">Green color</Color>
</Codelist>
```

Save the XML document into file "`colors.xml`". Start tracking the file,

```
fida add colors.xml
```

## Renaming a xid ##

Re-open the file. The purpose is to transform the red color into a more specific shade of red. Edit the file into the following state:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="1">
  <Color id="blue" rev="1">Blue color</Color>
  <Color id="ruby@red" rev="1">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

Notice the contents of the `@id` attribute. The attribute involves a special syntax which can used to achieve branching and merging. Here we are just renaming the id "`red`" into "`ruby`".

When the attributes `@id` and `@rev` are being employed for the xid specification, the special syntax for renaming xids is:

```
  <AnyElement id="new_id@original_id" ...>
```

That should be rather self-explanatory. The "`new_id`" specifies the target id for the renaming. Target id is followed by the at sign (`@`) and the original id of the element.

Run update first,
```
fida update
```

If you now re-open the file again, you will see that
the `@id` attribute has changed,
```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="2">
  <Color id="blue" rev="1">Blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

Then, inspect what the lifelines command shows you,

```
fida lifelines
```

The command should give you the following output,

```
1     blue:1
2     red:1 - ruby:2
3     green:1
4     colors:1 - r2
```

This output tells you that the successor of "`red:1`" is "`ruby:2`". That is, the id `red` was renamed into `ruby` while moving from revision one to revision two.

## Creating a branch ##

Lets do something more advanced which demonstrates true branching. An example for the branching will be splitting the id "`blue`" into two different, but more detailed colors.

At the same time we'll be demonstrating the characteristic properties of data, as opposed to code. Namely, the possibility of having multiple copies of the same item in the same file.

So, to begin with, re-open the file "`colors.xml`" and duplicate the line defining the color blue. After modifications, the file should look as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="2">
  <Color id="blue" rev="1">Blue color</Color>
  <Color id="blue" rev="1">Blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

This is completely legal to do. It is okay to have multiple instances of the same `@id` as long as their contents are identical. A duplicate instance does not modify the instance itself. It changes only the containing element.  Consequently, the `<Codelist>` element above will be a new revision, but the `<Color>` elements for blue will be untouched.

This situation would be more natural, if the different instances were in different files. Here the two instances are put in the same file for convenience and simplicity.

Having duplicate instances is something that you cannot do with source code. It is not possible to have two instances of the same function, even though the definitions were identical.

Okay, lets run update and see this all in practice.

```
fida update
```

Re-open the file, and you'll see that the `<Color>` elements for blue were not touched, and the `<Codelist>` did indeed receive a new revision.

It is possible to modify both `<Color>` elements for blue independently of each other. That is exactly what will be done here. For the purposes of this demonstration and for gaining an insight into revision tracking of XML elements, it is first demonstrated what happens if branching feature is not engaged.

To do this, both `<Color>` elements with `id="blue"` are edited separately. The descriptions of the  former and latter instance are changed into "Powder blue color" and "Liberty blue color" respectively. The contents of the resulting file are as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="3">
  <Color id="blue" rev="1">Powder blue color</Color>
  <Color id="blue" rev="1">Liberty blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

Now, this creates an unsolvable problem for `fida`. Which one of the instances of "`blue:1`" is the real next revision for the lifeline "`blue`"?. The program will complain when trying to run update,

```
fida update
```

This will result in the following error:

```
Processing file colors.xml
The updated element /Codelist/Color[2] is inconsistent with an already recorded element having the same xid=blue:4
```

This error message should give you an idea that there are multiple instances of an element with xid "`blue:4`" (ie. the next revision for "`blue:1`"), and their contents differ. The XML elements for "`blue:4`" have ambiguous contents, which is not acceptible. The retrieval of XML elemnent "`blue:4`" should procude unambiguous XML element, which is why the program cannot accept any ambiguities for its contents.

The solution is to _branch_ the xid "`blue:1`" into two different xids.
To achieve this, the special syntax for `@id` attributes is employed again.
Re-open the file, and edit the `@id` attributes so that they will be renamed differently. The first instance is renamed into "`powder_blue`", and the second instance is renamed into "`liberty_blue`".

After modifications, the file should have the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="3">
  <Color id="powder_blue@blue" rev="1">Powder blue color</Color>
  <Color id="liberty_blue@blue" rev="1">Liberty blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

Now the update should run without errors,
```
fida update
```

What we just did was that we created a branching for the xid "`blue:1`". This xid was branched into two different xids.

It is possible to verify the branching with the lifelines command,

```
fida lifelines
```

The output should be

```
1     blue:1 :< (powder_blue:4 liberty_blue:4)
2     red:1 - ruby:2
3     green:1
4     colors:1 - r2 - r3 - r4
5     ... powder_blue:4
6     ... liberty_blue:4
```

The special notation "`:<` (xid1 xid2 ...)" means the current lifeline branches at that point to multiple other xids. These xids are then displayed in their own individual lifelines. In the example above, the lifelines number five and six are the nodes that were branched off from the lifeline number one. These branched off xids may now evolve independently.

## The new command: `nodeinfo` ##

Another interesting way to inspect the evolution of a single lifeline is to use a command that was introduced simultaneously with the branching features. This command is "`nodeinfo`" which examines the commit object.

The syntax for the command is simple:

```
fida nodeinfo <xid>
```

So in the case of our example, it is interesting to see the nodeinfo for the xid "`blue:1`" which branches off to different xids. To see the nodeinfo, execute command

```
fida nodeinfo blue:1
```

The output of this command is

```
Node details
   Node xid:         #node!7ce7b2d4:1
   Payload xid:      blue:1
   Commit xid:       #commit!89e71fb0:1
   Date:             Fri Oct 10 21:15:22 EEST 2014
   Author:           hautamaki

   Next #1           powder_blue:4
   Next #2           liberty_blue:4
```

The successors for the xid "`blue:1`" are listed and enumerated in the end. If there were any predecessors, they would have been listed and enumerated similarly before the successors.

## Continuing a branched node ##

It is actually possible to continue the lifeline of the xid "`blue:1`". To achieve this, the node must reintroduced into the XML document again. So start by looking up what were the exact contents of this xid by querying the database with command

```
fida output blue:1
```

The results tell that the xid "`blue:1`" was an XML element with the following contents:

```
<Color id="blue" rev="1">Blue color</Color>
```

This is reintroduced into the current "`colors.xml`" file by just copy-pasting it to the contents. After copy-pasting the XML element and correcting the indentation, the contents of the file should be as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="4">
  <Color id="blue" rev="1">Blue color</Color>
  <Color id="powder_blue" rev="4">Powder blue color</Color>
  <Color id="liberty_blue" rev="4">Liberty blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

It is possible to run "`fida update`" as this stage if you like. It is, however, also possible to just edit the copy-pasted node directly, and that is what will be done here. After copy-pasting the node edit the contents of the node:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="4">
  <Color id="blue" rev="1">Blue color (hex triplet #0000FF)</Color>
  <Color id="powder_blue" rev="4">Powder blue color</Color>
  <Color id="liberty_blue" rev="4">Liberty blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

Save the file, and now run update:

```
fida update
```

It is interesting to see how the lifelines look now. Display the lifelines

```
fida lifelines
```

The command should output

```
1     blue:1 :< (powder_blue:4 liberty_blue:4) - r5
2     red:1 - ruby:2
3     green:1
4     colors:1 - r2 - r3 - r4 - r5
5     ... powder_blue:4
6     ... liberty_blue:4
```

The lifeline for xid "`blue:1`" (number one) shows nodes "`powder_blue:4`" and "`liberty_blue:4`" were branched off. This is familiar from the previous analysis. However, the lifeline continues and shows `r5`, which means that the next step in the evolution of "`blue:1`" is "`blue:5`".

If you look up "`fida nodeinfo blue:1`" you will see that it has now three successors, one of them being "`blue:5`".

Those xids, which have an id that does not match to any of their predecessors' id, are considered branch offs. Conversely, if a xid has multiple successors, and one of them has the same id, it is considered as the continuation of the current lifeline.

## Creating a merger ##

There are two different options for creating a merger. The first option is to produce a completely new xid by renaming two or more previously existing xids into a non-existing xid. Of course, the contents of the renamed nodes must become equivalent too. The second option is to merge a node into an already existing node. Both cases will be demonstrated.

### Creating a merger: method 1 ###

We'll merge "`powder_blue:4`" and "`liberty_blue:4`" into a common "`light_blue`". We'll do this in two steps. The first step is to equate the contents of the nodes, which is a necessary condition for the merger. The second step is to rename both xids into a common new name using the notation introduced earlier. Merging will not succeed unless the renamed nodes have identical contents.

That said, re-open the file and make the forementioned modifications. After the modifications the file should look as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="5">
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
  <Color id="light_blue@powder_blue" rev="4">Light blue color</Color>
  <Color id="light_blue@liberty_blue" rev="4">Light blue color</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

Run update

```
fida update
```

If you examine the contents of the file after the update, you will notice that there are two instances of the xid "`light_blue:6`" as expected.

It is more interesting to examine the lifelines, so execute the lifelines command,

```
fida lifelines
```

This should give you the following output:

```
1     blue:1 :< (powder_blue:4 liberty_blue:4) - r5
2     red:1 - ruby:2
3     green:1
4     colors:1 - r2 - r3 - r4 - r5 - r6
5     ... powder_blue:4 - light_blue:6(merged)
6     ... liberty_blue:4 - light_blue:6(merged)
7     :> (powder_blue:4 liberty_blue:4) light_blue:6
```

There are few things going on now. The nodes "`powder_blue:4`" and "`liberty_blue:4`" (lifelines number five and six) are shown evolve into the xid "`light_blue:6`" as the next step in their line of evolution. However, `fida` is helpfully pointing out that the node was actually merged into "`light_blue:6`". Later on the lifeline number seven begins with a special syntax  "`:> (xid1 xid2 ...) xid`" which means that the node has sprung into life from a merger of the xids listed inside the parenthesis. The new xid is shown after the parenthesis.

You might want to look up the nodeinfo for the node created in the merger by executing "`fida nodeinfo light_blue:6`".

### Creating a merger: method 2 ###

Lets do a new merger using a different approach. This time a node will be merged into a previously existing node. Namely, the node "`light_blue:6`" is going to be merged to the node "`blue:5`" - that is, back to the same lifeline from where it began. There is no need to touch to the destination node at all.

The strategy is to use the same renaming syntax as in the previous cases, but this time the revision number is included in the target xid. As before, the necessary condition for the merger to succeeded is that the nodes must be contentually equivalent. However, because the file now contains two different instances of "`light_blue:6`", one of them can be removed before merging the other. The steps are, then, as follows: 1) edit the contents of "`light_blue:6`" to match the contents of "`blue:5`", 2) rename the xid "`light_blue:6`" into "`blue:5`".

That said, re-open the file and make the forementioned modifications. After the modifications the file should look as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="6">
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
  <Color id="blue:5@light_blue" rev="6">Blue color (hex triplet #0000FF)</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color id="green" rev="1">Green color</Color>
</Codelist>
```

The renaming syntax used in the above shows a new way to use it, even though it is a quite logical extension of the form used previously. Here's the synopsis for the syntax:

```
  <AnyElement id="new_xid@original_id" ...>
```

This syntax should be self-explanatory. The difference between this and the previous renaming syntax is not big. Whereas the previous renaming syntax used "`new_id`" before the at sign, this specifies "`new_xid`" instead. The difference is the explicit specification of the revision number. In the first form (when just id is used), the revision number "`#`" is implicitly added to the renaming syntax, transforming it to into a full xid.

Run update,

```
fida update
```

If you examine the contents of the file after the update, you will find that its contents are pretty much just as you might expected. There are two instances of the xid "`blue:5`", just as it should be.

The lifelines are once again much more interesting, so execute the command

```
fida lifelines
```

The command should display the following information

```
1     blue:1 :< (powder_blue:4 liberty_blue:4) - r5(merges light_blue:6)
2     red:1 - ruby:2
3     green:1
4     colors:1 - r2 - r3 - r4 - r5 - r6 - r7
5     ... powder_blue:4 - light_blue:6(merged)
6     ... liberty_blue:4 - light_blue:6(merged)
7     :> (powder_blue:4 liberty_blue:4) light_blue:6 - blue:5(merged)
```

Now this begins to be a quite complicated picture. However, the merger that was just done resulted in the additions to lifelines number seven and number one. The lifeline number seven is the one that was merged, and this lifeline uses the similar notation which points out that at the end of its lifeline the node was actually merged into another node. The lifeline number one shows that the last evolution of the node is revision five, and it incorporates (merges) the node "`light_blue:6`" into itself.


### Creating a merger: method 3 ###

Lets examine closer what the last update did. Basically, the node "`light_blue:6`" was to merged into the node "`blue:5`". This was achieved with an XML element

```
  <Color id="blue:5@light_blue" rev="6">Blue color (hex triplet #0000FF)</Color>
```

which was then modified by "`fida update`" into the following XML element

```
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
```

Comparing these two lines give an idea that maybe merging could be done more easily by just removing the source xids, and modifying the target node's xid. Basically this would mean modifying the existing "`blue:5`" node into the node "`light_blue:6`" right before the update. This approach, however, would require the user to manually edit the attribute `@rev`, and that is highly discouraged. In fact, a special support was built into the renaming syntax to support this special case.

The special syntax for renaming a node into an existing node is as follows. Assume there is an XML element "`source:5`", which is going to be merged into the XML element "`dest:10`". The document contains the XML element "`dest:10`",

```
<Element id="dest" rev="10" />
```

Here's the trick, the source XML element "`source:5`" does not even need to exist in the current document/tree. It suffices that the XML element has existed at some point and the repository knows it. Now, to merge this currently unexisting "`source:5`" into "`dest:10`" shown above, the following modification is done:

```
<Element id="dest@source:5" rev="10" />
```

That is, this the source xid is appended to the attribute `@id` after an at-sign (`@`).

To demonstrate this method, we will merge "`green:1`" into "`ruby:2`". The modification steps are: 1) remove the XML element "`green:1`" from the document; 2) append the id of the XML element "`ruby:2`" with "`@green:1`". After these steps the resulting XML document should look as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="7">
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
  <Color id="ruby@green:1" rev="2">Ruby red color</Color>
</Codelist>
```

Execute update

```
fida update
```

Verify that the node "`green:1`" was merged into "`ruby:2`" by inspecting the lifelines, or by inspecting the nodeinfo for "`ruby:2`". Here is the nodeinfo for the node,

```
fida nodeinfo ruby:2
```

which outputs the following information

```
   Node xid:         #node!b566e179:2
   Payload xid:      ruby:2
   Commit xid:       #commit!482472ce:2
   Date:             Sat Oct 11 22:37:47 EEST 2014
   Author:           hautamaki

   Prev #1           red:1
   Prev #2           green:1
```

The node has two difference predecessors. Consequently, the node is a merger, as intended.

### Creating a merger: discussion ###

In each example regarding merging the document resulting after "`fida update`" could have been created simply by copy-pasting the destination xid in the place of the merged node. This begs a question to be asked: when merging a node is the proper action instead of replacing the node with another?

The author has difficulties conceiving a realistic use case for merging.

## What about reference migration? ##

### Different strategies ###

Creating branches affects the reference migration. Consider, for instance, the complete lifeline of the node "`blue:1`" at revision four. Here's a hand-drawn picture of it,

```
              .---> powder_blue:4
             /
   blue:1 --+
             \
              `---> liberty_blue:4

```

Now, consider a reference to the node "`blue:1`". How this reference should be migrated? There are three different options: 1) it should be migrated to "`powder_blue:4`", 2) it should be migrated to "`liberty_blue:4`", 3) it should not migrated to either one.

If the node should be migrated, the program has no way of knowing which one is the correct branch to migrate to. The program can, however, migrate the reference to the first successor listed for the node "`blue:1`", which would happen to be "`powder_blue:4`".

Next, consider the the complete lifeline of the node "`blue:1`" at revision eight, which is the latest revision. Here's a hand-drawn picture of it,

```

   blue:1 -+-----------------------------------------------------+---> blue:5
           |                                                     ^
           |\                                                    |
           | `---> powder_blue:4 ------.                         /
           \                            +---> light_blue:6 -----´
            `----> liberty_blue:4 -----´

```

Consider again a reference to the node "`blue:1`". How the reference should be migrated this time? This time there are four options. Three of these are the same ones as before. The fourth option is: 4) it should be migrated to "`blue:5´". This new option is somewhat different than the previous migration alternatives, because the node "`blue:5`" can be considered as the next revision of the node "`blue:1`" whereas the previous alternatives, nodes "`powder\_blue:4`" and "`liberty\_blue:4`", can be considered as branch offs. It is possible to distinguish a successor with the same id from a list of successors, so the program can be this migration too.

### Command-line options for reference migration strategies ###

There are three different migration strategies provided in `fida` for migrating over branching nodes. These strategies are: 1) cautious, 2) smart, and 3) rash. They can be invoked from the command-line with corresponding switches: `-cautious`, `-smart`, and `-rash`. The default strategy is "`smart`". Here's a brief description of each command-line switch:

**`-cautious`**: Migration finishes at branching nodes. Migration across branching nodes must be performed manually.

**`-smart`**: Migrates across branching nodes only there is a successor with the same id. Otherwise the migration finishes there.

**`-rash`**: Migrates across branching nodes always. If the node has a successor with the same id, it is used. Otherwise the first successor listed is used.

To display the reference migration decisions, use the following switch:

**`-report`**: Reports the reference migration decisions.

Next, these reference migration switches are demonstrated.

### Reference migration example ###

To put these into action, a reference must be created first. Re-open the document and add a referencing element to it,

```
<?xml version="1.0" encoding="UTF-8"?>
<Codelist id="colors" rev="8">
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
  <Color id="blue" rev="5">Blue color (hex triplet #0000FF)</Color>
  <Color id="ruby" rev="2">Ruby red color</Color>
  <Color ref="blue:1" />
</Codelist>
```

Save the document, and run update,

```
fida update
```

An impact analysis of the reference migration is carried out easily by dry-running it,

```
fida migrate2test -list
```

which shows that one referencing attribute is migrated as follows:

```
    colors.xml:
    /Codelist/Color[4]/@ref -> blue:5
1 attributes to migrate
```

The basic migration strategy is "smart", and because the node "blue:1`" has a successor with an identical id, the reference to it is migrated to "`blue:5`".

A more detailed report regarding the migration decisions can be produced with the switch "`-report`",

```
fida migrate2test -report
```

which outputs

```
URI:   file:/home/nuspirit/src-hg/xml-snippets/colors.xml
ITEM:  /Codelist/Color[4]/@ref="blue:1"
INFO:  Continuing from xid="blue:1" to branch "blue:5"; source has multiple successors: powder_blue:4 liberty_blue:4 blue:5

1 attributes to migrate
```

This gives more detailed information regarding the migration path. Specifically, it tells that even though the node "`blue:1`" has three alternative successors, the reference migration follows to node `"blue:5`".

Now, lets dry-run the cautious migration strategy,

```
fida migrate2test -cautious -list
```

This does not output much:

```
0 attributes to migrate
```

To get more detailed explanation of the result, ask `fida` to report the reference migration decision during the dry-run,

```
fida migrate2test -cautious -report
```

Now the program outputs,

```
URI:   file:/home/nuspirit/src-hg/xml-snippets/colors.xml
ITEM:  /Codelist/Color[4]/@ref="blue:1"
INFO:  Migration finished to xid="blue:1"; has multiple successors: powder_blue:4 liberty_blue:4 blue:5

0 attributes to migrate
```

This basically tells you that the migration finished to "`blue:1`", which happens to be the starting node, because it has multiple successors. The cautious reference migration strategy does not attempt to guess what the user wants, and therefore finishes at any branching node.

There is no point in demonstrating the rash reference migration strategy with this example document, since there is no fitting branches.

## End of tutorials ##

This is currently the last tutorial. You might want to go back to the Wiki's [table of contents](WikiTableOfContents.md) now.