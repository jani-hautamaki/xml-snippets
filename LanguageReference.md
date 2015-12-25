# Concise Language Reference #

This program does not require you to use any DTD or XML Schema. Instead, you can create your own XML Schema for constraining the language, if you like.

`fida` eats any XML file supplied to it, as long as the following conditions are met.

  1. Any element **_may_** be assigned an identity with `@id` attribute. The element is then said to be _identified_.
  1. Only the document's root element **_must_** have an `@id` attribute.
  1. User **_must not_** edit or create `@rev` attributes manually. The program will manage the contents of `@rev` attributes automatically.
  1. Any element **_may_** contain one or more referencing attributes. An attribute is said to be a _referencing attribute_ when its name 1) starts with "ref"; 2) ends with "ref"; or 3) ends with "Ref".
  1. A referencing attribute **_may_** have any content at ingest.
  1. Any element **_may_** be assigned an alias with `@a` attribute. The element is then said to be _aliased_.
  1. An alias **_must_** be unique within the nearest ancestor element that is aliased or identified.
  1. If any two elements have the same `@id` (and `@rev` after the first ingest), then their contents **_must_** be equivalent.

See the Wiki's [Table of Contents](WikiTableOfContents.md) for the topics that may be of interest.