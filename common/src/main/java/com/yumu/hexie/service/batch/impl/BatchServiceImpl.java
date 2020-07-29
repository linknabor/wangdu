package com.yumu.hexie.service.batch.impl;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yumu.hexie.integration.wechat.constant.ConstantWeChat;
import com.yumu.hexie.service.batch.BatchService;
import com.yumu.hexie.service.notify.NotifyQueueTask;

@Service
public class BatchServiceImpl implements BatchService {

	private static Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);

	@Autowired
	private NotifyQueueTask notifyQueueTask;
	
	@PostConstruct
	public void runBatch() {
		
		if (ConstantWeChat.isMainServer()) {	//BK程序不跑下面的队列轮询
			return;
		}
		notifyQueueTask.sendWuyeNotificationAysc();
		logger.info("异步队列任务启动完成。");
		
	}



}
