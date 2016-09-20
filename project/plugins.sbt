
libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

resolvers += "internal.nexus" at "http://nexus.oncue.verizon.net/nexus/content/groups/internal"

// addSbtPlugin("io.verizon.build" % "sbt-rig" % "1.0.0-SNAPSHOT")
