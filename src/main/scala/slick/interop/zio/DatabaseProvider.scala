package slick.interop.zio

import com.typesafe.config.Config
import zio._
import slick.jdbc.JdbcProfile
import slick.jdbc.JdbcBackend

object DatabaseProvider {

  trait Service {
    def db: UIO[JdbcBackend#Database]
    def profile: UIO[JdbcProfile]
  }

  val live: ZLayer[Has[Config] with Has[JdbcProfile], Throwable, DatabaseProvider] =
    ZLayer.fromServicesManaged[Config, JdbcProfile, Any, Throwable, Service] { (cfg: Config, p: JdbcProfile) =>
      ZManaged
        .make(ZIO.effect(p.backend.Database.forConfig("", cfg)))(db => ZIO.effectTotal(db.close()))
        .map(d =>
          new DatabaseProvider.Service {
            val db: UIO[JdbcBackend#Database] = ZIO.effectTotal(d)

            val profile: UIO[JdbcProfile] = ZIO.effectTotal(p)
          }
        )
    }
}
