package org.openmrs.module.outcomes.config;

import ClickSend.ApiClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class Config {
	
	@Bean("clickSendConfig")
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public ApiClient clickSendConfig() {
		ApiClient clickSendApiClient = new ApiClient();
		clickSendApiClient.setUsername(System.getenv("CLICK_SEND_USERNAME"));
		clickSendApiClient.setPassword(System.getenv("CLICK_SEND_API_KEY"));
		return clickSendApiClient;
	}
}
