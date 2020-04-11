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
import org.axonframework.modelling.command.Aggregate
import org.axonframework.modelling.command.Repository
import org.axonframework.serialization.Serializer
import org.axonframework.spring.config.AxonConfiguration
import org.simonjamesrowe.retro.boards.Board
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
    fun axonMongoTemplate(client: MongoClient) : MongoTemplate{
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
    fun mongoTokenStore(mongoTemplate: MongoTemplate, eventSerializer: Serializer) : MongoTokenStore{
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

class KafkaUtils {

    companion object {

        fun messageMetaData(headers: MessageHeaders, index: Int): Map<String, String?> {
            var messageMetaData = HashMap<String, String?>()
            (headers.get(KafkaHeaders.BATCH_CONVERTED_HEADERS, List::class.java)?.get(index) as Map<String, ByteArray>)
                    .forEach { (key, value) -> messageMetaData[key] = if (value == null) null else String(value)}
            return messageMetaData
        }
    }
}
