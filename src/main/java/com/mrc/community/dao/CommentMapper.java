package com.mrc.community.dao;

import com.mrc.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    int insertComment(Comment comment);

    List<Comment> selectCommentByUserId(int userId, int entityType, int entityId);

    List<Comment> selectEntityId(int userId,int entityType, int offset, int limit);

    int findCountByUserIdAndEntity(int userId, int entityType);

    Comment findCommentByEntity(int id);

    Comment selectCommentById(int id);

}
