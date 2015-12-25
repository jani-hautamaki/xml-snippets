# References and reference migration #

## The usual stuff ##

First, if there's a previous repository database existing in the working directory, it needs to be deleted before continuing with the tutorial. The removal of the previous repository is simply achieved by deleting the repository's database file:
```
del fida.xml
```

A new repository can now be created in the usual way:
```
fida init
```

Now the tutorial can really begin.

## A simple, but very helpful feature: `-autoref` ##

When creating a new XML document, the identified XML elements do not have any revision numbers yet. Also, as demonstrated in the previous tutorials, the user should NOT insert the revision numbers arbitrarily by hand. So for instance, consider that the following new XML document `sample.xml` is being created

```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="sample">
  <text id="a">
  </text>

  <text id="b">
  </text>
  
  <text id="c">
  </text>
  
  <text id="d">
  </text>

  <!-- Attach tags to a certain text -->
  <tags ref="??????">tag1, tag2, tag3</tags>
</doc>
```

There's a problem here. What to put into the `@ref` attribute? After creating few `<text>` elements the purpose of `<tags>` element is to attach tags (comma-separated list) to a certain `<text>` element. So, a reference to a xid is required, but the problem is: the newly created elements do not have a revision number yet, and therefore they cannot be referenced yet!

This is where the `-autoref` switch comes into the picture. The switch is meant to be used with `'fida add'` and `'fida update'` commands. When enabled, these commands preprocess the XML documents in a specific way, populating the missing revision numbers to the references.

There are two rules for populating the missing revision numbers:
  1. When a reference without a revision number is met, eg. `@ref='a'`, the program attempts to find out any lifeline with the designator `'a'`. If it finds such a lifeline, the reference will receive the latest xid of the element who leased the lifeline designator last.
  1. If no element have leased such a lifeline designator before, then the program assumes that the reference is not a typo, but a reference to an identified XML element which is introduced for the first time in the same update.

After knowing this, the reference in the XML document is fixed to be:
```
  <tags ref="a">tag1, tag2, tag3</tags>
```

and the `-autoref` switch is enabled during the `'add'` command:
```
fida add sample.xml -autoref
```

This produces the missing revision number into the reference correctly, since the element with the lifeline designator `'a'` was introduced for the first time in the same update. Lets verify that there's no need to migrate:
```
fida migrate2test
```

which outputs:
```
0 attributes to migrate
```

Lets also see the status of the references:
```
fida listrefs
```

which outputs:
```
    sample.xml:
    /doc/tags/@ref="a:1"
```

If, however, the `-autoref` switch wasn't used during the `'add'`, then the reference would still be without a revision number, and `'listrefs'` command would produce the following output:
```
/doc/tags/@ref: has an id but no revision; ignored.
```

## References in `fida` ##

It has been shown that `fida` is aware of the references put into attributes named as `ref`. However, these attributes are not the only attributes that `fida` recognizes as references. Whether an attribute is considered a reference depends on its name. If any of the following conditions is true, then the attribute is expected to contain a reference:

  * The name begins with "ref" (in Java: `String.beginsWith('ref')`)
  * The name ends with "ref" (in Java: `String.endsWith('ref')`)
  * The name ends with "Ref" (in Java: `String.endsWith('Ref')`)

The choice which XML nodes (ie. elements, attributes, and so on) are considered to be references is an arbitrary choice. So the ones presented here are a result of more or less arbitrary, human-made decision.

It is worth noting that these rules for attributes CANNOT be represented in XML Schema language. Well, at least not to the author's knowledge. However, in a typical context-free grammar it is usually possible to specify a regular expression for a terminal symbol. A terminal symbol represents a class of syntactically equivalent tokens.

## Multiple references in the same element ##

Because the naming rules for referencing attributes are quite unrestricted, it is possible to have multiple references in the same element.

Lets edit the `sample.xml` document into the following state:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="sample" rev="1">
  <text id="a" rev="1">
  </text>

  <text id="b" rev="1">
  </text>
  
  <text id="c" rev="1">
  </text>
  
  <text id="d" rev="1">
  </text>
 
  <!-- Multiple referencing attributes in a single element -->
  <multiple xref="a" yref="b" zref="c">lorem ipsum</multiple>
</doc>
```

Because we were lazy and did not append any revision numbers in the references, the `-autoref` switch needs to be used when running the update:
```
fida update -autoref
```

After update, re-examination of the `<multiple>` element reveals that the each reference received a revision number:
```
  <multiple xref="a:1" yref="b:1" zref="c:1">lorem ipsum</multiple>
```

Check out the status of the references:
```
fida listrefs
```

which outputs:
```
    sample.xml:
    /doc/multiple/@xref="a:1"
    /doc/multiple/@yref="b:1"
    /doc/multiple/@zref="c:1"
```

All references were recognized as expected.

## Complex reference migration scenarios ##

Different reference migration scenarios are examined on their own wiki page, [here](MigrationScenarios.md). The reference migration scenarios are:

  1. [Ripple Effect](MigrationScenarios#Scenario_1._Ripple_Effect.md)
  1. [Sibling-Sibling Circular](MigrationScenarios#Scenario_2._Sibling-Sibling_Circular.md)
  1. [Parent-Child Circular](MigrationScenarios#Scenario_3._Parent-Child_Circular.md)
  1. [Cross-Document Circular](MigrationScenarios#Scenario_4._Cross-Document_Circular.md)

These scenarios work as a test suite for the reference migration algorithm.

# The migration algorithm #

Basically, the idea is to find all nodes which are either directly or indirectly affected by certain operation, and update them. To get more clarity and precision to the idea, certain specific concepts are needed.

## Definitions: nouns ##

Define a _xidentified_ node to be an XML element which has `@id` and `@rev`.

Define a _xidentified parent_ node to be an XML element which is the most immediate xidentified ancestor of an XML element or an XML attribute.

Define a _referencing attribute_ to be an XML attribute which is referring either to a xidentified node or to its next future revision.

Define a _reference holder_ to be the xidentified parent (= most immediate xidentified ancestor) of the referencing attribute.

## Definitions: relations ##

Define _connects directly_ relation, `>`,  between two xidentified nodes `a` and `b` so that `a>b` (read: "`a` connects directly to `b`") iff one or more of the following is true:

  * `a` is the xidentified parent of xidentified node `b` (lineage),
  * `a` is the xidentified parent for a referencing attribute referring to `b` (jump).

Define _connects_ relation, `->`, between two xidentified nodes `a` and `b` such that `a->b` (read: "`a` connects to `b`", or more verbosely, "there are one or more connections from `a` to `b`") iff one of the following is true:

  * promotion: `a>b`,
  * transitivity: `a->x` and `x->b`.

In mathematics, a relation `R` is said to be _transitive_ iff `aRb` and `bRc` implies `aRc`. Transitivity is the key property in discovering the nodes which are indirectly affected by migrating a reference.

The "connects" relation, `->`, is a _transitive closure_ of the "connects directly" relation, `>`.

The above definition given for "connects" relation is a recursive definition. A _recursive definition_ (or _inductive definition_) is used to define an object in terms of itself. here the first case, `a->b` if `a>b`, is called the base case of the definition. A _base case_ (or _basis_) is a case that satisfies the definition without being defined in terms of the definition itself. This is the most important difference between circular definition and recursive definition.

## Migrating a referencing attribute ##

Given a referencing attribute, there are 3 different possibilities regarding the referent, a xidentified node:

  1. The referent is the newest (frozen) revision
  1. The referent has a newer (frozen) revision
  1. The referent has been modified (into an unfrozen state)

Each of these cases is discussed separately below.

**1. The referent is the newest (frozen) revision**

In this case, the referencing attribute is left unmodified. The reference is already pointing to the newest revision of the referent.

The possibility remains that later on, during the course of the algorithm, the referent is modified and therefore the reference needs to be migrated (case 3).

**2. The referent has a newer (frozen) revision**

In this case, the referencing attribute is migrated to the newest (frozen) revision of the referent. Migration involves updating the contents of the referencing attribute. The update renders the reference holder (ie. the directly connected parent node) into an unfrozen state. That change may trigger further updates to the nodes which are connected to the reference holder.

The possibility remains that later on, during the course of the algorithm, the referent is modified and therefore the reference needs to be migrated again (case 3).

**3. The referent has been modified (into an unfrozen state)**

In this case, the reference is migrated to the next future revision. Currently, a hash mark `'#'` is used as a pseudo revision number to denote the next future revision. Migration involves updating the contents of the referencing attribute. The update renders the reference holder (ie. the directly connected parent node) into an unfrozen state. That change may trigger further updates to the nodes which are connected to the reference holder.

Contrary to cases 1 and 2, once a referencing attribute has been migrated to refer to the next future revision, later modifications to the referent do **not** cause additional migrations, because the reference is already referring to the next future revision. In other words, a reference which has been migrated to refer to the next future revision is in a stable final state until a `'fida update'` is ran.

## Coloring ##

When a recursive graph algorithm is used, it is important to avoid looping recursions. This is done by determining somehow whether a node is already handled or being currently handled. The mechanism typically used is the "coloring" of the nodes.

There are three different colors for referencing attributes: white, gray, and black. White is the default color, and it means that the referencing attribute is untouched. When a referencing attribute is migrated to the newest (frozen) revision, it is marked gray. When a referencing attribute is migrated to the next future revision, it is marked black, which is the final stable state.

There are two different colors for xidentified nodes: white, and black. White is the default color, and it means that the xidentified node is untouched. When a referencing attribute is migrated to either a new revision or to the next future revision, the most immediate xidentified parent node is marked black, meaning that it has been modified to an unfrozen state.

A node marked black is in a final stable state with respect to connected nodes, after all the connected nodes have been processed, because later modifications to its contents do not cause any additional migrations to connected nodes.

## Evolution of the colors ##

The migration algorithm is interested only in referencing attributes. Therefore, the sources for any coloring activity are modifications to the referencing attributes. Color changes in the referencing attributes trigger color changes in the xidentified nodes. The possible color changes are:

  * For referencing attributes:
    * White -> Gray: migrated to newest (frozen) revision.
    * Gray -> Black: re-migrated from the newest (frozen) revision to the next future revision.
    * White -> Black: migrated to the next future revision.
  * For xidentified nodes:
    * White -> Black: the node has been modified.

Whenever a referencing attribute changes color, the reference holder is turned white->black unless it is already marked as black.

Whenever a xidentified node is turned white->black, the following steps are taken:

  1. All directly connected referencing attributes must be turned black
  1. The xidentified parent node must be turned black unless it is already marked as black.

This is a recursive definition of the process.

An important thing to recognize here is that: a xidentified node and the connected nodes need to be processed only once, but referencing attributes marked gray may require a second processing to turn them black.

## Discussion ##

The change propagation triggered by updating a referencing attribute is defined here as the _ripple effect_.

In fact, the author is aware of at least one web page which has a description of the ripple effect as a consequence of the reference migration. Excerpt from the [web page](http://www1.unece.org/stat/platform/pages/viewpage.action?pageId=78677815) (cited 2013 Aug 18):

_"If the existing metadata objects that refer to the object that just got "versioned" now need to refer to the newer version of that object, all those existing metadata objects themselves now need to get "versioned" (because they're pointing to a different version of the first object). All the objects that refer to the objects that referred to the original object now need to get assessed and potentially versioned themselves, and so on with a ripple effect potentially sweeping across the whole registry originating from just one object being versioned."_

The back-propagation of changes which occurs when a referencing attribute is modified is called here as the ripple effect. The ripple effect might sweep across the whole repository, but when it happens, it is a logical consequence of the modifications made. If, however, the ripples expand in a chaotic manner, then I would say that [everything is connected to everything](http://www.students.tut.fi/~hautama5/tmp2/failed_systems_development_project1.png) in the model and the [model makes no sense](http://www.students.tut.fi/~hautama5/tmp2/failed_systems_development_project2.png). This is a typical symptom of a mistake was made much earlier when a [Metadata Expert](http://isosmeta.wordpress.com/2011/11/17/sab-ddi-and-sdmx/) was hired to do the [metadata](http://isosmeta.wordpress.com/2009/12/22/the-three-definitions-of-metadata/) modeling.

## Next tutorial ##

The next tutorial demonstrates a very cool feature, [XML element aliasing](AliasingTutorial.md), which enables the XML elements to have local names. Local names enable the references to have a level of indirection.