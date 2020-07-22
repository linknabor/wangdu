package com.yumu.hexie.integration.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yumu.hexie.config.WechatPropConfig;
import com.yumu.hexie.model.user.User;

@Component
public class RequestUtil {
	
	@Autowired
	private WechatPropConfig wechatPropConfig;
	
	/**
	 * 获取需要请求的服务器地址
	 * 给wuyeUtil2用的，以后都调用这个
	 * @param user
	 * @param regionName
	 * @return
	 */
	public String getRequestUrl(User user, String regionName) {
	
		return wechatPropConfig.getRequestUrl();
	}

	/**
	 * 获取from_sys
	 * @return
	 */
	public String getSysName() {
		
		return wechatPropConfig.getSysName();
	}
}
