package slick.interop.zio

import slick.dbio.{ DBIO, StreamingDBIO }
import zio.interop.reactivestreams._
import zio.stream.ZStream
import zio.{ RIO, ZIO }

import scala.concurrent.ExecutionContext

object syntax {

  implicit class ZIOObjOps(private val obj: ZIO.type) extends AnyVal {

    def fromDBIO[R](f: ExecutionContext => DBIO[R]): RIO[DatabaseProvider, R] =
      for {
        db <- ZIO.environmentWithZIO[DatabaseProvider](_.get.db)
        r  <- ZIO.fromFuture(ec => db.run(f(ec)))
      } yield r

    def fromDBIO[R](dbio: => DBIO[R]): RIO[DatabaseProvider, R] =
      for {
        db <- ZIO.environmentWithZIO[DatabaseProvider](_.get.db)
        r  <- ZIO.fromFuture(_ => db.run(dbio))
      } yield r

    def fromStreamingDBIO[T](
      dbio: StreamingDBIO[_, T]
    ): ZIO[DatabaseProvider, Throwable, ZStream[Any, Throwable, T]] =
      for {
        db <- ZIO.environmentWithZIO[DatabaseProvider](_.get.db)
        r  <- ZIO.attempt(db.stream(dbio).toStream())
      } yield r

  }

}
