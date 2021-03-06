/**
 * 
 */
package com.yumu.hexie.service.shequ.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.yumu.hexie.common.util.DateUtil;
import com.yumu.hexie.dto.CommonDTO;
import com.yumu.hexie.model.ModelConstant;
import com.yumu.hexie.model.community.Annoucement;
import com.yumu.hexie.model.community.AnnoucementRepository;
import com.yumu.hexie.model.community.CommunityInfo;
import com.yumu.hexie.model.community.CommunityInfoRepository;
import com.yumu.hexie.model.community.Thread;
import com.yumu.hexie.model.community.ThreadComment;
import com.yumu.hexie.model.community.ThreadCommentRepository;
import com.yumu.hexie.model.community.ThreadRepository;
import com.yumu.hexie.model.user.User;
import com.yumu.hexie.model.user.UserRepository;
import com.yumu.hexie.service.common.GotongService;
import com.yumu.hexie.service.exception.BizValidateException;
import com.yumu.hexie.service.shequ.CommunityService;

@Service("communityService")
public class CommunityServiceImpl implements CommunityService {

	@Inject
	private ThreadRepository threadRepository;
	
	@Inject
	private ThreadCommentRepository threadCommentRepository;
	
	@Inject
	private CommunityInfoRepository communityInfoRepository;
	
	@Inject
	private AnnoucementRepository annoucementRepository;
	
	@Inject 
	private GotongService gotongService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Override
	public List<Thread> getThreadList(long userSectId, Pageable page) {
		
		return threadRepository.findByThreadStatusAndUserSectId(ModelConstant.THREAD_STATUS_NORMAL, userSectId, page);
		
	}
	
	@Override
	public List<Thread> getThreadList(Pageable page) {
		
		return threadRepository.findByThreadStatus(ModelConstant.THREAD_STATUS_NORMAL, page);
	}

	@Override
	public List<Thread> getThreadListByCategory(String category, long userSectId, Pageable page) {

		return threadRepository.getThreadListByCategory(ModelConstant.THREAD_STATUS_NORMAL, userSectId, category, page);
	}

	@Override
	public List<Thread> getThreadListByCategory(String category, Pageable page) {
		
		return threadRepository.getThreadListByCategory(ModelConstant.THREAD_STATUS_NORMAL, category, page);
	}

	@Transactional
	@Override
	public CommonDTO<User, Thread> addThread(User user, Thread thread) {
		
		if(StringUtils.isEmpty(user.getSect_id())) {
			throw new BizValidateException("用户未绑定房屋，不能使用当前功能。");
		}
		if(thread.getThreadContent().length()>200){
			throw new BizValidateException("发布信息内容超过200字。");
		}
		
		User currUser = userRepository.findOne(user.getId());
		thread.setCreateDateTime(System.currentTimeMillis());
		thread.setCreateDate(DateUtil.dtFormat(new Date(), "yyyyMMdd"));
		thread.setCreateTime(DateUtil.dtFormat(new Date().getTime(), "HHMMss"));
		thread.setThreadStatus(ModelConstant.THREAD_STATUS_NORMAL);
		thread.setUserHead(currUser.getHeadimgurl());
		thread.setUserId(currUser.getId());
		thread.setUserOpenId(currUser.getOpenid());
		thread.setUserName(currUser.getNickname());
		thread.setUserSectId(currUser.getXiaoquId());
		thread.setUserSectName(currUser.getXiaoquName());
		thread.setUserAddress(currUser.getCell_addr());
		thread.setUserTel(currUser.getTel());
		thread.setStickPriority("0");	//默认优先级0，为最低
		thread.setReplied(false);//未回复 默认
		thread.setSolved(false);
		thread.setExtraOpenId(thread.getExtraOpenId());//用户在悦生活的openid
		threadRepository.save(thread);
		gotongService.sendThreadPubNotify(currUser, thread);
		userRepository.updateUserExtraOpenId(thread.getExtraOpenId(), currUser.getId());
		CommonDTO<User, Thread> dto = new CommonDTO<>();
		dto.setData1(currUser);
		dto.setData2(thread);
		return dto;
	}

	@Override
	public void deleteThread(User user, long threadId) {

		Thread thread = threadRepository.findOne(threadId);
		if (thread == null) {
			throw new BizValidateException("帖子不存在。");
		}
		
		if (ModelConstant.THREAD_STATUS_DELETED.equals(thread.getThreadStatus())) {
			throw new BizValidateException("帖子已删除。");
		}
		
		if (thread.getUserId()!=user.getId()) {
			throw new BizValidateException("用户无权限删除帖子。");
		}
		
		thread.setThreadStatus(ModelConstant.THREAD_STATUS_DELETED);	//"0"正常，"1"废弃
		threadRepository.save(thread);
	}

	@Override
	public void updateThread(Thread thread) {

		Thread t = threadRepository.findOne(thread.getThreadId());
		if (t == null) {
			throw new BizValidateException("帖子不存在。");
		}
		threadRepository.save(thread);
	}

	@Override
	public void updateThreadComment(ThreadComment thread) {

		ThreadComment t = threadCommentRepository.findOne(thread.getCommentId());
		if (t == null) {
			throw new BizValidateException("帖子不存在。");
		}
		threadCommentRepository.save(thread);
	}
	
	@Transactional
	@Override
	public ThreadComment addComment(User user, ThreadComment comment) {
	
		comment.setCommentDateTime(System.currentTimeMillis());
		comment.setCommentDate(DateUtil.dtFormat(new Date(), "yyyyMMdd"));
		comment.setCommentTime(DateUtil.dtFormat(new Date().getTime(), "HHMMss"));
		comment.setCommentUserHead(user.getHeadimgurl());
		comment.setCommentUserId(user.getId());
		comment.setCommentUserName(user.getNickname());
		
		long threadId = comment.getThreadId();
		Thread thread = threadRepository.findOne(threadId);
		
		if(thread.getUserId()!=user.getId()) {
			thread.setReplied(true);//已回复
		}
		threadRepository.save(thread);
		threadCommentRepository.save(comment);
		
		if (thread.isReplied()) {
			gotongService.sendThreadReplyMsg(thread);
		}
		return comment;
		
	}
	
	@Override
	public List<ThreadComment> getCommentListByThreadId(long threadId) {
		
		return threadCommentRepository.findByThreadId(threadId);
	
	}

	@Override
	public Thread getThreadByTreadId(long threadId) {
	
		return threadRepository.findOne(threadId);

	}

	@Override
	public void deleteComment(User user, long threadCommentId) {
		
		
		ThreadComment comment = threadCommentRepository.findOne(threadCommentId);
		if (comment == null) {
			throw new BizValidateException("评论不存在。");
		}
		
		if (comment.getCommentUserId()!=user.getId()) {
			throw new BizValidateException("用户无权限删除帖子。");
		}
		
		threadCommentRepository.delete(threadCommentId);
		
	}

	@Override
	public List<Thread> getThreadListByUserId(long userId, Sort sort) {
		
		return threadRepository.findByThreadStatusAndUserId(ModelConstant.THREAD_STATUS_NORMAL, userId, sort);
	}

	@Override
	public List<CommunityInfo> getCommunityInfoBySectId(long sectId, Sort sort) {
		
		return communityInfoRepository.findBySectId(sectId, sort);
	}

	@Override
	public List<CommunityInfo> getCommunityInfoByCityIdAndInfoType(long cityId, String infoType, Sort sort) {
		
		return communityInfoRepository.findByCityIdAndInfoType(cityId, infoType, sort);
		
	}
	
	@Override
	public List<CommunityInfo> getCommunityInfoByRegionId(long regionId, Sort sort) {
		
		return communityInfoRepository.findByRegionId(regionId, sort);
	}

	@Override
	public List<Annoucement> getAnnoucementList(Sort sort) {
		
		return annoucementRepository.findAll(sort);
	}

	@Override
	public Annoucement getAnnoucementById(long annoucementId) {

		return annoucementRepository.findOne(annoucementId);
	}

	@Override
	public int getUnreadCommentsCount(String threadStatus, long toUserId){
		
		Integer i = threadCommentRepository.getUnreadCommentsCount(threadStatus, toUserId);
		return i;
	}
	
	@Override
	public void updateCommentReaded(long toUserId, long threadId){
	
		threadCommentRepository.updateCommentReaded(toUserId, threadId);
	}

	@Override
	public List<Thread> getThreadListByNewCategory(String category,
			long userSectId, Pageable page) {
		
		return threadRepository.getThreadListByNewCategory(ModelConstant.THREAD_STATUS_NORMAL, userSectId, category, page); 
	
	}

	@Override
	public List<Thread> getThreadListByNewCategory(String category, Pageable page) {

		return threadRepository.getThreadListByNewCategory(ModelConstant.THREAD_STATUS_NORMAL, category, page);
	}

	@Override
	public List<Thread> getThreadListByUserId(String category, long userId,
			Pageable page) {

		return threadRepository.getThreadListByNewCategory(ModelConstant.THREAD_STATUS_NORMAL, category, page);
	}

	@Override
	public List<Thread> getThreadListByUserId(long userId, Pageable page) {

		return threadRepository.findByThreadStatusAndUserId(ModelConstant.THREAD_STATUS_NORMAL, userId, page);
	}

	@Override
	public List<Thread> getThreadListByUserId(long userId, String category, Pageable page) {
		if("2".equals(category))
		{
			List<String> list = new ArrayList<String>();
			list.add("2");
			list.add("3");
			return threadRepository.findByThreadStatusAndUserIdAndThreadCategory(ModelConstant.THREAD_STATUS_NORMAL, userId, list, page);
		}else
		{
			return threadRepository.findByThreadStatusAndUserIdAndThreadCategory(ModelConstant.THREAD_STATUS_NORMAL, userId, category, page);
		}
	}
	
	@Override
	public ThreadComment getThreadCommentByTreadId(long threadCommentId) {
		return threadCommentRepository.findOne(threadCommentId);
	}

	@Override
	public Thread finishThread(long threadId) {
		Thread thread = threadRepository.findOne(threadId);
		thread.setSolved(true);
		return threadRepository.save(thread);
	}
	
	
}
