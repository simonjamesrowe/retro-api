package org.simonjamesrowe.retro.boards

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/board")
class BoardController(val commandGateway: CommandGateway) {

    @PostMapping
    suspend fun create(@RequestBody createBoardCommand: CreateBoardCommand) : String {
        var result = GlobalScope.async {commandGateway.sendAndWait<String>(createBoardCommand)}
        return result.await()
    }

}
