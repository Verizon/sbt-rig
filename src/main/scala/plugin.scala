package verizon.build

import sbt._, Keys._

object TravisPlugin extends AutoPlugin {
  object autoImport {
    // general stuff
    val isTravisBuild     = settingKey[Boolean]("true if this build is running as either a PR or a release build within Travis CI")
    val isTravisPR        = settingKey[Boolean]("true if the current job is a pull request, false if it’s not a pull request.")
    val travisRepoSlug    = settingKey[Option[String]]("The slug (in form: owner_name/repo_name) of the repository currently being built")
    val travisJobNumber   = settingKey[Option[String]]("The number of the current job (for example, 4.1).")
    val travisBuildNumber = settingKey[Option[String]]("The number of the current build (for example, 4)")
    val travisCommit      = settingKey[Option[String]]("The commit that the current build is testing")

    // testing
    val scalaTestVersion  = SettingKey[String]("scalatest-version")
    val scalaCheckVersion = SettingKey[String]("scalacheck-version")
  }

  import autoImport._

  override def trigger = allRequirements

  override def requires = sbtbuildinfo.BuildInfoPlugin

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    isTravisBuild     := sys.env.get("TRAVIS").isDefined,
    isTravisPR        := !sys.env.get("TRAVIS_PULL_REQUEST").forall(_.trim.toLowerCase == "false"),
    travisRepoSlug    := sys.env.get("TRAVIS_REPO_SLUG"),
    travisJobNumber   := sys.env.get("TRAVIS_JOB_NUMBER"),
    travisBuildNumber := sys.env.get("TRAVIS_BUILD_NUMBER"),
    travisCommit      := sys.env.get("TRAVIS_COMMIT")
  )

  override lazy val projectSettings = common.settings
}
