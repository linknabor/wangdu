package com.yumu.hexie.service.shequ.impl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.xml.bind.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.yumu.hexie.common.util.TransactionUtil;
import com.yumu.hexie.integration.wuye.WuyeUtil;
import com.yumu.hexie.integration.wuye.resp.BaseResult;
import com.yumu.hexie.integration.wuye.resp.BillListVO;
import com.yumu.hexie.integration.wuye.resp.CellListVO;
import com.yumu.hexie.integration.wuye.resp.HouseListVO;
import com.yumu.hexie.integration.wuye.resp.PayWaterListVO;
import com.yumu.hexie.integration.wuye.vo.BindHouseDTO;
import com.yumu.hexie.integration.wuye.vo.HexieHouse;
import com.yumu.hexie.integration.wuye.vo.HexieUser;
import com.yumu.hexie.integration.wuye.vo.PayResult;
import com.yumu.hexie.integration.wuye.vo.PaymentInfo;
import com.yumu.hexie.integration.wuye.vo.WechatPayInfo;
import com.yumu.hexie.model.community.RegionInfo;
import com.yumu.hexie.model.community.RegionInfoRepository;
import com.yumu.hexie.model.market.ServiceOrder;
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
	
	@Override
	public HouseListVO queryHouse(String userId) {
		return WuyeUtil.queryHouse(userId).getData();
	}

	@Override
	@Transactional
	public BindHouseDTO bindHouse(User user, String stmtId, HexieHouse house) {
		
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
		
		BindHouseDTO bindHouseDTO = new BindHouseDTO();
		bindHouseDTO.setUser(currUser);
		bindHouseDTO.setHexieUser(r.getData());
		return bindHouseDTO;
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
			List<RegionInfo>	list=regionInfoRepository.findAllByRegionType(r.getData().getSect_id());
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
	public PaymentInfo getBillDetail(String userId, String stmtId,
			String anotherbillIds) {
		try {
			return WuyeUtil.getBillDetail(userId, stmtId, anotherbillIds).getData();
		} catch (ValidationException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public WechatPayInfo getPrePayInfo(String userId, String billId,
			String stmtId, String openId, String couponUnit, String couponNum, 
			String couponId,String mianBill,String mianAmt, String reduceAmt, 
			String invoice_title_type, String credit_code, String mobile, String invoice_title) throws Exception {
		return WuyeUtil.getPrePayInfo(userId, billId, stmtId, openId, couponUnit, couponNum, couponId,mianBill,mianAmt, reduceAmt, 
				invoice_title_type, credit_code, mobile, invoice_title)
				.getData();
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
	public BillListVO quickPayInfo(String stmtId, String currPage, String totalCount) {
		return WuyeUtil.quickPayInfo(stmtId, currPage, totalCount).getData();
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
	
	public static void main(String[] args) {
		
		
		Function<Integer, Integer> name = e -> e * 2;
        Function<Integer, Integer> square = e -> e * e;
        
        int value = name.andThen(square).apply(3);
        System.out.println("andThen value=" + value);
        int value2 = name.compose(square).apply(3);
        System.out.println("compose value2=" + value2);
        //返回一个执行了apply()方法之后只会返回输入参数的函数对象
        Object identity = Function.identity().apply("huohuo");
        System.out.println(identity);

        ServiceOrder serviceOrder = new ServiceOrder();
        Consumer<ServiceOrder> orderConsumer = serviceOrderC -> serviceOrderC.setPrice(100f);
        orderConsumer.accept(serviceOrder);
        System.out.println("price:"+serviceOrder.getPrice());
        
        
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
	
//	@Override
//	@Transactional
//	public User setDefaultAddress(User user, HexieUser u) {
//
//		User currUser = userRepository.findOne(user.getId());
//		
//		Long totalBind = currUser.getTotal_bind();
//		if (totalBind == null) {
//			totalBind = 0l;
//		}
//		if (!StringUtils.isEmpty(u.getTotal_bind())) {
//			if (u.getTotal_bind() > 0) {
//				totalBind = u.getTotal_bind();	//如果值不为空，说明是跑批程序返回回来的，直接取值即可，如果值是空，走下面的else累加即可
//			}
//		}
//		if (totalBind == 0) {
//			totalBind = totalBind + 1;
//		}
//		currUser.setTotal_bind(totalBind);
//		currUser.setXiaoquName(u.getSect_name());
//		currUser.setProvince(u.getProvince_name());
//		currUser.setCity(u.getCity_name());
//		currUser.setCounty(u.getRegion_name());
//		currUser.setSectId(u.getSect_id());	
//		currUser.setCspId(u.getCsp_id());
//		currUser.setOfficeTel(u.getOffice_tel());
//		userRepository.updateUserByHouse(currUser.getXiaoquId(), currUser.getXiaoquName(), 
//				currUser.getTotalBind(), currUser.getProvince(), currUser.getCity(), currUser.getCountry(), 
//				currUser.getSectId(), currUser.getCspId(), currUser.getOfficeTel(), currUser.getId());
//		
//		return currUser;
//		
//	}

}
