package com.mabrcosta.slick.transactions.key.persistence

import java.util.UUID

import com.mabrcosta.slick.transactions.key.data.Key
import slick.ast.BaseTypedType
import slick.ast.ColumnOption.Unique
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class KeysRepository(val jdbcProfile: JdbcProfile) {

  import jdbcProfile.api._

  type TableType = Keys
  val tableQuery = TableQuery[Keys]
  val pkType = implicitly[BaseTypedType[UUID]]

  class Keys(tag: Tag) extends Table[Key](tag, None, "keys") {
    def id = column[UUID]("uid", O.PrimaryKey)
    def value = column[String]("value", Unique)
    def uidOwnerSubject = column[UUID]("uid_owner_user")

    def * = (id.?, value, uidOwnerSubject) <> (Key.tupled, Key.unapply)
  }

  def find(id: UUID): DBIO[Option[Key]] = {
    tableQuery.filter(_.id === id).result.headOption
  }

  def find(ids: Seq[UUID]): DBIO[Seq[Key]] = {
    tableQuery.filter(_.id inSet ids).result
  }

  def save(entity: Key)(implicit ec: ExecutionContext): DBIO[Key] = {
    (tableQuery += entity).map(_ => entity)
  }

}
