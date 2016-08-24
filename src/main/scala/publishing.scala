package verizon.build

import sbt._, Keys._

object publishing {
  import TravisPlugin.autoImport.isTravisBuild
  import bintray.BintrayKeys._

  def settings = bintraySettings ++ Seq(
    publishMavenStyle := true,
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("http://oncue.github.io/knobs/")),
    scmInfo := Some(ScmInfo(url("https://github.com/oncue/knobs"),
                                "git@github.com:oncue/knobs.git")),
    pomIncludeRepository := { _ => false },
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Compile, packageSrc) := true,
    publishArtifact in (Test, packageBin)    := false,
    publishArtifact in (Compile, packageDoc) := false
  )

  private def bintraySettings = Seq(
    bintrayPackageLabels := Seq("configuration", "functional programming", "scala", "reasonable"),
    bintrayOrganization := Some("oncue"),
    bintrayRepository := "releases",
    bintrayPackage := "knobs"
  )
}
