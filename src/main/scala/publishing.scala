package verizon.build

import sbt._, Keys._

object publishing {
  def ignore = Seq(
    publish := { () },
    publishLocal := { () },
    publishArtifact in Test := false,
    publishArtifact in Compile := false
  )
}
