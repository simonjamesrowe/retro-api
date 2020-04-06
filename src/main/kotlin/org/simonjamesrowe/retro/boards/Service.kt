package org.simonjamesrowe.retro.boards

import org.axonframework.commandhandling.CommandHandler
import org.springframework.stereotype.Service
import org.axonframework.modelling.command.AggregateLifecycle.apply
import java.util.*


@Service
class BoardCommandHandler {

    @CommandHandler
    fun createBoard(command: CreateBoardCommand) {
        apply(BoardCreatedEvent(UUID.randomUUID().toString(), command.name, command.createdBy, command.lanes))
    }


}
