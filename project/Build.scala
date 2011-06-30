import sbt._
import Keys._

object CovalentGradebookImportToolBuild extends Build {

  val localm2RepoPath = Path.userHome / ".m2" / "repository"

	lazy val mySettings = Seq(
    name := "Test Bug",
    version := "1.0",
    organization := "com.wintermutesys",
    scalaVersion := "2.9.0-1",

    resolvers ++= Seq(
      "Typesafe Repository" at "http://scala-tools.org/repo-snapshots"
    ),

    libraryDependencies ++= {
      Seq(
        "org.apache.activemq" % "activemq-core" % "5.5.0"
        )
    }

  )

  val customSettings = Defaults.defaultSettings ++ mySettings 

  lazy val myProject = Project("Test Bug", file("."), settings = customSettings)
}
