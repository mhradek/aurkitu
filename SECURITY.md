# Security policy

## Supported versions

Security fixes are made for the latest release on Maven Central (`io.github.mhradek:aurkitu-maven-plugin` and
`aurkitu-annotations`). Older 1.x versions are not patched; upgrade to the latest release.

| Version | Supported |
|---------|-----------|
| Latest 1.x | Yes |
| Older 1.x | No |
| 0.0.x (`com.michaelhradek`) | No |

## Reporting a vulnerability

Please report vulnerabilities privately, not in a public issue: use
[Report a vulnerability](https://github.com/mhradek/aurkitu/security/advisories/new) on the repository's Security tab.

Include the affected version, how to reproduce it and the impact you see. You should get an answer within a week.
When a fix is ready it is released, and the advisory is published with credit to you unless you prefer otherwise.

## Scope

Aurkitu is a build-time Maven plugin: it reads compiled classes on the project's compile classpath and writes a
FlatBuffers schema, and with `flatcPath` runs the `flatc` binary you configure. It does not run in production
applications. Reports about the generated schema's content, or about running untrusted models or an untrusted
`flatc`, are welcome but are usually out of scope.
