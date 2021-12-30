lazy val app = (project in file(".")).settings(
  name := "monitoring",
  version := "0.1",
  scalaVersion := "2.13.6",
  libraryDependencies  ++= Dependencies(),
  assemblyJarName := "monitoring.jar",
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full)
)
