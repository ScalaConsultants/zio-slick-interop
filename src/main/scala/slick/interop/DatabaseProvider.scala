package slick.interop

import com.typesafe.config.Config
import slick.basic.BasicBackend
import slick.jdbc.JdbcBackend
import zio._

object DatabaseProvider {

  trait Service {
    def db: UIO[BasicBackend#DatabaseDef]
  }

  val live: ZLayer[Has[Config] with Has[JdbcBackend], Throwable, DatabaseProvider] =
    ZLayer.fromServicesManaged[Config, JdbcBackend, Any, Throwable, Service] { (cfg: Config, backend: JdbcBackend) =>
      ZManaged
        .make(ZIO.effect(backend.Database.forConfig("", cfg)))(db => ZIO.effectTotal(db.close()))
        .map(d =>
          new DatabaseProvider.Service {
            val db: UIO[BasicBackend#DatabaseDef] = ZIO.effectTotal(d)
          }
        )
    }
}
