package slick.interop.zio.tests

import com.typesafe.config.ConfigFactory
import slick.interop.zio.DatabaseProvider
import zio.test.Assertion.{ equalTo, isSome, not }
import zio.test.TestAspect.sequential
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
    (ZLayer.succeed(config) ++ ZLayer.succeed(
      slick.jdbc.H2Profile.backend
    )) >>> DatabaseProvider.live >>> SlickItemRepository.live

  private val specs: Spec[ItemRepository, TestFailure[Throwable], TestSuccess] =
    suite("Item repository")(
      testM("Add and get items") {
        for {
          repo <- ZIO.service[ItemRepository.Service]
          _    <- repo.add("A")
          _    <- repo.add("B")
          a    <- repo.getById(1L)
          b    <- repo.getById(2L)
        } yield assert(a.map(_.name))(isSome(equalTo("A"))) &&
          assert(b.map(_.name))(isSome(equalTo("B")))
      },
      testM("Upsert items") {
        for {
          repo <- ZIO.service[ItemRepository.Service]
          cId  <- repo.upsert("C")
          c    <- repo.getById(cId)
          cId2 <- repo.upsert("C")
          c2   <- repo.getById(cId)
        } yield assert(c.map(_.name))(isSome(equalTo("C"))) &&
          assert(c2.map(_.name))(isSome(equalTo("C"))) &&
          assert(cId)(equalTo(cId2)) &&
          assert(cId)(not(equalTo(1L)))
      }
    ) @@ sequential

  def spec = specs.provideLayer(env.orDie)
}
