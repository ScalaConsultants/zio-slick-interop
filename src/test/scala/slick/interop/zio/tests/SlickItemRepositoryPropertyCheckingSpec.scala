package slick.interop.zio.tests

import com.typesafe.config.ConfigFactory
import slick.interop.zio.DatabaseProvider
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}
import zio.test.Gen._

import scala.collection.JavaConverters._

object SlickItemRepositoryPropertyCheckingSpec extends DefaultRunnableSpec {

  private val config = ConfigFactory.parseMap(
    Map(
      "url" -> "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1",
      "driver" -> "org.h2.Driver",
      "connectionPool" -> "disabled"
    ).asJava
  )

  private val env: ZLayer[Any, Throwable, ItemRepository] =
    (ZLayer.succeed(config) ++ ZLayer.succeed(slick.jdbc.H2Profile.backend)) >>> DatabaseProvider.live >>> SlickItemRepository.live

  private val nameGen: Gen[Random with Sized, String] = alphaNumericStringBounded(2, 20)

  private val specs: Spec[ItemRepository, TestFailure[Throwable], TestSuccess] =
    suite("Item repository")(
      testM("Add and get items") {
        checkM(nameGen) { name =>
          for {
            repo <- ZIO.access[ItemRepository](_.get)
            _ <- repo.add(name)
            item <- repo.getById(1L)
          } yield {
            assert(item)(isSome) &&
              assert(item.get.name)(equalTo(name))
          }
        }
      }
    )

  def spec = specs.provideLayer(env.orDie)
}
