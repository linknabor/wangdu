package com.yumu.hexie.web.shequ;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.yumu.hexie.common.Constants;
import com.yumu.hexie.common.util.DateUtil;
import com.yumu.hexie.common.util.MD5Util;
import com.yumu.hexie.common.util.StringUtil;
import com.yumu.hexie.dto.CommonDTO;
import com.yumu.hexie.integration.wechat.service.TemplateMsgService;
import com.yumu.hexie.integration.wuye.WuyeUtil;
import com.yumu.hexie.integration.wuye.dto.DiscountViewRequestDTO;
import com.yumu.hexie.integration.wuye.dto.PrepayRequestDTO;
import com.yumu.hexie.integration.wuye.resp.BillListVO;
import com.yumu.hexie.integration.wuye.resp.CellListVO;
import com.yumu.hexie.integration.wuye.resp.CellVO;
import com.yumu.hexie.integration.wuye.resp.HouseListVO;
import com.yumu.hexie.integration.wuye.resp.PayWaterListVO;
import com.yumu.hexie.integration.wuye.vo.Discounts;
import com.yumu.hexie.integration.wuye.vo.HexieHouse;
import com.yumu.hexie.integration.wuye.vo.HexieUser;
import com.yumu.hexie.integration.wuye.vo.PayResult;
import com.yumu.hexie.integration.wuye.vo.PayWater;
import com.yumu.hexie.integration.wuye.vo.PaymentInfo;
import com.yumu.hexie.integration.wuye.vo.WechatPayInfo;
import com.yumu.hexie.model.ModelConstant;
import com.yumu.hexie.model.promotion.coupon.Coupon;
import com.yumu.hexie.model.promotion.coupon.CouponCombination;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.model.user.UserRepository;
import com.yumu.hexie.service.common.SmsService;
import com.yumu.hexie.service.common.SystemConfigService;
import com.yumu.hexie.service.shequ.WuyeService;
import com.yumu.hexie.service.user.CouponService;
import com.yumu.hexie.service.user.PointService;
import com.yumu.hexie.service.user.UserService;
import com.yumu.hexie.web.BaseController;
import com.yumu.hexie.web.BaseResult;
import com.yumu.hexie.web.shequ.vo.DiscountViewReqVO;
import com.yumu.hexie.web.shequ.vo.PrepayReqVO;

@Controller(value = "wuyeController")
public class WuyeController extends BaseController {
	
	private static final Logger log = LoggerFactory.getLogger(WuyeController.class);

	@Inject
	private WuyeService wuyeService;
    @Inject
    private PointService pointService;
    @Inject
	protected SmsService smsService;
    @Inject
    protected CouponService couponService;
    
    @Inject
    protected UserRepository userRepository;
    @Inject
    protected UserService userService;
    
    @Inject
	private SystemConfigService systemConfigService;

    @Autowired
    private RestTemplate restTemplate;
    
	/*****************[BEGIN]房产********************/
	@RequestMapping(value = "/hexiehouses", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<List<HexieHouse>> hexiehouses(@ModelAttribute(Constants.USER)User user)
			throws Exception {
		if(StringUtil.isEmpty(user.getWuyeId())){
			//FIXME 后续可调转绑定房子页面
			return BaseResult.successResult(new ArrayList<HexieHouse>());
		}
		HouseListVO listVo = wuyeService.queryHouse(user.getWuyeId());
		if (listVo != null && listVo.getHou_info() != null) {
			return BaseResult.successResult(listVo.getHou_info());
		} else {
			return BaseResult.successResult(new ArrayList<HexieHouse>());
		}
	}

	@RequestMapping(value = "/getSect", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<List<CellVO>> getSect(@ModelAttribute(Constants.USER)User user)throws Exception {
		CellListVO cellMng = wuyeService.querySectList();
		if (cellMng != null && cellMng.getSect_info() != null) {
			return BaseResult.successResult(cellMng.getSect_info());
		} else {
			return BaseResult.successResult(new ArrayList<CellVO>());
		}
	}
	
	@RequestMapping(value = "/getcellbyid", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<CellVO> getCellById(@ModelAttribute(Constants.USER)User user, @RequestParam(required=false) String sect_id, 
			@RequestParam(required=false) String build_id, @RequestParam(required=false) String unit_id, @RequestParam(required=false) String data_type)throws Exception {
		CellListVO cellMng = wuyeService.querySectList(sect_id, build_id, unit_id, data_type);
		if (cellMng != null) {
			return BaseResult.successResult(cellMng);
		} else {
			return BaseResult.successResult(new ArrayList<CellVO>());
		}
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/hexiehouse/delete/{houseId}", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<List<HexieHouse>> deleteHouse(HttpSession httpSession,@ModelAttribute(Constants.USER)User user,
			@PathVariable String houseId) throws Exception {
		
		boolean isSuccess = wuyeService.deleteHouse(user, user.getWuyeId(), houseId);
		if (isSuccess) {
			httpSession.setAttribute(Constants.USER, user);
			return BaseResult.successResult("解绑房子成功！");
		}else {
			return BaseResult.fail("解绑房子失败！");
		}
	}
	
	@RequestMapping(value = "/hexiehouse", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<HexieHouse> hexiehouses(@ModelAttribute(Constants.USER)User user,
			@RequestParam(required=false) String stmtId, @RequestParam(required=false) String house_id) throws Exception {

		if(StringUtil.isEmpty(user.getWuyeId())){
			//FIXME 后续可调转绑定房子页面
			return BaseResult.successResult(null);
		}
		return BaseResult.successResult(wuyeService.getHouse(user.getWuyeId(),stmtId, house_id));
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/addhexiehouse", method = RequestMethod.POST)
	@ResponseBody
	public BaseResult<HexieHouse> addhouses(HttpSession httpSession, @ModelAttribute(Constants.USER)User user,
			@RequestParam(required=false) String stmtId, @RequestParam(required=false) String houseId, @RequestBody HexieHouse house) throws Exception {
		
		CommonDTO<User, HexieUser> dto = wuyeService.bindHouse(user, stmtId, house);
		HexieUser u = dto.getData2();
		User currUser = dto.getData1();
		log.info("HexieUser u = " + u);
		if (u != null) {
			pointService.addZhima(user, 1000, "zhima-house-"+user.getId()+"-"+houseId);
			httpSession.setAttribute(Constants.USER, currUser);
		}
		return BaseResult.successResult(u);
		
	}
	/*****************[END]房产********************/

	
	/*****************[BEGIN]缴费记录********************/
	@RequestMapping(value = "/paymentHistory", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<List<PayWater>> paymentHistory(@ModelAttribute(Constants.USER)User user,
			@RequestParam(required=false) String startDate,@RequestParam(required=false) String endDate)
			throws Exception {
		PayWaterListVO  listVo = wuyeService.queryPaymentList(user.getWuyeId(), startDate, endDate);
		if (listVo != null && listVo.getBill_info_water() != null) {
			return BaseResult.successResult(listVo.getBill_info_water());
		} else {
			return BaseResult.successResult(null);
		}
	}
	
	/**
	 * 查询缴费详情
	 * @param session
	 * @param trade_water_id	流水号
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/queryPaymentDetail/{trade_water_id}", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<PaymentInfo> queryPaymentDetail(@ModelAttribute(Constants.USER)User user,@PathVariable String trade_water_id)
					throws Exception {
		PaymentInfo info = wuyeService.queryPaymentDetail(user.getWuyeId(),trade_water_id);
		if (info != null) {
			return BaseResult.successResult(info);
		} else {
			return BaseResult.successResult(null);
		}
	}
	//FIXME 详情查询等记录有了再加
	/*****************[END]缴费记录********************/

	/*****************[BEGIN]账单查询********************/
	@RequestMapping(value = "/billList", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<BillListVO> billList(@ModelAttribute(Constants.USER)User user,
			@RequestParam(required=false) String payStatus,
			@RequestParam(required=false) String startDate,
			@RequestParam(required=false) String endDate,
			@RequestParam(required=false) String currentPage,
			@RequestParam(required=false) String totalCount,
			@RequestParam(required=false) String house_id)
			throws Exception {
		BillListVO  listVo = wuyeService.queryBillList(user.getWuyeId(), payStatus, startDate, endDate, currentPage, totalCount, house_id);
		if (listVo != null && listVo.getBill_info() != null) {
			return BaseResult.successResult(listVo);
		} else {
			return BaseResult.successResult(null);
		}
	}
	
	/*****************[END]账单查询********************/
	

	/*****************[BEGIN]缴费********************/
	
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/checkBillRestriction", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<PaymentInfo> checkBillRestriction(@ModelAttribute(Constants.USER)User user,
			@RequestParam(required=false) String billId) throws Exception {
		
		WuyeUtil.checkBillRestriction(billId);
		return BaseResult.successResult("succeeded");
	}
	
	/***************** [BEGIN]缴费 
	 * @throws Exception ********************/
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getBillDetail", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<PaymentInfo> getBillDetail(@ModelAttribute(Constants.USER) User user,
			@RequestParam(required = false) String billId, @RequestParam(required = false) String stmtId,
			@RequestParam(required = false) String regionname) throws Exception {
		
		PaymentInfo paymentInfo = wuyeService.getBillDetail(user, stmtId, billId, regionname);
		return BaseResult.successResult(paymentInfo);
	}
	
	/**
	 * 创建交易，获取预支付ID
	 * stmtId在快捷支付的时候会用到
	 * @param user
	 * @param prepayReq
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getPrePayInfo", method = RequestMethod.POST)
	@ResponseBody
	public BaseResult<WechatPayInfo> getPrePayInfo(@ModelAttribute(Constants.USER) User user,
			@RequestBody PrepayReqVO prepayReqVo) throws Exception {
		
		WechatPayInfo result = new WechatPayInfo();
		log.info("prepayReqVo : " + prepayReqVo);
		PrepayRequestDTO dto = new PrepayRequestDTO();
		BeanUtils.copyProperties(prepayReqVo, dto);
		dto.setUser(user);
		log.info("prepayRequestDTO : " + dto);
		result = wuyeService.getPrePayInfo(dto);
		return BaseResult.successResult(result);
	}	
	
	/**
	 * 通知支付成功，并获取支付查询的返回结果
	 * @param user
	 * @param billId
	 * @param stmtId
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/noticePayed", method = RequestMethod.POST)
	@ResponseBody
	public BaseResult<String> noticePayed(@ModelAttribute(Constants.USER)User user,
			@RequestParam(required=false) String billId,@RequestParam(required=false) String stmtId, 
			@RequestParam(required=false) String tradeWaterId, @RequestParam(required=false) String packageId,
			@RequestParam(required=false) String feePrice, @RequestParam(required=false) String couponId,
			@RequestParam(required=false) String bind_switch)
			throws Exception {
		PayResult payResult = wuyeService.noticePayed(user, billId, stmtId, tradeWaterId, packageId, bind_switch);
		
		String trade_status = payResult.getMerger_status();
//		if ("01".equals(trade_status)) {	//01表示支付成功，02表示未支付成功
//			
//			
//		}else {
//			return BaseResult.fail("支付结果未知，请稍后在支付记录界面查询。");
//		}
		pointService.addZhima(user, 10, "zhima-bill-"+user.getId()+"-"+billId);
		if (couponId!=null&&couponId!="") {
			couponService.comsume(feePrice, Long.valueOf(couponId));
		}
		
	    return BaseResult.successResult("支付成功。");
	}
	
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/quickPayBillList/{stmtId}/{currPage}/{totalCount}", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<BillListVO> quickPayBillList(@ModelAttribute(Constants.USER) User user,
			@PathVariable String stmtId, @PathVariable String currPage, @PathVariable String totalCount)
			throws Exception {
		BillListVO listVo = wuyeService.quickPayInfo(user, stmtId, currPage, totalCount);
		if (listVo != null) {
			return BaseResult.successResult(listVo);
		} else {
			return BaseResult.successResult(null);
		}
	}
	/*****************[END]缴费********************/
	
	
	
	/**
	 * 获取系统参数表中配置的活动时间
	 * 如果当前时间为活动时间段内，则:
	 * 1.推送短信给用户
	 * 2.推送微信模版消息给用户
	 * 3.跳转到成功页提示用户已获取代金券
	 * @param session
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "/sendNotification", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult sendNotification(HttpSession session){
		
		User user = (User)session.getAttribute(Constants.USER);
		String retValue = "false";
		String[] dates = systemConfigService.queryActPeriod();
		if(dates.length == 2) {
			String startDate = dates[0];
			String endDate = dates[1];
			
			Date sDate = DateUtil.getDateFromString(startDate);
			Date eDate = DateUtil.getDateFromString(endDate);
			Date currDate = new Date();
			if (currDate.after(sDate)&&currDate.before(eDate)) {
				retValue = "true";
			}
			/*如果在活动日期内，则：1发送短信告知用户。2推送微信模版消息*/
			if ("true".equals(retValue)) {
				sendMsg(user);
				sendRegTemplateMsg(user);
			}
		}
			
		return BaseResult.successResult(retValue);
		
	}
	
	@Async
	private void sendMsg(User user){
		String msg = "您好，欢迎加入东湖e家园。您已获得价值10元红包一份。感谢您对东湖e家园的支持。";
		smsService.sendMsg(user.getId(), user.getTel(), msg, 11, 3);
	}
	
	@Async
	private void sendRegTemplateMsg(User user){
		TemplateMsgService.sendRegisterSuccessMsg(user, systemConfigService.queryWXAToken());
	}
	
	/**
	 * 获取支付物业费时可用的红包
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getCouponsPayWuYe", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult<List<Coupon>> getCoupons(HttpSession session){
		User user = (User)session.getAttribute(Constants.USER);
		List<Coupon>list = couponService.findAvaibleCouponForWuye(user.getId());
		
		if (list==null) {
			list = new ArrayList<Coupon>();
		}
		return BaseResult.successResult(list);
		
	}
	
	/**
	 * 为物业缴费成功的用户发放红包
	 * @param session
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
	@RequestMapping(value = "sendCoupons4WuyePay", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult sendCoupons(HttpSession session, @RequestParam(required=false) String tradeWaterId, @RequestParam(required=false) String feePrice){
		
		User user = (User)session.getAttribute(Constants.USER);
		int couponCombination = 1;
		List<CouponCombination>list = couponService.findCouponCombination(couponCombination);
		
		addCouponsFromSeed(user, list);
		
		sendPayTemplateMsg(user, tradeWaterId, feePrice);
		
		return BaseResult.successResult("send succeeded !");
	}
	
	@SuppressWarnings({ "rawtypes" })
	@RequestMapping(value = "updateCouponStatus", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult updateCouponStatus(HttpSession session){
		
		if (session == null) {
			return BaseResult.fail("no session info ...");
		}
		
		User user = (User)session.getAttribute(Constants.USER);
		List<Coupon>list = couponService.findAvaibleCouponForWuye(user.getId());
		
		if (list.size()>0) {
			String result = wuyeService.queryCouponIsUsed(user.getWuyeId());
			for (int i = 0; i < list.size(); i++) {
				Coupon coupon = list.get(i);
				if ((coupon.getStatus() == ModelConstant.COUPON_STATUS_AVAILABLE)) {
					
					if (!StringUtil.isEmpty(result)) {
						
						if ("99".equals(result)) {
							return BaseResult.fail("网络异常，请刷新后重试");
						}
						
						String[]couponArr = result.split(",");
						
						for (int j = 0; j < couponArr.length; j++) {
							String coupon_id = couponArr[j];
							try {
								couponService.comsume("20", Integer.parseInt(coupon_id));	//这里写死20
							} catch (Exception e) {
								log.error("couponId : " + coupon_id + ", " + e.getMessage());
							}
						}
						
						
					}
					
				}
				
			}
		}
		
	    return BaseResult.successResult("succeeded");
		
	}
	
	@Async
	private void addCouponsFromSeed(User user, List<CouponCombination>list){
		
		try {
			
			for (int i = 0; i < list.size(); i++) {
				couponService.addCouponFromSeed(list.get(i).getSeedStr(), user);
			}
			
		} catch (Exception e) {

			log.error("add Coupons for wuye Pay : " + e.getMessage());
		}
		
	}
	
	@Async
	private void sendPayTemplateMsg(User user, String tradeWaterId, String feePrice){
		
		TemplateMsgService.sendWuYePaySuccessMsg(user, tradeWaterId, feePrice, systemConfigService.queryWXAToken());
	}
	
	/**
	 * 获取第三方的优惠券接口
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "/getOutsidCoupon", method = RequestMethod.POST)
	@ResponseBody
	public String getOutsidCoupon(@ModelAttribute(Constants.USER)User user) {
		
		String url = "http://www.gm4life.cn/yueshop/wap/promotion/appCouponGet";
		long time = System.currentTimeMillis();
		String key = "f37d62b840357c5949e4c68efcb6a0f7";
		String md5Key = MD5Util.MD5Encode(String.valueOf(time).concat(key), "UTF-8");
		url = url.concat("?time="+time).concat("&key="+md5Key);
		log.error("getOutsidCoupon url :" + url);
		String context = restTemplate.getForObject(url, String.class);
		log.error("getOutsidCoupon response context :" + context);
		return context;
	}
	
	@SuppressWarnings({ "rawtypes" })
	@RequestMapping(value = "initSession4Test/{userId}", method = RequestMethod.GET)
	@ResponseBody
	public BaseResult initSessionForTest(HttpSession session, @PathVariable String userId){
		
		User user = (User)session.getAttribute(Constants.USER);
		
		if (session != null) {
			
			if (!StringUtil.isEmpty(userId)) {
				user = userRepository.findOne(Long.valueOf(userId));
			}
			
			if (user == null) {
				user = new User();
				user.setCity("上海市");
				user.setCityId(20);
				user.setProvince("上海");
				user.setProvinceId(19);
				if (!StringUtil.isEmpty(userId)) {
					user.setId(Long.valueOf(userId));
				}else {
					user.setId(10);
				}
				user.setOpenid("o_3DlwdnCLCz3AbTrZqj4HtKeQYY");
				user.setName("yiming");
				user.setNickname("yiming");
				user.setXiaoquName("宜川一村");
				user.setXiaoquId(169);
				user.setCountyId(27);
				user.setWuyeId("130428400000000013");
				user.setHeadimgurl("http://wx.qlogo.cn/mmopen/ajNVdqHZLLBIY2Jial97RCIIyq0P4L8dhGicoYDlbNXqW5GJytxmkRDFdFlX9GScrsvo7vBuJuaEoMZeiaBPnb6AA/0");
			}
			
			//TODO set value on redis
			session.setAttribute("sessionUser", user);
		}
		
	    return BaseResult.successResult("succeeded");
		
	}
	
	/**
	 * 修正已绑房屋用户正确的绑定房屋数和小区、房屋地址
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "/fixUserBindHouses", method = RequestMethod.POST)
	@ResponseBody
	public BaseResult fixUserBindedHouses(@RequestParam String sign) {
		
		wuyeService.fixUserBindedHouses(sign);
		return BaseResult.successResult("succeeded!");
		
	}
	
	/**
	 * 获取用户绑定的银行卡信息
	 * @param user
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getDiscounts", method = RequestMethod.POST)
	@ResponseBody
	public BaseResult<Discounts> getDiscountDetail(@ModelAttribute(Constants.USER) User user,
			@RequestBody DiscountViewReqVO discountViewReqVO) throws Exception {
		
		Discounts discountDetail = new Discounts();
		log.info("discountViewReqVO : " + discountViewReqVO);
		DiscountViewRequestDTO dto = new DiscountViewRequestDTO();
		BeanUtils.copyProperties(discountViewReqVO, dto);
		dto.setUser(user);
		log.info("discountViewRequestDTO : " + dto);
		discountDetail = wuyeService.getDiscounts(dto);

		return BaseResult.successResult(discountDetail);
	}
	
	/**
	 * 创建交易，获取预支付ID
	 * stmtId在快捷支付的时候会用到
	 * @param user
	 * @param prepayReq
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getPrePayInfo4Qrcode", method = RequestMethod.POST)
	@ResponseBody
	public BaseResult<WechatPayInfo> getPrePayInfo4Qrcode(@RequestBody PrepayReqVO prepayReqVo) throws Exception {
		
		WechatPayInfo result = new WechatPayInfo();
		log.info("getPrePayInfo4Qrcode prepayReqVo : " + prepayReqVo);
		PrepayRequestDTO dto = new PrepayRequestDTO();
		BeanUtils.copyProperties(prepayReqVo, dto);
		
		User user = new User();
		dto.setUser(user);
		user.setAppId(prepayReqVo.getAppid());
		user.setOpenid(prepayReqVo.getOpenid());
		
		log.info("getPrePayInfo4Qrcode prepayRequestDTO : " + dto);
		result = wuyeService.getPrePayInfo(dto);
		return BaseResult.successResult(result);
	}
	
}
