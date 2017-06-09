//: ----------------------------------------------------------------------------
//: Copyright (C) 2017 Verizon.  All Rights Reserved.
//:
//:   Licensed under the Apache License, Version 2.0 (the "License");
//:   you may not use this file except in compliance with the License.
//:   You may obtain a copy of the License at
//:
//:       http://www.apache.org/licenses/LICENSE-2.0
//:
//:   Unless required by applicable law or agreed to in writing, software
//:   distributed under the License is distributed on an "AS IS" BASIS,
//:   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//:   See the License for the specific language governing permissions and
//:   limitations under the License.
//:
//: ----------------------------------------------------------------------------
package verizon.build

import sbt._, Keys._

object MetadataPlugin extends AutoPlugin {
  import sbtbuildinfo.{BuildInfoPlugin,BuildInfoKeys,BuildInfoKey}, BuildInfoKeys._
  import RigPlugin.autoImport._

  override def trigger = noTrigger

  override def requires =
    RigPlugin &&
    sbtbuildinfo.BuildInfoPlugin

  override lazy val projectSettings = Seq(
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
