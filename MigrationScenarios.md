# Reference migration scenarios #

This wiki page is a compilation of different reference migration scenarios. These scenarios are

  1. [Ripple Effect](MigrationScenarios#Scenario_1._Ripple_Effect.md)
  1. [Sibling-Sibling Circular](MigrationScenarios#Scenario_2._Sibling-Sibling_Circular.md)
  1. [Parent-Child Circular](MigrationScenarios#Scenario_3._Parent-Child_Circular.md)
  1. [Cross-Document Circular](MigrationScenarios#Scenario_4._Cross-Document_Circular.md)

These scenarios work as a test suite for the reference migration algorithm.

# Scenario 1. Ripple Effect #

Remove the previous repository, if any, and create a new one:
```
del fida.xml
fida init
```

Then, create a new XML file called `ripple.xml` with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="ripple">
  <text id="a3">
    <to ref="a2" />
  </text>
  
  <text id="a2">
    <to ref="a1" />
  </text>
  
  <text id="a1">
    <to ref="middle" />
  </text>

  <text id="middle">
    this will be edited
  </text>

  <text id="z1">
    <to ref="middle" />
  </text>
  
  <text id="z2">
    <to ref="z1" />
  </text>
  
  <text id="z3">
    <to ref="z2" />
  </text>
</doc>
```

This is a specially crafted XML document. All reference chains point to the `@id='middle'` element. Lets add the file:
```
fida add ripple.xml -autoref
```

Next, the contents of the middle `<text>` element are modified. Write the following text into it:
```
  <text id="middle" rev="1">
    This has been edited
  </text>
```

Run update:
```
fida update
```

Now all has been prepared for the ripple effect seen in the reference migration. Only the references `@ref='middle'` needs to be directly modified, because there's a new revision of the middle element. This can be verified with the command:
```
fida listrefs
```

which outputs:
```
    ripple.xml:
    /doc/text[1]/to/@ref="a2:1"
    /doc/text[2]/to/@ref="a1:1"
 *  /doc/text[3]/to/@ref="middle:1"
 *  /doc/text[5]/to/@ref="middle:1"
    /doc/text[6]/to/@ref="z1:1"
    /doc/text[7]/to/@ref="z2:1"
```

Indirectly, however, all the references needs to modified. To understand this, lets think about the flow. First, the program edits the references to `'middle:1'` to refer to  `'middle:2'`. These modification causes the elements containing the references, ie. `@id='a1'` and `@id='z1'` to receive a new revision. After the first modification the references to `'a1:1'` and `'z1:1'` need to be migrated to `'a1:3'` and `'z1:3'` as well. And once those references have been migrated, the containing elements will receive a new revision, and the same cycle repeats. In conclusion, the modification of the middle element propagates like an avalanche, or a snowball or a ripple, to all references having _a transitive relation_ to the modified middle element.

Impact analysis can be carried out easily by dry-running the reference migration:
```
fida migrate2test -list
```

which outputs:
```
    ripple.xml:
    /doc/text[3]/to/@ref -> middle:2
    /doc/text[2]/to/@ref -> a1:#
    /doc/text[1]/to/@ref -> a2:#
    /doc/text[5]/to/@ref -> middle:2
    /doc/text[6]/to/@ref -> z1:#
    /doc/text[7]/to/@ref -> z2:#
6 attributes to migrate
```

All references get migrated! Just as expected. The hash mark in a reference has a special meaning. It means that during the preprocess phase, while updating or adding, the newest revision number will be inserted.

Lets migrate the references for real this time:
```
fida migrate2
```

Examine the file now, before running the update. The inspection reveals that the modifications to the references seen in the dry-run have now been written to disk.

Run update:
```
fida update
```

After the update the contents of the file `ripple.xml` are:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="ripple" rev="3">
  <text id="a3" rev="3">
    <to ref="a2:3" />
  </text>
  
  <text id="a2" rev="3">
    <to ref="a1:3" />
  </text>
  
  <text id="a1" rev="3">
    <to ref="middle:2" />
  </text>

  <text id="middle" rev="2">
    This has been edited
  </text>

  <text id="z1" rev="3">
    <to ref="middle:2" />
  </text>
  
  <text id="z2" rev="3">
    <to ref="z1:3" />
  </text>
  
  <text id="z3" rev="3">
    <to ref="z2:3" />
  </text>
</doc>
```

which clearly shows that the references have been migrated, and ripple effect has been accounted for during the migration. Lets check out the status of the references:
```
fida listrefs
```

which outputs:
```
    ripple.xml:
    /doc/text[1]/to/@ref="a2:3"
    /doc/text[2]/to/@ref="a1:3"
    /doc/text[3]/to/@ref="middle:2"
    /doc/text[5]/to/@ref="middle:2"
    /doc/text[6]/to/@ref="z1:3"
    /doc/text[7]/to/@ref="z2:3"
```

All is fine. Lets also see if there are any references left to migrate:
```
fida migrate2test
```

which outputs:
```
0 attributes to migrate
```

Nothing to migrate which implies that the reference migration worked correctly as expected.

# Scenario 2. Sibling-Sibling Circular #

Remove the previous repository, if any, and create a new one:
```
del fida.xml
fida init
```

Then, create a new XML file called `circular.xml` with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="circular">

  <!-- Forward circular -->

  <text id="a1">
    <to ref="a2" />
  </text>

  <text id="a2">
    <to ref="a3" />
  </text>

  <text id="a3">
    <to ref="a1" />
  </text>

  <!-- Backward circular -->

  <text id="z1">
    <to ref="z3" />
  </text>

  <text id="z2">
    <to ref="z1" />
  </text>

  <text id="z3">
    <to ref="z2" />
  </text>

</doc>
```

The references have been specially crafted. The first part is forward-cicular: a1->a2->a3->a1->etc. The second part is backward-circular: z1<-z2<-z3<-z1<-etc.

Lets add the file with autoref enabled:
```
fida add circular.xml -autoref
```

Lets edit one of the elements in both circles:
```
[...]
  <text id="a1" rev="1">
    this element will trigger the forward change
    <to ref="a2:1" />
  </text>
[...]
  <text id="z3" rev="1">
    this element will trigger the backward change
    <to ref="z2:1" />
  </text>
[...]
```

Then update:
```
fida update
```

Lets do the impact analysis. Again, there are only two direct references which are subject to migration. This can be seen from
```
fida listrefs
```

which outputs
```
    circular.xml:
    /doc/text[1]/to/@ref="a2:1"
    /doc/text[2]/to/@ref="a3:1"
 *  /doc/text[3]/to/@ref="a1:1"
 *  /doc/text[4]/to/@ref="z3:1"
    /doc/text[5]/to/@ref="z1:1"
    /doc/text[6]/to/@ref="z2:1"
```

What about transitive relations? All references which are subject to migration can be seen from
```
fida migrate2test -list
```

which outputs
```
    circular.xml:
    /doc/text[3]/to/@ref -> a1:#
    /doc/text[2]/to/@ref -> a3:#
    /doc/text[1]/to/@ref -> a2:#
    /doc/text[4]/to/@ref -> z3:#
    /doc/text[5]/to/@ref -> z1:#
    /doc/text[6]/to/@ref -> z2:#
6 attributes to migrate
```

After inspecting dry-runs about what will happen, it is time to execute the reference migration real:
```
fida migrate2
fida update
```

The contents of the file `circular.xml` are now:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="circular" rev="3">

  <!-- Forward circular -->

  <text id="a1" rev="3">
    this element will trigger the forward change
    <to ref="a2:3" />
  </text>

  <text id="a2" rev="3">
    <to ref="a3:3" />
  </text>

  <text id="a3" rev="3">
    <to ref="a1:3" />
  </text>

  <!-- Backward circular -->

  <text id="z1" rev="3">
    <to ref="z3:3" />
  </text>

  <text id="z2" rev="3">
    <to ref="z1:3" />
  </text>

  <text id="z3" rev="3">
    this element will trigger the backward change
    <to ref="z2:3" />
  </text>

</doc>
```

Check if there were any additional reference migrations produced by the process itself:
```
fida migrate2test
```

which outputs
```
0 attributes to migrate
```

The reference migration worked correctly as expected.

# Scenario 3. Parent-Child Circular #

Remove the previous repository, if any, and create a new one:
```
del fida.xml
fida init
```

Then, create a new XML file called `circular2.xml` with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="circular2">

  <!-- From child to grand-parent -->
  <text id="a1">
    <text id="a2">
      <text id="a3">
        <to ref="a1" />
      </text>
    </text>
  </text>

  <!-- From grand-parent to child (before and after) -->
  <text id="z1">
    <to ref="z3" />
    
    <text id="z2">
      <text id="z3">
      </text>
    </text>
    
    <to ref="z3" />
  </text>

</doc>
```

The references have been specially crafted. The first part is circling from the child to grand-parent. The second part is circling from the grand-parent to its child (before and after the child itself).

Lets add the file with autoref enabled:
```
fida add circular2.xml -autoref
```

Edit the file into the following state:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="circular2" rev="1">

  <!-- From child to grand-parent -->
  <text id="a1" rev="1">
    <text id="a2" rev="1">
      TEXT IN THE MIDDLE
      <text id="a3" rev="1">
        <to ref="a1:1" />
      </text>
    </text>
  </text>

  <!-- From grand-parent to child (before and after) -->
  <text id="z1" rev="1">
    <to ref="z3:1" />
    
    <text id="z2" rev="1">
      <text id="z3" rev="1">
        TEXT IN THE CHILD
      </text>
    </text>
    
    <to ref="z3:1" />
  </text>

</doc>
```

Then update:
```
fida update
```

Lets do the impact analysis. This time all references are directly subjects to migration. This can be seen from
```
fida listrefs
```

which outputs
```
    circular2.xml:
 *  /doc/text[1]/text/text/to/@ref="a1:1"
 *  /doc/text[2]/to[1]/@ref="z3:1"
 *  /doc/text[2]/to[2]/@ref="z3:1"
```

Lets see how the referencing attributes are going to be modified.
```
fida migrate2test -list
```

which outputs
```
    circular2.xml:
    /doc/text[1]/text/text/to/@ref -> a1:#
    /doc/text[2]/to[1]/@ref -> z3:2
    /doc/text[2]/to[2]/@ref -> z3:2
3 attributes to migrate
```

After inspecting dry-runs about what will happen, it is time to execute the reference migration real:
```
fida migrate2
fida update
```

The contents of the file `circular2.xml` are now:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="circular2" rev="3">

  <!-- From child to grand-parent -->
  <text id="a1" rev="3">
    <text id="a2" rev="3">
      TEXT IN THE MIDDLE
      <text id="a3" rev="3">
        <to ref="a1:3" />
      </text>
    </text>
  </text>

  <!-- From grand-parent to child (before and after) -->
  <text id="z1" rev="3">
    <to ref="z3:2" />
    
    <text id="z2" rev="2">
      <text id="z3" rev="2">
        TEXT IN THE CHILD
      </text>
    </text>
    
    <to ref="z3:2" />
  </text>

</doc>
```

Check if there were any additional reference migrations produced by the process itself:
```
fida migrate2test
```

which outputs
```
0 attributes to migrate
```

The reference migration worked correctly.

# Scenario 4. Cross-Document Circular #

Remove the previous repository, if any, and create a new one:
```
del fida.xml
fida init
```

Create the first XML file called `crossdoc1.xml` with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="crossdoc1">
  <text id="a1">
    <to ref="a2" />
  </text>
  
  <text id="a2">
    <to ref="b1" />
  </text>
</doc>
```

Create the second XML file called `crossdoc2.xml` with the following contents:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="crossdoc2">
  <text id="b1">
    <to ref="b2" />
  </text>
  
  <text id="b2">
    <to ref="a1" />
  </text>
</doc>
```

The references of these files have been specially crafted. The files reference each others XML elements: from `crossdoc1.xml` there's a reference (`b1`) to `crossdoc2.xml` and vice versa. The references form a circle which crosses file boundaries.

Lets add the files with autoref enabled:
```
fida add crossdoc1.xml crossdoc2.xml -autoref
```

Now it is time to introduce a modification somewhere. For instance, lets modify the second file `crossdoc2.xml` into the following state:
```
<?xml version="1.0" encoding="UTF-8"?>
<doc id="crossdoc2" rev="1">
  <text id="b1" rev="1">
    <to ref="b2:1" />
  </text>
  
  <text id="b2" rev="1">
    <to ref="a1:1" />
    <p>Some paragraph with dummy text</p>
  </text>
</doc>
```

Next, the update is ran as usual:
```
fida update
```

Lets to all the usual impact analysis. First, lets list the status of the references
```
fida listrefs
```

which outputs
```
    crossdoc1.xml:
    /doc/text[1]/to/@ref="a2:1"
    /doc/text[2]/to/@ref="b1:1"
    crossdoc2.xml:
 *  /doc/text[1]/to/@ref="b2:1"
    /doc/text[2]/to/@ref="a1:1"
```

Dry-run with explicit listing of the modifications:
```
fida migrate2test -list
```

which outputs:
```
    crossdoc1.xml:
    /doc/text[2]/to/@ref -> b1:#
    /doc/text[1]/to/@ref -> a2:#
    crossdoc2.xml:
    /doc/text[1]/to/@ref -> b2:#
    /doc/text[2]/to/@ref -> a1:#
4 attributes to migrate
```

After inspecting dry-run diagnostics, it is time to execute the reference migration real. Lets execute `migrate2` and `update` in a sequence:
```
fida migrate2
fida update
```

Check out the status of the references after the migration:
```
fida listrefs
```

which outputs
```
    crossdoc1.xml:
    /doc/text[1]/to/@ref="a2:3"
    /doc/text[2]/to/@ref="b1:3"
    crossdoc2.xml:
    /doc/text[1]/to/@ref="b2:3"
    /doc/text[2]/to/@ref="a1:3"
```

Lets also check that further migration should not be needed:
```
fida migrate2test
```

which outputs
```
0 attributes to migrate
```

Migration work correctly as expected.