import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtStartScript

object MyBuild extends Build {

  val groupName = "burndown"

  def id(name: String) = "%s-%s" format(groupName, name)

  override val settings = super.settings :+
    (shellPrompt := { s => Project.extract(s).currentProject.id + "> " })

  val defaultSettings = Defaults.defaultSettings ++ Seq(
    version := "0.1",
    organization := "net.physalis",
    crossScalaVersions := Seq("2.10.1"),
    scalaVersion := "2.10.1",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(
      "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
      "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
    )
  )

  object Dependency {

    val basic = {
      Seq(
        "com.github.nscala-time" %% "nscala-time" % "0.4.0",
        "com.github.scopt" %% "scopt" % "2.1.0"
      )
    }

    val logging = Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.11",
      "org.fusesource.jansi" % "jansi" % "1.8",
      "org.codehaus.groovy" % "groovy" % "1.8.6",
      "org.slf4j" % "slf4j-api" % "1.7.4",
      "org.clapper" %% "grizzled-slf4j" % "1.0.1"
    )

    val default = basic ++ logging
  }

  lazy val main = Project(groupName, file("."),
    settings = defaultSettings ++ Seq(
      libraryDependencies := Dependency.default,
      initialCommands := """
          |import burndown._
          |import com.github.nscala_time.time.Imports._
          |import java.nio.file._
        """.stripMargin
    ) ++ SbtStartScript.startScriptForClassesSettings
  )

}

