package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import io.swagger.annotations.ApiParam
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import springfox.documentation.service.AllowableListValues
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.ExpandedParameterBuilderPlugin
import springfox.documentation.spi.service.ParameterBuilderPlugin
import springfox.documentation.spi.service.contexts.ParameterContext
import springfox.documentation.spi.service.contexts.ParameterExpansionContext
import springfox.documentation.spring.web.readers.parameter.ExpandedParameterBuilder
import springfox.documentation.swagger.common.SwaggerPluginSupport
import springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER


/**
 * Handles parameters with allowed value list that is dynamically retrieved from database. To activate this plugin, set
 * the allowableValues property of the @ApiParam annotation to "dynamic[whatever]".
 *
 * @link https://github.com/springfox/springfox/issues/1877
 */
@Component
@Order(SWAGGER_PLUGIN_ORDER + 1)
class ParameterDynamicAllowedValuesBuilder : ParameterBuilderPlugin {

    override fun apply(context: ParameterContext) {
        val apiParam = context.resolvedMethodParameter().findAnnotation(ApiParam::class.java)

        if (apiParam.isPresent) {
            val allowableValuesString = apiParam.get().allowableValues
            if ("dynamic[whatever]" == allowableValuesString) {
                context.parameterBuilder().parameterType("query")
                context.parameterBuilder().allowableValues(AllowableListValues(listOf("a", "b"), "LIST"))
            }
        }
    }

    override fun supports(delimiter: DocumentationType): Boolean {
        return SwaggerPluginSupport.pluginDoesApply(delimiter)
    }
}
