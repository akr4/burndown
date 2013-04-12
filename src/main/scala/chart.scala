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
        "chco=FF0000,00FF00",
        s"chxr=1,${min},${max}",
        "chm=s,FF0000,0,-1,1|s,00FF00,1,-1,5|s,00aa00,2,-1,5",
        "chd=" + chartDataParam(data, max)
      ).mkString("&")
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
    encoder.label + ":" + encoder.encode(List(Some(max), Some(0.0)), max) + "," + encoder.encode(data.map(_.remaining), max)
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

