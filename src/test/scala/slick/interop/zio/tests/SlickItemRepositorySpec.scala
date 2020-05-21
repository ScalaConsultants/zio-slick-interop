package slick.interop.zio.tests

import com.typesafe.config.ConfigFactory
import slick.interop.zio.DatabaseProvider
import zio.test.Assertion.equalTo
import zio.test._
import zio.{ ZIO, ZLayer }

import scala.jdk.CollectionConverters._

object SlickItemRepositorySpec extends DefaultRunnableSpec {

  private val config = ConfigFactory.parseMap(
    Map(
      "url"            -> "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1",
      "driver"         -> "org.h2.Driver",
      "connectionPool" -> "disabled"
    ).asJava
  )

  private val env: ZLayer[Any, Throwable, ItemRepository] =
    (ZLayer.succeed(config) ++ ZLayer.succeed(slick.jdbc.H2Profile.backend)) >>> DatabaseProvider.live >>> SlickItemRepository.live

  private val specs: Spec[ItemRepository, TestFailure[Throwable], TestSuccess] =
    suite("Item repository")(
      testM("Add and get items") {
        for {
          repo <- ZIO.access[ItemRepository](_.get)
          _    <- repo.add("A")
          _    <- repo.add("B")
          a    <- repo.getById(1L)
          b    <- repo.getById(2L)
        } yield assert(a.map(_.name))(equalTo(Some("A"))) && assert(b.map(_.name))(equalTo(Some("B")))
      }
    )

  def spec = specs.provideLayer(env.orDie)
}
