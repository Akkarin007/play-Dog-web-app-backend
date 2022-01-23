name := """play-scala-seed"""
organization := "zeybek"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.7"

lazy val branch = "W02-play"
lazy val dog = RootProject(uri("git://github.com/Akkarin007/Dog.git#%s".format(branch)))

lazy val root = Project("play-dog", file("."))
  .enablePlugins(PlayScala)
  .aggregate(dog)
  .dependsOn(dog)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "zeybek.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "zeybek.binders._"
