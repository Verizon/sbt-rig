import sbt.{AutoPlugin, ModuleID}
import sbt.impl.DependencyBuilders

import scala.util.matching.Regex

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
object ScalazPlugin extends AutoPlugin {

  val ScalazSuffix = "scalaz"
  val SuffixRegex: Regex = "-(\\w+)".r


  val autoImport = AutoImport

  object AutoImport {

    implicit final class ScalazModuleId(val moduleId: ModuleID) extends AnyVal {

      /**
        * Add scalaz suffix to module.
        *
        * ```scala
        * val a = "com.example" %% "hello-world" % "1.0" scalaz "7.1"
        * val b = "com.example" %% "hello-world" % "1.0-scalaz-7.1"
        * assert(a == b)
        *
        * val c = "com.example" %% "hello-world" % "1.0-RC1-SNAPSHOT" scalaz "7.1"
        * val d = "com.example" %% "hello-world" % "1.0-scalaz-7.1-RC1-SNAPSHOT"
        * assert(c == d)
        * ```
        * @param ver Scalaz major version. For example 7.1 or 7.2
        * @return
        */
      def scalaz(ver: String): ModuleID = {
        val rev = moduleId.revision
        SuffixRegex.findFirstIn(moduleId.revision) match {
          case Some(suffix) =>
            val repl = s"-$ScalazSuffix-$ver$suffix"
            val updRev = SuffixRegex.replaceFirstIn(rev, repl)
            moduleId.copy(revision = updRev)
          case None =>
            val updRev = s"$rev-$ScalazSuffix-$ver"
            moduleId.copy(revision = updRev)
        }
      }

    }
  }
}

