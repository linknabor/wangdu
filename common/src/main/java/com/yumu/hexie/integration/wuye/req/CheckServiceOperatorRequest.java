package com.yumu.hexie.integration.wuye.req;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckServiceOperatorRequest extends WuyeRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4952595585227757485L;
	
	@JsonProperty("user_id")
	private String userId;
	private String openid;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getOpenid() {
		return openid;
	}
	public void setOpenid(String openid) {
		this.openid = openid;
	}
	@Override
	public String toString() {
		return "CheckServiceOperatorRequest [userId=" + userId + ", openid=" + openid + "]";
	}
	
	
}
