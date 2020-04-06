package org.simonjamesrowe.retro.boards

import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate

data class CreateBoardCommand(val name: String,
                              val createdBy: String,
                              val lanes: List<String>)

data class BoardCreatedEvent(@AggregateIdentifier val id: String,
                             val name: String,
                             val createdBy: String,
                             val lanes: List<String>)

@Aggregate
class Board {
    @AggregateIdentifier
    lateinit var id: String
    lateinit var name: String
    lateinit var createdBy : String
    var lanes: MutableMap<String, List<Card>> = HashMap()


    @EventSourcingHandler
    fun on(boardCreatedEvent: BoardCreatedEvent) {
        id = boardCreatedEvent.id
        name = boardCreatedEvent.name
        createdBy = boardCreatedEvent.createdBy
        boardCreatedEvent.lanes.forEach{
            this.lanes[it] = ArrayList()
        }
    }

}

data class Card(val id: String, val text: String,
                val createdBy: String)
