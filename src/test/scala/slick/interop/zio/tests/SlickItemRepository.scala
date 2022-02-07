package slick.interop.zio.tests

import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import zio._

object SlickItemRepository {
  val live: ZLayer[DatabaseProvider, Throwable, ItemRepository] =
    (for {
      dp   <- ZIO.service[DatabaseProvider]
      repo <- dp.profile.flatMap { profile =>
                import profile.api._
                val dbLayer    = ZLayer.succeed(dp)
                val initialize = ZIO.fromDBIO(ItemsTable.table.schema.createIfNotExists)

                val repository = new ItemRepository {
                  private val items = ItemsTable.table

                  def add(name: String): IO[Throwable, Long] =
                    ZIO
                      .fromDBIO((items returning items.map(_.id)) += Item(0L, name))
                      .provide(dbLayer)

                  def getById(id: Long): IO[Throwable, Option[Item]] = {
                    val query = items.filter(_.id === id).result

                    ZIO
                      .fromDBIO(query)
                      .map(_.headOption)
                      .provide(dbLayer)
                  }

                  def upsert(name: String): IO[Throwable, Long] =
                    ZIO.fromDBIO { implicit ec =>
                      (for {
                        itemOpt <- items.filter(_.name === name).result.headOption
                        id      <- itemOpt.fold[DBIOAction[Long, NoStream, Effect.Write]](
                                     (items returning items.map(_.id)) += Item(0L, name)
                                   )(item => (items.map(_.name) update name).map(_ => item.id))
                      } yield id).transactionally
                    }.provide(dbLayer)
                }

                initialize.as(repository).provide(dbLayer)
              }
    } yield repo).toLayer
}
