package com.mrc.community.service;

import com.mrc.community.dao.CommentMapper;
import com.mrc.community.entity.Comment;
import com.mrc.community.util.CommunityConstant;
import com.mrc.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit){
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId ){
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if(comment == null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        //更新评论数量
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntity(comment.getEntityType(),comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(),count);
        }

        return rows;
    }

    public List<Comment> selectCommentByUserId(int userId, int entityType, int entityId){
        return commentMapper.selectCommentByUserId(userId, entityType, entityId);
    }

    public List<Comment> selectEntityId(int userId, int entityType, int offset, int limit){
        return commentMapper.selectEntityId(userId, entityType, offset, limit);
    }

    public int findCountByUserIdAndEntity(int userId, int entityType){
        return commentMapper.findCountByUserIdAndEntity(userId, entityType);
    }

    public Comment findCommentByEntity(int id){
        return commentMapper.findCommentByEntity(id);
    }

    public Comment selectCommentById(int id){
        return commentMapper.selectCommentById(id);
    }
}
