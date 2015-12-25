# XML element references and reference migration #

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

## An XML document to drive a survey software ##

The imaginary objective of the sample XML document used for this tutorial is that it could be used to drive an application which interrogates the user and records the results into a data file. In other words, the sample XML document could be used to create and execute surveys. The program should a) provide a stimulus to the user, and b) record some aspect of the user's reaction to the stimulus in some fashion.

With this motivation, create an XML document with the file name `sample.xml` having the following contents:
```
<Questionnaire id="questionnaire">
  <Question id="Q1">
    <Text>What is your name? (in format "First Last")</Text>
  </Question>
  
  <Question id="Q2">
    <Text>How old are you? (in whole years)</Text>
  </Question>

  <Variable id="V1">
    <Label>Respondent's name in format "firt_name last_name"</Label>
    <DataType>string</DataType>
  </Variable>

  <Variable id="V2">
    <Label>Respodent's age as an integer</Label>
    <DataType>int</DataType>
  </Variable>
</Questionnaire>
```

The file structure is quite self-documenting: it consists of instructions how to

  * a) stimulate the subject, and
  * b) how to record the response of the subject.

The stimuli is provided by `<Question>` elements, and response record storage and instructions are provided by `<Variable>` elements. Put into engineering jargon, the questions works as _actuators_, and UI input widgets work as _sensors_ which provide measurements of the variables.

Okay, lets make `fida` track the file in the usual manner by issuing
the command
```
fida add sample.xml
```

Re-open the document in a text editor. The identified elements got revision numbers just as expected. However, there are few serious and obvious shortcomings in the current XML document regarding the imaginary objective of driving a survey application with it.

One obvious problem is that the imaginary application has no way of knowing which questions provide stimuli to which variable measurements. In other words, there's no formal connection, or pairing, between questions and variables. Another obvious shortcoming is that the order in which the question-variable pairs are executed is not explicitly defined.

Both of these problems could be fixed by specifying an alternating list of questions and variables which would control the _flow_ of the survey. The order would be explicit and the pairing could be inferred from the alternating list.

How to actually implement this alternating list of questions and variables? Clearly, duplicating the already defined `<Question>` and `<Variable>` elements by copy-pasting them into a separate list isn't a good idea. It would cause difficulties when one of the questions or variables were modified, because the modifications would have to be copy-pasted (ie. propagated) into each instance of the element. Of coure, the `migrate` command could help in doing that, but still, duplication isn't very appealing idea.

One possible solution, and probably the best solution, is to use references.

## References in general ##

According to [Wikipedia](http://en.wikipedia.org/wiki/Reference),

"**Reference** is a relation between objects in which one object designates, or acts as a means by which to connect to or link to, another object. The first object in this relation is said to **_refer to_** the second object. The second object – the one to which the first object refers – is called the **_referent_** of the first object."

Wikipedia's [article](http://en.wikipedia.org/wiki/Reference_%28computer_science%29) about references in computer science is more specific:

"a **reference** is a value that enables a program to indirectly access a particular datum, such as a variable or a record, in the computer's memory or in some other storage device. The reference is said to **_refer to_** the datum, and accessing the datum is called **_dereferencing_** the reference."

The article makes also a good point about the difference between references and keys or identifiers that "uniquely identify the data item, but give access to it only through a non-trivial lookup".

## Using references ##

Lets edit the sample XML document. Re-open the file `sample.xml` in a text editor. An element, `<Flow>`, containing an alternating list of  references to questions and variables is added to the document. Now the XML document looks as follows:
```
<?xml version="1.0" encoding="UTF-8"?>
<Questionnaire id="questionnaire" rev="1">
  <Question id="Q1" rev="1">
    <Text>What is your name? (in format "First Last")</Text>
  </Question>
  
  <Question id="Q2" rev="1">
    <Text>How old are you? (in whole years)</Text>
  </Question>
  
  <Variable id="V1" rev="1">
    <Label>Respondent's name in format "firt_name last_name"</Label>
    <DataType>string</DataType>
  </Variable>

  <Variable id="V2" rev="1">
    <Label>Respodent's age as an integer</Label>
    <DataType>int</DataType>
  </Variable>
  
  <Flow id="flow">
    <Question ref="Q1:1" />
    <Variable ref="V1:1" />
    
    <Question ref="Q2:1" />
    <Variable ref="V2:1" />
  </Flow>
  
</Questionnaire>
```

Note that the elements inside `<Flow>` contain `@ref` attribute which refers to an identified `<Variable>` or `<Question>` element. A reference is simply an attribute containing the referent's xid.

Lets commit the modifications by running update:
```
fida update
```

## Command: listrefs ##

Is `fida` somehow aware of these references? The answer is yes. To see the references as `fida` sees them, issue the command
```
fida listrefs
```

This should give the following output:
```
    sample.xml:
    /Questionnaire/Flow/Question[1]/@ref="Q1:1"
    /Questionnaire/Flow/Variable[1]/@ref="V1:1"
    /Questionnaire/Flow/Question[2]/@ref="Q2:1"
    /Questionnaire/Flow/Variable[2]/@ref="V2:1"
```

What should happen if one of the questions were modified? Lets find out.

Modify the first question in the XML document so that it looks as follows:
```
  <Question id="Q1" rev="1">
    <Text>What is your name? Give the answer as: FirstName LastName</Text>
  </Question>
```

Now, run update:
```
fida update
```

After the update, the `Q3` has received a new revision number. The revision number is now 3 in the actual element, but the reference in the `<Flow>` element refers to `Q3:1`, which is the previous revision.

Lets see, if `fida` is aware of this. List the references seen by the program by issuing again the command
```
fida listrefs
```

which gives the following output:
```
    sample.xml:
 *  /Questionnaire/Flow/Question[1]/@ref="Q1:1"
    /Questionnaire/Flow/Variable[1]/@ref="V1:1"
    /Questionnaire/Flow/Question[2]/@ref="Q2:1"
    /Questionnaire/Flow/Variable[2]/@ref="V2:1"
```

The asterisk preceding the XPath of the attribute referencing
to `Q1:1` means that the referred XML element has a newer revision.

It is usually desirable that 1) the references are referring to
the newest revisions, and 2) the referents are present in
in the same revision of the tree. Therefore, to get the document into a desirable state, the references need to be updated. The process of updating the references to refer to the newest revisions of their referents is called _reference migration_.

It would be quite easy to just manually edit the references so that they would refer to the newest revisions. However, this method of manually updating the references becomes rather quickly unfeasible and error-prone as the XML document repository grows.

Therefore, the migration of references should be performed automatically by the program itself. Fortunately, there is a command in `fida` just for that!

## Command: migrate2test ##

Before actually performing the reference migration, it is appropriate and a good practice to perform a dry-run of the reference migration. The dry-run reference migration is perfromed by issuing the command
```
fida migrate2test
```

The output should be as follows:
```
1 attributes to migrate
```

The output doesn't tell much, but fortunately there are few command-line switches that can be used to elaborate the output. To get a more specific information on the referencing attributes which are subject to migration and on how they are migrated, execute
```
fida migrate2test -list
```

which gives more elaborate output:
```
    sample.xml:
    /Questionnaire/Flow/Question[1]/@ref -> Q1:3
1 attributes to migrate
```

This list tells the XPath of the attribute and its new value. It does not, however, tell the file in which the specified attribute lives in. That feature is on the TODO list.

The reference migration dry-run can be elaborated even further. The results can be written to the screen (instead of being written to the actual files in the case of actual migration). This is achieved by appending the command-line with the following switch:
```
fida migrate2test -onscreen
```

The output is the contents of the migrated `sample.xml`. The notable difference between the current revision and the migrated revision can be summarized as:
```
[...]
  <Flow id="flow" rev="2">
    <Question ref="Q1:3" />
    [...]
  </Flow>
[...]
```

The references in the `<Flow>` element have been updated just as expected. That is, the reference to `Q1:1` has been modified to `Q1:3` which is the newest revision of `Q1`.

This is still a dry-run of the reference migration.

## Command: migrate2 ##

To actually perform the reference migration for real, issue the command
```
fida migrate2
```

which should give the following output.
```
1 attributes to migrate
Files updated. Run "fida update"
```

The modifications performed to the XML documents have been written into the actual files. They have been modified, as can be seen from the status command:
```
fida status
```

which tells that
```
r3     M    sample.xml
Current revision 3
```

Okay, lets update as suggested by the program:
```
fida update
```

Now the interesting parts of the file `sample.xml` are as follows:
```
<?xml version="1.0" encoding="UTF-8"?>
<Questionnaire id="questionnaire" rev="4">
  [...]
  <Flow id="flow" rev="4">
    <Question ref="Q1:3" />
    <Variable ref="V1:1" />
    
    <Question ref="Q2:1" />
    <Variable ref="V2:1" />
  </Flow>
</Questionnaire>
```

An observation can be made from the above: the update of the reference caused the parent `<Flow>` element to be revisioned.

Now a question arises: what would have happened if some other reference was referring to that `<Flow>` element which got a new revision due to the reference migration procedure itself? Intuitively, the other reference should also be migrated, and it should be migrated simultaneously.

That is actually what would have happened, as can been seen from the next tutorial.

## Next tutorial ##

In this tutorial referencing mechanism and reference migration procedure were introduced. However, some questions were left unanswered or unexplored. The gaps left by this tutorial are meant to be filled by the next tutorial. It goes much further into the subject, and shows some [complex referencing and reference migration](ReferenceMigrateTutorial.md).