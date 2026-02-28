package com.jonquass.guardianhub.filter

import com.jonquass.guardianhub.manager.auth.AuthManager
import com.jonquass.guardianhub.manager.auth.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFilterTest {

  private val filter = AuthFilter()

  private fun mockContext(
      path: String,
      method: String = "GET",
      authHeader: String? = null,
      acceptHeader: String? = null,
  ): ContainerRequestContext {
    val uriInfo = mockk<UriInfo>(relaxed = true) { every { getPath() } returns path }
    return mockk(relaxed = true) {
      every { getUriInfo() } returns uriInfo
      every { getMethod() } returns method
      every { getHeaderString("Authorization") } returns authHeader
      every { getHeaderString("Accept") } returns acceptHeader
    }
  }

  @BeforeEach
  fun setUp() {
    mockkObject(AuthManager)
    mockkObject(SessionManager)
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  // --- public paths ---

  @Test
  fun `filter should allow login html`() {
    val context = mockContext("login.html")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should allow login js`() {
    val context = mockContext("login.js")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should allow css files`() {
    val context = mockContext("styles.css")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should allow svg files`() {
    val context = mockContext("icon.svg")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should allow ico files`() {
    val context = mockContext("favicon.ico")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should allow auth login endpoint`() {
    val context = mockContext("api/auth/login", method = "POST")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should allow auth reset-to-factory endpoint`() {
    val context = mockContext("api/auth/reset-to-factory", method = "POST")
    filter.filter(context)
    verify(exactly = 0) { context.abortWith(any()) }
  }

  @Test
  fun `filter should not allow non-login html`() {
    every { AuthManager.getToken(any()) } returns null
    val context = mockContext("dashboard.html")
    filter.filter(context)
    verify(exactly = 1) { context.abortWith(any()) }
  }

  @Test
  fun `filter should not allow non-login js`() {
    every { AuthManager.getToken(any()) } returns null
    val context = mockContext("app.js")
    filter.filter(context)
    verify(exactly = 1) { context.abortWith(any()) }
  }

  // --- missing token ---

  @Test
  fun `filter should abort with 401 JSON when no token and accept is not html`() {
    every { AuthManager.getToken(any()) } returns null
    val responseSlot = slot<Response>()
    val context = mockContext("api/config", acceptHeader = "application/json")
    every { context.abortWith(capture(responseSlot)) } returns Unit

    filter.filter(context)

    assertThat(responseSlot.captured.status).isEqualTo(Response.Status.UNAUTHORIZED.statusCode)

    @Suppress("UNCHECKED_CAST") val body = responseSlot.captured.entity as Map<String, String>
    assertThat(body["error"]).isEqualTo("Unauthorized")
    assertThat(body["message"]).isEqualTo("No authentication token provided")
  }

  @Test
  fun `filter should redirect to login when no token and accept is html`() {
    every { AuthManager.getToken(any()) } returns null
    val responseSlot = slot<Response>()
    val context = mockContext("api/config", acceptHeader = "text/html")
    every { context.abortWith(capture(responseSlot)) } returns Unit

    filter.filter(context)

    assertThat(responseSlot.captured.status).isEqualTo(Response.Status.SEE_OTHER.statusCode)
    assertThat(responseSlot.captured.location.toString()).isEqualTo("/login.html")
  }

  @Test
  fun `filter should abort when token is empty string`() {
    every { AuthManager.getToken(any()) } returns ""
    val context = mockContext("api/config", acceptHeader = "application/json")
    filter.filter(context)
    verify(exactly = 1) { context.abortWith(any()) }
  }

  // --- invalid token ---

  @Test
  fun `filter should abort with 401 when token is invalid`() {
    every { AuthManager.getToken(any()) } returns "bad-token"
    every { SessionManager.isValidSession("bad-token") } returns false
    val responseSlot = slot<Response>()
    val context = mockContext("api/config", acceptHeader = "application/json")
    every { context.abortWith(capture(responseSlot)) } returns Unit

    filter.filter(context)

    assertThat(responseSlot.captured.status).isEqualTo(Response.Status.UNAUTHORIZED.statusCode)

    @Suppress("UNCHECKED_CAST") val body = responseSlot.captured.entity as Map<String, String>
    assertThat(body["message"]).isEqualTo("Invalid or expired authentication token")
  }

  @Test
  fun `filter should redirect to login when token is invalid and accept is html`() {
    every { AuthManager.getToken(any()) } returns "bad-token"
    every { SessionManager.isValidSession("bad-token") } returns false
    val responseSlot = slot<Response>()
    val context = mockContext("api/config", acceptHeader = "text/html")
    every { context.abortWith(capture(responseSlot)) } returns Unit

    filter.filter(context)

    assertThat(responseSlot.captured.status).isEqualTo(Response.Status.SEE_OTHER.statusCode)
    assertThat(responseSlot.captured.location.toString()).isEqualTo("/login.html")
  }

  // --- valid token ---

  @Test
  fun `filter should allow request with valid token`() {
    every { AuthManager.getToken(any()) } returns "valid-token"
    every { SessionManager.isValidSession("valid-token") } returns true
    val context = mockContext("api/config", authHeader = "Bearer valid-token")

    filter.filter(context)

    verify(exactly = 0) { context.abortWith(any()) }
  }
}
