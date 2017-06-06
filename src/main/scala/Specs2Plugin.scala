//: ----------------------------------------------------------------------------
//: Copyright (C) 2016 Verizon.  All Rights Reserved.
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

object Specs2Plugin extends AutoPlugin {
  object autoImport {
    val specs2version = SettingKey[String]("specs2-version")
  }

  import autoImport._

  override def trigger = noTrigger

  override lazy val projectSettings = {
    val specs2V = "3.9.0"
    Seq(
      specs2version := specs2V,
      libraryDependencies ++= Seq(
        "org.specs2" %% "specs2-core" % specs2V % "test",
        "org.specs2" %% "specs2-junit" % specs2V % "test"
      ),
      testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")
    )
  }
}
