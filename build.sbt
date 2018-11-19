lazy val root = (project in file("."))
  .settings(
    commonSettings,
    compilerPlugins,
    compilerOptions,
    dependencies,
    testSettings,
  )

lazy val commonSettings = Seq(
  organization := "com.ocado.gm.wms",
  name := "tagless-logging",
  scalaVersion := "2.12.4",
  // due to a bug in sbt 1.0.x - should be removed when using 0.13.x
  updateOptions := updateOptions.value.withGigahorse(false),
)

val compilerPlugins = Seq(
  addCompilerPlugin("io.tryp" % "splain" % "0.3.1" cross CrossVersion.patch),
  addCompilerPlugin("com.softwaremill.clippy" %% "plugin" % "0.5.3" classifier "bundle"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  addCompilerPlugin(("org.scalameta" % "paradise" % "3.0.0-M11").cross(CrossVersion.full)),
)

lazy val compilerOptions =
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-language:higherKinds",
    "-Ypartial-unification",
    "-P:splain:all:true",
    "-P:clippy:colors=true",
    "-language:implicitConversions"
  )

lazy val dependencies = {
  val cats = Seq(
    "org.typelevel" %% "cats-core" % "1.4.0",
    "org.typelevel" %% "cats-effect" % "1.0.0",
    "org.typelevel" %% "cats-mtl-core" % "0.4.0",
    "com.github.mpilquist" %% "simulacrum" % "0.14.0",
    "org.typelevel" %% "cats-tagless-macros" % "0.1.0"
  )

  val config = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.9.1",
  )

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
  )

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.1"
  )

  Seq(
    libraryDependencies ++= cats ++ config ++ logging ++ akkaHttp
  )
}

lazy val testSettings = {
  val dependencies = {
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % "3.0.4",
      "org.scalatest" %% "scalatest" % "3.0.5",
      "com.ironcorelabs" %% "cats-scalatest" % "2.3.1",
    ).map(_ % Test)
  }

  Seq(
    logBuffered in Test := false,
    dependencies
  )
}
