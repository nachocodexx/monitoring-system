
lazy val app = (project in file(".")).settings(
  name := "monitoring",
  version := "0.1",
  scalaVersion := "2.13.6",
  libraryDependencies  ++= Dependencies(),
  assemblyJarName := "monitoring.jar",
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full),
  ThisBuild / assemblyMergeStrategy := {
    case x if x.contains("reflect.properties")=> MergeStrategy.last
    case x if x.contains("scala-collection-compat.properties")=> MergeStrategy.last
//    case x if x.contains("META-INF/io.netty.versions.properties")=> MergeStrategy.last
//    case x if x.contains("META-INF/versions/9/module-info.class")=> MergeStrategy.last
    case x =>
      //      println(x)
      val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)
