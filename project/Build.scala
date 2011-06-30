import java.io.File
import sbt._
import UpdateReport._
import Keys._
import Path._
//import org.sbtidea._

object CovalentGradebookImportToolBuild extends Build {

  val localm2RepoPath = Path.userHome / ".m2" / "repository"

	lazy val mySettings = Seq(
    name := "Covalent Gradebook Import Tool",
    version := "1.0",
    organization := "com.cengage",
    scalaVersion := "2.9.0-1",

    resolvers ++= Seq(
      "Local Maven Repository" at "file://"+localm2RepoPath.absolutePath,
      "Typesafe Repository" at "http://scala-tools.org/repo-snapshots"
    ),

    libraryDependencies ++= {
      val snapshotVersion = "2.0.0-SNAPSHOT"
      Seq(
        "com.cengage.covalent.gradebook" % "CovalentAkkaSupport" % snapshotVersion withSources() changing,
        "com.cengage.covalent.gradebook" % "CovalentAggregationMessages" % snapshotVersion withSources() changing,
        "com.cengage.covalent.gradebook" % "CovalentGradebookPersistor" % snapshotVersion classifier "classes" withSources() changing,
        "com.cengage.covalent.gradebook" % "CovalentGradebookTakeAggregator" % snapshotVersion classifier "classes" withSources() changing,
        "com.cengage.covalent.gradebook" % "MindTapGradebookAggregator" % snapshotVersion classifier "classes" withSources() changing,
        "com.cengage.nextbook" % "MindTapGradebookReportModel" % snapshotVersion withSources() changing,
        "org.apache.camel" % "camel-quartz" % "2.7.1",
        "org.apache.activemq" % "activemq-core" % "5.5.0"
        )
    },

    initialCommands := {
      """
        import com.cengage.covalent.gradebook._
        import GradebookSystemBoot._
        import akka.actor._
        import org.apache.camel._
        import scala.collection.JavaConversions._
        import java.net._
        import akka.camel._
        import com.cengage.mongo.MongoWrapper._
        import com.google.code.morphia._
        import com.cengage.covalent.gradebook.model._
        import course._
        import activity._
        import aggregate._
        import grader._
        import property._
        import usage._
        import user._
        boot
      """ }

  )

  lazy val customClasspath = Seq(Compile, Test, Runtime).map { config =>
    managedClasspath in config <<= (classpathConfiguration in config, classpathTypes in config, update, streams) map mavenManagedJars
  }

  /**
   * Customized version of the default task that maps to the local maven repository if present, otherwise falls back on Ivy.
   */
  def mavenManagedJars(config: Configuration, jarTypes: Set[String], up: UpdateReport, s: TaskStreams): Classpath = {
    val f = DependencyFilter.make(configurationFilter(config.name), moduleFilter(), artifactFilter(`type` = jarTypes))
    val moduleIdsAndArtifacts = for(cReport <- up.configurations; mReport <- cReport.modules; (artifact, file) <- mReport.artifacts if f(cReport.configuration, mReport.module, artifact))  yield {
      if(artifact == null) error("Null file: conf=" + cReport.configuration + ", module=" + mReport.module + ", art: " + artifact)
      (mReport.module, artifact, file)
    }

		moduleIdsAndArtifacts.foreach { e =>
      val (modId, artifact, ivyFile) = e
      s.log.debug("Got module %s, artifact %s".format(modId, artifact))
    }
    val mavenFiles = for ((id, a, ivyFile) <- moduleIdsAndArtifacts)
      yield {
        val f = localm2RepoPath / id.organization.replace(".", "/") / id.name / id.revision /
          "%s-%s%s.%s".format(id.name, id.revision, if (a.classifier.isDefined) "-"+a.classifier.get else "", a.extension)
        Attributed(if (f.exists) f else ivyFile)(AttributeMap.empty.put(artifact.key, a).put(configuration.key, config))
    }

    val uniqueFiles = mavenFiles.distinct

    uniqueFiles.foreach(e => s.log.debug("Classpath entry: %s".format(e)))
    s.log.debug("Total classpath entries: %d".format(uniqueFiles.size))
    uniqueFiles
  }

  val customSettings = Defaults.defaultSettings ++ mySettings ++ customClasspath

  lazy val myProject = Project("Covalent Gradebook Import Tool", file("."), settings = customSettings)
}
