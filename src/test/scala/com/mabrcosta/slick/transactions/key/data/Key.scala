package com.mabrcosta.slick.transactions.key.data

import java.util.UUID

case class Key(id: Option[UUID] = Some(UUID.randomUUID()), value: String, uidOwnerSubject: UUID)
