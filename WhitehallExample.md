# Testing with real data #

This tutorial shows how `fida` could work with actual XML data files.
Certainly, there aren't many XML formats out there having similar
notions regarding the identities and versions. To the best of author's knowledge there are two, SDMX and DDI.

They both, SDMX and DDI, seem to have incomplete descriptions how the versioning of hierarchical structures should be handled. This is might be due to insufficient problem exploration and framing. So far, the versioning model of `fida` can be considered much more precise than the models provided by DDI and SDMX documentation. In addition, `fida` provides a _working_ model of revision tracking and version control.

## MRC's Whitehall DDI3 data ##

This is probably the biggest DDI3 instance that the author is aware of
so far. It is about 50 megabytes, which is quite impressive.
The web page hosting a link to the file is
https://www.datagateway.mrc.ac.uk/node/53337

Lets download the file
```
wget https://www.datagateway.mrc.ac.uk/system/files/whitehallDDI3_out.xml
```

Make a copy of the original, so that it doesn't need to be downloaded
again after `fida` has chewed it up.
```
cp whitehallDDI3_out whitehall.xml
```

Initialize the repository in the usual way:
```
fida init
```

However, because DDI uses `@version` attribute and three-level
version numbers rather than `@rev` attribute and revision numbers,
the repository needs to be switched to use version numbers:
```
fida setversion 0.0
```

DDI3 format is such that there are some XML elements for which
`@version` attribute is not allowed while in others it is allowed.
The already-existing version numbers in the file will be forced
into the repository, and during ingest the XML elements, which have
`@id` but not `@version` attribute, are assigned a version by `fida`.

The version was therefore intentionally set to "0.0", so that later
on it is possible distinguish between the version numbers set by `fida`
and the version numbers which already existed in the file.

Ingesting the XML file will render it into an invalid DDI with respect
to the XML schema, because there will be `@version` attributes in XML elements where they aren't allowed. However, that is not a big concern, since the purpose is to just test `fida` in a realistic setting.

Everything should be ready. It is time to chew up the huge XML file
with forcing:
```
fida add whitehall.xml -force
```

This time the program takes up a noticeable amount of time, but
the program finishes gracefully, and reports happily that it
"Committed revision` `2".

The resulting repository database file "`fida.xml`" has size
of 64 megabytes. The overhead can be calculated, it appears
to be around around 20% in this case.

Lets see the lifelines. The command would mediate for some time,
and then, after the meditation, it would display thousands of lifelines. Consequently, it is perhaps better to store the output to a file in the first place:
```
fida lifelines > output.txt
```

Calculating the lines:
```
wc -l output.txt
```

gives a result of 40449 lines.

A quick glance through of all 40k lines shows that there are few
XML elements with multiple revisions. How is this possible? Lets
look at it more closely. First, lets dig up those elements in
a more elegant manner:
```
cat output.txt | grep "> r"
```

This gives the following list:
```
82    f19e558e-4e97-4479-a4d5-3481746dce0f:6.0.0 > r2
86    130ad16b-676d-41de-8644-fcd04bb4ac57:5.0.0 > r2
91    1a6c97a3-606a-4772-aa05-cc6e90402cf0:5.0.0 > r2
95    420c958b-4239-48c9-a58c-8f94f10aae6f:5.0.0 > r2
99    c0ad0d51-53ff-4039-a257-df3b6c4cb7fb:5.0.0 > r2
103   0cd632d0-2cd4-4efa-84d9-88505c30f710:5.0.0 > r2
107   9046fa06-3eff-4f0e-8cf1-f2107249c5a4:5.0.0 > r2
111   96816fe3-04c1-491d-a76d-a8d18d96730f:5.0.0 > r2
```

Because the version numbers don't start with "0.0.", all of
these are original version numbers - excluding the second revisions,
which the program has created because it has encountered xids
which were already ingested, but with different content.

Lets pick the lifeline number 111 and ask `fida` to output both
versions into different files and then run `diff` on them.

```
fida output 96816fe3-04c1-491d-a76d-a8d18d96730f:0 > test1.xml
fida output 96816fe3-04c1-491d-a76d-a8d18d96730f:2 > test2.xml
```

and then run `diff` on those files:
```
diff test1.xml test2.xml
```

The output is (excluding the root element due to differing `@version`
number):
```
<   <r:Note id="9198d27d-c661-400a-adb3-71393ad746ae" type="System" version="0.0.2">
---
>   <r:Note id="6dce8a7c-c42c-4d9f-8538-462c6e883db3" type="System" version="0.0.2">
```

In other words the file contains the same `<s:StudyUnit>` element
twice. However, those elements are not identical. Specifically,
they have a child `<r:Note>` elements with differing `@id`s, even though
the contents of the note elements are otherwise identical.

Going through the other cases in the same way reveals that they all
are `<s:StudyUnit>` elements which all appear twice in the file, but
with `<r:Note>` having differing `@id`s. This is clearly a systematic
error, and it is probably due to Colectica.

_Update 2014_: Colectica generates the IDs for notes on-the-fly while serializing the data. In other words, the IDs will be different on each save. Consequently, the when the same Study is serialized twice, the notes will receive different IDs even though their contents are identical. This issue will be fixed, not by updating Colectica, but by updating the DDI standard so that it won't allow Notes to have IDs.

In conclusion, `fida` succeeded in ingesting the huge DDI file, and at the same time was able to enhance the file's internal integrity.