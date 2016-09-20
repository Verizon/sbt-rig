# sbt-rig

[![Build Status](https://travis-ci.org/verizon/sbt-rig.svg)](https://travis-ci.org/verizon/sbt-rig)

Base plugin for building and releasing code, with all the boilerplate settings that one usually needs.

## Usage

In your `project/plugins.sbt`:

```
addSbtPlugin("io.verizon.build" % "sbt-rig" % "1.0.+")

```

Then in your build.sbt, add the below (the simplest possible usage of the plugin)

```
import verizon.build._

enablePlugins(RigPlugin)
```

If you want to publish to maven central (this plugin mostly assumes you do), then in that case you will need to set the following:

```
licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://yoursite.com"))

scmInfo := Some(ScmInfo(url("https://github.com/yourorg/yourproj"),
                            "git@github.com:yourorg/yourproj.git"))
```

In addition to these modules


<table>
  <thead>
    <tr>
      <td><strong>key</strong></td>
      <td><strong>description</strong></td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>metadata.settings</code></td>
      <td>Automatically generates a <code>BuildInfo</code> source file that your system can use to determine the build version of this system at runtime.</td>
    </tr>
    <tr>
      <td><code>specs.settings</code></td>
      <td>Add support for using the compatible version of Specs2 in your test scope</td>
    </tr>
  </tbody>
</table>
