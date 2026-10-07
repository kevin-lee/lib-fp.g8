import sbt.*
import sbt.Keys.*
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*
import sbtcrossproject.CrossProject
import scalafix.sbt.ScalafixPlugin.autoImport.*
import scoverage.ScoverageKeys.*
import wartremover.WartRemover.autoImport.*
import devoops.DevOopsGitHubPlugin.autoImport.findRepoOrgAndName

/** @author Kevin Lee
  */
object ProjectInfo {

  object props {
    val ScalaVersion = "$scalaVersion$"
    val Org          = "$organization$"
    val OrgName      = "$organizationName$"

    private val gitHubRepo = findRepoOrgAndName

    val GitHubUsername = gitHubRepo.fold("$github_username$")(_.orgToString)
    val RepoName       = gitHubRepo.fold("$repo_name$")(_.nameToString)

    val ProjectName = "$project_name$"

    val ProjectVersion = "0.1.0-SNAPSHOT"

    val licenses = List(License.MIT)

    val Refined4sVersion = "$refined4s_version$"

    val HedgehogVersion = "$hedgehog_version$"

    val CatsVersion = "$cats_version$"

    val CatsEffectVersion = "$cats_effect_version$"

    val KittensVersion = "$kittens_version$"

    val IncludeTest: String = "compile->compile;test->test"
  }

  object libs {

    lazy val refined4sAll = Def.setting(
      List(
        "io.kevinlee" %%% "refined4s-core" % props.Refined4sVersion,
        "io.kevinlee" %%% "refined4s-cats" % props.Refined4sVersion,
      )
    )

    lazy val cats = Def.setting(
      "org.typelevel" %%% "cats-core" % props.CatsVersion,
    )

    lazy val catsEffect = Def.setting(
      List(
        "org.typelevel" %%% "cats-effect-std" % props.CatsEffectVersion,
      )
    )

    lazy val kittens = Def.setting(
      "org.typelevel" %%% "kittens" % props.KittensVersion
    )

    object tests {
      lazy val hedgehog = Def.setting(
        List(
          "qa.hedgehog" %%% "hedgehog-core"   % props.HedgehogVersion % Test,
          "qa.hedgehog" %%% "hedgehog-runner" % props.HedgehogVersion % Test,
          "qa.hedgehog" %%% "hedgehog-sbt"    % props.HedgehogVersion % Test,
        )
      )

      lazy val catsEffect = Def.setting(
        List(
          "org.typelevel" %%% "cats-effect" % props.CatsEffectVersion % Test
        )
      )
    }

  }

  def isScala3(scalaVersion: String): Boolean = scalaVersion.startsWith("3.")

  // format: off
  def prefixedProjectName(name: String) = s"\${props.RepoName}\${if (name.isEmpty) "" else s"-\$name"}"
  // format: on

  ////

  def module(projectName: String, crossProject: CrossProject.Builder): CrossProject = {
    val names          = projectName.split("/")
    val theProjectName = names.last
    val location       = names.toList
    val prefixedName   = prefixedProjectName(theProjectName)
    commonModule(prefixedName, location, crossProject)
  }

  def testModule(projectName: String, crossProject: CrossProject.Builder): CrossProject = {

    val names          = projectName.split("/")
    val theProjectName = names.last

    val prefixedName = s"test-\${prefixedProjectName(theProjectName)}"
    val location     = (names.init :+ prefixedName).toList

    commonModule(prefixedName, location, crossProject)
  }

  def commonModule(prefixedName: String, path: List[String], crossProject: CrossProject.Builder): CrossProject = {
    val modulePath = file(("modules" :: path).mkString("/"))
    List(
      modulePath / "shared" / "src" / "main" / "scala",
      modulePath / "shared" / "src" / "test" / "scala",
    ).foreach(IO.createDirectory)
    crossProject
      .in(modulePath)
      .settings(
        name := prefixedName,
        fork := true,
        semanticdbEnabled := true,
        scalafixConfig := (
          if (scalaVersion.value.startsWith("3"))
            ((ThisBuild / baseDirectory).value / ".scalafix-scala3.conf").some
          else
            ((ThisBuild / baseDirectory).value / ".scalafix-scala2.conf").some
        ),
        scalacOptions ++= (if (isScala3(scalaVersion.value)) List("-no-indent", "-explain") else List("-Xsource:3")),
        //      scalacOptions ~= (ops => ops.filter(_ != "UTF-8")),
        libraryDependencies ++= libs.tests.hedgehog.value,
        wartremoverErrors ++= Warts.allBut(Wart.Any, Wart.Nothing, Wart.ImplicitConversion, Wart.ImplicitParameter),
        Compile / console / scalacOptions :=
          (console / scalacOptions)
            .value
            .filterNot(option => option.contains("wartremover") || option.contains("import")),
        Test / console / scalacOptions :=
          (console / scalacOptions)
            .value
            .filterNot(option => option.contains("wartremover") || option.contains("import")),
        /* } WartRemover and scalacOptions */
        licenses := props.licenses,
        /* coverage { */
        coverageHighlighting := (CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, 10)) | Some((2, 11)) =>
            false
          case _ =>
            true
        }),
        /* } coverage */

        //      scalacOptions ~= (_.filterNot(_.startsWith("-language"))),
        //      scalacOptions ++= List(
        //        "-language:dynamics",
        //        "-language:existentials",
        //        "-language:higherKinds",
        //        "-language:reflectiveCalls",
        //        "-language:experimental.macros",
        //        "-language:implicitConversions",
        //      ),
      )
  }

  lazy val jsSettingsForFuture: SettingsDefinition = List(
    Test / fork := false,
    Test / scalacOptions ++= (if (scalaVersion.value.startsWith("3")) List.empty
                              else List("-P:scalajs:nowarnGlobalExecutionContext")),
    Test / compile / scalacOptions ++= (if (scalaVersion.value.startsWith("3")) List.empty
                                        else List("-P:scalajs:nowarnGlobalExecutionContext")),
    coverageEnabled := false,
  )

  lazy val nativeSettings: SettingsDefinition = List(
    Test / fork := false,
    coverageEnabled := false,
  )

}
