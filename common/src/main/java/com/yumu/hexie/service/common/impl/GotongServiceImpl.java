/**
 * Yumu.com Inc.
 * Copyright (c) 2014-2016 All Rights Reserved.
 */
package com.yumu.hexie.service.common.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.yumu.hexie.common.util.ConfigUtil;
import com.yumu.hexie.integration.notify.PayNotification.AccountNotification;
import com.yumu.hexie.integration.wechat.constant.ConstantWeChat;
import com.yumu.hexie.integration.wechat.entity.customer.Article;
import com.yumu.hexie.integration.wechat.entity.customer.News;
import com.yumu.hexie.integration.wechat.entity.customer.NewsMessage;
import com.yumu.hexie.integration.wechat.service.CustomService;
import com.yumu.hexie.integration.wechat.service.TemplateMsgService;
import com.yumu.hexie.model.community.RegionInfo;
import com.yumu.hexie.model.community.RegionInfoRepository;
import com.yumu.hexie.model.community.Staffing;
import com.yumu.hexie.model.community.StaffingRepository;
import com.yumu.hexie.model.localservice.ServiceOperator;
import com.yumu.hexie.model.localservice.ServiceOperatorRepository;
import com.yumu.hexie.model.localservice.bill.YunXiyiBill;
import com.yumu.hexie.model.localservice.repair.RepairOrder;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.service.common.GotongService;
import com.yumu.hexie.service.common.SystemConfigService;
import com.yumu.hexie.service.o2o.OperatorService;
import com.yumu.hexie.service.user.UserService;

/**
 * <pre>
 * 
 * </pre>
 *
 * @author tongqian.ni
 * @version $Id: GotongServiceImple.java, v 0.1 2016年1月8日 上午10:01:41  Exp $
 */
@Service("gotongService")
public class GotongServiceImpl implements GotongService {

    private static final Logger LOG = LoggerFactory.getLogger(GotongServiceImpl.class);
    
    public static String WEIXIU_NOTICE = ConfigUtil.get("weixiuNotice");

    public static String XIYI_NOTICE = ConfigUtil.get("weixiuNotice");
    
    public static String WEIXIU_DETAIL = ConfigUtil.get("weixiuDetail");
    
    public static String SUBSCRIBE_IMG = ConfigUtil.get("subscribeImage");
    
    public static String SUBSCRIBE_DETAIL = ConfigUtil.get("subscribeDetail");
    
    public static String THREAD_DETAIL = ConfigUtil.get("threadDetail");	//--> ori templateUrl
    
    public static String PAY_NOTIFY_URL = ConfigUtil.get("payNotifyUrl");
    
    public static Map<String, String>categoryMap;
    
    @PostConstruct   
    public void init(){
    	
    	categoryMap = new HashMap<String, String>();
    	categoryMap.put("0", "服务需求");
    	categoryMap.put("1", "意见建议");
    	categoryMap.put("2", "报修");
    
    }
    
    @Inject
    private ServiceOperatorRepository  serviceOperatorRepository;
    @Inject
    private UserService  userService;
    @Inject
    private OperatorService  operatorService;
    @Inject
    private SystemConfigService systemConfigService;
    @Inject
    private  StaffingRepository staffingRepository;
    @Inject
    private  RegionInfoRepository regionInfoRepository;
    
    @Async
    @Override
    public void sendRepairAssignMsg(long opId,RepairOrder order,int distance){
        ServiceOperator op = serviceOperatorRepository.findOne(opId);
        String accessToken = systemConfigService.queryWXAToken();
        TemplateMsgService.sendRepairAssignMsg(order, op, accessToken);
    }
    @Async
    @Override
    public void sendRepairAssignedMsg(RepairOrder order){
        User user = userService.getById(order.getUserId());
        News news = new News(new ArrayList<Article>());
        Article article = new Article();
        article.setTitle("您的维修单已被受理");
        article.setDescription("点击查看详情");
        article.setUrl(WEIXIU_DETAIL+order.getId());
        news.getArticles().add(article);
        NewsMessage msg = new NewsMessage(news);
        msg.setTouser(user.getOpenid());
        msg.setMsgtype(ConstantWeChat.RESP_MESSAGE_TYPE_NEWS);
        String accessToken = systemConfigService.queryWXAToken();
        CustomService.sendCustomerMessage(msg, accessToken);
    }
    
    @Async
    @Override
	public void sendSubscribeMsg(User user) {

         Article article = new Article();
         
         article.setTitle("欢迎加入东湖e家园！");
         article.setDescription("您已获得关注红包，点击查看。");
         article.setPicurl(SUBSCRIBE_IMG);
         article.setUrl(SUBSCRIBE_DETAIL);
         News news = new News(new ArrayList<Article>());
         news.getArticles().add(article);
         NewsMessage msg = new NewsMessage(news);
         msg.setTouser(user.getOpenid());
         msg.setMsgtype(ConstantWeChat.RESP_MESSAGE_TYPE_NEWS);
         String accessToken = systemConfigService.queryWXAToken();
         CustomService.sendCustomerMessage(msg, accessToken);
	}

    /** 
     * @param opId
     * @param bill
     * @see com.yumu.hexie.service.common.GotongService#sendXiyiAssignMsg(long, com.yumu.hexie.model.localservice.bill.YunXiyiBill)
     */
    @Override
    public void sendXiyiAssignMsg(long opId, YunXiyiBill bill) {
        ServiceOperator op = serviceOperatorRepository.findOne(opId);
        News news = new News(new ArrayList<Article>());
        Article article = new Article();
        article.setTitle(op.getName()+":您有新的洗衣订单！");
        article.setDescription("有新的维修单"+bill.getProjectName()+"快来抢单吧");
        //article.setPicurl(so.getProductPic());
        article.setUrl(XIYI_NOTICE+bill.getId());
        news.getArticles().add(article);
        NewsMessage msg = new NewsMessage(news);
        msg.setTouser(op.getOpenId());
        msg.setMsgtype(ConstantWeChat.RESP_MESSAGE_TYPE_NEWS);
        String accessToken = systemConfigService.queryWXAToken();
        CustomService.sendCustomerMessage(msg, accessToken);
    }
    /** 
     * @param count
     * @param billName
     * @param requireTime
     * @param url
     * @see com.yumu.hexie.service.common.GotongService#sendYuyueBillMsg(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Async
    @Override
    public void sendCommonYuyueBillMsg(int serviceType,String title, String billName, String requireTime, String url) {
        LOG.error("发送预约通知！["+serviceType+"]" + billName + " -- " + requireTime);
        List<ServiceOperator> ops = operatorService.findByType(serviceType);
        String accessToken = systemConfigService.queryWXAToken();
        for(ServiceOperator op: ops) {
            LOG.error("发送到操作员！["+serviceType+"]" + billName + " -- " + op.getName() + "--" + op.getId());
            TemplateMsgService.sendYuyueBillMsg(op.getOpenId(), title, billName, requireTime, url, accessToken);    
        }
        
    }
    
    /**
     *发送模板消息通知客服人员
     */
    @Async
	@Override
	public void sendThreadPubNotify(User user, com.yumu.hexie.model.community.Thread thread) {
    	String sect_id = user.getSect_id();
    	List<RegionInfo> listregioninfo = regionInfoRepository.findBySectId(sect_id);
    	String accessToken = systemConfigService.queryWXAToken();
    	for (int k = 0; k < listregioninfo.size(); k++) {
        	List<Staffing> list = staffingRepository.getStaffing(Long.toString(listregioninfo.get(k).getId()));
        	if (list!=null && !list.isEmpty()) {
				for (Staffing staff : list) {
					try {
						User staffUser = userService.getById(Long.valueOf(staff.getStaffing_userid()));
						TemplateMsgService.sendThreadPubMsg(thread, staffUser, accessToken);
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
	        		
				}
			}
		}
    }
    
    /**
     *维修、业主意见添加评论后通知业主
     */
    @Async
	@Override
	public void sendThreadReplyMsg(com.yumu.hexie.model.community.Thread thread) {
    	
    	String accessToken = systemConfigService.queryWXAToken();
		TemplateMsgService.sendThreadReplyMsg(thread, accessToken);
	
    }
    
    /**
	 * 交易到账通知
	 */
	@Override
	public void sendPayNotification(AccountNotification accountNotify) {
		
		String accessToken = systemConfigService.queryWXAToken();
		TemplateMsgService.sendPayNotification(accountNotify, accessToken);
		
	}
    
}
