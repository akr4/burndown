package burndown

import com.github.nscala_time.time.Imports._

object BurndownChart {
  private val LOGGER = grizzled.slf4j.Logger[this.type]

  def url(data: Iterable[DailyStatus]): String = {
    val values = data.flatMap { _.remaining }
    if (values.isEmpty) {
      throw new IllegalArgumentException("no valid data found.")
    }

    val min = values.min
    val max = values.max
    LOGGER.debug(s"min: $min, max: $max")

    "http://chart.apis.google.com/chart?" +
      List(
        "chs=600x250",
        "chtt=Burndown",
        "cht=lc",
        "chdl=Estimated|Actual",
        "chxt=x,y",
        "chxl=" + chartAxisLabelParam(data),
        "chxr=" + chartAxisRangeParam(data, max),
        "chco=FF0000,00FF00",
        "chm=s,FF0000,0,-1,1|s,00FF00,1,-1,5|s,00aa00,2,-1,5",
        "chd=" + chartDataParam(data, max)
      ).mkString("&")
  }

  private def chartAxisRangeParam(data: Iterable[DailyStatus], max: Double) = {
    "1,0,%1.1f,10".format(max)
  }

  private def chartAxisLabelParam(data: Iterable[DailyStatus]) = {
    val F = DateTimeFormat.forPattern("M-d")
    val days = data.map(_.date).toList
    if (days.size < 10) "0:|%s|%s".format(days.head.toString(F), days.last.toString(F))
    else {
      val n = days.size / 4
      "0:|%s|%s|%s|%s|%s".format(
        days.head.toString(F),
        days(n * 1).toString(F),
        days(n * 2).toString(F),
        days(n * 3).toString(F),
        days.last.toString(F)
      )
    }
  }

  private def chartDataParam(data: Iterable[DailyStatus], max: Double) = {
    val encoder = SimpleDataEncoder
    encoder.label + ":" + encoder.encode(List(Some(max), Some(0.0)), max) + "," +
      encoder.encode(fillNoCommitDayUntilToday(data).map(_.remaining), max)
  }

  /** fill previous day's value to render continuous line chart */
  private def fillNoCommitDayUntilToday(data: Iterable[DailyStatus]): Iterable[DailyStatus] = {
    @annotation.tailrec
    def f(acc: List[DailyStatus], data: Iterable[DailyStatus], lastValue: Option[Double]): List[DailyStatus] = {
      data match {
        case x::xs => x.remaining match {
          case v@Some(_) => f(acc :+ x, xs, v)
          case v =>
            if (x.date <= LocalDate.today) f(acc :+ new DailyStatus(x.date, lastValue), xs, lastValue)
            else f(acc :+ x, xs, v)
        }
        case _ => acc
      }
    }

    f(Nil, data, None)
  }
}

sealed trait DataEncoder {
  def encode(values: Iterable[Option[Double]], max: Double): String
  val label: String
}
object SimpleDataEncoder extends DataEncoder {
  val label = "s"
  def encode(values: Iterable[Option[Double]], max: Double): String =
    values.map { optx => optx match {
      case Some(x) => toChar(x, max)
      case _ => '_'
    }}.mkString
  private val CHARS = ('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9')
  private def toChar(v: Double, max: Double): Char =
    CHARS(math.floor(v / ((max + 1) / CHARS.size)).toInt)
}

