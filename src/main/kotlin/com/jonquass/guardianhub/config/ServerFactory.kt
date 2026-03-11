package com.jonquass.guardianhub.config

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import java.net.URI
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig

object ServerFactory {

  const val API_BASE = "http://0.0.0.0"
  const val API_URL_TEMPLATE = "${API_BASE}:%d/api/"
  private val RESOURCE_CONFIG =
      ResourceConfig()
          .packages("com.jonquass.guardianhub.resource")
          .packages("com.jonquass.guardianhub.filter")
          .register(AppConfig::class.java)
          .register(OpenApiResource::class.java)
  private val OPEN_API =
      OpenAPI()
          .info(
              Info()
                  .title("Guardian Hub API")
                  .version("1.0.0")
                  .description("Configuration UI for Guardian Hub"))
          .servers(listOf(Server().url("http://localhost:8888/api").description("Local")))
          .components(
              Components()
                  .addSecuritySchemes(
                      "bearerAuth",
                      SecurityScheme()
                          .type(SecurityScheme.Type.HTTP)
                          .scheme("bearer")
                          .name("bearerAuth")
                          .description("Session token from /auth/login")))
  private val OPEN_API_CONFIG =
      SwaggerConfiguration()
          .openAPI(OPEN_API)
          .resourcePackages(
              setOf("com.jonquass.guardianhub.resource", "com.jonquass.guardianhub.core.api"))
          .prettyPrint(true)

  fun createHttpServer(port: Int): HttpServer {
    configureOpenApi()
    val uri = URI.create(API_URL_TEMPLATE.format(port))
    return GrizzlyHttpServerFactory.createHttpServer(uri, RESOURCE_CONFIG, false)
  }

  private fun configureOpenApi() {
    @Suppress("UNCHECKED_CAST")
    val builder =
        JaxrsOpenApiContextBuilder::class.java.getDeclaredConstructor().newInstance()
            as JaxrsOpenApiContextBuilder<*>
    builder.openApiConfiguration(OPEN_API_CONFIG).buildContext(true)
  }
}
