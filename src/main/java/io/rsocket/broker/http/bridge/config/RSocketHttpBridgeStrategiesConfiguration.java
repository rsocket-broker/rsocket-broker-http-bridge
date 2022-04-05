package io.rsocket.broker.http.bridge.config;

import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * Default {@link RSocketStrategies} configuration for HTTP RSocket Bridge.
 *
 * @author Olga Maciaszek-Sharma
 * @since 0.3.0
 */
@Configuration(proxyBeanMethods = false)
public class RSocketHttpBridgeStrategiesConfiguration {

	@Bean
	RSocketStrategiesCustomizer bridgeRSocketStrategiesCustomizer() {
		return strategies -> strategies
				.encoders(encoders -> encoders.add(new Jackson2JsonEncoder()))
				.decoders(decoders -> decoders.add(new Jackson2JsonDecoder()));
	}
}
