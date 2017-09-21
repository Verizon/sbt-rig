

scalaVersion in Global  := "2.11.11"

resolvers in Global += {
  val testRepoUri = baseDirectory.value.toURI + "/repo"
  "Test Maven" at testRepoUri
}

lazy val root = project.in(file(".")).aggregate(core, http, rpc)

lazy val core = project

lazy val http = project.dependsOn(core % "test->test;compile->compile")

lazy val rpc = project.dependsOn(core % "test->test;compile->compile")
