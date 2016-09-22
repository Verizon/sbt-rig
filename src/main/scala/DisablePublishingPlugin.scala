package verizon.build

import sbt._, Keys._

object DisablePublishingPlugin extends AutoPlugin {

  override def trigger = noTrigger

  override def requires = RigPlugin

  override lazy val projectSettings = Seq(
    publishLocal := {},
    publish := {},
    publishArtifact in Test := false,
    publishArtifact in Compile := false,
    publishArtifact in (Test, packageBin) := false,
    publishArtifact in (Test, packageDoc) := false,
    publishArtifact in (Test, packageSrc) := false,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact := false
  )
}
