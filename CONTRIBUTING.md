# Contributing to Putman

Thanks for your interest in Putman. This document explains how to set up a
development environment, how changes are proposed and reviewed, and the
conventions the codebase follows.

## Before you start

- For anything beyond a small fix, **open an issue first** describing what you
  want to change and why. That way we can agree on the approach before you
  invest time in code.
- Check the [open issues](https://github.com/Civerooni/putman/issues) in case
  someone is already working on it.
- Be kind. This project follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development setup

Follow the **Building from source** section of the [README](README.md). In short:

1. Install Eclipse IDE for RCP and RAP Developers (2024-12 or newer) and a Java 21 JDK.
2. Import all projects from your clone.
3. Open `civerooni.com.putman.target/putman.target` and click
   **Set as Active Target Platform**. Nothing compiles until this is done.
4. Run the **Putman** launch configuration, or run `civerooni.com.putman.ui` as
   an Eclipse Application to test it as an IDE view.

There is no command-line test runner today. `mvn -B verify` compiles the
bundles against the target platform and packages the feature. It runs on every
pull request, so please run it locally before you push.

## Testing an exported build

To test the real install path, export the feature (File → Export → Plug-in
Development → Deployable Features, with **Generate p2 repository** ticked) into
an empty folder, then install it into an Eclipse IDE from that folder via
**Help → Install New Software…**.

Two wizard settings matter:

- **Untick "Contact all update sites during install to find required
  software".** Leaving it on lets p2 pull unrelated upgrades from every
  enabled update site into your IDE at the same time. If your IDE is older
  than the newest Eclipse release, that can leave it unable to start.
- **Untick "Group items by category"** for a local export, because the export
  wizard does not publish a category.

## Workflow

1. Fork the repository and create a branch from `main`:
   ```
   git checkout -b fix/short-description
   ```
2. Make your change in focused commits. Each commit should build on its own.
3. Verify in the IDE that the app still starts and your change behaves as intended.
   Check both modes if your change touches the UI: standalone and as an IDE view.
4. Run `mvn -B verify`.
5. Push your branch and open a pull request against `main`. Fill in the
   pull request template. Screenshots help for UI changes.
6. A maintainer will review it. Expect a round or two of feedback. Once approved
   it will be merged, usually by squash.

Direct pushes to `main` are not accepted. Everything goes through a pull request.

## Developer Certificate of Origin

Putman uses the [Developer Certificate of Origin](https://developercertificate.org/)
(DCO) rather than a contributor license agreement. By signing off a commit you
certify that you wrote the change or otherwise have the right to submit it
under the project's license, the Eclipse Public License 2.0.

Add a sign-off line to every commit:

```
git commit -s -m "Describe the change"
```

That adds a line like this to the commit message:

```
Signed-off-by: Your Name <you@example.com>
```

The name and email must be real and must match the commit author. Pull requests
with unsigned commits will be asked to add the sign-off before merge.

## Coding conventions

The codebase is small and the conventions are simple. Match the surrounding code.

**Java**

- Java 21. Use modern language features where they make code clearer
  (records, switch expressions, `var` for obvious types), but don't rewrite
  working code just to modernise it.
- Four-space indentation, no tabs. Braces on the same line.
- One top-level class per file. Package names follow the bundle name.
- Every source file starts with the EPL-2.0 license header (copy it from any
  existing file). Add it to new files.
- Prefer `PutmanLogger` over `System.out` for diagnostics.
- No new third-party dependencies without discussing it in an issue first.
  Everything currently ships from the Eclipse platform itself.

**Eclipse 4 patterns**

- UI structure lives in `Application.e4xmi`. Add parts, menus, commands, and
  handlers there, not in Java.
- Handlers are small classes with `@Execute` and, if needed, `@CanExecute`.
  They must not reference menu or toolbar items directly.
- Parts create their widgets in `@PostConstruct` and clean up in `@PreDestroy`.
  Get services through injection, not through static lookups.
- Parts talk to each other through the `IEventBroker`, not direct references.
- Keep `civerooni.com.putman.core` free of SWT, JFace, and workbench
  dependencies. Anything that needs the UI belongs in `civerooni.com.putman.ui`.
- If you add a bundle dependency, add it to `META-INF/MANIFEST.MF` using
  `Require-Bundle` for Eclipse bundles and `Import-Package` for Jakarta
  packages, matching the existing entries. Remember to update `putman.target`
  if the dependency is not already part of the target platform.

**Bundle versions**

Leave `Bundle-Version` and the feature version alone. Versions are bumped by
the maintainer as part of a release.

## Commit messages

- Short imperative summary line, under about 70 characters.
- A blank line, then as much explanation as the change needs: what was wrong,
  why this is the right fix, anything a reviewer should look at.
- Reference issues with `Fixes #123` or `Refs #123`.
- End with your `Signed-off-by` line.

## Reporting bugs

Use the bug report template. Please include the Eclipse version, Java version,
operating system, and the exact steps to reproduce. If Eclipse shows an error
dialog or stack trace, attach the relevant part of the **Error Log** view
(**Window → Show View → Error Log**).

## Security issues

Please do not open a public issue for a security problem. See
[SECURITY.md](SECURITY.md).

## Releases

Releases are built, signed, and published by the maintainer. The process is
intentionally not automated yet because it requires the code-signing keystore.
If you need a change in a release sooner, say so in the pull request.
