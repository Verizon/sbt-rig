package verizon.build

import sbt._, Keys._
import scala.xml.transform.{RewriteRule, RuleTransformer}

object common {
  import TravisPlugin.autoImport._
  import scoverage.ScoverageKeys.{coverageReport,coverageEnabled,coverageHighlighting,coverageMinimum,coverageFailOnMinimum}

  def settings =
    compilationSettings ++
    coverageSettings ++
    testSettings ++
    releaseSettings ++
    prompt.settings ++
    publishing.settings

  def testSettings = Seq(
    scalaTestVersion     := "2.2.6",
    scalaCheckVersion    := "1.12.5",
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
      "-Xfuture"
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
        )
      }),
      // Tim P: Removed this because of the tendancy to introduce
      // issues with the bytecode: https://issues.scala-lang.org/browse/SI-3882
      // scalacOptions <++= (version) map { v =>
      //   if (v.endsWith("SNAPSHOT")) Nil else Seq("-optimize")
      // },
      scalacOptions in Test := (scalacOptions in Compile).value :+ "-language:reflectiveCalls"
    )
  }

  import java.time.Instant

  def coverageSettings = Seq(
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
        v.qualifier.forall(_.isEmpty) && v.minor.isDefined && v.bugfix.isDefined
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

    // this is a total hack, and only works because version in ThisBuild is
    // the only thing we're changing outside of the reloaded state. This is
    // not an advised approach, and i wish it was not needed, but cést la vie.
    val updatedSettings = ex1.structure.allProjectRefs.map(proj
      => coverageEnabled in proj := false) ++ Seq(
      version in ThisBuild := (version in ThisBuild).get(ex1.structure.data).get,
      scalaVersion in Global := (scalaVersion in thisRef).get(ex1.structure.data).get
    )

    val newState = ex1.append(updatedSettings, state1)
    val ex2 = Project.extract(newState)

    val state3 = releaseStepTaskAggregated((packageBin in Compile) in thisRef)(newState)
    val ex3 = Project.extract(state3)

    state3
  })

  def releaseSettings = Seq(
    releaseCrossBuild := false,
    releaseVersion := { ver =>
      travisBuildNumber.value.orElse(sys.env.get("BUILD_NUMBER"))
        .map(s => try Option(s.toInt) catch { case _: NumberFormatException => Option.empty[Int] })
        .flatMap(ci => Version(ver).map(_.withoutQualifier.copy(bugfix = ci).string))
        .orElse(Version(ver).map(_.withoutQualifier.string))
        .getOrElse(versionFormatError)
    },
    releaseTagName := s"${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}",
    releaseProcess := Seq(
      Seq(
        checkEnvironment(travisRepoSlug.value, travisBuildNumber.value),
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        checkReleaseVersion,
        tagRelease,
        runTestWithCoverage,
        runPackageBinaries,
        publishArtifacts
      ),
      // only job *.1 pushes tags, to avoid each independent job attempting to retag the same release
      travisJobNumber.value
        .filter { _ endsWith ".1" }
        .map { _ => pushChanges.copy(check = identity) }
        .toSeq
    ).flatten
  )
}
