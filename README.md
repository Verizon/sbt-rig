# sbt-verizon

[![Build Status](https://travis.oncue.verizon.net/iptv/sbt-verizon.svg?token=Lp2ZVD96vfT8T599xRfV&branch=master)](https://travis.oncue.verizon.net/iptv/sbt-verizon)

Base plugin for building and releasing code at verizon.

## Usage


In your `project/plugins.sbt`:

```
resolvers += "internal.nexus" at "http://nexus.oncue.verizon.net/nexus/content/groups/internal"

addSbtPlugin("verizon.inf.build" % "sbt-verizon" % "1.0.+")

```

This plugin can also generate full documentation sites. Add the following to your `docs` module:

```
docs.settings

```

Then in your project.sbt, add the below (the simplest possible usage of the plugin)

```
import verizon.build._

common.settings
```

<table>
  <thead>
    <tr>
      <td><strong>key</strong></td>
      <td><strong>description</strong></td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>container.settings</code></td>
      <td>Adds support for publishing docker containers to any given module of your project.</td>
    </tr>
    <tr>
      <td><code>metadata.settings</code></td>
      <td>Automatically generates a <code>BuildInfo</code> source file that your system can use to determine the build version of this system at runtime.</td>
    </tr>
    <tr>
      <td><code>ghrelease.settings</code></td>
      <td>These settings should only ever be included at the root of your project (i.e. once per build), such that only a single Github Release is created per-build.</td>
    </tr>
    <tr>
      <td><code>specs.settings</code></td>
      <td>Add support for using the compatible version of Specs2 in your test scope</td>
    </tr>
  </tbody>
</table>
