package org.simonjamesrowe.retro

import org.axonframework.commandhandling.CommandBus
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.commandhandling.SimpleCommandBus
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.messaging.Message
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor
import org.axonframework.spring.config.AxonConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@SpringBootApplication
class RetrospectiveApiApplication

fun main(args: Array<String>) {
	runApplication<RetrospectiveApiApplication>(*args)
}

@Configuration
class EventSourcingConfiguration {


}
