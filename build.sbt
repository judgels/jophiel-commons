import de.johoop.testngplugin.TestNGPlugin
import de.johoop.jacoco4sbt.JacocoPlugin.jacoco

name := """judgels-frontend-commons"""

version := "1.0-SNAPSHOT"

lazy val frontendcommons = (project.in(file(".")))
                    .enablePlugins(PlayJava)
                    .disablePlugins(plugins.JUnitXmlReportPlugin)
                    .dependsOn(commons, gabrielcommons)
                    .aggregate(commons, gabrielcommons)

lazy val commons = (RootProject(file("../judgels-play-commons")))

lazy val gabrielcommons = (RootProject(file("../gabriel-commons")))

scalaVersion := "2.11.1"

resolvers += "IA TOKI Artifactory" at "http://artifactory.ia-toki.org/artifactory/repo"

libraryDependencies ++= Seq(
  javaJdbc,
  javaWs,
  javaJpa.exclude("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api"),
  filters,
  cache,
  "commons-io" % "commons-io" % "2.4",
  "com.google.guava" % "guava" % "r05",
  "mysql" % "mysql-connector-java" % "5.1.26",
  "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final",
  "org.iatoki.judgels.sealtiel" % "sealtiel-message" % "1.0.3"
)

TestNGPlugin.testNGSettings

TestNGPlugin.testNGSuites := Seq("testng.xml")

TestNGPlugin.testNGOutputDirectory := "target/testng"

jacoco.settings

parallelExecution in jacoco.Config := false

javaOptions in Test ++= Seq(
  "-Dconfig.resource=test.conf"
)

javacOptions ++= Seq("-s", "app")

javacOptions ++= Seq("-Xlint:unchecked")
