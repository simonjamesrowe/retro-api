package org.simonjamesrowe.retro

import com.mongodb.MongoClient
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventsourcing.EventSourcingRepository
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.extensions.mongo.DefaultMongoTemplate
import org.axonframework.extensions.mongo.eventsourcing.eventstore.MongoEventStorageEngine
import org.axonframework.modelling.command.Repository
import org.axonframework.spring.config.AxonConfiguration
import org.simonjamesrowe.retro.boards.Board
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.stereotype.Component


@SpringBootApplication
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
    fun eventStore(storageEngine: EventStorageEngine?, configuration: AxonConfiguration): EmbeddedEventStore? {
        return EmbeddedEventStore.builder()
                .storageEngine(storageEngine)
                .messageMonitor(configuration.messageMonitor(EventStore::class.java, "eventStore"))
                .build()
    }

    @Bean
    fun storageEngine(client: MongoClient?): EventStorageEngine? {
        return MongoEventStorageEngine.builder().mongoTemplate(DefaultMongoTemplate.builder()
                .mongoDatabase(client)
                .domainEventsCollectionName("retro_events")
                .snapshotEventsCollectionName("retro_snapshots")
                .build())
                .build()
    }

    @Bean
    fun boardRepository(eventStore: EventStore) : Repository<Board> {
        return EventSourcingRepository.builder(Board::class.java).eventStore(eventStore).build()
    }
}
