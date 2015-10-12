package com.workforceware.tools.jenkins.guard;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class BuildStatusService {

	private static final String JENKINS_URL_TEMPLATE = "%s/job/%s/lastSuccessfulBuild/api/json";
	private static final String RESULT_ENTRY = "result";
	private static final String SUCCESS = "SUCCESS";
	@Value("${jenkins.url}")
	private String jenkinsUrl;
	@Value("${jenkins.job.name}")
	private String jenkinsJobName;
	@Value("${jenkins.user}")
	private String jenkinsUser;
	@Value("${jenkins.api.token}")
	private String jenkinsApiToken;

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private JsonParser jsonParser;

	@Scheduled(cron = "0/10 * * * * ?")
	public void checkStatus() {
		log.info("check build status");
		ResponseEntity<String> responseEntity = restTemplate.exchange(
				String.format(JENKINS_URL_TEMPLATE, jenkinsUrl, jenkinsJobName), HttpMethod.GET, prepareRequest(),
				String.class);
		checkResult(responseEntity.getBody());
	}

	private void checkResult(String responseBody) {
		if (SUCCESS.equals(jsonParser	.parseMap(responseBody)
										.get(RESULT_ENTRY))) {
			log.info("BUILD OK!!!");
		} else {
			log.info("!!!BUILD FAILED!!!");
		}
	}

	private HttpEntity<String> prepareRequest() {
		HttpHeaders headers = new HttpHeaders();
		if (StringUtils.isNotBlank(jenkinsUser) && StringUtils.isNotBlank(jenkinsApiToken)) {
			String plainCreds = String.format("%s:%s", jenkinsUser, jenkinsApiToken);
			byte[] plainCredsBytes = plainCreds.getBytes();
			byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
			String base64Creds = new String(base64CredsBytes);
			headers.add("Authorization", "Basic " + base64Creds);
		}
		HttpEntity<String> request = new HttpEntity<>(headers);
		return request;
	}

}
