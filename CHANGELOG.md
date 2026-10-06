<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Session Attribute Type Confusion Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- First real type-compatibility check in this catalog
  (`PsiType.isAssignableFrom`, not a text comparison of type names)
  combined with whole-project correlation by exact string-literal
  session/context attribute key: flags a `getAttribute("key")` cast to
  a type genuinely incompatible with every known `setAttribute("key",
  ...)` write site for that key, a guaranteed `ClassCastException` at
  runtime (CWE-843).

[Unreleased]: https://github.com/GapHunterLabs/session-attribute-type-confusion-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/session-attribute-type-confusion-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/session-attribute-type-confusion-companion/commits/0.1.0
