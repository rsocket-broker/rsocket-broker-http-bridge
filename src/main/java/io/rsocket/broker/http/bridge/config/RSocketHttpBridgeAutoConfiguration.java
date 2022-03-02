/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.broker.http.bridge.config;

import java.util.function.Function;

import io.rsocket.broker.client.spring.BrokerRSocketRequester;
import io.rsocket.broker.client.spring.BrokerRSocketRequesterBuilder;
import io.rsocket.broker.common.spring.ClientTransportFactory;
import io.rsocket.broker.http.bridge.core.FireAndForgetFunction;
import io.rsocket.broker.http.bridge.core.RequestChannelFunction;
import io.rsocket.broker.http.bridge.core.RequestResponseFunction;
import io.rsocket.broker.http.bridge.core.RequestStreamFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.Message;
import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * AutoConfiguration for HTTP RSocket Bridge.
 *
 * @author Olga Maciaszek-Sharma
 * @since 0.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({BrokerRSocketRequester.class, BrokerRSocketRequesterBuilder.class})
@ConditionalOnProperty(value = "spring.cloud.rsocket.broker.http-bridge.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RSocketHttpBridgeProperties.class)
public class RSocketHttpBridgeAutoConfiguration implements ApplicationContextAware, InitializingBean {

	private final BrokerRSocketRequester requester;
	private final RSocketHttpBridgeProperties properties;
	private final ObjectProvider<ClientTransportFactory> transportFactories;
	private ConfigurableApplicationContext applicationContext;

	public RSocketHttpBridgeAutoConfiguration(BrokerRSocketRequester requester, RSocketHttpBridgeProperties properties,
			ObjectProvider<ClientTransportFactory> transportFactories) {
		this.requester = requester;
		this.properties = properties;
		this.transportFactories = transportFactories;
	}

	// Stay with four different endpoints or switch to some other way of differentiating?

	@Bean
	public Function<Mono<Message<Object>>, Mono<Message<Object>>> rr() {
		return new RequestResponseFunction(requester, transportFactories, properties);
	}

	@Bean
	public Function<Flux<Message<Object>>, Flux<Message<Object>>> rc() {
		return new RequestChannelFunction(requester, transportFactories, properties);
	}

	@Bean
	public Function<Mono<Message<Object>>, Flux<Message<Object>>> rs() {
		return new RequestStreamFunction(requester, transportFactories, properties);
	}

	@Bean
	public Function<Mono<Message<Object>>, Mono<Void>> ff() {
		return new FireAndForgetFunction(requester, transportFactories, properties);
	}

	@Bean
	public RSocketStrategies rSocketStrategies() {
		return RSocketStrategies.builder()
				.encoders(encoders -> encoders.add(new Jackson2JsonEncoder()))
				.decoders(decoders -> decoders.add(new Jackson2JsonDecoder()))
				.build();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	// Setting the default Spring Cloud Function function definition.
	@Override
	public void afterPropertiesSet() throws Exception {
		FunctionProperties functionProperties = applicationContext
				.getBean(FunctionProperties.class);
		String definition = functionProperties.getDefinition();
		if (definition == null && properties.isRequestResponseDefault()) {
			functionProperties.setDefinition("rr");
		}
	}
}
