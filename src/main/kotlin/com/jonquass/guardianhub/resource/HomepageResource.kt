package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.HomepageLinkResponse
import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.HomepageManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/homepage")
class HomepageResource {

  @GET
  @Path("/link")
  @Operation(
      summary = "Gets the link to the homepage",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description =
                      "Returns a link to the homepage. This will either be a URL, or an IP address, depending on which resolves.",
                  content =
                      [Content(schema = Schema(implementation = HomepageLinkResponse::class))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun getHomepageLink(): Response = HomepageManager.getHomepageLink().toResponse()
}
