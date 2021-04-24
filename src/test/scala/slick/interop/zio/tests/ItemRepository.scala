package slick.interop.zio.tests

import zio.IO

trait ItemRepository {
  def add(name: String): IO[Throwable, Long]
  def getById(id: Long): IO[Throwable, Option[Item]]
  def upsert(name: String): IO[Throwable, Long]
}
