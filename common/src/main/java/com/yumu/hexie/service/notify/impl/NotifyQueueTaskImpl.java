package com.yumu.hexie.service.notify.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yumu.hexie.common.util.JacksonJsonUtil;
import com.yumu.hexie.integration.notify.PayNotification.AccountNotification;
import com.yumu.hexie.model.ModelConstant;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.model.user.UserRepository;
import com.yumu.hexie.service.common.GotongService;
import com.yumu.hexie.service.maintenance.MaintenanceService;
import com.yumu.hexie.service.notify.NotifyQueueTask;

public class NotifyQueueTaskImpl implements NotifyQueueTask {
	
	private static Logger logger = LoggerFactory.getLogger(NotifyQueueTaskImpl.class);
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private MaintenanceService maintenanceService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GotongService gotongService;
	
	/**
	 * 异步发送到账模板消息
	 */
	@Override
	@Async("taskExecutor")
	public void sendWuyeNotificationAysc() {
		
		while(true) {
			try {
				if (!maintenanceService.isQueueSwitchOn()) {
					logger.info("queue switch off ! ");
					Thread.sleep(60000);
					continue;
				}
				String json = redisTemplate.opsForList().leftPop(ModelConstant.KEY_NOTIFY_PAY_QUEUE, 30, TimeUnit.SECONDS);
				if (StringUtils.isEmpty(json)) {
					continue;
				}
				ObjectMapper objectMapper = JacksonJsonUtil.getMapperInstance(false);
				AccountNotification queue = objectMapper.readValue(json, new TypeReference<AccountNotification>(){});
				
				logger.info("start to consume wuyeNotificatione queue : " + queue);
				
				boolean isSuccess = false;
				List<Map<String, String>> openidList = queue.getOpenids();
				if (openidList == null || openidList.isEmpty()) {
					continue;
				}
				List<Map<String, String>> resendList = new ArrayList<>();
				for (Map<String, String> openidMap : openidList) {
					
					User user = null;
					String openid = openidMap.get("openid");
					if (StringUtils.isEmpty(openid)) {
						logger.warn("openid is empty, will skip. ");
						continue;
					}
					List<User> userList = userRepository.findByOpenid(openid);
					if (userList!=null && !userList.isEmpty()) {
						user = userList.get(0);
					}else {
						logger.warn("can not find user, openid : " + openid);
					}
					if (user!=null) {
						try {
							queue.setUser(user);
							gotongService.sendPayNotification(queue);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);	//发送失败的，需要重发
							resendList.add(openidMap);
							
						}
					}
					
				}
				if (resendList.isEmpty()) {
					isSuccess = true;
				}
				
				if (!isSuccess) {
					queue.setOpenids(resendList);
					String value = objectMapper.writeValueAsString(queue);
					redisTemplate.opsForList().rightPush(ModelConstant.KEY_NOTIFY_PAY_QUEUE, value);
				}
			
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	
		
	}


}
