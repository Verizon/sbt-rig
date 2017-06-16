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
import scala.language.existentials

object DocsPlugin extends AutoPlugin { self =>
  object autoImport {
    val preStageSiteDirectory = SettingKey[File]("pre-stage-site-directory")
    val siteStageDirectory    = SettingKey[File]("site-stage-directory")
    val copySiteToStage       = TaskKey[Unit]("copy-site-to-stage")
    val gitRemoteHost         = SettingKey[String]("git-remote-host", "the host name of the remote git server (ex: github.com)")
    val githubOrg             = SettingKey[String]("github-org", "the organization name of a project within GitHub (ex: Verizon)")
    val githubRepoName        = SettingKey[String]("github-repo-name", "the repository name of a project within GitHub (ex: knobs)")
    val siteUnidocPath        = SettingKey[String]("site-unidoc-path", "the path of the site at which the scaladoc should be located (ex: api)")
  }

  import autoImport._

  override def trigger = noTrigger

  override def requires =
    RigPlugin &&
    DisablePublishingPlugin &&
    com.typesafe.sbt.site.SitePlugin &&
    com.typesafe.sbt.sbtghpages.GhpagesPlugin &&
    com.typesafe.sbt.site.hugo.HugoPlugin &&
    sbtunidoc.ScalaUnidocPlugin &&
    tut.TutPlugin

  override lazy val projectSettings = self.settings

  import java.net.URI
  // site
  import com.typesafe.sbt.site.SitePreviewPlugin.autoImport.previewFixedPort
  import com.typesafe.sbt.site.SitePlugin, SitePlugin.autoImport._
  import com.typesafe.sbt.site.hugo.HugoPlugin, HugoPlugin.autoImport._
  import com.typesafe.sbt.sbtghpages.GhpagesPlugin, GhpagesPlugin.autoImport._
  import com.typesafe.sbt.SbtGit.git
  // unidoc
  import sbtunidoc.BaseUnidocPlugin, BaseUnidocPlugin.autoImport._
  import sbtunidoc.ScalaUnidocPlugin, ScalaUnidocPlugin.autoImport._
  // release
  import sbtrelease.ReleasePlugin.autoImport._
  // tut
  import tut.TutPlugin.autoImport._
  // rig
  import RigPlugin.autoImport._
  import scoverage.ScoverageKeys.coverageEnabled

  /**
   * These settings should only ever be used in isolation, on a documentation
   * specific module of your project.
   */
  def settings =
    Seq(
      coverageEnabled := false,
      gitRemoteHost := "github.com",
      githubOrg := repoFromSlug.map(_.org).getOrElse("example"),
      githubRepoName := repoFromSlug.map(_.name).getOrElse("test"),
      baseURL in Hugo := {
        if(isTravisBuild.value){
          if(gitRemoteHost.value == "github.com"){
            new URI(s"https://${githubOrg.value}.github.io/${githubRepoName.value}")
          } else {
            new URI(s"https://${gitRemoteHost.value}/pages/${githubOrg.value}/${githubRepoName.value}")
          }
        } else {
          new URI(s"http://127.0.0.1:${previewFixedPort.value.getOrElse(1313)}")
        }
      },
      preStageSiteDirectory := sourceDirectory.value / "hugo",
      siteStageDirectory := target.value / "site-stage",
      sourceDirectory in Hugo := siteStageDirectory.value,
      watchSources := {
        // nasty hack to remove the target directory from watched sources
        watchSources.value
          .filterNot(_.getAbsolutePath.startsWith(
            target.value.getAbsolutePath))
      },
      copySiteToStage := {
        streams.value.log.debug(s"copying ${preStageSiteDirectory.value} to ${siteStageDirectory.value}")

        IO.copyDirectory(
          source = preStageSiteDirectory.value,
          target = siteStageDirectory.value,
          overwrite = false,
          preserveLastModified = true)
      },
      copySiteToStage := copySiteToStage.dependsOn(tut).value,
      makeSite := makeSite.dependsOn(copySiteToStage).value,
      tutSourceDirectory := sourceDirectory.value / "tut",
      // all .md|markdown files go into `content` dir for hugo processing
      tutTargetDirectory := siteStageDirectory.value / "content",
      ghpagesNoJekyll := true,
      git.remoteRepo := s"git@${gitRemoteHost.value}:${githubOrg.value}/${githubRepoName.value}.git",
      includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.yml" | "*.md",
      releasePublishArtifactsAction := Def.taskDyn {
        // only job *.1 pushes site, to avoid each independent job attempting to publish it
        if (sys.env.get("TRAVIS_JOB_NUMBER").exists(_.endsWith(".1"))) Def.task(ghpagesPushSite.value)
        else Def.task(())
      }.value,
      ghpagesPushSite := ghpagesPushSite.dependsOn(makeSite).value,
      // unidoc settings
      siteUnidocPath := "api",
      autoAPIMappings := true,
      addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteUnidocPath),
      scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-doc-source-url", s"https://${gitRemoteHost.value}/${githubOrg.value}/${githubRepoName.value}/blob/master€{FILE_PATH}.scala",
        "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
        "-no-link-warnings", // Suppresses problems with Scaladoc @throws links
                             // don't generate ScalaDoc for scalaz package that for some reason shows up
        "-skip-packages", "scalaz")
    )

  final case class GitHubRepo(org: String, name: String)

  def repoFromSlug: Option[GitHubRepo] =
    sys.env.get("TRAVIS_REPO_SLUG").map(_.split("/").toList) collect {
      case (org :: name :: Nil) => GitHubRepo(org, name)
    }
}
