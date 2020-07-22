package com.yumu.hexie.web.repair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yumu.hexie.integration.wuye.resp.BaseResponse;
import com.yumu.hexie.integration.wuye.resp.BaseResponseDTO;
import com.yumu.hexie.integration.wuye.vo.BaseRequestDTO;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.service.exception.IntegrationBizException;
import com.yumu.hexie.service.repair.RepairService;
import com.yumu.hexie.web.BaseController;

@Controller
@RequestMapping(value = "/servplat/repair")
public class NewRepairController extends BaseController{
	
	@Autowired
	private RepairService repairService;
	
	
	@RequestMapping(value = "/getHexieUserInfo", method = RequestMethod.POST,produces = "application/json")
	@ResponseBody
	public BaseResponseDTO<Map<String,Object>> getHexieUserInfo(@RequestBody BaseRequestDTO<String> baseRequestDTO) {
		Map<String,Object> map=new HashMap<>();
		try {
			List<User> userList=repairService.getHexieUserInfo(baseRequestDTO.getData());
			if(userList.size()<=0){
				map.put("result", 0);	
			}else{
				map.put("result", 1);
				map.put("list",userList);
			}
			
			
		} catch (Exception e) {
			throw new IntegrationBizException(e.getMessage(), e, baseRequestDTO.getRequestId());
		}
		return BaseResponse.success(baseRequestDTO.getRequestId(),map);
	}
	
}
