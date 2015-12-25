# Aliased XML elements #

The latest feature in `fida` at the time of writing this (2014 Mar 3) is the XML element aliasing. The XML element aliasing solves a referencing problem that is described below.

## The problem ##

Suppose you have an XML document which describes a game in which
the player may control Fry from Futurama. The character, Fry, can
play holophonor which a musical instrument in the 30th century. Playing holophonor requires the use of his both hands. Unfortunately, Fry's hands are not very well suited to playing holophonor. In fact, his hands suck at playing holophonor. Lucky for him, there's also the Robot Devil from Futurama. Robot Devil's hand are well-suited to playing holophonor.

In the game data document the following elements used:

  * `<Character>` elements describe a character (Fry or Robot Devil), and the character's both hands.
  * `<Hand>` elements are children of `<Character>` elements; these are used to give a description of each hand whether it is suited to holophonor playing.
  * `<Actions>` is a list of individual actions.
  * `<Action>` describes an individual action. The action may require the use of a character's body parts. For instance, playing a holophonor requires the use of both hands.

Here's how the initial revision of gamedata document "`gamedata.xml`" looks like:

```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama" rev="1">

  <Character id="fry" rev="1"> <!-- Fry from Futurama -->
    <Hand id="fry.left_hand" rev="1">
      <Text>Left hand has stupid fingers</Text>
    </Hand>
    <Hand id="fry.right_hand" rev="1">
      <Text>Right hand has stupid fingers</Text>
    </Hand>
  </Character>

  <Character id="robot_devil" rev="1">
    <Hand id="robot_devil.left_hand" rev="1">
      <Text>Left hand has fingers suited to holophonor playing</Text>
    </Hand>
    <Hand id="robot_devil.right_hand" rev="1">
      <Text>Right hand has fingers suited to holophonor playing</Text>
    </Hand>
  </Character>

  <Actions id="fry.actions" rev="1">
    <Action id="fry.actions.play_holophonor" rev="1">
      <Requires ref="fry.left_hand:1" />
      <Requires ref="fry.right_hand:1" />
    </Action>
    <!-- Maybe some other actions here too -->
  </Actions>
  
</GameData>
```

Now, Robot Devil and Fry swap hands, physically. That is possible in the Futurama's universe. The game data document is updated accordingly. That is, the hands of both characters are swapped simply by copy-pasting them.

The contents of the updated document are:

```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama" rev="2">

  <Character id="fry" rev="2"> <!-- Fry from Futurama -->
    <Hand id="robot_devil.left_hand" rev="1">
      <Text>Left hand has fingers suited to holophonor playing</Text>
    </Hand>
    <Hand id="robot_devil.right_hand" rev="1">
      <Text>Right hand has fingers suited to holophonor playing</Text>
    </Hand>
  </Character>

  <Character id="robot_devil" rev="2">
    <Hand id="fry.left_hand" rev="1">
      <Text>Left hand has stupid fingers</Text>
    </Hand>
    <Hand id="fry.right_hand" rev="1">
      <Text>Right hand has stupid fingers</Text>
    </Hand>
  </Character>

  <Actions id="fry.actions" rev="1">
    <Action id="fry.actions.play_holophonor" rev="1">
      <Requires ref="fry.left_hand:1" />
      <Requires ref="fry.right_hand:1" />
    </Action>
    <!-- Maybe some other actions here too -->
  </Actions>
  
</GameData>
```

**The problem**: the action "`fry.actions.play_holophonor:1`" still points to (or references) Fry's physical hands, which now belong to the Robot Devil. To rectify this situation, the actions would need to be modified manually. That, however, is highly undesirable. A better way to fix the action would be to a different kind of reference in the first place. That new kind of reference would 1) specify the `<Character>` element, and then 2) select `<Hand>` within the specified `<Character>` element. The updating of actions would then rely on the automatic migration of the references.

This was not possible earlier, not until the aliased/named XML elements were introduced. Aliased XML elements can be though of as being _properties_ of their immediate xidentified parent element. One might consider Fry being an instance of Character class, and his both left and right hand being attributes of the Character class.

## Create a new repository ##

First, if there's a previous repository database existing in the working directory, it needs to be deleted before continuing with the tutorial. The removal of the previous repository is simply achieved by deleting the repository's database file:
```
del fida.xml
```

A new repository can now be created in the usual way:
```
fida init
```

## Create the game data document ##

Create a new XML file with name `gamedata.xml`. Add the root element to the document:

```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama">
  <!-- Will be populated... -->
</GameData>
```

Now, it's time to add a character, Fry, to the game data. Before doing that the property id syntax must be introduced.

The _property name_ syntax is introduced. The alias, or the name, for an XML element which represents a property/an attribute of its nearest xidentified parent is defined with the XML attribute `a`. The 'a' could stand for 'alias' or for 'attribute'.

If a xidentified XML element `fry:1` has a child XML element `<Hand>` and that `<Hand>` has an attribute `@a="left_hand"`, then the hand element can be referenced by first pointing to the xidentified XML element and then continuing the reference with slash followed by a **path** to the named property. For instance, in this case the left hand would be referenced with an expression "`fry:1/left_hand`".

Lets use this aliasing feature to the character's hands:

```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama">
    
  <Character id="fry"> <!-- Fry from Futurama -->
    <Hand a="left_hand" id="fry.left_hand">
      <Text>Left hand has stupid fingers</Text>
    </Hand>
    <Hand a="right_hand" id="fry.right_hand">
      <Text>Right hand has stupid fingers</Text>
    </Hand>
  </Character>
    
  <!-- More contents are added later on -->
    
</GameData>
```

Now, the first `<Hand>` child element is aliased as `"left_right"`. The left hand is a property of the `<Character>` element, and the property's value is the contents of the named XML element. The second `<Hand>` child element is named in a similar manner as `right_hand`.

## Properties of the aliases ##

It is now time to discuss the properties of the aliases themselves. They have two very important properties.

**Property names are local**. More specifically, they are local to their nearest xidentified or aliased ancestor. So, inside a named XML element it is possible to have another named XML element with the same name.

**Property names are unique within their scope**. That is, witihin the local scope of a xidentified or aliased XML element, the property name must be unique. Other XML elements with the same property name are NOT allowed, and ingesting such XML documents results in rejection.

Locality of the property names is explored later on.

Property names have two different automatic values that can be used.

1. When `@a=""` the property name is set to match the XML element name. For instance, the XML element `<Hand a="" />` would therefore be equal to `<Hand a="Hand" />`. If all children of a certain element choose to use `@a=""`, this has the same effect as restricting each element tag name to appear only once.

2. When `@a="#"` the property name is set to match the XML element's identity name. For instance, the XML element `<Hand a="" id="right_hand" rev="1" />` would be equal to `<Hand a="right_hand" rev="1" />`. If all children of a certain element choose to use this symbolic property name, this has the same effect as constraining the element identity names to be unique within the certain element.

Since element's property name is completely independent of the element's identity by default, it is therefore possible to _choose_ whether the user wants to set further constraints to the identity namess than they originally have. This mutual independence, or orthogonality if you will, of property names and identity names is  desirable and, from architectural point of view, very elegant. The orthogonality of aliases and identities truly facilitates the feasibility and the compactness of the design.

## Finishing the game data document ##

Lets add the Robot Devil in the similar manner. As `<Character>` element is used again, it'll have the same properties as the earlier instance. Namely, there'll be `left_hand` and `right_hand` as properties, or as named/aliased XML elements.

```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama">

  <Character id="fry"> <!-- Fry from Futurama -->
    <Hand a="left_hand" id="fry.left_hand">
      <Text>Left hand has stupid fingers</Text>
    </Hand>
    <Hand a="right_hand" id="fry.right_hand">
      <Text>Right hand has stupid fingers</Text>
    </Hand>
  </Character>

  <Character id="robot_devil">
    <Hand a="left_hand" id="robot_devil.left_hand">
      <Text>Left hand has fingers suited to holophonor playing</Text>
    </Hand>
    <Hand a="right_hand" id="robot_devil.right_hand">
      <Text>Right hand has fingers suited to holophonor playing</Text>
    </Hand>
  </Character>
    
  <!-- Actions will be added later -->
  
</GameData>
```

It is time to add the `<Actions>` element containing the references to Fry`s hands. The property names, or aliases, are finally put into use.

As explained earlier, the property names are local. This means that in order to use them in a reference, the reference must include the nearest xidentified parent. In fact, the reference _must begin with a xid_.

Since property names are also local to other property names, referencing a nested property requires the complete path to it. The syntax for a reference is therefore `<xid>[/<alias>]+`. Here's an example of a reference to a nested property: `fry:1/left_hand/index_finger`.

Here's the final game data document prior to commiting it:
```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama">

  <Character id="fry"> <!-- Fry from Futurama -->
    <Hand a="left_hand" id="fry.left_hand">
      <Text>Left hand has stupid fingers</Text>
    </Hand>
    <Hand a="right_hand" id="fry.right_hand">
      <Text>Right hand has stupid fingers</Text>
    </Hand>
  </Character>

  <Character id="robot_devil">
    <Hand a="left_hand" id="robot_devil.left_hand">
      <Text>Left hand has fingers suited to holophonor playing</Text>
    </Hand>
    <Hand a="right_hand" id="robot_devil.right_hand">
      <Text>Right hand has fingers suited to holophonor playing</Text>
    </Hand>
  </Character>

  <Actions id="fry.actions">
    <Action id="fry.actions.play_holophonor">
      <Requires ref="fry/left_hand" />
      <Requires ref="fry/right_hand" />
    </Action>
    <!-- Maybe some other actions here too -->
  </Actions>
  
</GameData>
```

Since the game data document has not yet been commited, the XML elements do not have revision numbers. Consequently, the revision numbers will be left out from the references too, and `-autoref` feature is used to set them.

Commit the game data document with the following command:
```
fida add gamedata.xml -autoref
```

If you now reopen the game data document and look for the references you'll see that the revision numbers were inserted automatically.

## Inspecting the references ##

There are two commands that can be used to inspect how the references will resolve. These two commands are `resolve` and `resolve2`. They work in similar manner to `output` and `output2`. That is, the first one rebuilds the resulting XML element and the second one outputs the XML element as it is stored into database.

Try the following command:
```
fida resolve2 fry:1
```

The program outputs the following XML element:
```
<Character id="fry" rev="1">
  <!-- Fry from Futurama -->
  <Hand a="left_hand" ref_xid="fry.left_hand:1" link_xid="#link!cae85ee3:1" />
  <Hand a="right_hand" ref_xid="fry.right_hand:1" link_xid="#link!aad5f66b:1" />
</Character>
```

(Attribute values for `link_xid` may be different).

Notice how the property names of the child XML elements are stored into the contents of the parent element.

Now, try to resolve one of the named properties within `fry:1`:
```
fida resolve2 fry:1/right_hand
```

The program outputs the following XML element:
```
<Hand id="fry.right_hand" rev="1">
  <Text>Right hand has stupid fingers</Text>
</Hand>
```

We conclude that Fry's right hand is `fry.right_hand:1`.

Also, make a note that the element itself does not contain any property name specifications. It does not know about its aliases. This is what it means for the property name to be local to the parent.

## Swap hands ##

Now it is time to swap the hands of the Robot Devil and Fry. Just copy-paste the hands from one character to the other. After modifications, the game data document should have the following contents:

```
<?xml version="1.0" encoding="UTF-8"?>
<GameData id="futurama" rev="1">

  <Character id="fry" rev="1"> <!-- Fry from Futurama -->
    <Hand a="left_hand" id="robot_devil.left_hand" rev="1">
      <Text>Left hand has fingers suited to holophonor playing</Text>
    </Hand>
    <Hand a="right_hand" id="robot_devil.right_hand" rev="1">
      <Text>Right hand has fingers suited to holophonor playing</Text>
    </Hand>
  </Character>

  <Character id="robot_devil" rev="1">
    <Hand a="left_hand" id="fry.left_hand" rev="1">
      <Text>Left hand has stupid fingers</Text>
    </Hand>
    <Hand a="right_hand" id="fry.right_hand" rev="1">
      <Text>Right hand has stupid fingers</Text>
    </Hand>
  </Character>

  <Actions id="fry.actions" rev="1">
    <Action id="fry.actions.play_holophonor" rev="1">
      <Requires ref="fry:1/left_hand" />
      <Requires ref="fry:1/right_hand" />
    </Action>
    <!-- Maybe some other actions here too -->
  </Actions>
  
</GameData>
```

Commit the modifications:
```
fida update
```

If you re-open the game data document and inspect the references, you'll find that the references point to the older revision of Fry. This can be seen from the output of `fida listrefs` command too:

```
 ABCD   Reference XPath and value
 ----   -------------------------
        gamedata.xml:
 *...   /GameData/Actions/Action/Requires[1]/@ref="fry:1/left_hand"
 *...   /GameData/Actions/Action/Requires[2]/@ref="fry:1/right_hand"
```

The asterisk (`*`) in the front of a reference indicates that there's a newer revision of the XML element identified by the the xid in the beginning of the reference.

Do an automatic reference migration, and run update after it:
```
fida migrate2
fida update
```

If you now re-open the game data document and inspect the rferences, you will see that the references are now pointing to the current revision of Fry.

## Inspecting the updated references ##

Working in the same way as earlier, lets inspect how the references to Fry's hands resolve now. First, take a look at the game data document, to see what the references in the `<Actions>` element are:

```
      ...
      <Requires ref="fry:2/left_hand" />
      <Requires ref="fry:2/right_hand" />
      ...
```

Resolve the second reference:
```
fida resolve2 fry:2/right_hand
```

The program outputs the following XML element:
```
<Hand id="robot_devil.right_hand" rev="1">
  <Text>Right hand has fingers suited to holophonor playing</Text>
</Hand>
```

We conclude that Fry's right hand is now `robot_devil.right_hand:1`. The Robot Devil and Fry have swapped their hands successfully.

Here is the important lesson: _XML element aliases enable the underlying XML element's identity (=xid) to vary independently of its alias_. Consequently, varying XML element's identity will not affect references to it.

## Next tutorial ##

In the [next tutorial](CodeListTutorial.md) code lists (also known as enumerations or controlled vocabularies) are explored. The tutorial shows how to implement and use a code list. Also, XML element aliasing is put into use while a problem caused by a very typical modeling error is solved.