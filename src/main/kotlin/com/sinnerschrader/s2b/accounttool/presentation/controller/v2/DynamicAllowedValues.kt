package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import springfox.documentation.service.AllowableListValues
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.ParameterBuilderPlugin
import springfox.documentation.spi.service.contexts.ParameterContext
import springfox.documentation.swagger.common.SwaggerPluginSupport
import springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER


/**
 * Handles parameters with allowed value list that is dynamically retrieved.
 *
 * @link https://github.com/springfox/springfox/issues/1877
 * @link https://gist.github.com/rehevkor5/6374faa255a56174ec9307df5049f9a1
 */
@Component
@Order(SWAGGER_PLUGIN_ORDER + 1)
class DynamicAllowedValues : ParameterBuilderPlugin {

    @Autowired
    lateinit var ldapConfiguration: LdapConfiguration

    override fun apply(context: ParameterContext) {
        context.apiParam()?.apply {
            when (allowableValues) {
                COMPANIES -> context.allowableValues { ldapConfiguration.companies.keys.toList().sorted() }
                USERTYPE -> context.allowableValues { ldapConfiguration.permissions.defaultGroups.keys.toList().sorted() }
                else -> Unit
            }
        }
    }
    private fun ParameterContext.apiParam() = resolvedMethodParameter().findAnnotation(ApiParam::class.java).orNull()
    private fun ParameterContext.allowableValues(block: () -> List<String>) {
        parameterBuilder().apply {
            parameterType("query")
            allowableValues(object : AllowableListValues(emptyList(), "LIST") {
                override fun getValues() = block.invoke()
            })
        }
    }

    override fun supports(delimiter: DocumentationType) = SwaggerPluginSupport.pluginDoesApply(delimiter)

    companion object {
        const val COMPANIES = "dynamic[companies]"
        const val USERTYPE = "dynamic[usertype]"
    }
}
