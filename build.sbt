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

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <developers>
    <developer>
      <id>timperrett</id>
      <name>Timothy Perrett</name>
      <url>github.com/timperrett</url>
    </developer>
  </developers>
}

sonatypeProfileName := "io.verizon"

pomPostProcess := { identity }

addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"    % "0.4.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.4")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.0.0")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"   % "0.3.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages"   % "0.6.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-site"      % "1.2.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.tpolecat"      % "tut-plugin"    % "0.5.2")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.0")

addCommandAlias("validate", ";test;scripted")
