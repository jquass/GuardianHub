package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.manager.HomepageManager
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/homepage")
class HomepageResource {

  @GET
  @Path("/link")
  @Produces(MediaType.APPLICATION_JSON)
  fun getHomepageLink(): Response = HomepageManager.getHomepageLink()
}
