## The software ##

This is the home of a program, called fida, that shows how to do _element-wise XML revision tracking_ and _version control_.

The system's key features are

  * Centralized architecture,
  * Repository itself is an XML file,
  * Command-line interface similar to SVN or CVS,
  * XML element instances can be revision tracked individually,
  * Tracked XML elements can be referenced,
  * Named XML elements within tracked XML elements can be referenced,
  * References can be migrated,
  * Tracked XML elements can branch and merge to other tracked XML elements.

Start with the [quick installation guide](QuickInstallGuide.md), and then continue to the [introduction tutorial](IntroductionTutorial.md).

List of all online tutorials is available in the  [Wiki's Table of Contents](WikiTableOfContents.md). Good for an overall view.

For the more impatient ones, there's the [concise language reference](LanguageReference.md).

This program and these pages do not concentrate on issues such as producing XML deltas or 3-way merges of the tree-structures. The main objective here is to demonstrate how to track revisions of XML elements.

## The source code ##

`fida` is licensed under the terms of GPLv3, and its source code is available for both Mercurial and Git users.

  * Mercurial repository at [Google Code](http://code.google.com/p/xml-snippets/source/checkout)
  * Git repository at [GitHub](https://github.com/hautamaki/fida).

The Mercurial repository contains the complete development history, and is used as the primary repository.

The quality of source of is not high, and the whole code base needs complete rewriting.

Distribution packages with pre-compiled binaries are also made available for those, who don't want to build the software themselves:

  * Distribution packages at [SourceForge](https://sourceforge.net/projects/fida/files/).

Distribution packages come with pre-compiled binaries in addition to the source code.

## Latest updates ##
  * 2014 Oct 14 - `fida` v0.9.6 released, pre-compiled distribution package available from [SourceForge](https://sourceforge.net/projects/fida/files/).
  * 2014 Oct 14 - Added the [branching and merging tutorial](BranchingMergingTutorial.md).
  * 2014 Oct 1 - Branching and merging of xids is now supported.
  * 2014 Mar 5 - `fida` v0.9.5 released, pre-compiled distribution package available from [SourceForge](https://sourceforge.net/projects/fida/files/).
  * 2014 Mar 5 - Added the [code list tutorial](CodeListTutorial.md).
  * 2014 Mar 4 - Added the [concise language reference](LanguageReference.md) wiki page.
  * 2014 Mar 3 - Added the [XML element aliasing tutorial](AliasingTutorial.md).
  * 2014 Mar 3 - XML element aliasing is now fully supported in ingestion and reference migration.

(C) 2012–2014 Jani Hautamäki