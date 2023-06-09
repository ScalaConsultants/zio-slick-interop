val zioVersion       = "2.0.0"
val zioRSVersion     = "2.0.0"
val slickVersion     = "3.4.1"
val scalaTestVersion = "3.1.1"

inThisBuild(
  List(
    organization := "io.scalac",
    homepage     := Some(url("https://github.com/ScalaConsultants/zio-slick-interop")),
    licenses     := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    developers   := List(
      Developer(
        id = "jczuchnowski",
        name = "Jakub Czuchnowski",
        email = "jakub.czuchnowski@gmail.com",
        url = url("https://github.com/jczuchnowski")
      )
    )
  )
)

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

val root = (project in file("."))
  .settings(
    name               := "zio-slick-interop",
    scalaVersion       := "2.13.11",
    crossScalaVersions := Seq("2.12.17", "2.13.11"),
    // JavaConverters ¯\_(ツ)_/¯
    Test / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
    scalacOptions ++= {
      if (priorTo2_13(scalaVersion.value)) compilerOptions
      else
        compilerOptions.flatMap {
          case "-Ywarn-unused-import" => Seq("-Ywarn-unused:imports")
          case "-Xfuture"             => Nil
          case other                  => Seq(other)
        }
    },
    testFrameworks     := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Seq(
      "com.typesafe.slick"     %% "slick"                       % slickVersion % Provided,
      "dev.zio"                %% "zio"                         % zioVersion   % Provided,
      "dev.zio"                %% "zio-interop-reactivestreams" % zioRSVersion % Provided,
      "org.scala-lang.modules" %% "scala-collection-compat"     % "2.9.0"      % Test,
      "com.h2database"          % "h2"                          % "2.1.214"    % Test,
      "dev.zio"                %% "zio-test-sbt"                % zioVersion   % Test
    )
  )

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }
