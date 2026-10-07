import devoops.DevOopsGitHubPlugin.autoImport.findRepoOrgAndName

/** @author Kevin Lee
  * @since 2026-10-07
  */
object ProjectInfo {

  object props {
    val Org = "io.kevinlee"

    val ProjectName = "library-template-fp"

    private val gitHubRepo = findRepoOrgAndName

    val GitHubUsername: String = gitHubRepo.fold("kevin-lee")(_.orgToString)
    val RepoName: String       = gitHubRepo.fold("lib-fp.g8")(_.nameToString)

  }
}
