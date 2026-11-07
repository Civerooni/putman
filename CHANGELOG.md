# Changelog

All notable changes to Putman are recorded here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Open-source project scaffolding: README, contribution guide, code of conduct,
  security policy, issue and pull request templates.
- Maven/Tycho build (`mvn verify`) and a GitHub Actions workflow that runs it on
  every push and pull request.
- EPL-2.0 license file at the repository root and license headers in all Java
  sources.
- Shared `Putman` launch configuration for running the standalone app from the IDE.

### Changed
- Target platform definition moved from the product project to
  `civerooni.com.putman.target/putman.target`.
- The product definition no longer hard-codes a workspace-relative logging
  configuration path. Debug logging is now enabled by the shared launch
  configuration instead.
- Unified vendor and copyright strings across bundles and the feature.
- Target platform now includes the Eclipse Platform feature so the IDE view can be
  tested with **Run As → Eclipse Application** instead of only via an installed build.

### Removed
- Obsolete `civerooni.com.putman.site` category definition. The update site is
  maintained in the separate `putman-site` repository.

## [1.0.0] - 2026-03-13

### Added
- Initial public release on the update site and Eclipse Marketplace.
- Send GET, POST, PUT, PATCH, DELETE, HEAD, and OPTIONS requests.
- Params, Headers, and Body tabs with Raw and JSON body modes.
- Raw, JSON, and HTML response views.
- Multiple request tabs with rename and clone.
- Response replay in a different format.
- Installable as an Eclipse IDE view or runnable as a standalone RCP app.

[Unreleased]: https://github.com/Civerooni/putman/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Civerooni/putman/releases/tag/v1.0.0
