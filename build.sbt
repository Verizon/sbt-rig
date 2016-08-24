
organization := "com.verizon.labs"

name := "sbt-travis-ci"

scalacOptions ++= Seq("-deprecation", "-feature")

sbtPlugin := true

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-Dplugin.version=" + version.value,
  "-Dscripted=true")

scriptedBufferLog := false

fork := true

// coverageEnabled := false

addSbtPlugin("me.lessis"         % "bintray-sbt"   % "0.3.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.0")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"   % "0.1.8")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.5.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.3.5")

addCommandAlias("validate", ";test;scripted")
