package com.yumu.hexie.service.shequ.impl;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.yumu.hexie.common.util.TransactionUtil;
import com.yumu.hexie.dto.CommonDTO;
import com.yumu.hexie.integration.wuye.WuyeUtil;
import com.yumu.hexie.integration.wuye.WuyeUtil2;
import com.yumu.hexie.integration.wuye.dto.DiscountViewRequestDTO;
import com.yumu.hexie.integration.wuye.dto.PrepayRequestDTO;
import com.yumu.hexie.integration.wuye.resp.BaseResult;
import com.yumu.hexie.integration.wuye.resp.BillListVO;
import com.yumu.hexie.integration.wuye.resp.CellListVO;
import com.yumu.hexie.integration.wuye.resp.HouseListVO;
import com.yumu.hexie.integration.wuye.resp.PayWaterListVO;
import com.yumu.hexie.integration.wuye.vo.Discounts;
import com.yumu.hexie.integration.wuye.vo.HexieHouse;
import com.yumu.hexie.integration.wuye.vo.HexieUser;
import com.yumu.hexie.integration.wuye.vo.PayResult;
import com.yumu.hexie.integration.wuye.vo.PaymentInfo;
import com.yumu.hexie.integration.wuye.vo.WechatPayInfo;
import com.yumu.hexie.model.community.RegionInfo;
import com.yumu.hexie.model.community.RegionInfoRepository;
import com.yumu.hexie.model.user.BankCard;
import com.yumu.hexie.model.user.BankCardRepository;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.model.user.UserRepository;
import com.yumu.hexie.service.exception.BizValidateException;
import com.yumu.hexie.service.shequ.WuyeService;

@Service("wuyeService")
public class WuyeServiceImpl<T> implements WuyeService {
	
	private static final Logger logger = LoggerFactory.getLogger(WuyeServiceImpl.class);

	@Inject
	private UserRepository userRepository;
	@Inject
	private RegionInfoRepository regionInfoRepository;
	@Inject
	private TransactionUtil<T> transactionUtil;
	@Autowired
	private WuyeUtil2 wuyeUtil2;
	@Autowired
	private BankCardRepository bankCardRepository;
	
	@Override
	public HouseListVO queryHouse(String userId) {
		return WuyeUtil.queryHouse(userId).getData();
	}

	@Override
	@Transactional
	public CommonDTO<User, HexieUser> bindHouse(User user, String stmtId, HexieHouse house) {
		
		logger.info("userId : " + user.getId());
		logger.info("hosue is :" + house.toString());
		User currUser = userRepository.findOne(user.getId());
		BaseResult<HexieUser> r = WuyeUtil.bindHouse(currUser.getWuyeId(), stmtId, house.getMng_cell_id());
		
		if("04".equals(r.getResult())){
			throw new BizValidateException("当前用户已经认领该房屋!");
		}
		if ("05".equals(r.getResult())) {
			throw new BizValidateException("用户当前绑定房屋与已绑定房屋不属于同个小区，暂不支持此功能。");
		}
		if ("01".equals(r.getResult())) {
			throw new BizValidateException("账户不存在");
		}
		
		if (r.isSuccess()) {
			//添加电话到user表
			currUser.setOfficeTel(r.getData().getOffice_tel());	//保存到数据库
			currUser.setTotal_bind(1);
			currUser.setSect_id(house.getSect_id());
			currUser.setSect_name(house.getSect_name());
			currUser.setCell_id(house.getMng_cell_id());
			currUser.setCell_addr(house.getCell_addr());
			userRepository.updateUserByHouse(currUser.getTotal_bind(), currUser.getSect_id(), currUser.getSect_name(),
					currUser.getCell_id(), currUser.getCell_addr(), currUser.getOfficeTel(), currUser.getId());
		}
		
		CommonDTO<User, HexieUser> dto = new CommonDTO<>();
		dto.setData1(currUser);
		dto.setData2(r.getData());
		return dto;
	}


	@Override
	@Transactional
	public boolean deleteHouse(User user, String userId, String houseId) {
		
		User currUser = userRepository.findOne(user.getId());
		BaseResult<String> result = WuyeUtil.deleteHouse(currUser.getWuyeId(), houseId);
		if (result.isSuccess()) {
			String data = result.getData();
			int totalBind = 0;
			if (!StringUtils.isEmpty(data)) {
				totalBind = Integer.valueOf(data);
			}
			if (totalBind < 0) {
				totalBind = 0;
			}
			if (totalBind == 0) {
				userRepository.updateUserByHouse(0l, "", "", "", "", "", currUser.getId());
			}else {
				userRepository.updateUserTotalBind(totalBind, currUser.getId());
			}
			
		} else {
			throw new BizValidateException("解绑房屋失败。");
		}
		
		return result.isSuccess();
	}

	@Override
	public HexieHouse getHouse(String userId, String stmtId, String house_id) {
		BaseResult<HexieHouse>	r=WuyeUtil.getHouse(userId, stmtId, house_id);
		if ("03".equals(r.getResult())){
			throw new BizValidateException("无效的账单编号，无法查询到房屋!");
		}
		if ("02".equals(r.getResult())) {
			throw new BizValidateException("账单编号不存在!");
		}
		if (r.getData() != null ) {
			List<RegionInfo>	list=regionInfoRepository.findBySectId(r.getData().getSect_id());
			if(list.size()==0){
				throw new BizValidateException("无法绑定当前小区！");
			}
		}
		return r.getData();
	}

	@Override
	public HexieUser userLogin(String openId) {
		return WuyeUtil.userLogin(openId).getData();
	}

	@Override
	public PayWaterListVO queryPaymentList(String userId, String startDate,
			String endDate) {
		return WuyeUtil.queryPaymentList(userId, startDate, endDate).getData();
	}

	@Override
	public PaymentInfo queryPaymentDetail(String userId, String waterId) {
		return WuyeUtil.queryPaymentDetail(userId, waterId).getData();
	}

	@Override
	public BillListVO queryBillList(String userId, String payStatus,
			String startDate, String endDate,String currentPage, String totalCount, String house_id) {
		return WuyeUtil.queryBillList(userId, payStatus, startDate, endDate, currentPage, totalCount, house_id).getData();
	}

	@Override
	public PaymentInfo getBillDetail(User user, String stmtId, String anotherbillIds, String regionName) throws Exception {
		
		String targetUrl = getRegionUrl(regionName);
		return wuyeUtil2.getBillDetail(user, stmtId, anotherbillIds, targetUrl).getData();
	}

	@Override
	@Transactional
	public WechatPayInfo getPrePayInfo(PrepayRequestDTO prepayRequestDTO) throws Exception {
		
		User user = prepayRequestDTO.getUser();
		if (user.getId() == 0) {
			logger.info("qrcode pay, no user id .");
		}else {
			User currUser = userRepository.findOne(user.getId());
			prepayRequestDTO.setUser(currUser);
		}
		
		if ("1".equals(prepayRequestDTO.getPayType())) {	//银行卡支付
			String remerber = prepayRequestDTO.getRemember();
			if ("1".equals(remerber)) {	//新卡， 需要记住卡号的情况
				
				Assert.hasText(prepayRequestDTO.getCustomerName(), "持卡人姓名不能为空。");
				Assert.hasText(prepayRequestDTO.getAcctNo(), "卡号不能为空。");
				Assert.hasText(prepayRequestDTO.getCertId(), "证件号不能为空。");
				Assert.hasText(prepayRequestDTO.getPhoneNo(), "银行预留手机号不能为空。");
				
				BankCard bankCard = bankCardRepository.findByAcctNo(prepayRequestDTO.getAcctNo());
				if (bankCard == null) {
					bankCard = new BankCard();
				}
				bankCard.setAcctName(prepayRequestDTO.getCustomerName());
				bankCard.setAcctNo(prepayRequestDTO.getAcctNo());
				bankCard.setBankCode("");	//TODO 
				bankCard.setBankName("");	//TODO
				bankCard.setBranchName("");	//TODO
				bankCard.setBranchNo("");	//TODO
				bankCard.setPhoneNo(prepayRequestDTO.getPhoneNo());
				bankCard.setUserId(prepayRequestDTO.getUser().getId());
				bankCard.setUserName(prepayRequestDTO.getUser().getName());
				//支付成功回调的时候还要保存quickToken
				bankCardRepository.save(bankCard);
			} 
			if (!StringUtils.isEmpty(prepayRequestDTO.getCardId())) {	//选卡支付
				BankCard selBankCard = bankCardRepository.findOne(Long.valueOf(prepayRequestDTO.getCardId()));
				if (StringUtils.isEmpty(selBankCard.getQuickToken())) {
					throw new BizValidateException("未绑定的银行卡。");
				}
				prepayRequestDTO.setQuickToken(selBankCard.getQuickToken());
				prepayRequestDTO.setPhoneNo(selBankCard.getPhoneNo());
			}
		}
		WechatPayInfo wechatPayInfo = wuyeUtil2.getPrePayInfo(prepayRequestDTO).getData();
		return wechatPayInfo;
	}

	public PaymentInfo queryPayMent(String userId, String waterId) {
		return WuyeUtil.queryPaymentDetail(userId, waterId).getData();
	}
	
	@Override
	public PayResult noticePayed(User user, String billId, String stmtId, String tradeWaterId, String packageId, String bind_switch) {
		PayResult pay = WuyeUtil.noticePayed(user.getWuyeId(), billId, stmtId, tradeWaterId, packageId).getData();
		//如果switch为1，则顺便绑定该房屋
		if("1".equals(bind_switch))
		{
			BaseResult<String> result = WuyeUtil.getPayWaterToCell(user.getWuyeId(), tradeWaterId);
			String ids = result.getResult();
			String[] idsSuff = ids.split(",");
			//因为考虑一次支持存在多套房子的情况
			for (int i = 0; i < idsSuff.length; i++) {
				try {
					HexieHouse house = getHouse(user.getWuyeId(), stmtId, idsSuff[i]);
					if(house!=null)
					{
						bindHouse(user, stmtId, house);
					}
				} catch(Exception e)
				{
					//不影响支付完整性，如果有问题则不向外面抛
					logger.error("bind house error:"+e);
				}
			}
		}
		return pay;
	}

	@Override
	public BillListVO quickPayInfo(User user, String stmtId, String currPage, String totalCount) throws Exception {
		return wuyeUtil2.quickPayInfo(user, stmtId, currPage, totalCount).getData();
	}

	@Override
	public String queryCouponIsUsed(String userId) {

		BaseResult<String> r = WuyeUtil.couponUseQuery(userId);
		return r.getResult();
	}

	@Override
	public CellListVO querySectList() {
		return WuyeUtil.getSectList().getData();
	}

	@Override
	public CellListVO querySectList(String sect_id, String build_id,
			String unit_id, String data_type) {
		return WuyeUtil.getMngList(sect_id, build_id, unit_id, data_type).getData();
	}

	@Override
	public void fixUserBindedHouses(String guangmingsign) {

		if ("guangmingsign".equals(guangmingsign)) {
			
			long count = userRepository.count();
			Long loops = count/1000;
			if (count%1000!=0) {
				loops++;
			}
			//每页1000条循环user表
			for (int i = 0; i < loops.intValue(); i++) {
				
				Pageable pageable = new PageRequest(i, 1000);
				Page<User> userList = userRepository.findAll(pageable);
				for (User user : userList) {
					BaseResult<HouseListVO> hio = WuyeUtil.queryHouse(String.valueOf(user.getWuyeId()));
					transactionUtil.transact(s->fixBindedHouse(user, hio.getData()));
				}
			}
			
		}
		
	}
	
	/**
	 * 获取缴费优惠信息
	 * @param discountViewRequestDTO
	 * @return
	 * @throws Exception
	 */
	@Override
	public Discounts getDiscounts(DiscountViewRequestDTO discountViewRequestDTO) throws Exception {
		
		Discounts discountDetail = wuyeUtil2.getDiscounts(discountViewRequestDTO).getData();
		return discountDetail;
	
	}
	
	/**
	 * 是否为物业二维码收费人员
	 * @param user
	 */
	@Override
	public Boolean isQrcodeOperator(User user) {
		
		Boolean returnData = Boolean.FALSE;
		if (StringUtils.isEmpty(user.getWuyeId())) {
			logger.warn("no wuye id, will return . userId : " + user.getId());
			return returnData;
		}
		try {
			String isOper = wuyeUtil2.isServiceOperator(user).getData();
			returnData = Boolean.valueOf(isOper);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return returnData;
	}
	
	/**
	 * 修正已绑房屋用户正确的绑定房屋数和小区、房屋地址
	 * @param user
	 * @param vo
	 */
	public void fixBindedHouse(User user, HouseListVO vo) {
		
		List<HexieHouse> houList = vo.getHou_info();
		if (houList!=null && houList.size()>0) {
			user.setTotal_bind(houList.size());
			user.setSect_id(houList.get(0).getSect_id());
			user.setSect_name(houList.get(0).getSect_name());
			user.setCell_id(houList.get(0).getMng_cell_id());
			user.setCell_addr(houList.get(0).getCell_addr());
		}else {
			user.setTotal_bind(0);
			user.setSect_id("");
			user.setSect_name("");
			user.setCell_id("");
			user.setCell_addr("");
		}
		userRepository.save(user);
		
		
	}
	
	/**
	 * 获取需要发送的链接地址
	 * @param regionName
	 * @return
	 */
	private String getRegionUrl(String regionName) {
		
		return "";
		
	}

}
