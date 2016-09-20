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

  /*
      licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("http://oncue.github.io/knobs/")),
    scmInfo := Some(ScmInfo(url("https://github.com/oncue/knobs"),
                                "git@github.com:oncue/knobs.git")),
  */