import ReleaseTransformations._
import ReleasePlugin.autoImport._

val zioVersion       = "1.0.0"
val zioRSVersion     = "1.0.3.5"
val slickVersion     = "3.3.2"
val scalaTestVersion = "3.1.1"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xfatal-warnings",
  "-Ywarn-unused-import"
)

val publishSettings = Seq(
  releaseUseGlobalVersion := true,
  releaseVersionFile := file(".") / "version.sbt",
  releaseCommitMessage := s"Set version to ${version.value}",
  releaseIgnoreUntrackedFiles := true,
  releaseCrossBuild := true,
  homepage := Some(url("https://github.com/ScalaConsultants/zio-slick-interop")),
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/ScalaConsultants/zio-slick-interop"),
      "scm:git:git@github.com:ScalaConsultants/zio-slick-interop.git"
    )
  ),
  developers := List(
    Developer(
      id = "vpavkin",
      name = "Vladimir Pavkin",
      email = "vpavkin@gmail.com",
      url = url("https://pavkin.ru")
    )
  ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

val root = (project in file("."))
  .settings(
    organization := "io.scalac",
    name := "zio-slick-interop",
    scalaVersion := "2.13.2",
    crossScalaVersions := Seq("2.12.11", "2.13.2"),
    // JavaConverters ¯\_(ツ)_/¯
    scalacOptions in Test ~= (_ filterNot (_ == "-Xfatal-warnings")),
    scalacOptions ++= {
      if (priorTo2_13(scalaVersion.value)) compilerOptions
      else
        compilerOptions.flatMap {
          case "-Ywarn-unused-import" => Seq("-Ywarn-unused:imports")
          case "-Xfuture"             => Nil
          case other                  => Seq(other)
        }
    },
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick"                       % slickVersion,
      "dev.zio"            %% "zio"                         % zioVersion,
      "dev.zio"            %% "zio-interop-reactivestreams" % zioRSVersion,
      "com.h2database"     % "h2"                           % "1.4.200" % Test,
      "dev.zio"            %% "zio-test-sbt"                % zioVersion % Test
    )
  )
  .settings(publishSettings: _*)

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }
