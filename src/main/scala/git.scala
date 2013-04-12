package burndown

import scala.language.postfixOps

import scala.sys.process.Process
import java.nio.file.Path
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat

case class Commit(timestamp: DateTime, text: String)

class Git(localRepoDir: Path) {
  type Hash = String
  private val LOGGER = grizzled.slf4j.Logger[this.type]

  /** get a text file content at a given time */
  def getContent(path: String, at: DateTime): String = {
    val hash = getHash(path, at)
    show(path, hash)
  }

  def findCommits(path: String): Iterable[Commit] = {
    val command = Seq("git", "log", "--format=%at %H", path)
    LOGGER.debug(command.mkString(" "))
    val lines = Process(command, localRepoDir.toFile).lines
    lines.map(_.split(" ")).map { x =>
      Commit(new DateTime((x(0) + "000").toLong), show(path, x(1)))
    }
  }

  def oldestTimestamp(path: String): DateTime = findTimestamp(path, (list) => list.sorted.head)
  def newestTimestamp(path: String): DateTime = findTimestamp(path, (list) => list.sortWith(_ > _).head)

  private def findTimestamp(path: String, f: List[Int] => Int): DateTime = {
    import scala.sys.process._

    val gitCommand = Seq("git", "log", "--format=%at", path)
    LOGGER.debug(gitCommand.mkString(" "))
    val lines = Process(gitCommand, localRepoDir.toFile).lines
    val unixTimestamp = f(lines.map(_.toInt).toList)
    new DateTime(unixTimestamp.toLong * 1000)
  }

  private def getHash(path: String, at: DateTime): Hash = {
    val command = Seq("git", "log", "--format=%H", "-n1", "--until", ISODateTimeFormat.dateTime.print(at), path)
    LOGGER.debug(command.mkString(" "))
    val hash = Process(command, localRepoDir.toFile) !!

    hash.trim
  }

  private def show(path: String, hash: Hash): String = {
    val command = Seq("git", "show", hash + ":" + path)
    LOGGER.debug(command.mkString(" "))
    Process(command, localRepoDir.toFile) !!
  }
}
