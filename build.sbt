import sbt.Keys.version

import sbtassembly.AssemblyPlugin.assemblySettings  // put this at the top of the file

lazy val root = project
  .in(file("."))
  .settings(
    name := "ObchodniRejstrik",
    version := "0.0.2",
    scalaVersion := "2.13.4",
    compileOrder := CompileOrder.JavaThenScala,
    javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-g:lines"),
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
    libraryDependencies += "org.rogach" %% "scallop" % "4.0.2",
    //assemblySettings()
)