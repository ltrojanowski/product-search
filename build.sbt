ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

ThisBuild / crossScalaVersions := Seq("2.13.8", "2.12.17")

ThisBuild / name := "product-search"

ThisBuild / organization := "com.ltrojanowski"

ThisBuild / licenses := Seq("MIT" -> url("https://github.com/ltrojanowski/product-search/blob/master/LICENSE.md"))

ThisBuild / description := "This library allows you to find a specific type in a nested tuple."

import xerial.sbt.Sonatype._
ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("ltrojanowski", "product-search", ""))

// publish to the sonatype repository
ThisBuild / publishTo := sonatypePublishToBundle.value

lazy val root = (project in file("."))
  .settings(
    name := "product-search",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalameta" %% "munit" % "0.7.28" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(core)

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalameta" %% "munit" % "0.7.28" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
