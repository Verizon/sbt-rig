
scalaVersion in Global  := "2.11.7"

lazy val root = project.in(file(".")).aggregate(core, http, rpc)

lazy val core = project

lazy val http = project.dependsOn(core % "test->test;compile->compile")

lazy val rpc = project.dependsOn(core % "test->test;compile->compile")
