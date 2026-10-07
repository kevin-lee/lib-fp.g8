import sbtcrossproject.CrossProject
import ProjectInfo.*

ThisBuild / scalaVersion := props.ScalaVersion
ThisBuild / version := props.ProjectVersion
ThisBuild / organization := props.Org
ThisBuild / organizationName := props.OrgName
ThisBuild / developers := List(
  Developer(
    props.GitHubUsername,
    "$author_name$",
    "$author_email$",
    url(s"https://github.com/\${props.GitHubUsername}"),
  )
)
ThisBuild / homepage := url(s"https://github.com/\${props.GitHubUsername}/\${props.RepoName}").some
ThisBuild / scmInfo :=
  ScmInfo(
    url(s"https://github.com/\${props.GitHubUsername}/\${props.RepoName}"),
    s"https://github.com/\${props.GitHubUsername}/\${props.RepoName}.git",
  ).some

lazy val root = (project in file("."))
  .settings(
    name := props.ProjectName
  )
  .settings(noPublish)
  .aggregate(
    coreJvm,
    coreJs,
    coreNative,
  )

lazy val core = module("core", crossProject(JVMPlatform, JSPlatform, NativePlatform))
  .settings(
    libraryDependencies ++=
      libs.refined4sAll.value ++
        List(
          libs.cats.value,
          libs.kittens.value,
        ) ++
        libs.catsEffect.value ++
        libs.tests.catsEffect.value
  )

lazy val coreJvm    = core.jvm
lazy val coreJs     = core.js.settings(jsSettingsForFuture)
lazy val coreNative = core.native.settings(nativeSettings)
