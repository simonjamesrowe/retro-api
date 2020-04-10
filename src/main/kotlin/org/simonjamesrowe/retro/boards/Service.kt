package org.simonjamesrowe.retro.boards

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.modelling.command.Repository
import org.springframework.stereotype.Service
import java.util.*


@Service
class BoardCommandHandler(private val boardRepository: Repository<Board>) {


    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.ALWAYS)
    fun createBoard(createBoardCommand: CreateBoardCommand) {
        var aggregate = boardRepository.newInstance { Board(UUID.randomUUID().toString()) }
        aggregate.execute { AggregateLifecycle.apply(BoardCreatedEvent(it.id, createBoardCommand.name, createBoardCommand.createdBy, createBoardCommand.lanes)) }

    }


}
