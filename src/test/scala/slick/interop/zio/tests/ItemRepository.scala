package slick.interop.zio.tests

import zio.IO

object ItemRepository {

  trait Service {
    def add(name: String): IO[Throwable, Long]
    def getById(id: Long): IO[Throwable, Option[Item]]
    def upsert(name: String): IO[Throwable, Long]
  }
}
