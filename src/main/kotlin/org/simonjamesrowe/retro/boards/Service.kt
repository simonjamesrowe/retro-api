package org.simonjamesrowe.retro.boards

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventhandling.EventHandler
import org.axonframework.messaging.MetaData
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.modelling.command.Repository
import org.simonjamesrowe.retro.AggregateUtils
import org.simonjamesrowe.retro.KafkaAxonUtils
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.messaging.Message
import org.springframework.stereotype.Service
import java.util.*


@Service
class BoardCommandHandler(private val boardRepository: Repository<Board>) {

    companion object {
        val LOG = LoggerFactory.getLogger(BoardCommandHandler::class.java)
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.ALWAYS)
    fun createBoard(createBoardCommand: CreateBoardCommand) : Board {
        var aggregate = boardRepository.newInstance { Board(UUID.randomUUID().toString()) }
        aggregate.execute { AggregateLifecycle.apply(BoardCreatedEvent(it.id, createBoardCommand.name, createBoardCommand.lanes),
                MetaData.with(CREATED_BY, "srowe")) }
        return AggregateUtils.aggregateRoot(aggregate)
    }

    @StreamListener(target = Sink.INPUT)
    fun consumeKafka(message: Message<List<String>>) {
        message.payload.forEachIndexed{ index, value ->
            LOG.info("Kafka message is ${value}, headers are ${KafkaAxonUtils.messageMetaData(message.headers, index)}")
        }
    }



}
