
libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

resolvers += "internal.nexus" at "http://nexus.oncue.verizon.net/nexus/content/groups/internal"
