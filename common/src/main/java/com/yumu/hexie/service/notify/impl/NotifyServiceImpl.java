package com.yumu.hexie.service.notify.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yumu.hexie.common.util.JacksonJsonUtil;
import com.yumu.hexie.common.util.RedisLock;
import com.yumu.hexie.integration.notify.PayNotification;
import com.yumu.hexie.integration.notify.PayNotification.AccountNotification;
import com.yumu.hexie.integration.notify.PayNotification.ServiceNotification;
import com.yumu.hexie.model.ModelConstant;
import com.yumu.hexie.model.promotion.coupon.Coupon;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.model.user.UserRepository;
import com.yumu.hexie.service.notify.NotifyService;
import com.yumu.hexie.service.user.CouponService;
import com.yumu.hexie.service.user.PointService;

@Service
public class NotifyServiceImpl implements NotifyService {
	
	private static final Logger log = LoggerFactory.getLogger(NotifyServiceImpl.class);
	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CouponService couponService;
	@Autowired
	private PointService pointService;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	

	/**
	 * 	1.优惠券核销
		2.积分
		3.绑卡记录quickToken和卡号
		4.绑定房屋
		5.缴费到账通知
		6.自定服务
	 */
	@Transactional
	@Override
	public void notify(PayNotification payNotification) {
		
		String tradeWaterId = payNotification.getOrderId();
		String key = ModelConstant.KEY_NOITFY_PAY_DUPLICATION_CHECK + tradeWaterId;
		Long result = RedisLock.lock(key, redisTemplate, 3600l);
		if (0 == result) {
			log.info("tradeWaterId : " + tradeWaterId + ", already notified, will skip ! ");
			return;
		}
		//1.更新红包状态
		User user = null;
		Coupon coupon = null;
		if (!StringUtils.isEmpty(payNotification.getCouponId())) {
			coupon = couponService.findOne(Long.valueOf(payNotification.getCouponId()));
			if (coupon != null) {
				try {
					couponService.comsume("999999", coupon.getId());
				} catch (Exception e) {
					//如果优惠券已经消过一次，里面会抛异常提示券已使用，但是步骤2和3还是需要进行的
					log.error(e.getMessage(), e);
				}
			}
		}
		if (user == null) {
			if (coupon != null) {
				user = userRepository.findOne(coupon.getUserId());
			}
		}
		if (user == null) {
			List<User> userList = userRepository.findByWuyeId(payNotification.getWuyeId());
			if (userList == null || userList.isEmpty()) {
				log.info("can not find user, wuyeId : " + payNotification.getWuyeId() + ", tradeWaterId : " + payNotification.getOrderId());
			}else {
				user = userList.get(0);
			}
			
		}
		//2.添加芝麻积分
		if (user != null) {
			String pointKey = "zhima-bill-" + user.getId() + "-" + payNotification.getOrderId();
			pointService.addZhima(user, 10, pointKey);
		}
		
		//3.通知物业相关人员，收费到账
		AccountNotification accountNotify = payNotification.getAccountNotify();
		if (accountNotify!=null) {
			accountNotify.setOrderId(payNotification.getOrderId());
			if (accountNotify.getFeePrice() == null) {
				log.warn("tranAmt is null, accountNotify : " + accountNotify);
				return;
			}
			sendPayNotificationAsync(accountNotify);
		}
		
	}
	
	/**
	 * 到账消息推送
	 */
	@Override
	public void sendPayNotificationAsync(AccountNotification accountNotification) {
		
		if (accountNotification == null) {
			log.info("accountNotification is null, will return ! ");
			return;
		}
		
		int retryTimes = 0;
		boolean isSuccess = false;
		
		while(!isSuccess && retryTimes < 3) {
			try {
				ObjectMapper objectMapper = JacksonJsonUtil.getMapperInstance(false);
				String value = objectMapper.writeValueAsString(accountNotification);
				redisTemplate.opsForList().rightPush(ModelConstant.KEY_NOTIFY_PAY_QUEUE, value);
				isSuccess = true;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				retryTimes++;
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					log.error(e.getMessage(), e);
				}
			}
		}
		
	}
	
	/**
	 * 服务消息推送
	 */
	@Override
	public void sendServiceNotificationAsync(ServiceNotification serviceNotification) {
		
		if (serviceNotification == null) {
			return;
		}
		
		String key = ModelConstant.KEY_ASSIGN_CS_ORDER_DUPLICATION_CHECK + serviceNotification.getOrderId();
		Long result = RedisLock.lock(key, redisTemplate, 3600l);
		log.info("result : " + result);
		if (0 == result) {
			log.info("trade : " + serviceNotification.getOrderId() + ", already in the send queue, will skip .");
			return;
		}
		
		int retryTimes = 0;
		boolean isSuccess = false;
		
		while(!isSuccess && retryTimes < 3) {
			try {
				ObjectMapper objectMapper = JacksonJsonUtil.getMapperInstance(false);
				String value = objectMapper.writeValueAsString(serviceNotification);
				redisTemplate.opsForList().rightPush(ModelConstant.KEY_NOTIFY_SERVICE_QUEUE, value);
				isSuccess = true;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				retryTimes++;
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					log.error(e.getMessage(), e);
				}
			}
		}
		
	}
	
	
}
