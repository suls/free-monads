package example 

import scalaz.{~>,Id,Free,Functor}, Id.Id

sealed trait LogF[A]

object Logging {
  type Log[A] = Free[LogF, A]

  implicit def logFFunctor[B]: Functor[LogF] = new Functor[LogF]{
    def map[A,B](fa: LogF[A])(f: A => B): LogF[B] = 
      fa match {
        case Debug(msg,a) => Debug(msg,f(a))
        case Info(msg,a)  => Info(msg,f(a))
        case Warn(msg,a)  => Warn(msg,f(a))
        case Error(msg,a) => Error(msg,f(a))
      }
  }

  implicit def logFToFree[A](logf: LogF[A]): Free[LogF,A] =
    Free.liftF(logf)

  case class Debug[A](msg: String, o: A) extends LogF[A]
  case class Info[A](msg: String, o: A) extends LogF[A]
  case class Warn[A](msg: String, o: A) extends LogF[A]
  case class Error[A](msg: String, o: A) extends LogF[A]

  object log {
    def debug(msg: String): Log[Unit] = Debug(msg, ())
    def info(msg: String): Log[Unit]  = Info(msg, ())
    def warn(msg: String): Log[Unit]  = Warn(msg, ())
    def error(msg: String): Log[Unit] = Error(msg, ())
  }
}

object Println {
  import Logging._

  private def write(prefix: String, msg: String): Unit = 
    println(s"[$prefix] $msg")

  private def debug(msg: String): Unit = write("DEBUG", msg)
  private def info(msg: String): Unit  = write("INFO", msg)
  private def warn(msg: String): Unit  = write("WARN", msg)
  private def error(msg: String): Unit = write("ERROR", msg)

  private val exe: LogF ~> Id = new (LogF ~> Id) {
    def apply[B](l: LogF[B]): B = l match { 
      case Debug(msg,a) => { debug(msg); a } 
      case Info(msg,a) => { info(msg); a } 
      case Warn(msg,a) => { warn(msg); a } 
      case Error(msg,a) => { error(msg); a } 
    }
  }

  def apply[A](log: Log[A]): A = 
    log.runM(exe.apply[Log[A]])
}

/**
  * Interpreter for SLF4J
  */ 
object SLF4J {
  import Logging._
  import org.slf4j.{Logger,LoggerFactory}

  private val log = LoggerFactory.getLogger(SLF4J.getClass)

  private val exe: LogF ~> Id = new (LogF ~> Id) {
    def apply[B](l: LogF[B]): B = l match { 
      case Debug(msg,a) => { log.debug(msg); a } 
      case Info(msg,a) => { log.info(msg); a } 
      case Warn(msg,a) => { log.warn(msg); a } 
      case Error(msg,a) => { log.error(msg); a } 
    }
  }

  def apply[A](log: Log[A]): A = 
    log.runM(exe.apply[Log[A]])
}

object Main {
  import Logging.log

  val program: Free[LogF, Unit] = 
    for {
      a <- log.info("fooo")
      b <- log.error("OH NOES")
    } yield b

  def main(args: Array[String]): Unit = {
    SLF4J(program)
  }
}