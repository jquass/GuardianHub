package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.manager.TimezoneManager
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/timezone")
class TimezoneResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getTimezones(): Response = TimezoneManager.getTimezones()

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateTimezone(request: UpdateTimezoneRequest): Response =
      TimezoneManager.updateTimezones(request)
}
