package com.jrcr.gestorofertasweb.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;

/**
 * Configuración de MessageSource para la internacionalización (i18n).
 *
 * Aunque Spring Boot autoconfigura MessageSource si existe messages.properties,
 * esta configuración explícita garantiza:
 * 1. Codificación UTF-8 correcta (evita problemas con tildes y ).
 * 2. Integración con las anotaciones de validación de beans (@Valid).
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }
}