# Security Policy

## Supported versions

Only the latest release published on the update site and the Eclipse
Marketplace receives fixes.

## Reporting a vulnerability

Please do not report security problems through public GitHub issues.

Use GitHub's private vulnerability reporting instead: open the **Security** tab
of this repository and choose **Report a vulnerability**. You will get an
acknowledgement within a week.

Please include steps to reproduce, the affected version, and any relevant
request or response data with secrets removed.

## Scope

Putman sends HTTP requests you compose to servers you choose. Treat it like a
browser or `curl`: it does not validate that a destination is safe, and request
history is kept in memory for the session only. Reports about third-party
servers you tested with Putman are out of scope.
