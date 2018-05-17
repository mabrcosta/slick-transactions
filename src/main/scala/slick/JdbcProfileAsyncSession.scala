package slick

import java.sql.Savepoint

import slick.jdbc.JdbcBackend

import scala.concurrent.{ExecutionContext, Future}

trait JdbcProfileAsyncSession {

  /**
    * Extends Session to add methods for session management.
    */
  implicit class AsyncSession(session: JdbcBackend#Session) {

    def withTransaction[T](f: => Future[T])(isSuccess: T => Boolean)(implicit ec: ExecutionContext): Future[T] = {
      val s = session.asInstanceOf[JdbcBackend#BaseSession]

      if (s.isInTransaction) {
        val savepoint = setSavepoint(s)
        handleExecution(s, f, isSuccess, () => (), () => rollback(s, savepoint))
      } else {
        s.startInTransaction
        handleExecution(s, f, isSuccess, () => commitAndEndTransaction(s), () => rollbackAndEndTransaction(s))
      }
    }

    private def handleExecution[T](session: JdbcBackend#BaseSession,
                                   f: => Future[T],
                                   isSuccess: T => Boolean,
                                   success: () => Unit,
                                   failure: () => Unit)(implicit ec: ExecutionContext): Future[T] = {
      f.map(v => {
          if (isSuccess(v)) success() else failure()
          v
        })
        .recoverWith({
          case ex: Throwable => {
            failure()
            Future.failed(ex)
          }
        })
    }

    private def commitAndEndTransaction(session: JdbcBackend#BaseSession): Unit =
      session.endInTransaction(session.conn.commit())

    private def rollbackAndEndTransaction(session: JdbcBackend#BaseSession): Unit =
      session.endInTransaction(session.conn.rollback())

    private def rollback(session: JdbcBackend#BaseSession, savepoint: Savepoint): Unit =
      session.conn.rollback(savepoint)

    private def setSavepoint(session: JdbcBackend#BaseSession): Savepoint = session.conn.setSavepoint()

  }
}
