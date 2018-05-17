package com.mabrcosta.slick.transactions

import java.sql.SQLException
import java.util.UUID

import com.mabrcosta.slick.transactions.key.data.Key
import com.mabrcosta.slick.transactions.key.persistence.KeysRepository
import org.scalatest.{AsyncWordSpec, FutureOutcome}
import slick.jdbc.{H2Profile, JdbcBackend, JdbcProfile}

import scala.concurrent.Future

class JdbcProfileAsyncDatabaseSpec extends AsyncWordSpec {

  val db: JdbcProfile#Backend#Database =
    JdbcBackend.Database.forURL("jdbc:h2:mem:test_mem;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")

  val backend = new WithSessionJdbcBackend(db)
  val database = new JdbcProfileAsyncDatabase(db, backend)

  val profile = H2Profile
  val repository = new KeysRepository(profile)

  val uidKey: UUID = UUID.randomUUID()
  val keyValue = "key value"
  val key = Key(id = Some(uidKey), value = keyValue, uidOwnerSubject = UUID.randomUUID())

  import database._
  import profile.api._

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    complete {
      new FutureOutcome(db.run(repository.tableQuery.schema.create).flatMap(_ => super.withFixture(test).toFuture))
    } lastly {
      db.run(repository.tableQuery.schema.drop)
    }
  }

  "having a database and a repository" when {

    "doing a single insert on a success transaction" should {
      "return the key when searched" in {
        withTransaction(database, s => s.run(repository.save(key))).flatMap(
          _ =>
            db.run(repository.find(uidKey))
              .map({
                case Some(k) => assert(k.equals(key))
                case None    => fail()
              }))
      }
    }

    "doing multiple inserts on a success transaction" should {
      "return the keys when searched" in {
        val key2 = Key(id = Some(UUID.randomUUID()), value = "key value 2", uidOwnerSubject = UUID.randomUUID())
        val key3 = Key(id = Some(UUID.randomUUID()), value = "key value 3", uidOwnerSubject = UUID.randomUUID())
        val key4 = Key(id = Some(UUID.randomUUID()), value = "key value 4", uidOwnerSubject = UUID.randomUUID())
        val key5 = Key(id = Some(UUID.randomUUID()), value = "key value 5", uidOwnerSubject = UUID.randomUUID())
        withTransaction(database, implicit s => {
            for {
              _ <- repository.save(key).run
              _ <- repository.save(key2).run
              _ <- repository.save(key3).run
              _ <- repository.save(key4).run
              _ <- repository.save(key5).run
            } yield key
          }
        ).flatMap(_ =>
          db.run(repository.find(Seq(uidKey, key2.id.get, key3.id.get, key4.id.get, key5.id.get)))
            .map(res => {
              if (res.size == 5) succeed else fail()
            }))
      }
    }

    "doing an insert on a failed single action transaction" should {
      "return absent when searched" in {
        withFailedTransaction(database, s => s.run(repository.save(key))).flatMap(
          _ =>
            db.run(repository.find(uidKey))
              .map({
                case Some(_) => fail()
                case None    => succeed
              }))
      }
    }

    "doing an insert on a failed multiple actions transaction" should {
      "return absent when searched" in {
        val key2 = Key(id = Some(UUID.randomUUID()), value = keyValue, uidOwnerSubject = UUID.randomUUID())

        withTransaction(database, s => {
          s.run(repository.save(key)).flatMap(_ => s.run(repository.save(key2)))
        }).recover({
            case _: SQLException => {
              "Failed Transaction"
            }
          })
          .flatMap(_ =>
            db.run(repository.find(Seq(uidKey, key2.id.get)))
              .map(res => {
                if (res.nonEmpty) fail() else succeed
              }))
      }
    }

    "doing an insert with a nested outer transaction fail" should {
      "return absent when searched" in {
        val key2 = Key(id = Some(UUID.randomUUID()), value = "key value 2", uidOwnerSubject = UUID.randomUUID())

        withFailedTransaction(database, s => {
          s.run(repository.save(key)).flatMap(_ => s.withTransaction(s.run(repository.save(key2)))(_ => true))
        }).flatMap(_ =>
          db.run(repository.find(Seq(uidKey, key2.id.get)))
            .map(res => {
              if (res.nonEmpty) fail() else succeed
            }))
      }
    }

    "doing an insert with a nested inner transaction fail" should {
      "return just the outer saved key" in {
        val key2 = Key(id = Some(UUID.randomUUID()), value = "key value 2", uidOwnerSubject = UUID.randomUUID())

        withTransaction(database, s => {
          s.run(repository.save(key)).flatMap(_ => s.withTransaction(s.run(repository.save(key2)))(_ => false))
        }).flatMap(_ =>
          db.run(repository.find(Seq(uidKey, key2.id.get)))
            .map(res => {
              if (res.equals(Seq(key))) succeed else fail()
            }))
      }
    }

  }

  private def withTransaction[T](database: JdbcProfileAsyncDatabase,
                                 f: (WithProvidedSessionJdbcBackend#WithSessionDatabase) => Future[T]) = {
    database.withTransaction { session =>
      f(session)
    } { _ =>
      true
    }
  }

  private def withFailedTransaction[T](database: JdbcProfileAsyncDatabase,
                                       f: (WithProvidedSessionJdbcBackend#WithSessionDatabase) => Future[T]) = {
    withTransaction(database, s => {
      f(s).flatMap(_ => Future.failed(new TestException()))
    }).recover({
      case _: TestException => "Failed Transaction"
    })
  }

  class TestException extends Exception

}
