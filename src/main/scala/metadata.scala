package verizon.build

import sbt._, Keys._
import sbtbuildinfo.{BuildInfoPlugin,BuildInfoKeys,BuildInfoKey}

object metadata {
  import BuildInfoKeys._

  def settings: Seq[Def.Setting[_]] = BuildInfoPlugin.projectSettings ++ Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value,
    buildInfoKeys ++= Seq(
      BuildInfoKey.action("gitRevision")(fetchGitHash),
      BuildInfoKey.action("buildDate")(fetchBuildDate)
    )
  )

  private val fetchBuildDate =
    (new java.util.Date).toString

  private def fetchGitHash: String = {
    import sys.process._

    try "git rev-parse HEAD".!!.trim
    catch {
      case e: RuntimeException =>
        "-unavalible-"
    }
  }
}
