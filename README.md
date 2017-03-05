# sbt-rig

![sbt-rig](docs/img/logo.png)

[![Build Status](https://travis-ci.org/Verizon/sbt-rig.svg?branch=master)](https://travis-ci.org/Verizon/sbt-rig)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.verizon.build/sbt-rig/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.verizon.build/sbt-rig)

This plugin does all the rigging and fiddly bits and bobs that the typical open source users wants when releasing their software for others. The following assumptions are made:

1. You want to do continuous integration and release, and you'll be using [travis-ci.org](https://travis-ci.org) to do this.
1. You'll be releasing to oss.sonatype.org, and that you already claimed your top-level profile name in maven central. See [this sonatype docs for information](http://central.sonatype.org/pages/ossrh-guide.html) on that.

## Usage

Before looking over the items below, you might find it useful to read [this article](http://timperrett.com/2016/10/02/continuous-delivery-for-scala-with-travisci/) which explains the rational of sbt-rig, and common setup instructions for users.

In your `project/plugins.sbt`:

```
addSbtPlugin("io.verizon.build" % "sbt-rig" % "1.1.+")
```

That's all you need to do. The plugin itself makes use of SBT auto-plugins, so you never need to explicitly enable it for the common functionality sbt-rig provides. There are a set of optional modules (see below) that you can explicitly enable for extra functionality.

### Publishing to Central

If you want to publish to maven central (this plugin assumes you do), then the first thing you need to do is configure PGP signing. Under the hood the sbt-rig plugin makes use of sbt-pgp, so please [read the docs](http://www.scala-sbt.org/sbt-pgp/) for that, and once you have a ring setup, and your GPG ring passphrase is available to SBT (this usually lives in `~/.sbt/0.13/gpg.sbt`), set the following settings in `project/CentralRequirementsPlugin.scala`:

```scala
import sbt._, Keys._
import xerial.sbt.Sonatype.autoImport.sonatypeProfileName

object CentralRequirementsPlugin extends AutoPlugin {
  // tells sbt to automatically enable this plugin where ever
  // the sbt-rig plugin is enabled (which should be all sub-modules)
  override def trigger = allRequirements

  override def requires = verizon.build.RigPlugin

  override lazy val projectSettings = Seq(
    // this tells sonatype what profile to use
    // (usually this is what you registered when you signed up
    // for maven central release via their OSS JIRA ticket process)
    sonatypeProfileName := "com.yourdomain",
    // inform central who was explicitly involved in developing
    // this project. Note that this is *required* by central.
    developers += Developer("timperrett", "Timothy Perrett", "", url("http://github.com/timperrett")),
    // what license are you releasing this under?
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    // where can users find information about this project?
    homepage := Some(url("http://verizon.github.io/quiver/")),
    // show users where the source code is located
    scmInfo := Some(ScmInfo(url("https://github.com/yourorg/yourproj"),
                                "git@github.com:yourorg/yourproj.git"))
  )
}
```

These values enable your build to meet the maven central requirements for publishing. All artifacts must be signed using your registered GPG keyring.

### Reporting Code Coverage

By default, when building on Travis `sbt-rig` will conduct two passes of compilation: one with code coverage enabled (making use of [scoverage](https://github.com/scoverage/sbt-scoverage)) and another without instrumentation so that the byte code in the output JARs is free of any scoverage plumbing. In order to report on this code coverage, all you need to do is add the following to your `.travis.yml`:

```
after_success:
  - "bash <(curl -s https://codecov.io/bash) -r $TRAVIS_REPO_SLUG -t $CODECOV_TOKEN"

```

This assumes that you have fetched your codecov report token and encrypted it into your `.travis.yml`. This is usually done on the command line inside your project, something like this:

```
travis encrypt --add CODECOV_TOKEN=XXXXXXXXXXXX
```

That's it. Codecov.io will now display the code coverage reports for you, and comment on your pull requests with deltas in coverage values.

### Disabling Code Coverage

Sometimes you might have modules that you simply do not want code coverage reporting enabled on, and in this case you can simply add the following setting to that particular `build.sbt`:

```
coverageEnabled := false
```

By default the plugin detects if the build is running on travis-ci and enables the coverage during the first compilation pass, and disables it on the second pass.

If you find yourself having problems with the coverage implementation (this sometimes happens) then be sure to read the [scoverage troubleshooting guide](https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages).

### Optional Plugins

In addition to the following plugins are provided by `sbt-rig` but are not explicitly enabled by default. These are optional, and you may never use them.

<table>
  <thead>
    <tr>
      <td><strong>key</strong></td>
      <td><strong>description</strong></td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>enablePlugins(Specs2Plugin)</code></td>
      <td>Add support for using the compatible version of Specs2 in your test scope</td>
    </tr>
    <tr>
      <td><code>enablePlugins(DisablePublishingPlugin)</code></td>
      <td>Disables all the artifact publishing for that particular module - no documentation, no binaries, no pos etc</td>
    </tr>
  </tbody>
</table>

