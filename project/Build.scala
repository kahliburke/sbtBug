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
      "Local Maven Repository" at "file://"+localm2RepoPath.absolutePath,
      "Typesafe Repository" at "http://scala-tools.org/repo-snapshots"
    ),

    libraryDependencies ++= {
      Seq(
        "org.apache.activemq" % "activemq-core" % "5.5.0"
        )
    }

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

  lazy val myProject = Project("Test Bug", file("."), settings = customSettings)
}
