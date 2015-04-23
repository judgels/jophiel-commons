import de.johoop.testngplugin.TestNGPlugin
import de.johoop.jacoco4sbt.JacocoPlugin.jacoco

lazy val frontendcommons = (project in file("."))
    .enablePlugins(PlayJava, SbtWeb)
    .disablePlugins(plugins.JUnitXmlReportPlugin)
    .dependsOn(playcommons, gabrielcommons)
    .aggregate(playcommons, gabrielcommons)
    .settings(
        name := "frontendcommons",
        version := IO.read(file("version.properties")).trim,
        scalaVersion := "2.11.1",
        libraryDependencies ++= Seq(
            "com.ibm.icu" % "icu4j" % "55.1",
            "org.webjars" % "ckeditor" % "4.4.1",
            "org.webjars" % "prettify" % "4-Mar-2013",
            "com.warrenstrange" % "googleauth" % "0.4.3"
        )
    )
    .settings(TestNGPlugin.testNGSettings: _*)
    .settings(
        aggregate in test := false,
        aggregate in jacoco.cover := false,
        TestNGPlugin.testNGSuites := Seq("test/resources/testng.xml")
    )
    .settings(jacoco.settings: _*)
    .settings(
        parallelExecution in jacoco.Config := false
    )

lazy val playcommons = RootProject(file("../judgels-play-commons"))

lazy val gabrielcommons = RootProject(file("../judgels-gabriel-commons"))
