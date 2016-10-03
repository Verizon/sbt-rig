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
