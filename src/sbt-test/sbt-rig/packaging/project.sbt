

scalaVersion in Global  := "2.11.7"

resolvers in Global += {
  val testRepoUri = baseDirectory.value.toURI + "/repo"
  "Test Maven" at testRepoUri
}

lazy val root = project.in(file(".")).aggregate(core, http, rpc, withScalaz)

lazy val core = project

lazy val withScalaz = project.settings(
  libraryDependencies ++= Seq(
    "com.example" %% "hello-world" % "1.0" scalaz "7.1",
    "com.example" %% "foo-bar" % "1.0-SNAPSHOT" scalaz "7.2"
  )
)

lazy val http = project.dependsOn(core % "test->test;compile->compile")

lazy val rpc = project.dependsOn(core % "test->test;compile->compile")
