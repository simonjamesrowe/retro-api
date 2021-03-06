package org.simonjamesrowe.retro

import com.mongodb.MongoClient
import org.axonframework.eventsourcing.EventSourcingRepository
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.extensions.mongo.DefaultMongoTemplate
import org.axonframework.extensions.mongo.MongoTemplate
import org.axonframework.extensions.mongo.eventsourcing.eventstore.MongoEventStorageEngine
import org.axonframework.extensions.mongo.eventsourcing.eventstore.StorageStrategy
import org.axonframework.extensions.mongo.eventsourcing.tokenstore.MongoTokenStore
import org.axonframework.messaging.Headers
import org.axonframework.modelling.command.Aggregate
import org.axonframework.modelling.command.Repository
import org.axonframework.serialization.Serializer
import org.axonframework.spring.config.AxonConfiguration
import org.simonjamesrowe.retro.boards.Board
import org.simonjamesrowe.retro.boards.CREATED_BY
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageHeaders
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import java.nio.ByteBuffer
import java.time.Instant


@SpringBootApplication
@EnableBinding(Sink::class)
class RetrospectiveApiApplication

fun main(args: Array<String>) {
    runApplication<RetrospectiveApiApplication>(*args)
}

@Configuration
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(
            http: ServerHttpSecurity): SecurityWebFilterChain? {
        return http.cors().disable().csrf().disable()
                .build()
    }

}

@Configuration
class EventSourcingConfiguration {

    @Bean
    fun axonMongoTemplate(client: MongoClient): MongoTemplate {
        return DefaultMongoTemplate.builder()
                .mongoDatabase(client.getDatabase("retro"))
                .domainEventsCollectionName("retro_events")
                .snapshotEventsCollectionName("retro_snapshots")
                .trackingTokensCollectionName("retro_tracking_tokens")
                .build()
    }

    @Bean
    fun eventStore(storageEngine: EventStorageEngine?, configuration: AxonConfiguration): EmbeddedEventStore? {
        return EmbeddedEventStore.builder()
                .storageEngine(storageEngine)
                .messageMonitor(configuration.messageMonitor(EventStore::class.java, "eventStore"))
                .build()
    }

    @Bean
    fun mongoTokenStore(mongoTemplate: MongoTemplate, eventSerializer: Serializer): MongoTokenStore {
        return MongoTokenStore.builder().mongoTemplate(mongoTemplate).serializer(eventSerializer).build()
    }

    @Bean
    fun storageEngine(mongoTemplate: MongoTemplate, eventSerializer: Serializer): EventStorageEngine? {
        return MongoEventStorageEngine.builder().mongoTemplate(mongoTemplate)
                .eventSerializer(eventSerializer)
                .build()
    }

    @Bean
    fun boardRepository(eventStore: EventStore): Repository<Board> {
        return EventSourcingRepository.builder(Board::class.java).eventStore(eventStore).build()
    }
}

class AggregateUtils {

    companion object {
        fun <T> aggregateRoot(aggregate: Aggregate<T>): T {
            var t: T? = null
            aggregate.execute { t = it }
            return t!!
        }
    }
}


/**
 * , headers are {axon-message-id=ce21cdcf-4c7e-4653-8567-29e1f0fb1701,
 * axon-message-aggregate-seq=        ,
 * axon-metadata-correlationId=7f254132-ca9f-49ce-a0aa-b28a1759708b,
 * axon-message-aggregate-type=Board,
 * axon-message-revision=null,
 * axon-message-timestamp=  qm!�],
 * axon-message-type=org.simonjamesrowe.retro.boards.BoardCreatedEvent,
 * axon-metadata-traceId=7f254132-ca9f-49ce-a0aa-b28a1759708b,
 * axon-message-aggregate-id=daa94d63-467e-45ba-aa06-292c981ee260}

 */
data class MessageMetaData(val messageId: String,
                           val aggregateSequence: Long,
                           val aggregateType: String,
                           val messageType: String,
                           val timestamp: Instant,
                           val aggregateId: String,
                           val createdBy: String)

class KafkaAxonUtils {

    companion object {

        fun messageMetaData(headers: MessageHeaders, index: Int): MessageMetaData {
            var messageMetaData = HashMap<String, String?>()
            var headers = (headers.get(KafkaHeaders.BATCH_CONVERTED_HEADERS, List::class.java)?.get(index) as Map<String, ByteArray>)

            return MessageMetaData(messageId = String(headers[Headers.MESSAGE_ID]!!),
                    aggregateSequence = ByteBuffer.wrap(headers[Headers.AGGREGATE_SEQ]!!).long,
                    aggregateType = String(headers[Headers.AGGREGATE_TYPE]!!),
                    messageType = String(headers[Headers.MESSAGE_TYPE]!!),
                    aggregateId = String(headers[Headers.AGGREGATE_ID]!!),
                    timestamp = Instant.ofEpochMilli(ByteBuffer.wrap(headers[Headers.MESSAGE_TIMESTAMP]!!).long),
                    createdBy = String(headers["axon-metadata-$CREATED_BY"]!!)
            )
        }
    }
}
