package slick.interop.tests

import slick.jdbc.H2Profile.api._

object ItemsTable {

  class Items(tag: Tag) extends Table[Item](tag, "ITEMS") {
    def id   = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def *    = (id, name) <> ((Item.apply _).tupled, Item.unapply _)
  }

  val table = TableQuery[ItemsTable.Items]
}
