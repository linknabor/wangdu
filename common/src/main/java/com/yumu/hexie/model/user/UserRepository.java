package com.yumu.hexie.model.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
	
	public List<User> findByOpenid(String openid);
	public List<User> findByShareCode(String shareCode);
	
	@Modifying
	@Transactional
	@Query(value = "update user set wuyeId = ?1 where id = ?2 ", nativeQuery = true)
	public int updateUserWuyeId(String wuyeId, long id);
	
	/**
	 * 更新业主地址
	 * @param xiaoquId
	 * @param xiaoquName
	 * @param totalBind
	 * @param province
	 * @param city
	 * @param county
	 * @param sectId
	 * @param cspId
	 * @param officeTel
	 * @param id
	 * @return
	 */
	
	
//	if (currUser.getTotal_bind() <= 0) {//从未绑定过的做新增
//		currUser.setTotal_bind(1);
//		currUser.setSect_id(house.getSect_id());
//		currUser.setSect_name(house.getSect_name());
//		currUser.setCell_id(house.getMng_cell_id());
//		currUser.setCell_addr(house.getCell_addr());
//		
//		user.setTotal_bind(1);
//		user.setSect_id(house.getSect_id());
//		user.setSect_name(house.getSect_name());
//		user.setCell_id(house.getMng_cell_id());
//		user.setCell_addr(house.getCell_addr());	//set到session
//		
//	}else {
//		long bind = currUser.getTotal_bind()+1;
//		user.setTotal_bind(bind);
//		currUser.setTotal_bind(bind);
//	}
//	
	
	@Modifying
	@Transactional
	@Query(value = "update user set total_bind = ?1, sect_id = ?2, "
			+ "sect_name = ?3, cell_id = ?4, cell_addr = ?5 office_tel = ?6 "
			+ " where id = ?10 ", nativeQuery = true)
	public int updateUserByHouse(long totalBind, String sectId, String sectName, 
			String cellId, String cellAddr, String officeTel, long id);

	
	@Modifying
	@Transactional
	@Query(value = "update user set total_bind = ?1 where id = ?2 ", nativeQuery = true)
	public int updateUserTotalBind(long totalBind, long id);
}
