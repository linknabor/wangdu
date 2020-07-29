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
	
	@Modifying
	@Transactional
	@Query(value = "update user set total_bind = ?1, sect_id = ?2, "
			+ " sect_name = ?3, cell_id = ?4, cell_addr = ?5, officeTel = ?6 "
			+ " where id = ?7 ", nativeQuery = true)
	public int updateUserByHouse(long totalBind, String sectId, String sectName, 
			String cellId, String cellAddr, String officeTel, long id);

	
	@Modifying
	@Transactional
	@Query(value = "update user set total_bind = ?1 where id = ?2 ", nativeQuery = true)
	public int updateUserTotalBind(long totalBind, long id);
	
	@Modifying
	@Transactional
	@Query(value = "update user set extraOpenId = ?1 where id = ?2 ", nativeQuery = true)
	public int updateUserExtraOpenId(String extraOpenId, long id);
	
	public List<User> findByTel(String tel);
	
	public List<User> findByWuyeId(String wuyeId);

}
