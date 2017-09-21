
libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
)

addSbtPlugin("io.verizon.build" % "sbt-rig" % "4.0.36")
