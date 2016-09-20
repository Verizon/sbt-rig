
import verizon.build._

// enablePlugins(RigPlugin)

organization := "io.verizon.build"

name := "sbt-rig"

scalacOptions ++= Seq("-deprecation", "-feature")

sbtPlugin := true

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-Dplugin.version=" + version.value,
  "-Dscripted=true")

scriptedBufferLog := false

fork := true

coverageEnabled := false

licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/verizon/sbt-rig"))

scmInfo := Some(ScmInfo(url("https://github.com/verizon/sbt-rig"),
                            "git@github.com:verizon/sbt-rig.git"))

pomPostProcess := { () }

credentials ++= for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  username,
  password)

addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.0")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"   % "0.1.8")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.5.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.3.5")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "1.1")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.0.0")

addCommandAlias("validate", ";test;scripted")
