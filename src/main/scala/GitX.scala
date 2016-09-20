package verizon.build

class GitX(baseDir: java.io.File) extends sbtrelease.Git(baseDir){
  /*
   * push --tags is broken, and pushCommits is dangerous.
   * using --follow-tags should push both commits (if there are any) and
   * also any tags that got created for which commits exist in remote.
   * http://stackoverflow.com/questions/5195859/push-a-tag-to-a-remote-repository-using-git
   */
  override def pushChanges = cmd("push", "--tags", trackingRemote)
}