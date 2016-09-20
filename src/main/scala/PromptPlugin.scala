package verizon.build

import sbt._, Keys._

object PromptPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    shellPrompt := {
    state: State =>
      val extracted = Project.extract(state)
      import extracted._
      //get name of current project and construct prompt string
      (name in currentRef get structure.data).map {
        name => "[" + cyan(name) + "] λ "
      }.getOrElse(red("> "))
    }
  )

  //////////////////////// INTERNALS ////////////////////////

  import scala.Console._

  lazy val isANSISupported = {
    Option(System.getProperty("sbt.log.noformat")).map(_ != "true").orElse {
      Option(System.getProperty("os.name"))
        .map(_.toLowerCase)
        .filter(_.contains("windows"))
        .map(_ => false)
    }.getOrElse(true)
  }

  def red(str: String): String =
    if(isANSISupported) RED + str + RESET
    else str

  def cyan(str: String) =
    if (isANSISupported) (CYAN + str + RESET)
    else str
}



