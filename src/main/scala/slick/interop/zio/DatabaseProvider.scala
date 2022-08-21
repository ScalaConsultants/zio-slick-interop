package slick.interop.zio

import com.typesafe.config.Config
import zio._
import slick.jdbc.JdbcProfile
import slick.jdbc.JdbcBackend

trait DatabaseProvider {
  def db: UIO[JdbcBackend#Database]
  def profile: UIO[JdbcProfile]
}

object DatabaseProvider {

  val live: ZLayer[Config with JdbcProfile, Throwable, DatabaseProvider] = {
    val dbProvider = for {
      cfg <- ZIO.service[Config]
      p   <- ZIO.service[JdbcProfile]
      db   = ZIO.attempt(p.backend.Database.forConfig("", cfg))
      a   <- ZIO.acquireRelease(db)(db => ZIO.succeed(db.close()))
    } yield new DatabaseProvider {
      override val db: UIO[JdbcBackend#Database] = ZIO.succeed(a)
      override val profile: UIO[JdbcProfile]     = ZIO.succeed(p)
    }

    ZLayer.scoped(dbProvider)
  }
}
