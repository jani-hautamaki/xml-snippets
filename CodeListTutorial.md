# Code lists: implementation and usage #

## Introduction ##

Code lists are also known as _enumerations_ or _controlled vocabularies_. In this tutorial a code list is built, and then used from another document. The code list is then supplemented, which raises difficulties around the ubiquitous "other" code. These difficulties are then explained and solved.

## Create a new repository ##

First, if there's any previous repository database existing in the working directory, it needs to be deleted:

```
del fida.xml
```

A new repository can now be created in the usual way:

```
fida init
```

The first thing is the code list for colors.

## The colors and the berries ##

Create a new XML document called "`colors.xml`". Then add the following contents to document:

```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors">
  <Color id="red">Red</Color>
  <Color id="green">Green</Color>
  <Color id="blue">Blue</Color>
  <Color id="other">Other</Color>
</ColorList>
```

Shouldn't be too complicated, right? There are three colors in the file: red, green and blue. Of course, there are berries that aren't red, green or blue. For those, the code "other" can be used signal that the berry's color is something else than red, green or blue.

Create a new document called "`berries.xml`". Then add the following contents to the document:

```
<?xml version="1.0" encoding="UTF-8"?>
<BerryList id="berries">
  <Berry id="lingonberry">
    <Name>Lingonberry</Name>
    <Color ref="red" />
  </Berry>
  <Berry id="blueberry">
    <Name>Blueberry</Name>
    <Color ref="blue" />
  </Berry>
  <Berry id="blackberry">
    <Name>Blackberry</Name>
    <Color ref="other" /> <!-- NOTE: other is used -->
  </Berry>
</BerryList>
```

Plain and simple. There are three berries in the list: lingonberry, blueberry and blackberry. Each berry has a name and color. Note how the contents of the `<Color>` elements hold simply references to the actual color elements specified in the color code list.

Note also how the revision numbers are missing from the references. That is because the XML elements do not have any revision numbers yet. To automatically set the revision numbers to the references, the command-line option `-autoref` must be used when ingesting these files.

Ingest both files at the same time:
```
fida add colors.xml berries.xml -autoref
```

If you now re-open the document "`berries.xml`" you should see that all references received a revision number at the end.

Verify that the references are pointing to existing elements:
```
fida listrefs
```

You should get an output similar to this:
```
 ABCD   Reference XPath and value
 ----   -------------------------
        colors.xml:
        berries.xml:
 ....   /BerryList/Berry[1]/Color/@ref="red:1"
 ....   /BerryList/Berry[2]/Color/@ref="blue:1"
 ....   /BerryList/Berry[3]/Color/@ref="other:1"
```

This output indicates that the document "`colors.xml`" contain no references, and the document "`berries.xml`" contain three references which are then listed. The four dots in front of each XPath express the fact that these references are valid and up-to-date.

## The code list evolves ##

Code lists evolve. Codes get added, modified and removed. Consequently,
revision tracking for code lists is generally highly desirable. Fortunately, revision tracking is child's play with `fida`.

In the berry list there was blackberry, which, as the name suggests, should have black color. The color code list, however, did not include a code for black. To overcome this, blackberry used the color "other".

When someone reads the description of the blackberry and sees that its color is "`other:1`", he/she goes to the code list next and looks up what colors there are in the list. From that information he/she can deduce that blackberry is not red and not green and not blue.

More formally, at revision one:
  * `other:1` implies not(`red:1`) and not(`green:1`) and not(`blue:1`)

Since blackberry's color is not found from the color code list, the next logical improvement to the code list would the inclusion of a code for black. Lets do that, lets include a code for color black.

Re-open the document "`colors.xml`" and insert a new `<Color>` element for black right before the `other` code. Here's how the document should look after inserting the new color:

```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors" rev="1">
  <Color id="red" rev="1">Red</Color>
  <Color id="green" rev="1">Green</Color>
  <Color id="blue" rev="1">Blue</Color>
  <Color id="black">Black</Color>
  <Color id="other" rev="1">Other</Color>
</ColorList>
```

As usual, the modifications introduced to the color code list document must be ingested by `fida`:

```
fida update
```

Now the color black is included in the color code list as wished.

## A problem appears ##

The color code list has now been supplemented with a new code for black. If someone now reads the description of the blackberry and sees that its color is "`other:1`", he/she would go to the code list next and look up what colors there are in the list. From that information he/she can deduce that blackberry is not red and not green and not blue and not black.

More formally, at revision two:
  * `other:1` implies not(`red:1`) and not(`green:1`) and not(`blue:1`) and not (`black:2`)

The blackberry should be black, but now its description clearly states that it is not black!

What happened?!

Here's the explanation: the meaning of the code "`other:1`" is tied to the other codes in the list. By changing the list we changed the meaning of the code. The code's meaning was changed without any formal modifications to the code itself.

In other words: the code does not formally express it's meaning.

## Solutions ##

There are two different approaches to solving the problem. Using one does not exclude the other. The options are:

  1. Add a reference to the "`other`" code that points to the list itself (ie. to its parent element).
  1. Add an alias to each color, and then reference a color by via its alias.

Both options are explored separately.

## Solution 1: model the dependency formally ##

The first way to solve the issue is to model the "`other`" code's dependency of the list itself. The dependency can be readily modeled by using a reference.

In this solution the color code list that should have been used in the first place is given below. Please note that the current situation can't be fixed with automatic migration easily, so do not attempt to just edit the current code list. You need to execute "`del fida.xml`" followed by "`fida init`" and start from the beginning.

What the contents of "`colors.xml`" should have been in the first place:
```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors">
  <Color id="red">Red</Color>
  <Color id="green">Green</Color>
  <Color id="blue">Blue</Color>
  <Color id="other">
    <NotAnyOf ref="colors:#" />
  </Color>
</ColorList>
```

Add both files with `-autoref`. Next, insert black to the color code list as a new color, and run `update`. Now the contents of the color code list file should be:

```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors" rev="2">
  <Color id="red" rev="1">Red</Color>
  <Color id="green" rev="1">Green</Color>
  <Color id="blue" rev="1">Blue</Color>
  <Color id="black" rev="2">Black</Color>
  <Color id="other" rev="1">
    <NotAnyOf ref="colors:1" />
  </Color>
</ColorList>
```

Notice how the color code list is currently in an inconsistent state: the code `other` points to an older revision of the `colors` list. That is, the code list `colors:1` is currently _not self-contained_, because it has references that require knowledge of elements located outside it.

Nonetheless, the person reading the berry list and looking up the `other:1` code will see the same information regardless of the code list's revision. In both revisions the contents of `other:1` are

```
fida resolve other:1
```

which outputs
```
<Color id="other" rev="1">
  <NotAnyOf ref="colors:1" />
</Color>
```

This pretty much solves the problem. The code's meaning is invariant with respect to changes to other codes. The meaning is, in other words, independent of the surrounding codes.

To make the code list self-contained again, its references need to be migrated. That's child's play. However, some caution must be taken in order to avoid reference migrating the berry list accidentally. So, lets reference migrate ONLY the color code list, and lets run impact analysis first as it is a good practice:
```
fida migrate2test -list colors.xml
```

The dry-run tells that there's one attribute to migrate, as expected. Run the reference migration for real this time, and then update:
```
fida migrate2 colors.xml
fida update
```

Re-opening the color code list shows that there are no references to outside elements. The code list is self-contained again, just as wished:

```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors" rev="3">
  <Color id="red" rev="1">Red</Color>
  <Color id="green" rev="1">Green</Color>
  <Color id="blue" rev="1">Blue</Color>
  <Color id="black" rev="2">Black</Color>
  <Color id="other" rev="3">
    <NotAnyOf ref="colors:3" />
  </Color>
</ColorList>
```

In general, it is undesirable to do automatic, or "blind", reference migration for any object that is using a code list. The code definitions might have changed. Some codes may have been merged and some may have been split. Some definitions may have become narrower and some wider. A good example is that you can't just blindly migrate from "`other:1`" to "`other:3`". Instead, each case should be considered separately to see whether the new revision of the code is applicable, or is there a different but better fitting code in new code list.

Because the program can easily provide a list of references that have newer revisions (the command `fida listrefs`), it could be used as an aid in the manual reference migration process.

## Solution 2: use aliasing ##

The second way to solve the issue is to use aliases for the codes.

In this solution each code in the color code list will have an alias, in addition to the identity. It is usually implicitly understood that a code list won't contain multiple revisions of a code. That is, the code list itself won't contain its history. Furthermore, it is usually the case that the identities of the codes are not renamed during their life time. Lets summarize these two properties:

  1. The code list contains only one revision of each code with an identity.
  1. The code list code's identities are never renamed.

If these two assumptions hold, then a much stronger claim can be said about the codes: each code's alias can be bound to its identity. This is exactly what will be done.

You need to execute "`del fida.xml`" followed by "`fida init`" start from the beginning. First, create the color code list "`colors.xml`":

```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors">
  <Color a="#" id="red">Red</Color>
  <Color a="#" id="green">Green</Color>
  <Color a="#" id="blue">Blue</Color>
  <Color a="#" id="other">Other</Color>
</ColorList>
```

Notice how each color specifies the symbolic name `#` as its alias to indicate that the alias is the same as the element's id.

Next, create the berry list "`berries.xml`", and this time use aliases for referencing the colors:
```
<?xml version="1.0" encoding="UTF-8"?>
<BerryList id="berries">
  <Berry id="lingonberry">
    <Name>Lingonberry</Name>
    <Color ref="colors/red" />
  </Berry>
  <Berry id="blueberry">
    <Name>Blueberry</Name>
    <Color ref="colors/blue" />
  </Berry>
  <Berry id="blackberry">
    <Name>Blackberry</Name>
    <Color ref="colors/other">Black</Color>
  </Berry>
</BerryList>
```

Notice how in the references the first part, `colors`, is without a revision number. It is expected that `fida` will set them right at ingest, when we use the `-autoref` option. Lets ingest both files:

```
fida add colors.xml berries.xml -autoref
```

Re-opening the berry list shows that the references have receivedproper revision numbers. These are the contents of "`berries.xml`" after ingest:
```
<?xml version="1.0" encoding="UTF-8"?>
<BerryList id="berries" rev="1">
  <Berry id="lingonberry" rev="1">
    <Name>Lingonberry</Name>
    <Color ref="colors:1/red" />
  </Berry>
  <Berry id="blueberry" rev="1">
    <Name>Blueberry</Name>
    <Color ref="colors:1/blue" />
  </Berry>
  <Berry id="blackberry" rev="1">
    <Name>Blackberry</Name>
    <Color ref="colors:1/other">Black</Color>
  </Berry>
</BerryList>
```

Now it is time to introduce the color black to the color code list. Lets see what kind of problems it causes this time. Here is the edited "`colors.xml`" before ingest:

```
<?xml version="1.0" encoding="UTF-8"?>
<ColorList id="colors" rev="1">
  <Color a="#" id="red" rev="1">Red</Color>
  <Color a="#" id="green" rev="1">Green</Color>
  <Color a="#" id="blue" rev="1">Blue</Color>
  <Color a="#" id="black">Black</Color>
  <Color a="#" id="other" rev="1">Other</Color>
</ColorList>
```

Run update to ingest the modifications:
```
fida update
```

Because the contents of the element "`colors:1`" were changed the revision is triggered for it. Consequently, the whole list receives a new revision number, and it becomes "`colors:2`".

This, however, does not affect the berry list in any way, because it is using "`color:1`" as a starting point in each reference. The whole problem caused by the mutating meanings was completely avoided!

## Next tutorial ##

In the [next tutorial](BranchingMergingTutorial.md) xids are renamed, branched and merged. Because branched xids create a special situations with respect to reference migration, the next tutorial also shows various different strategies provided for migrating references across branched nodes.