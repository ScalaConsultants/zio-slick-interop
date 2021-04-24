package slick.interop.zio

import slick.dbio.{ DBIO, StreamingDBIO }
import zio.{ Has, ZIO }
import zio.stream.ZStream
import zio.interop.reactivestreams._

import scala.concurrent.ExecutionContext

object syntax {

  implicit class ZIOObjOps(private val obj: ZIO.type) extends AnyVal {
    def fromDBIO[R](f: ExecutionContext => DBIO[R]): ZIO[Has[DatabaseProvider], Throwable, R] =
      for {
        db <- ZIO.accessM[Has[DatabaseProvider]](_.get.db)
        r  <- ZIO.fromFuture(ec => db.run(f(ec)))
      } yield r

    def fromDBIO[R](dbio: => DBIO[R]): ZIO[Has[DatabaseProvider], Throwable, R] =
      for {
        db <- ZIO.accessM[Has[DatabaseProvider]](_.get.db)
        r  <- ZIO.fromFuture(_ => db.run(dbio))
      } yield r

    def fromStreamingDBIO[T](
      dbio: StreamingDBIO[_, T]
    ): ZIO[Has[DatabaseProvider], Throwable, ZStream[Any, Throwable, T]] =
      for {
        db <- ZIO.accessM[Has[DatabaseProvider]](_.get.db)
        r  <- ZIO.effect(db.stream(dbio).toStream())
      } yield r
  }

}
