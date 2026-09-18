package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import org.autojs.plugin.mail.api.MailContract

/**
 * The op table of one session (roadmap P2.5 `RequestRouter`, started in P2.1 with the session
 * operations): each handler receives the `args` object as a JSON string and returns the `result`
 * as a JSON document. Ops of the contract without a handler answer `UNSUPPORTED_OPERATION`; names
 * outside the contract answer `INVALID_ARGUMENT`. The table is static so the JVM snapshot test
 * can compare it with `MailContract.OPS` without a session.
 */
internal class RequestRouter(private val session: MailSession) {

    /** Outcome of routing an op name, before any handler runs. */
    sealed class Route {
        class Handled(val handler: Handler) : Route()
        data object Unsupported : Route()
        data object Unknown : Route()
    }

    fun interface Handler {
        /** Runs the operation on the session executor thread; throws [MailException] or anything the mapper understands. */
        fun handle(session: MailSession, argsJson: String): String
    }

    fun route(op: String?): Route = when {
        op == null -> Route.Unknown
        !MailContract.isKnownOp(op) -> Route.Unknown
        else -> HANDLERS[op]?.let { Route.Handled(it) } ?: Route.Unsupported
    }

    /** Runs [op] and returns the result document; failures come back as [MailException]. */
    fun execute(op: String?, argsJson: String): String {
        val handler = when (val route = route(op)) {
            is Route.Handled -> route.handler
            Route.Unsupported -> throw MailException.unsupported("$op is not implemented yet")
            Route.Unknown -> throw MailException.invalidArgument(if (op == null) "request carries no op" else "unknown op: $op")
        }
        return try {
            handler.handle(session, argsJson)
        } catch (e: MailException) {
            throw e
        } catch (e: Throwable) {
            throw session.mapper.map(e, op)
        }
    }

    companion object {
        val HANDLERS: Map<String, Handler> = linkedMapOf(
            MailContract.OP_SESSION_TEST to Handler { session, _ -> session.test().toJson() },
            // The Binder closes the session after this result went out (contract B.3 ordering).
            MailContract.OP_SESSION_CLOSE to Handler { _, _ -> "true" },
        )

        val SUPPORTED_OPS: Set<String> get() = HANDLERS.keys

        /** Contract ops still answering `UNSUPPORTED_OPERATION`; shrinks as P2.2 to P2.4 land. */
        val PENDING_OPS: List<String> get() = MailContract.OPS.filter { it !in HANDLERS }

        fun errorCodeFor(route: Route): String? = when (route) {
            is Route.Handled -> null
            Route.Unsupported -> MailErrorCode.UNSUPPORTED_OPERATION
            Route.Unknown -> MailErrorCode.INVALID_ARGUMENT
        }
    }
}
