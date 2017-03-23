package com.sinnerschrader.s2b.accounttool.config;

import com.sinnerschrader.s2b.accounttool.config.web.CustomErrorPageRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ErrorPageRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.Charset;

@Configuration
public class ServerConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ServerConfiguration.class);

	@Bean
	@SuppressWarnings("unused")
	public ErrorPageRegistrar errorPageRegistrar() {
		return new CustomErrorPageRegistrar();
	}

	@Bean
	@SuppressWarnings("unused")
	public EmbeddedServletContainerFactory embeddedServletContainerFactory() throws IOException {
		log.info("Configuring ServletContainerFactory");
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		log.info("Setting default uri encoding to {}", Charset.defaultCharset());
		factory.setUriEncoding(Charset.defaultCharset());
		log.info("Setting default http port to {}", factory.getPort());
		return factory;
	}
}
