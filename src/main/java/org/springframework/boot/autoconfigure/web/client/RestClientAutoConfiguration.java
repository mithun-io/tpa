package org.springframework.boot.autoconfigure.web.client;

/**
 * Dummy class to fix TypeNotPresentException caused by Spring AI 1.0.0-M1 
 * expecting this class from older Spring Boot versions.
 * Spring Boot 4.x has likely moved or removed this class, but OpenAiAutoConfiguration
 * references it strictly in an annotation.
 */
public class RestClientAutoConfiguration {
}
