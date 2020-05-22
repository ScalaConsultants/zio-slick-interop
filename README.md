# zio-slick-interop

![CI](https://github.com/ScalaConsultants/zio-slick-interop/workflows/Scala%20CI/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.scalac/zio-slick-interop_2.13.svg)](https://github.com/ScalaConsultants/zio-slick-interop)

Small library, that provides interop between [Slick](http://scala-slick.org/) and [ZIO](https://zio.dev/)

### How to use

Include zio-slick-interop in your build:

```
libraryDependencies += "io.scalac" %% "zio-slick-interop" % "0.1.0"
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

// repository service, defined by ZLayer guidelines
type ItemRepository = Has[ItemRepository.Service]

object ItemRepository {

  trait Service {

    def getById(id: Long): IO[Throwable, Option[Item]]
  }
}
```

Then we can implement it using `DatabaseProvider`:

```scala


import zio._

import scala.concurrent.duration._
import slick.interop.zio.DatabaseProvider
// adds ZIO.fromDBIO extension
import slick.interop.zio.syntax._
// import your specific DB driver here
import slick.jdbc.H2Profile.api._

final class SlickItemRepository(db: DatabaseProvider)
    extends ItemRepository.Service {

  def getById(id: Long): IO[Throwable, Option[Item]] = {
    val query = ItemsTable.table.filter(_.id === id).result

    ZIO.fromDBIO(query).map(_.headOption).provide(db)
  }

}

object SlickItemRepository {

  val live: ZLayer[DatabaseProvider, Throwable, ItemRepository] =
    ZLayer.fromFunctionM { db =>
      val initialize = ZIO.fromDBIO(ItemsTable.table.schema.createIfNotExists)

      initialize.map(_ => new SlickItemRepository(db)).provide(db)
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

val dbConfigLayer = ZLayer.fromEffect (ZIO.effect(rootConfig.getConfig("db")))
val dbBackendLayer = ZLayer.succeed(slick.jdbc.H2Profile.backend)

(dbConfigLayer ++ dbBackendLayer) >>> DatabaseProvider.live
```
