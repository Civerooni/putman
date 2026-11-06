# Putman

[![Build](https://github.com/Civerooni/putman/actions/workflows/build.yml/badge.svg)](https://github.com/Civerooni/putman/actions/workflows/build.yml)
[![License: EPL-2.0](https://img.shields.io/badge/License-EPL%202.0-blue.svg)](LICENSE)

Putman is a lightweight REST API client that runs inside the Eclipse IDE. It is
inspired by Postman and built on the Eclipse 4 (E4) application model, so it
feels like a native Eclipse view rather than a separate tool.

## Features

- Send `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, and `OPTIONS` requests
- Edit query parameters, headers, and request bodies in dedicated tabs
- Body editor with Raw and JSON modes, including JSON validation and pretty-printing
- View responses as Raw, JSON, or HTML, with the status code shown alongside
- Keep several requests open at once in closable tabs; rename or clone a tab from its context menu
- Replay a stored response in a different format without re-sending the request
- Runs as an Eclipse IDE view (**Window → Show View → Putman**) or as a standalone RCP application

## Installation

### From the Eclipse Marketplace

Search for **Putman REST Client** in **Help → Eclipse Marketplace…** and click Install.

### From the update site

1. In Eclipse, open **Help → Install New Software…**
2. Click **Add…** and enter `https://civerooni.github.io/putman-site/`
3. Select **Putman REST Client**, then finish the wizard and restart Eclipse
4. Open the view with **Window → Show View → Other… → Putman → Putman**

Putman requires Eclipse 2024-12 or newer running on Java 21 or newer.

## Building from source

Putman is a standard Eclipse Plug-in Development Environment (PDE) project.
Day-to-day development happens in the Eclipse IDE. A Maven/Tycho build is
provided for command-line verification and continuous integration.

### Prerequisites

- [Eclipse IDE for RCP and RAP Developers](https://www.eclipse.org/downloads/packages/), 2024-12 or newer
- A Java 21 JDK (Eclipse Temurin is a good choice)
- Optionally, Apache Maven 3.9 or newer for the command-line build

### Set up the workspace

1. Clone the repository:
   ```
   git clone https://github.com/Civerooni/putman.git
   ```
2. In Eclipse, choose **File → Import… → General → Existing Projects into Workspace**,
   select the cloned folder, and import all projects.
3. **Set the target platform.** This step is required, or nothing will compile.
   Open `civerooni.com.putman.target/putman.target` and click
   **Set as Active Target Platform** in the top-right corner of the editor.
   Eclipse will download the required bundles from the 2024-12 release repository
   the first time, which can take a few minutes.
4. Wait for the workspace build to finish. There should be no errors in the
   **Problems** view.

### Run

- **As a standalone app:** choose **Run → Run Configurations… → Eclipse Application → Putman**.
  This shared launch configuration lives in `civerooni.com.putman.product/Putman.launch`
  and enables debug logging via `logging.properties` in the same folder.
- **As an IDE view:** right-click `civerooni.com.putman.ui` → **Run As → Eclipse Application**.
  A second Eclipse instance starts with Putman installed. Open it with
  **Window → Show View → Other… → Putman**.

### Command-line build

```
mvn -B verify
```

This compiles both bundles against the target platform and packages the
feature. The first run downloads the Eclipse target platform, so expect it to
take a while. The same command runs on every pull request through GitHub Actions.

## Project layout

| Folder | Purpose |
|---|---|
| `civerooni.com.putman.core` | HTTP client, request and response models, logging. No UI dependencies. |
| `civerooni.com.putman.ui` | E4 user interface: parts, handlers, `Application.e4xmi`, and the IDE view. |
| `civerooni.com.putman.feature` | Feature that packages the two bundles for installation. |
| `civerooni.com.putman.product` | Product definition for the standalone app, plus the shared launch configuration. |
| `civerooni.com.putman.target` | Target platform definition (`putman.target`). |

### Architecture notes

- UI structure (window, parts, menus, commands, handlers) is declared in
  `Application.e4xmi`, not in Java. Java classes are referenced from the model
  by `bundleclass://` URIs.
- Commands are declared in the model. Handlers are plain classes with an
  `@Execute` method and optionally `@CanExecute`. Handlers never reference menu
  or toolbar items directly.
- Parts build their SWT widgets in a `@PostConstruct` method and receive
  services through Eclipse dependency injection (`@Inject`, `@Named`).
- Parts communicate through the E4 event broker rather than direct references.
- The `core` bundle has no Eclipse UI dependencies and can be unit-tested in isolation.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for
the development workflow, coding conventions, and the Developer Certificate of
Origin sign-off that every commit needs. This project follows the
[Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).

Bug reports and feature requests go in the
[issue tracker](https://github.com/Civerooni/putman/issues).

## Releases

Releases are published to the update site at
<https://civerooni.github.io/putman-site/> and to the Eclipse Marketplace by the
maintainer. See [CHANGELOG.md](CHANGELOG.md) for what changed in each release.

## License

Putman is licensed under the [Eclipse Public License 2.0](LICENSE).

Putman is an independent open-source project maintained by Civerooni. The
`civerooni.com` bundle namespace comes from the maintainer's domain and does
not imply any relationship with products or services offered under that name.
