package com.evmap.serverapp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebMvcConfig(
    @Value("\${app.media.root:./storage/media}")
    private val mediaRoot: String,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val absoluteMediaRoot = Paths.get(mediaRoot).toAbsolutePath().normalize()
        val location = absoluteMediaRoot.toUri().toString().let { if (it.endsWith("/")) it else "$it/" }

        registry.addResourceHandler("/media/**")
            .addResourceLocations(location)
    }
}
