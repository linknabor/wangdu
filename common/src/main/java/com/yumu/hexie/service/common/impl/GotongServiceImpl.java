/**
 * Yumu.com Inc.
 * Copyright (c) 2014-2016 All Rights Reserved.
 */
package com.yumu.hexie.service.common.impl;

import java.io.InputStream;
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
import com.yumu.hexie.integration.wechat.constant.ConstantWeChat;
import com.yumu.hexie.integration.wechat.entity.customer.Article;
import com.yumu.hexie.integration.wechat.entity.customer.DataJsonVo;
import com.yumu.hexie.integration.wechat.entity.customer.DataVo;
import com.yumu.hexie.integration.wechat.entity.customer.News;
import com.yumu.hexie.integration.wechat.entity.customer.NewsMessage;
import com.yumu.hexie.integration.wechat.entity.customer.Template;
import com.yumu.hexie.integration.wechat.service.CustomService;
import com.yumu.hexie.integration.wechat.service.FileService;
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
    
    public static String THREAD_NOTICE_URL = ConfigUtil.get("threadUrl");
    
    public static String TEMPLATE_NOTICE_URL = ConfigUtil.get("templateUrl");
    
    public static String TEMPLATE_NOTICE_ID = ConfigUtil.get("templateId");
    
    public static String THREAD_NOTICE_DESC = "业主姓名：NAME\r联系方式：TEL\r业主地址：CELL_ADDR\r消息类型：CATEGORY\r消息内容：CONTENT";
    
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
     * 发送模板消息
     */
    @Async
	@Override
	public void sendThreadPubNotify(User user, com.yumu.hexie.model.community.Thread thread) {
    	String sect_id = user.getSect_id();
    	List<RegionInfo> listregioninfo = regionInfoRepository.findAllByRegionType(sect_id);
    	for (int k = 0; k < listregioninfo.size(); k++) {
        	List<Staffing> list = staffingRepository.getStaffing(Long.toString(listregioninfo.get(k).getId()));
        	for (int i = 0; i < list.size(); i++) {
        		User useropenId = userService.getById(Long.parseLong(list.get(i).getStaffing_userid()));
        		pushweixin(useropenId.getOpenid(),TEMPLATE_NOTICE_URL+Long.toString(thread.getThreadId()),TEMPLATE_NOTICE_ID, "您好，您有新的消息", Long.toString(thread.getThreadId()), user.getName(), user.getTel(), user.getSect_name(), "请点击查看具体信息");
    		}
		}
    }
    
    public static void main(String[] args) {
    	
    	InputStream inputStream = FileService.downloadFile("WmGichEzXkPuDpyRdwJLO43ubNKX1_arFC18Hj5p3cLt23plwL_VZb76CwW4ne30", "30_J1ovVpgaF5_HA37VUnIFZiXjwjM5J0jczDK1c7ypZlAhbRr8o6HiM2Yw8svAIRLvBhIIyBysx0ZrsWYxNxLWDAR4AjoV0GixYpj_AduG7qYEznJR2WxSH3R1e_GRwRPw3tT79TMX1cv80l8jJMBgAHAUXM");		//下载图片
    	System.out.println("123");
//		Template msg = new Template();
//    	msg.setTouser("ohZbVwGsGK-Rl4SqsEgyuoIxJtP0");//get systemConfig:ACCESS_TOKEN:
//    	msg.setUrl("https://test.e-sehqu.com/wangdu/weixin/wuye/index.html?state=123#/threadDetail?threadId=850");//跳转地址
//    	msg.setTemplate_id("sCEAGYSna4EyTZqw-0hCHsW1QSk60I6MZO3YER_5fZg");//模板id
//		DataVo data = new DataVo();
//		
//		DataJsonVo first = new DataJsonVo();
//		first.setValue("您好，您有新的消息"); //标题
//		first.setColor("#173177");
//		data.setFirst(first);
//		
//		DataJsonVo keyword1 = new DataJsonVo();
//		keyword1.setValue("850");//内容1
//		keyword1.setColor("#173177"); 
//		data.setKeyword1(keyword1);
//		
//		DataJsonVo keyword2 = new DataJsonVo();
//		keyword2.setValue("内脏坏了");//内容2
//		keyword2.setColor("#173177");
//		data.setKeyword2(keyword2);
//		
//		DataJsonVo keyword3 = new DataJsonVo();
//		keyword3.setValue("18037947781");//内容3
//		keyword3.setColor("#173177");
//		data.setKeyword3(keyword3);
//		
//		DataJsonVo keyword4 = new DataJsonVo();
//		keyword4.setValue("汇诚佳苑");//内容4
//		keyword4.setColor("#173177");
//		data.setKeyword4(keyword4);
//		
//		DataJsonVo remark = new DataJsonVo();
//		remark.setValue("请点击查看具体信息");//结尾
//		remark.setColor("#173177");
//		data.setRemark(remark);
//		
//		msg.setData(data);
//		String accessToken = "30_J1ovVpgaF5_HA37VUnIFZiXjwjM5J0jczDK1c7ypZlAhbRr8o6HiM2Yw8svAIRLvBhIIyBysx0ZrsWYxNxLWDAR4AjoV0GixYpj_AduG7qYEznJR2WxSH3R1e_GRwRPw3tT79TMX1cv80l8jJMBgAHAUXM";
//		CustomService.sendCustomerMessage(msg, accessToken);
//    	System.out.println("123");
    }
    
	@Override
    public void pushweixin(String openId,String threadid,String template,String firstval,String keyword1val,String keyword2val,String keyword3val,String keyword4val,String remarkval) {
		LOG.error("openId:"+openId+"   threadid:"+threadid+"   template:"+template+"   firstval:"+firstval+"   keyword1val:"+keyword1val+"   keyword2val:"+keyword2val+"   keyword3val:"+keyword3val+"   keyword4val:"+keyword4val+"   remarkval:"+remarkval);
		
		Template msg = new Template();
    	msg.setTouser(openId);//openID  wywOpenId:og0nw09cdaJZjwQlg8ICnsSCclTE 测试推送使用     测试机备份openid o08kOwJu6eA1Vr3cbV-Miqpm_EMA  内脏坏了
    	msg.setUrl(threadid);//跳转地址
    	msg.setTemplate_id(template);//模板id
		DataVo data = new DataVo();
		
		DataJsonVo first = new DataJsonVo();
		first.setValue(firstval); //标题
		first.setColor("#173177");
		data.setFirst(first);
		
		DataJsonVo keyword1 = new DataJsonVo();
		keyword1.setValue(keyword1val);//内容1
		keyword1.setColor("#173177"); 
		data.setKeyword1(keyword1);
		
		DataJsonVo keyword2 = new DataJsonVo();
		keyword2.setValue(keyword2val);//内容2
		keyword2.setColor("#173177");
		data.setKeyword2(keyword2);
		
		DataJsonVo keyword3 = new DataJsonVo();
		keyword3.setValue(keyword3val);//内容3
		keyword3.setColor("#173177");
		data.setKeyword3(keyword3);
		
		DataJsonVo keyword4 = new DataJsonVo();
		keyword4.setValue(keyword4val);//内容4
		keyword4.setColor("#173177");
		data.setKeyword4(keyword4);
		
		DataJsonVo remark = new DataJsonVo();
		remark.setValue(remarkval);//结尾
		remark.setColor("#173177");
		data.setRemark(remark);
		
		msg.setData(data);
		String accessToken = systemConfigService.queryWXAToken();
//		String accessToken = "18_OMUjOTiLieCH7qbjBnGCvnMIA-Tm_8jeVH_1KNeYPZibssougXAsTsDEU1G_xCqrSyUZ2Lfo46YxqRCFvXo_rBq_V7-6Qp1DUstm2LKM5N4BS4QCAZ5VcBV1X5W3q48cledTIwcIaU3RSvSsWPPbABACVV";
		CustomService.sendCustomerMessage(msg, accessToken);
    }
    
}
