package burndown

import com.github.nscala_time.time.Imports._
import java.nio.file._
import scopt.immutable.OptionParser
import org.joda.time.format.ISODateTimeFormat

object Main {

  private val LOGGER = grizzled.slf4j.Logger[this.type]

  case class Config(
    gitLocalRepo: Path = FileSystems.getDefault.getPath("."),
    dataFilePath: String = "",
    start: Option[LocalDate] = None,
    end: Option[LocalDate] = None,
    columnIndex: Int = 0,
    separator: String = "\t"
  )

  def main(args: Array[String]) {
    val parser = new OptionParser[Config]("burndown", "1.0") {
      def options = Seq(
        opt("r", "repo", "<directory>", "git local repo directory") {
          (v: String, c: Config) => c.copy(gitLocalRepo = FileSystems.getDefault.getPath(v))
        },
        arg("<data-file>", "data file (tsv) path") {
          (v: String, c: Config) => c.copy(dataFilePath = v)
        },
        opt("s", "start", "<start>", "start date") {
          (v: String, c: Config) => c.copy(start = Some(DateParser.parseDate(v)))
        },
        opt("e", "end", "<end>", "end date") {
          (v: String, c: Config) => c.copy(end = Some(DateParser.parseDate(v)))
        },
        intOpt("c", "column", "<column>", "remaining days column number (1 offset)") {
          (v: Int, c: Config) => c.copy(columnIndex = v - 1)
        },
        opt("f", "separator", "<separator>", "field separator (regex)") {
          (v: String, c: Config) => c.copy(separator = v)
        }
      )
    }

    parser.parse(args, Config()).
      map { c => main(c) }.
      getOrElse {
      }
  }

  object DateParser {
    import org.joda.time.format.DateTimeFormatter

    val formatters = List(
      DateTimeFormat.fullDate,
      ISODateTimeFormat.date
    )

    def parseDate(s: String): LocalDate = parse(formatters, s)

    @annotation.tailrec
    private def parse(formatters: List[DateTimeFormatter], s: String): LocalDate = {
      formatters match {
        case x::xs =>
          try {
            x.parseLocalDate(s)
          } catch {
            case _: Throwable => parse(xs, s)
          }
        case _ =>
          throw new IllegalArgumentException
      }
    }
  }

  def main(config: Config) {
    try {
      val git = new Git(config.gitLocalRepo)
      val start = config.start.orElse { Some(git.oldestTimestamp(config.dataFilePath).toLocalDate) }.get
      val end = config.end.orElse { Some(git.newestTimestamp(config.dataFilePath).toLocalDate) }.get
      val data = DailyStatus.list(git, config.dataFilePath, config.columnIndex, start, end, config.separator)

      val url = BurndownChart.url(data)
      println(url)
    } catch {
      case e: Exception => LOGGER.error(e.getMessage)
    }
  }
}

