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

class GitX(baseDir: java.io.File) extends sbtrelease.Git(baseDir){
  /*
   * push --tags is broken, and pushCommits is dangerous.
   * using --follow-tags should push both commits (if there are any) and
   * also any tags that got created for which commits exist in remote.
   * http://stackoverflow.com/questions/5195859/push-a-tag-to-a-remote-repository-using-git
   */
  override def pushChanges = cmd("push", "--tags", trackingRemote)
}