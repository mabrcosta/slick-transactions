package com.mabrcosta.slick.transactions

import slick.JdbcProfileAsyncSession
import slick.dbio.DBIO
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.{ExecutionContext, Future}

class JdbcProfileAsyncDatabase(db: JdbcProfile#Backend#Database, backend: WithSessionJdbcBackend)
    extends JdbcProfileAsyncSession {

  def withSimpleSession[T](f: (JdbcBackend#Session) => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val session = db.createSession()
    val res = f(session)
    res.onComplete(_ => session.close())
    res
  }

  def withSimpleTransaction[T](f: (JdbcBackend#Session) => Future[T])(isSuccess: T => Boolean)(
      implicit ec: ExecutionContext): Future[T] =
    withSimpleSession { s =>
      s.withTransaction(f(s))(isSuccess)
    }

  def withSession[T](f: (WithProvidedSessionJdbcBackend#WithSessionDatabase) => Future[T])(
      implicit ec: ExecutionContext): Future[T] = {
    withSimpleSession { session =>
      f(backend.withSession(session))
    }
  }

  def withTransaction[T](f: (WithProvidedSessionJdbcBackend#WithSessionDatabase) => Future[T])(isSuccess: T => Boolean)(
      implicit ec: ExecutionContext): Future[T] = {
    withSession { db =>
      db.session.withTransaction(f(db))(isSuccess)
    }
  }

  implicit class DBIORunner[T](action: DBIO[T]) {
    def run(implicit database: WithProvidedSessionJdbcBackend#WithSessionDatabase): Future[T] = database.run(action)
  }
}
