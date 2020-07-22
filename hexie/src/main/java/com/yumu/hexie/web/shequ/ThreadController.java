package com.yumu.hexie.web.shequ;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

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
import com.yumu.hexie.service.user.UserService;
import com.yumu.hexie.web.BaseController;

@Controller
@RequestMapping(value = "/servplat/thread")
public class ThreadController extends BaseController {

	@Inject
	private UserService userService;
    
	
	@RequestMapping(value = "/getUserInfo", method = RequestMethod.POST,produces = "application/json")
	@ResponseBody
	public BaseResponseDTO<Map<String,Object>> getUserInfo(@RequestBody BaseRequestDTO<Map<String,String>> baseRequestDTO) {
		     	Map<String,Object> map=new HashMap<>();	
		    try {
			    String userId=baseRequestDTO.getData().get("userId");
				User user=userService.getById(Long.parseLong(userId));
				map.put("userInfo", user);
			} catch (Exception e) {
				throw new IntegrationBizException(e.getMessage(), e, baseRequestDTO.getRequestId());
			}
			return BaseResponse.success(baseRequestDTO.getRequestId(), map);
	}

}
