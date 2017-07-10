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

  def compilationSettings = {
    val flags = Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-unchecked",
      "-feature",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-Ywarn-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Xmax-classfile-name", (255 - 15).toString
    )

    Seq(
      scalacOptions in Compile ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 10)) => flags ++ Seq("-Xlint", "-Ywarn-value-discard")
        case Some((2, n)) if n >= 11 => flags ++ Seq(
          "-Xlint:adapted-args",
          "-Xlint:nullary-unit",
          "-Xlint:inaccessible",
          "-Xlint:nullary-override",
          "-Xlint:missing-interpolator",
          "-Xlint:doc-detached",
          "-Xlint:private-shadow",
          "-Xlint:type-parameter-shadow",
          "-Xlint:poly-implicit-overload",
          "-Xlint:option-implicit",
          "-Xlint:delayedinit-select",
          "-Xlint:by-name-right-associative",
          "-Xlint:package-object-classes",
          "-Xlint:unsound-match",
          "-Xlint:stars-align"
        ) ++ (
          if(n >= 12) {
            Seq("-Ypartial-unification")
          } else {
            Seq()
          }
        )
      }),
      scalacOptions in Test := (scalacOptions in Compile).value :+ "-language:reflectiveCalls"
    ) ++ sys.env.get("TRAVIS_SCALA_VERSION").map(scalaVersion := _)
  }

  def coverageSettings = Seq(
    /* don't delete the coverage data, so we have it to upload later */
    // cleanKeepFiles += crossTarget.value / "scoverage-data",
    coverageFailOnMinimum := false,
    coverageEnabled := {
      /* if we're running on travis, use coverage, don't otherwise */
      isTravisBuild.value
    },
    coverageHighlighting := {
      isTravisBuild.value && scalaVersion.value.startsWith("2.11")
    },
    // Without this, scoverage shows up as a dependency in the POM file.
    // See https://github.com/scoverage/sbt-scoverage/issues/153.
    // This code was borrowed from https://github.com/mongodb/mongo-spark.
    pomPostProcess ~= { ppp => (node: xml.Node) =>
      new RuleTransformer(
        new RewriteRule {
          override def transform(node: xml.Node): Seq[xml.Node] = node match {
            case e: xml.Elem
                if e.label == "dependency" && e.child.exists(child => child.label == "groupId" && child.text == "org.scoverage") => Nil
            case _ => Seq(node)
          }
        }).transform(ppp(node)).head
    }
  )

  import sbtrelease._
  import sbtrelease.ReleasePlugin.autoImport._
  import sbtrelease.ReleaseStateTransformations._
  import sbtrelease.Utilities._

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
      val rf = extracted.get(releaseVersion)
      val actualVersion = rf(v)
      Version(actualVersion).filter(v =>
        v.qualifier.forall(_.isEmpty) && v.subversions.size == 2
      ).getOrElse(sys.error(s"version $v does not match the expected pattern of x.y.z where x, y, and z are all integers."))
      st
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

  /** Forcibly disable coverage on all projects. `coverageOff` just disables
    * `coverageEnabled in ThisBuild``, but leaves coverage on projects
    * explicitly opted in. This forcibly removes it, and should be run before we
    * publish.
    */
  val turnOffCoverage = ReleaseStep { state =>
    val ex = Project.extract(state)
    reapply(ex.structure.allProjectRefs.map { proj =>
      coverageEnabled in proj := false
    }, state)}

  /** Some release steps can undo turning off coverage.  In the case of rig's
   *  default settings, sonatypeOpen turns coverage back on.  This step turns
   *  off coverage just before publishing, which will ensure that we don't
   *  publish instrumented code
   */
  val publishArtifacsWithoutInstrumentation: ReleaseStep =
    publishArtifacts.copy(
      action = turnOffCoverage.action andThen publishArtifacts.action
    )

  /** Opens a sonatype repo.  We open one explicitly so concurrent builds in
   *  the same organization to not publish to the same staging repo and
   *  prematurely release.
   */
  val openSonatypeRepo: ReleaseStep = ReleaseStep { state =>
    val ex = Project.extract(state)
    val thisRef = ex.get(thisProjectRef)
    val slug = (travisRepoSlug in thisRef).get(ex.structure.data).flatten
    val jobNumber = (travisJobNumber in thisRef).get(ex.structure.data).flatten
    val state1 = Command.process(s"sonatypeOpen ${slug.getOrElse("unknown")}-${jobNumber.getOrElse("0.0")}", state)

    val ex1 = Project.extract(state1)
    val thisRef1 = ex1.get(thisProjectRef)
    reapply(ex1.structure.allProjectRefs.flatMap { proj => Seq(
      publishTo in proj := (publishTo in thisRef1).get(ex1.structure.data).get,
      sonatypeStagingRepositoryProfile in proj := (sonatypeStagingRepositoryProfile in thisRef).get(ex1.structure.data).get
    )}, state)
  }

  val releaseAndClose: ReleaseStep = ReleaseStep(action = state1 => {
    val ex1 = Project.extract(state1)
    val thisRef = ex1.get(thisProjectRef)
    val repoId = (sonatypeStagingRepositoryProfile in thisRef).get(ex1.structure.data).get.repositoryId

    Command.process(s"sonatypeRelease $repoId", state1)
  })

  import com.typesafe.sbt.SbtPgp.autoImport._, PgpKeys._

  def releaseSettings = Seq(
    sonatypeStagingRepositoryProfile := Sonatype.StagingRepositoryProfile("unknown","unknown","unknown","unknown","unknown"),
    releasePublishArtifactsAction := publishSigned.value,
    releaseCrossBuild := false,
    releaseVcs := Some(new GitX(baseDirectory.value)), // only work with Git, sorry SVN people.
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
    releaseTagName := s"${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}",
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
        publishArtifacsWithoutInstrumentation,
        releaseAndClose
      ),
      // only job *.1 pushes tags, to avoid each independent job attempting to retag the same release
      travisJobNumber.value
        .filter { _ endsWith ".1" }
        .map { _ => pushChanges.copy(check = identity) }
        .toSeq
    ).flatten
  )
}
