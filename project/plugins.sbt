
libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

addSbtPlugin("io.verizon.build" % "sbt-rig" % "1.1.20")
