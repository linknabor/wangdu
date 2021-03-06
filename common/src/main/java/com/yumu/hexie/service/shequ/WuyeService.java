package com.yumu.hexie.service.shequ;

import com.yumu.hexie.dto.CommonDTO;
import com.yumu.hexie.integration.wuye.dto.DiscountViewRequestDTO;
import com.yumu.hexie.integration.wuye.dto.PrepayRequestDTO;
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
import com.yumu.hexie.model.user.User;

public interface WuyeService {

	//0. 快捷缴费信息
	public BillListVO quickPayInfo(User user, String stmtId, String currPage, String totalCount) throws Exception;
	// 1.房产列表
	public HouseListVO queryHouse(String userId);
	// 2.绑定房产
	public CommonDTO<User, HexieUser> bindHouse(User user,String stmtId,HexieHouse house);
	// 3.删除房产
	public boolean deleteHouse(User user, String userId,String houseId);
	// 4.根据订单查询房产信息
	public HexieHouse getHouse(String userId,String stmtId, String house_id);
	// 5.用户登录
	public HexieUser userLogin(String openId);
	// 6.缴费记录查询
	public PayWaterListVO queryPaymentList(String userId,String startDate,String endDate);
	// 7.缴费详情
	public PaymentInfo queryPaymentDetail(String userId,String waterId);
	//status 00,01,02? startDate 2015-02
	// 8.账单记录
	public BillListVO queryBillList(String userId,String payStatus,String startDate,String endDate,String currentPage, String totalCount, String house_id);
	// 9.账单详情 anotherbillIds(逗号分隔) 汇总了去支付,来自BillInfo的bill_id
	PaymentInfo getBillDetail(User user, String stmtId, String anotherbillIds, String regionName) throws Exception;
	// 10.缴费
	WechatPayInfo getPrePayInfo(PrepayRequestDTO prepayRequestDTO) throws Exception;
	// 11.通知已支付
	public PayResult noticePayed(User user,String billId,String stmtId, String tradeWaterId, String packageId, String bind_switch);
	// 12.查询是否已经用过红包
	public String queryCouponIsUsed(String userId);
	
	//13.查询小区信息
	public CellListVO querySectList();
	
	//14.根据数据类型查询指定的物业单元信息
	public CellListVO querySectList(String sect_id, String build_id, String unit_id, String data_type);
	
	//修正已绑定房屋用户房屋不正确的问题
	public <T> void fixUserBindedHouses(String userId);
	//查询是否为二维码收费员
	Boolean isQrcodeOperator(User user);
	//获取缴费优惠信息
	Discounts getDiscounts(DiscountViewRequestDTO discountViewRequestDTO) throws Exception;
	
	
	
}
