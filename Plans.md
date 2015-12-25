# Ideas and Plans #

This is a list of things that would be nice to do when there's time...

The ones having the greatest priority are probably these:

  * Refactor the program.
  * An actual database for storing and retrieving the XML elements.
  * Process model for using git/hg in tandem with fida.

Besides these, there are some smaller goals that are equally important,
  * Tutorial page: show how fida can be used to track revisions and version control DDI-Lifecycle v3.2.
  * Tutorial page: namespace collisions, and different bubbling strategies: greedy and prudent.
  * Refactoring of the normalization routine to a more granular version which accounts for modified children by remapping the links instead of providing the new revision as a whole. That is, instead of doing "snapshot" revisioning, the _locally unmodified_ ancestor elements are revisioned by "deltas" instead of snapshots. These deltas would include "relink" operation which provides a new target xid for a link.
  * Wiki page: one that would motivate and demonstrate clearly why using deltas instead of snapshots is so important for locally unmodified ancestors of a modified element.

The ones having less priority and belonging more into the class of nice and/or useful ideas:

  * An overview of few scientific papers around the subject and around XML deltas.
  * Element-wise _access control_. This needs to include a brief study of the differences between the properties related to _XML elements_ and to their _manifestations_.
  * Managing _authoritative_ and _non-authoritative_ XML element manifestations. There are at least two possibilities: either with an explicit notation or by implicit deduction. Implicit determination needs to include a brief discussion about constraining modifiable XML elements with _xid namespace scoping_.
  * _Multiple repositories_ aware of each others within an organization, and enabling referential integrity over the repositories within an organization.
  * _Constraining the revision tracking_ of a single data set to a single repository. This is the main driving force behind multi-repository scenarios. One possibility to do this is to divide the revision tracking responsibilities of the repositories based on the xid namespace scopes. A repository would know the namespace scopes of the other repositories within the organization. From there onwards, it simply a question of resolution service.
  * Refactoring of the migration and ingest code so that ingest could be used to determine the modified elements, and then migration could opearate on that information.
  * Few additional applications: binary file tracking by injecting checksums into XML documents, and controlled vocabularies by tracking the definitions and providing translations.
