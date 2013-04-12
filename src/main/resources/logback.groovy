import static ch.qos.logback.classic.Level.*
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender("STDERR", ConsoleAppender) {
  withJansi = true
  encoder(PatternLayoutEncoder) {
    pattern = "%highlight(%-5level) %msg%n"
  }
  target = System.err
}
root(INFO, [ "STDERR" ])
logger("burndown", DEBUG, [ "STDERR" ], false)

