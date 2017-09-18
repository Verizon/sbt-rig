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

object RigPlugin extends AutoPlugin {
  object autoImport {
    // general stuff
    val isTravisBuild     = settingKey[Boolean]("true if this build is running as either a PR or a release build within Travis CI")
    val isTravisPR        = settingKey[Boolean]("true if the current job is a pull request, false if it’s not a pull request.")
    val travisRepoSlug    = settingKey[Option[String]]("The slug (in form: owner_name/repo_name) of the repository currently being built")
    val travisJobNumber   = settingKey[Option[String]]("The number of the current job (for example, 4.1).")
    val travisBuildNumber = settingKey[Option[String]]("The number of the current build (for example, 4)")
    val travisCommit      = settingKey[Option[String]]("The commit that the current build is testing")
  }

  import autoImport._

  override def trigger = allRequirements

  override def requires =
    PromptPlugin &&
    xerial.sbt.Sonatype &&
    sbtrelease.ReleasePlugin &&
    scoverage.ScoverageSbtPlugin

  override lazy val projectSettings = common.settings

  // By making these build settings instead of project settings, we retain
  // compatibility with the coverageOn and coverageOff aliases that some people
  // will use.
  override lazy val buildSettings = common.travisSettings ++ common.coverageSettings
}

import sbt._, Keys._
import scala.xml.transform.{RewriteRule, RuleTransformer}

object common {
  import RigPlugin.autoImport._
  import scoverage.ScoverageKeys.{coverageReport,coverageEnabled,coverageHighlighting,coverageMinimum,coverageFailOnMinimum}
  import xerial.sbt.Sonatype, Sonatype.autoImport._

  def settings =
    compilationSettings ++
    releaseSettings ++
    publishingSettings

  def travisSettings = Seq(
    isTravisBuild     := sys.env.get("TRAVIS").isDefined,
    isTravisPR        := !sys.env.get("TRAVIS_PULL_REQUEST").forall(_.trim.toLowerCase == "false"),
    travisRepoSlug    := sys.env.get("TRAVIS_REPO_SLUG"),
    travisJobNumber   := sys.env.get("TRAVIS_JOB_NUMBER"),
    travisBuildNumber := sys.env.get("TRAVIS_BUILD_NUMBER"),
    travisCommit      := sys.env.get("TRAVIS_COMMIT")
  )

  def publishingSettings = Seq(
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Compile, packageSrc) := true,
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (for {
      username <- sys.env.get("SONATYPE_USERNAME")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        username, password)).toSeq
  )

  def compilationSettings =
    sys.env.get("TRAVIS_SCALA_VERSION").toList.map(scalaVersion := _)

  def coverageSettings = Seq(
    coverageFailOnMinimum := false,
    coverageEnabled := {
      /* if we're running on travis, use coverage, don't otherwise */
      isTravisBuild.value
    },
    coverageHighlighting := {
      // https://github.com/scoverage/sbt-scoverage#highlighting
      val VersionNumber((major +: minor +: patch +: _), _, _) = scalaVersion.value
      import scala.math.Ordered.orderingToOrdered
      ((major, minor, patch)) > ((2, 11, 1))
    }
  )

  import sbtrelease._
  import sbtrelease.ReleasePlugin.autoImport._, ReleasePlugin.runtimeVersion
  import sbtrelease.ReleaseStateTransformations._

  def checkEnvironment(slug: Option[String], bn: Option[String]) =
    ReleaseStep(action = st => {
      (for {
        a <- slug
        b <- bn
      } yield st).getOrElse {
        sys.error("""Cannot release from anywhere but Travis.
          Requires 'TRAVIS_REPO_SLUG' and 'TRAVIS_BUILD_NUMBER',
          to be set appropriately.""")
      }
    })

  val checkReleaseVersion = ReleaseStep(
    action = identity,
    check = { st =>
      val extracted = Project.extract(st)
      val v = extracted.get(Keys.version)
      val (st1, rf) = extracted.runTask(releaseVersion, st)
      val actualVersion = rf(v)
      Version(actualVersion).filter(v =>
        v.qualifier.forall(_.isEmpty) && v.subversions.size == 2
      ).getOrElse(sys.error(s"version $v does not match the expected pattern of x.y.z where x, y, and z are all integers."))
      st1
    })

  val runTestWithCoverage: ReleaseStep = ReleaseStep(action = state1 => {
    val extracted = Project.extract(state1)
    val thisRef = extracted.get(thisProjectRef) // this is needed to properly run tasks in all aggregated projects

    val state2 = extracted.runAggregated((test in Test) in thisRef, state1)
    val ex1 = Project.extract(state2)

    val state3 = releaseStepTaskAggregated((coverageReport in thisRef))(state2)
    val ex2 = Project.extract(state3)

    state3
  })

  /** Forcibly turns off coverage on all projects, sets publishTo to the
    * open sonatype repository, and invokes `publishArtifacts`.
    */
  val publishToSonatypeWithoutInstrumentation: ReleaseStep = ReleaseStep { state =>
    val ex = Project.extract(state)
    val thisRef = ex.get(thisProjectRef)
    // This does too much, but each call to reapply discards the rest of the release
    // settings.  We've apparently only got one bite at this apple.
    val state1 = reapply(ex.structure.allProjectRefs.flatMap { proj =>
      val repo = (sonatypeStagingRepositoryProfile in thisRef).get(ex.structure.data).get
      val path = "/staging/deployByRepositoryId/"+repo.repositoryId
      // This disappears if we don't propagate it. :(
      (sonatypeStagingRepositoryProfile in proj := repo) +:
      Seq(
        // As of sbt-sonatype-2.0, we're on our own to set this
        publishTo in proj := Some(MavenRepository(repo.repositoryId, sonatypeRepository.value + path)),
        // Never, ever, ever publish with instrumentation
        coverageEnabled in proj := false
      )
    }, state)
    publishArtifacts.action(state1)
  }

  /** Opens a sonatype repo.  We open one explicitly so concurrent builds in
   *  the same organization to not publish to the same staging repo and
   *  prematurely release.
   */
  val openSonatypeRepo: ReleaseStep = ReleaseStep { state =>
    val ex = Project.extract(state)
    val thisRef = ex.get(thisProjectRef)
    val slug = (travisRepoSlug in thisRef).get(ex.structure.data).flatten
    val jobNumber = (travisJobNumber in thisRef).get(ex.structure.data).flatten
    releaseStepCommand(s"sonatypeOpen ${slug.getOrElse("unknown")}-${jobNumber.getOrElse("0.0")}")(state)
  }

  val releaseAndClose: ReleaseStep = ReleaseStep(action = state => {
    val ex1 = Project.extract(state)
    val thisRef = ex1.get(thisProjectRef)
    val repoId = (sonatypeStagingRepositoryProfile in thisRef).get(ex1.structure.data).get.repositoryId
    state.log.info(s"Closing staging repo ${repoId}")
    releaseStepCommand(s"sonatypeDrop $repoId")(state)
  })

  import com.typesafe.sbt.SbtPgp.autoImport._, PgpKeys._

  def releaseSettings = Seq(
    releasePublishArtifactsAction := publishSigned.value,
    releaseCrossBuild := false,
    releaseVersion := { ver =>
      travisBuildNumber.value.orElse(sys.env.get("BUILD_NUMBER"))
        .flatMap(s => try Option(s.toInt) catch { case _: NumberFormatException => Option.empty[Int] })
        .flatMap(ci => Version(ver).map { v =>
          val subversions = v.subversions.take(1) :+ ci
          v.copy(subversions = subversions, qualifier = None).string
        })
        .orElse(Version(ver).map(_.withoutQualifier.string))
        .getOrElse(versionFormatError)
    },
    releaseTagName := runtimeVersion.value, // 'Round these parts, we don't add the "v" prefix
    releaseProcess := Seq(
      Seq[ReleaseStep](
        checkEnvironment(travisRepoSlug.value, travisBuildNumber.value),
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        checkReleaseVersion,
        tagRelease,
        runTestWithCoverage,
        openSonatypeRepo,
        publishToSonatypeWithoutInstrumentation,
        releaseAndClose,
      ),
      // only job *.1 pushes tags, to avoid each independent job attempting to retag the same release
      travisJobNumber.value
        .filter { _ endsWith ".1" }
        .map { _ => pushChanges.copy(check = identity) }
        .toSeq
    ).flatten
  )
}
