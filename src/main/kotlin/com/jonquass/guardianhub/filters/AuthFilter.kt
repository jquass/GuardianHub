package com.jonquass.guardianhub.filters

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.managers.auth.AuthManager
import com.jonquass.guardianhub.managers.auth.SessionManager
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

@Provider
@PreMatching
class AuthFilter :
    ContainerRequestFilter,
    Loggable {
    private val logger = logger()

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path
        val method = requestContext.method

        logger.debug("Request: {} {}", method, path)

        // Check if this is a public path
        if (isPublicPath(path)) {
            logger.debug("Public path allowed: {}", path)
            return
        }

        // All other paths require authentication
        val authHeader = requestContext.getHeaderString("Authorization")
        val token = AuthManager.getToken(authHeader)

        if (token.isNullOrEmpty()) {
            logger.warn("No auth token provided for: {}", path)
            abortWithUnauthorized(requestContext, "No authentication token provided")
            return
        }

        if (!SessionManager.isValidSession(token)) {
            logger.warn("Invalid or expired token for: {}", path)
            abortWithUnauthorized(requestContext, "Invalid or expired authentication token")
            return
        }

        logger.debug("Authenticated request to: {}", path)
    }

    private fun isPublicPath(path: String): Boolean {
        // Static files
        if (path.endsWith(".html") && path.contains("login")) return true
        if (path.endsWith(".js") && path.contains("login")) return true
        if (path.endsWith(".css")) return true
        if (path.endsWith(".svg")) return true
        if (path.endsWith(".ico")) return true

        // API endpoints
        if (path.endsWith("auth/login")) return true
        if (path.endsWith("auth/reset-to-factory")) return true

        return false
    }

    private fun abortWithUnauthorized(
        requestContext: ContainerRequestContext,
        message: String,
    ) {
        // For browser requests (HTML), redirect to login
        val accept = requestContext.getHeaderString("Accept") ?: ""
        if (accept.contains("text/html")) {
            requestContext.abortWith(
                Response.seeOther(java.net.URI.create("/login.html")).build(),
            )
        } else {
            // For API requests, return 401 JSON
            requestContext.abortWith(
                Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(
                        mapOf(
                            "error" to "Unauthorized",
                            "message" to message,
                        ),
                    ).build(),
            )
        }
    }
}
