# Slick Transactions
This provides an non-blocking api to take control of Slick 3 transactions, with an Slick 2 similar api. 
It uses a **naive** implementation to attempt to address some of current slick api limitations when it comes to taking control of sessions and transactions in some cases.

An example use case could be:
```
type Session = WithProvidedSessionJdbcBackend#WithSessionDatabase
val database: JdbcProfileAsyncDatabase
import database._

def createUser(name: String)(implicit session: Session): Future[User] = {
  val dbioAction = userRepository.save(User(name))
  dbioAction.run
}

def createKey(name: User)(implicit session: Session): Future[Either[Throwable, User] = {
  Future.successful(Left(new Exception()))
}

val result = database.withTransaction { implicit session =>
  for {
    user <- createUser("username")
    key <- createKey(user)
  } yield key
}({
  case Left(_)  => false
  case Right(_) => true
})

```

At the end the result is evaluated to ensure the transaction should be committed or rolled back. 
Should the future fail the transaction is also rolled back.

In the example above the transaction above would be rolled back as it would not pass the result evaluation.

#### Features

It currently supports the following features:

* Transactions across scala's Future
* Result validation for error handling
* Concurrently **unsafe** nested transactions

Nested transactions are supported using ```session.withTransaction``` but should be handled with extreme care as 
they are based on JDBC's savepoints and not thread safe. 



#### Examples

For more usage examples please check the [JdbcProfileAsyncDatabaseSpec](https://github.com/mabrcosta/slick-transactions/blob/master/src/test/scala/com/mabrcosta/slick/transactions/JdbcProfileAsyncDatabaseSpec.scala)
        
        
        

#### Blocking API

A blocking API alternative is already provided by [Blocking-slick](https://github.com/takezoe/blocking-slick)