package org.simonjamesrowe.retro.boards

import org.axonframework.eventhandling.*
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.messaging.Headers
import org.axonframework.messaging.MetaData
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.modelling.command.AggregateMember
import org.axonframework.modelling.command.EntityId
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.time.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val CREATED_BY = "created-by"

open class Command {
    open var createdBy: String

    constructor(createdBy: String) {
        this.createdBy = createdBy
    }
}

data class CreateBoardCommand(val name: String,
                              override var createdBy: String,
                              val lanes: List<String>) : Command(createdBy)

data class BoardCreatedEvent(@AggregateIdentifier val id: String,
                             val name: String,
                             val lanes: List<String>)

open class Entity {
    var revision = 0L
    lateinit var createdBy: String
    lateinit var createdAt: ZonedDateTime
    var updatedAt: ZonedDateTime? = null
    var updatedBy: String? = null

    fun doAudit(eventMessage: DomainEventMessage<Any>) {
        Board.LOG.info("audit properties $eventMessage")
        val timestamp = eventMessage.timestamp
        val revision = eventMessage.sequenceNumber
        if (revision == 0L) {
            createdAt = ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC)
            createdBy = eventMessage.metaData[CREATED_BY] as String
        } else {
            updatedAt = ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC)
            updatedBy = eventMessage.metaData[CREATED_BY] as String
        }
        this.revision = revision
    }
}

@Aggregate
class Board : Entity {

    companion object {
        val LOG = LoggerFactory.getLogger(Board::class.java)
    }

    @AggregateIdentifier
    lateinit var id: String
    lateinit var name: String

    var lanes: MutableMap<String, List<Card>> = HashMap()


    constructor()

    constructor(id: String) {
        this.id = id
    }

    @EventSourcingHandler
    fun on(boardCreatedEvent: BoardCreatedEvent, eventMessage: DomainEventMessage<BoardCreatedEvent>) {
        LOG.info("Handling $boardCreatedEvent")
        id = boardCreatedEvent.id
        name = boardCreatedEvent.name
        boardCreatedEvent.lanes.forEach {
            this.lanes[it] = ArrayList()
        }
        doAudit(eventMessage as DomainEventMessage<Any>)
    }

}


class Card : Entity() {

    @EntityId
    lateinit var id: String
    var text: String = ""

}
