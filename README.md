# zio-slick-interop

![CI](https://github.com/ScalaConsultants/zio-slick-interop/workflows/Scala%20CI/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.scalac/zio-slick-interop_2.13.svg)

Small library, that provides interop between [Slick](http://scala-slick.org/) and [ZIO](https://zio.dev/)

### How to use

Include zio-slick-interop in your build:

```
libraryDependencies += "io.scalac" %% "zio-slick-interop" % "0.5.0"
```

It gives you a couple of nice things:

#### 1. `DatabaseProvider` service

Database is usually a dependency for your other services. Since `ZLayer` is the best and recommended way to do DI with ZIO, 
zio-slick-interop provides some convenience bindings for using slick with `ZLayer`.

Specifically, it's `slick.interop.zio.DatabaseProvider`, which is a service, defined by `ZLayer` guidelines and provides access to underlying slick database.

You'd generally want to use it when defining your repository services, like the following.

Say we have a simple raw Slick table:
```scala
// import your specific DB driver here
import slick.jdbc.H2Profile.api._

final case class Item(id: Long, name: String)

// Raw Slick table
object ItemsTable {

  class Items(tag: Tag) extends Table[Item](tag, "items") {
    def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("name")
    def *     = (id, name) <> ((Item.apply _).tupled, Item.unapply _)
  }

  val table = TableQuery[ItemsTable.Items]
}
```

And we want to have a repository service on top of it with the following contract:

```scala
import zio._

trait ItemRepository {

    def getById(id: Long): IO[Throwable, Option[Item]]

}
```

Then we can implement it as a database-agnostic Slick repository using `DatabaseProvider`:

```scala
import zio._

import slick.interop.zio.DatabaseProvider
// adds ZIO.fromDBIO extension
import slick.interop.zio.syntax._

object SlickItemRepository {

  val live: ZLayer[DatabaseProvider, Throwable, ItemRepository] =
    ZLayer {
      for {
        db <- ZIO.service[DatabaseProvider]
        profile <- db.profile
      } yield {
        import profile.api._

        val dbLayer = ZLayer.succeed(db)

        new ItemRepository {
          private val items = ItemsTable.table

          def getById(id: Long): IO[Throwable, Option[Item]] = {
            val query = items.filter(_.id === id).result

            ZIO.fromDBIO(query)
              .map(_.headOption)
              .provide(dbLayer)
          }
        }
      }
    }
}
```
`SlickItemRepository.live` is a repository layer that depends on raw underlying database.

You can notice `ZIO.fromDBIO` which is provided by zio-slick-interop and is described below.

#### 2. Lifting DBIO into ZIO

There's a syntax extension allowing to lift `DBIO` actions into `ZIO`:

```scala
import slick.interop.zio.syntax._
import slick.interop.zio.DatabaseProvider
import zio._
// import your specific DB driver here
import slick.jdbc.H2Profile.api._

val insert = ItemsTable.table += Item(0L, "name")

val z: ZIO[DatabaseProvider, Throwable, Int] = ZIO.fromDBIO(insert)
```
This is a ZIO, that can be run given a `DatabaseProvider` is present in the environment.

⚠️ Make sure to have `slick.interop.zio.syntax._` imported.

There's also `ZIO.fromStreamingDBIO`, which works with streaming slick actions.

When executing a sequence of actions (when "`flatMap`-ing"), in the context of a transaction with `transactionally` for example,
Slick requires an implicit `ExecutionContext` in scope. For this use-case, another overload to `ZIO.fromDBIO` takes a function with an 
`ExecutionContext` as argument:
```scala
import slick.interop.zio.syntax._
import slick.interop.zio.DatabaseProvider
import zio._

// import your specific DB driver here
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

val id: Long = ???

val z: ZIO[DatabaseProvider, Throwable, Unit] = 
  ZIO.fromDBIO { implicit ec: ExecutionContext =>
    (for {
      _ <- ItemsTable.table += Item(0L, "name")
      _ <- ItemsTable.table.filter(_.id === id).map(_.name).update("new name")
    } yield ()).transactionally
  }
``` 

### Creating a `DatabaseProvider`.

`DatabaseProvider` provides a `live` layer that needs:

* a typesafe `Config`, that points to a [standard Slick configuration block](https://scala-slick.org/doc/3.3.2/api/index.html#slick.jdbc.JdbcBackend$DatabaseFactoryDef@forConfig(String,Config,Driver,ClassLoader):Database);
* specific database backend you're using.

Here's an example of creating a `DatabaseProvider` layer:

```scala
import zio._
import com.typesafe.config.Config
import slick.interop.zio.DatabaseProvider

val rootConfig: Config = ???

val dbConfigLayer = ZLayer(ZIO.attempt(rootConfig.getConfig("db")))
val dbBackendLayer = ZLayer.succeed(slick.jdbc.H2Profile)

(dbConfigLayer ++ dbBackendLayer) >>> DatabaseProvider.live
```

Developed by [Scalac](https://scalac.io/?utm_source=scalac_github&utm_campaign=scalac1&utm_medium=web)
