package slick.interop.zio

import com.typesafe.config.Config
import slick.jdbc.{JdbcBackend, JdbcProfile}
import zio._

trait DatabaseProvider {
  def db: UIO[JdbcBackend#Database]
  def profile: UIO[JdbcProfile]
}

case class DatabaseProviderLive(_db: JdbcBackend#Database, _profile: JdbcProfile) extends DatabaseProvider {
  override def db: UIO[JdbcBackend#Database] = UIO.succeed(_db)

  override def profile: UIO[JdbcProfile] = UIO.succeed(_profile)
}

object DatabaseProvider {
  val live: RLayer[Config with JdbcProfile, DatabaseProvider] = (
    for {
      profile  <- ZManaged.service[JdbcProfile]
      config   <- ZManaged.service[Config]
      provider <- ZManaged
                    .acquireReleaseWith(acquire(config, profile))(release)
                    .map(db => DatabaseProviderLive(db, profile))
    } yield provider
  ).toLayer

  private def acquire(cfg: Config, p: JdbcProfile): Task[p.backend.Database] =
    Task.attempt(p.backend.Database.forConfig(path = "", config = cfg))

  private def release(db: JdbcBackend#Database): UIO[Unit] =
    UIO.succeed(db.close())
}
