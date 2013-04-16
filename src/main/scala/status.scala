package burndown

import com.github.nscala_time.time.Imports._
import scala.util.control.Exception

class DailyStatus(val date: LocalDate, val remaining: Option[Double])
object DailyStatus {
  private val LOGGER = grizzled.slf4j.Logger[this.type]

  def list(git: Git, dataFilePath: String, columnIndex: Int, start: LocalDate, end: LocalDate, separator: String):
    List[DailyStatus] = {
    val lastCommitDate = List(git.newestTimestamp(dataFilePath).toLocalDate, end).min

    val allCommits = git.findCommits(dataFilePath)
    val dailyStatuStream = for {
      date <- Stream.iterate(start)(_ + 1.days).takeWhile(_ <= end)
    } yield {
      if (date <= lastCommitDate) {
        val todaysCommits =
          allCommits.filter { x => x.timestamp.toLocalDate == date }.toList.sortWith(_.timestamp > _.timestamp)
        // toStream to only compute needed to find
        todaysCommits.toStream.map { commit: Commit =>
          (commit, new DailyStatus(date, remaining(commit.text, columnIndex, separator)))
        }.find(_._2.remaining.isDefined) match {
          case Some((commit, status)) =>
            LOGGER.info(s"use commit at ${commit.timestamp} for ${date}")
            status
          case _ =>
            LOGGER.info(s"no valid commit found for ${date}")
            new DailyStatus(date, None)
        }
      } else {
        new DailyStatus(date, None)
      }
    }
    dailyStatuStream.toList
  }

  /** @return total remaining days. error message if error */
  private def remaining(tsvData: String, columnIndex: Int, separator: String): Option[Double] = {
    def extractRemaining(x: (Array[String], Int)): Double = {
      val (columns, line) = x

      if (columns.size < columnIndex + 1) {
        throw new IllegalArgumentException(s"line ${line}: does not have enough columns.")
      } else {
        val value = columns(columnIndex)
        Exception.allCatch.opt { value.toDouble } match {
          case Some(x) => x
          case _ => throw new IllegalArgumentException(s"line ${line}: not a number '${value}'.")
        }
      }
    }

    Exception.allCatch.
      either {
      tsvData.
        split("\n").
        drop(1). // skip title line
        map(_.split(separator)).
        zipWithIndex.
        map(extractRemaining).
        sum
    } match {
      case Right(x) => Some(x)
      case Left(x) =>
        LOGGER.warn(x.getMessage)
        None
    }
  }
}
