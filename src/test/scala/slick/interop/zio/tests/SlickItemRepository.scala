package slick.interop.zio.tests

import slick.interop.zio.syntax._
import slick.interop.zio.DatabaseProvider
import zio._

object SlickItemRepository {

  val live: ZLayer[DatabaseProvider, Throwable, Has[ItemRepository]] =
    ZLayer.fromServiceM { db =>
      db.profile.flatMap { profile =>
        import profile.api._

        val initialize = ZIO.fromDBIO(ItemsTable.table.schema.createIfNotExists)

        val repository = new ItemRepository {
          private val items = ItemsTable.table

          def add(name: String): IO[Throwable, Long] =
            ZIO
              .fromDBIO((items returning items.map(_.id)) += Item(0L, name))
              .provide(Has(db))

          def getById(id: Long): IO[Throwable, Option[Item]] = {
            val query = items.filter(_.id === id).result

            ZIO.fromDBIO(query).map(_.headOption).provide(Has(db))
          }

          def upsert(name: String): IO[Throwable, Long] =
            ZIO.fromDBIO { implicit ec =>
              (for {
                itemOpt <- items.filter(_.name === name).result.headOption
                id      <- itemOpt.fold[DBIOAction[Long, NoStream, Effect.Write]](
                             (items returning items.map(_.id)) += Item(0L, name)
                           )(item => (items.map(_.name) update name).map(_ => item.id))
              } yield id).transactionally
            }.provide(Has(db))
        }

        initialize.as(repository).provide(Has(db))
      }
    }
}
