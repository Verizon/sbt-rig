package verizon.build

import sbt._, Keys._

object Specs2Plugin extends AutoPlugin {
  object autoImport {
    val specs2version = SettingKey[String]("specs2-version")
  }

  import autoImport._

  override def trigger = noTrigger

  override lazy val projectSettings = Seq(
    specs2version := "3.6.1",
    libraryDependencies <++= (specs2version) { v =>
      Seq(
        "org.specs2" %% "specs2-core" % v % "test",
        "org.specs2" %% "specs2-junit" % v % "test"
      )
    },
    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")
  )
}
