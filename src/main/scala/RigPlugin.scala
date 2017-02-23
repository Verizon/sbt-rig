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
    // testing
    val scalaTestVersion  = SettingKey[String]("scalatest-version")
    val scalaCheckVersion = SettingKey[String]("scalacheck-version")
  }

  import autoImport._

  override def trigger = allRequirements

  override def requires =
    PromptPlugin &&
    xerial.sbt.Sonatype &&
    sbtrelease.ReleasePlugin &&
    scoverage.ScoverageSbtPlugin

  override lazy val projectSettings = common.settings
}

import sbt._, Keys._
import scala.xml.transform.{RewriteRule, RuleTransformer}

object common {
  import RigPlugin.autoImport._
  import scoverage.ScoverageKeys.{coverageReport,coverageEnabled,coverageHighlighting,coverageMinimum,coverageFailOnMinimum}
  import xerial.sbt.Sonatype, Sonatype.autoImport._

  def settings =
    compilationSettings ++
    coverageSettings ++
    testSettings ++
    releaseSettings ++
    publishingSettings ++ Seq(
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

  def testSettings = Seq(
    scalaTestVersion     := "3.0.1",
    scalaCheckVersion    := "1.13.4",
    libraryDependencies ++= Seq(
      "org.scalatest"  %% "scalatest"  % scalaTestVersion.value  % "test",
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion.value % "test"
    )
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
    )
  }

  def coverageSettings = Seq(
    /* don't delete the coverage data, so we have it to upload later */
    // cleanKeepFiles += crossTarget.value / "scoverage-data",
    coverageFailOnMinimum := false,
    coverageEnabled := {
      /* if we're running on travis, use coverage, don't otherwise */
      // isTravisBuild.value
      false // make coverage opt-in
    },
    coverageHighlighting := {
      isTravisBuild.value && scalaVersion.value.startsWith("2.11")
    },
    // Without this, scoverage shows up as a dependency in the POM file.
    // See https://github.com/scoverage/sbt-scoverage/issues/153.
    // This code was borrowed from https://github.com/mongodb/mongo-spark.
    pomPostProcess := { (node: xml.Node) =>
      new RuleTransformer(
        new RewriteRule {
          override def transform(node: xml.Node): Seq[xml.Node] = node match {
            case e: xml.Elem
                if e.label == "dependency" && e.child.exists(child => child.label == "groupId" && child.text == "org.scoverage") => Nil
            case _ => Seq(node)
          }
        }).transform(node).head
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

  val runTestWithCoverage = ReleaseStep(action = state1 => {
    val extracted = Project.extract(state1)
    val thisRef = extracted.get(thisProjectRef) // this is needed to properly run tasks in all aggregated projects

    val state2 = extracted.runAggregated((test in Test) in thisRef, state1)
    val ex1 = Project.extract(state2)

    val state3 = releaseStepTaskAggregated((coverageReport in thisRef))(state2)
    val ex2 = Project.extract(state3)

    state3
  })

  // we do this step here to force a re-compilation of the code
  // as we already know the tests have passed, but we dont want to
  // roll the instrumented code into production.
  // DOING THE NASTY SO YOU DONT HAVE TOO.
  val runPackageBinaries = ReleaseStep(action = state1 => {
    val ex1 = Project.extract(state1)
    val thisRef = ex1.get(thisProjectRef) // this is needed to properly run tasks in all aggregated projects
    // println("A. >>>>>>>>>>>> " + (version in ThisBuild).get(ex1.structure.data))
    // println("A.scala >>>>>>>>>>>> " + (scalaVersion in thisRef).get(ex1.structure.data))
    // println("A.publishTo >>>>>>>>>>>> " + (publishTo in thisRef).get(ex1.structure.data))

    // ex1.structure.allProjectRefs.foreach { proj =>
    //   println("A1. >>>>>>>>>>>> " + (coverageEnabled in proj).get(ex1.structure.data))
    // }

    // this is a total hack, and only works because version in ThisBuild is
    // the only thing we're changing outside of the reloaded state. This is
    // not an advised approach, and i wish it was not needed, but cést la vie.
    val updatedSettings = ex1.structure.allProjectRefs.map(proj
      => coverageEnabled in proj := false) ++ Seq(
      version in ThisBuild := (version in ThisBuild).get(ex1.structure.data).get,
      scalaVersion := (scalaVersion in thisRef).get(ex1.structure.data).get,
      publishTo := (publishTo in thisRef).get(ex1.structure.data).get
    )

    val newState = ex1.append(updatedSettings, state1)
    val ex2 = Project.extract(newState)
    // println("B. >>>>>>>>>>>> " + (version in ThisBuild).get(ex2.structure.data))
    // println("B.scala >>>>>>>>>>>> " + (scalaVersion in thisRef).get(ex2.structure.data))
    // println("B.publishTo >>>>>>>>>>>> " + (publishTo in thisRef).get(ex2.structure.data))
    // ex2.structure.allProjectRefs.foreach { proj =>
    //   println("B1. >>>>>>>>>>>> " + (coverageEnabled in proj).get(ex2.structure.data))
    // }

    val state3 = releaseStepTaskAggregated((packageBin in Compile) in thisRef)(newState)
    val ex3 = Project.extract(state3)
    // println("C. >>>>>>>>>>>> " + (version in ThisBuild).get(ex3.structure.data))
    // println("C.scala >>>>>>>>>>>> " + (scalaVersion in thisRef).get(ex3.structure.data))
    // println("C.publishTo >>>>>>>>>>>> " + (publishTo in thisRef).get(ex3.structure.data))

    state3
  })

  val useTravisScalaVersion = ReleaseStep(action = state1 => {
    val ex1 = Project.extract(state1)
    val thisRef = ex1.get(thisProjectRef) // this is needed to properly run tasks in all aggregated projects

    // println("X1.sonatypeRepo >>>>>>>>>>>> " +  (sonatypeStagingRepositoryProfile in thisRef).get(ex1.structure.data).get.repositoryId)


    // TIM: This is so fucking hacky words cannot even begin to explain.
    val verfun = (releaseVersion in thisRef).get(ex1.structure.data).get

    val updatedSettings =
      ex1.structure.allProjectRefs.map(proj => publishTo in proj := (publishTo in thisRef).get(ex1.structure.data).get) ++
      ex1.structure.allProjectRefs.map(proj => sonatypeStagingRepositoryProfile in proj := (sonatypeStagingRepositoryProfile in thisRef).get(ex1.structure.data).get) ++
      Seq(
        version in ThisBuild := verfun((version in ThisBuild).get(ex1.structure.data).get),
        scalaVersion := sys.env.get("TRAVIS_SCALA_VERSION").getOrElse((scalaVersion in thisRef).get(ex1.structure.data).get)
      )

    val state2 = ex1.append(updatedSettings, state1)
    val ex2 = Project.extract(state2)

    // println("X. >>>>>>>>>>>> " +  verfun((version in ThisBuild).get(ex2.structure.data).get))
    // println("X2.sonatypeRepo >>>>>>>>>>>> " +  (sonatypeStagingRepositoryProfile in thisRef).get(ex2.structure.data).get.repositoryId)
    // println("X.scala >>>>>>>>>>>> " + (scalaVersion in thisRef).get(ex2.structure.data))
    // ex2.structure.allProjectRefs.foreach { proj =>
    //   println("X.publishTo >>>>>>>>>>>> " + (publishTo in proj).get(ex2.structure.data).get)
    // }

    state2
  })

  val releaseAndClose = ReleaseStep(action = state1 => {
    val ex1 = Project.extract(state1)
    val thisRef = ex1.get(thisProjectRef)
    val repoId = (sonatypeStagingRepositoryProfile in thisRef).get(ex1.structure.data).get.repositoryId
    // println("X1.sonatypeRepo >>>>>>>>>>>> " + repoId )

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
      Seq(
        checkEnvironment(travisRepoSlug.value, travisBuildNumber.value),
        checkSnapshotDependencies,
        // ReleaseStep(action = Command.process("show scalaVersion", _)),
        inquireVersions,
        setReleaseVersion,
        checkReleaseVersion,
        tagRelease,
        runTest,
        runPackageBinaries,
        // ReleaseStep(action = Command.process("show version", _)),
        // ReleaseStep(action = Command.process("show publishTo", _)),
        // ReleaseStep(action = Command.process("show scalaVersion", _)),
        ReleaseStep(action = Command.process(s"sonatypeOpen ${travisRepoSlug.value.getOrElse("unknown")}-${travisJobNumber.value.getOrElse("0.0")}", _)),
        // setReleaseVersion,
        // ReleaseStep(action = Command.process("show version", _)),
        // ReleaseStep(action = Command.process(s"show sonatypeStagingRepositoryProfile", _)),
        // ReleaseStep(action = Command.process("show publishTo", _)),
        useTravisScalaVersion, // terrible hack. `sonatypeOpen` seems to blow away `scalaVersion`
        // ReleaseStep(action = Command.process("show version", _)),
        // ReleaseStep(action = Command.process("show publishTo", _)),
        // ReleaseStep(action = Command.process("show scalaVersion", _)),
        // ReleaseStep(action = Command.process("show sonatypeStagingRepositoryProfile", _)),
        publishArtifacts,
        // ReleaseStep(action = Command.process("show sonatypeStagingRepositoryProfile", _)),
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
