package com.mrc.community.controller;

import com.mrc.community.entity.*;
import com.mrc.community.event.EventProducer;
import com.mrc.community.service.CommentService;
import com.mrc.community.service.DiscussPostService;
import com.mrc.community.service.LikeService;
import com.mrc.community.service.UserService;
import com.mrc.community.util.CommunityConstant;
import com.mrc.community.util.CommunityUtil;
import com.mrc.community.util.HostHolder;
import com.mrc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path="/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser();
        if(user == null){
            return CommunityUtil.getJSONString(403, "你还没有登录！");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());

        return CommunityUtil.getJSONString(0, "发布成功！");
    }

    @RequestMapping(path="/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);

        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        //点赞
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUser() == null? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        //查询评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        //评论:给帖子的评论
        //回复:给评论的评论
        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论VO列表
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        if(commentList != null){
            for(Comment comment : commentList){
                //评论VO
                Map<String, Object> commentVo = new HashMap<>();
                //评论
                commentVo.put("comment", comment);
                //作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));

                //点赞
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser() == null? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if(replyList != null){
                    for(Comment reply : replyList){
                        Map<String, Object> replyVo = new HashMap<>();
                        //回复
                        replyVo.put("reply", reply);
                        //回复的作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //回复目标
                        User target = reply.getTargetId() == 0? null: userService.findUserById(reply.getTargetId());

                        //点赞
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUser() == null? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        replyVo.put("target", target);

                        replyVoList.add(replyVo);
                    }
                }

                commentVo.put("replys", replyVoList);

                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount",replyCount);

                commentVoList.add(commentVo);
            }
        }
        User userOwn = hostHolder.getUser();
        model.addAttribute("owner", userOwn);

        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";
    }

    //TA的帖子
    @RequestMapping(path="/my-post/{userId}",method = RequestMethod.GET)
    public String getMyDisCuss(@PathVariable("userId") int userId, Model model, Page page){

        //查询帖子分页信息
        long myPostCount = discussPostService.findDiscussPostRows(userId);
        page.setLimit(5);
        page.setPath("/discuss/my-post/" + userId);
        page.setRows((int)myPostCount);

        List<DiscussPost> list = discussPostService.findDiscussPosts(userId,page.getOffset(),page.getLimit(), 0);

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for(DiscussPost post : list){
            System.out.println(post);
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("post", post);
            //点赞
            long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
            postMap.put("likeCount", likeCount);

            discussPosts.add(postMap);
        }
        int discussPostCount = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("discussPostCount", discussPostCount);

        User user = hostHolder.getUser();
        model.addAttribute("owner", user);

        model.addAttribute("userId", userId);

        model.addAttribute("discussPosts", discussPosts);


        return "/site/my-post";
    }

    //TA的回复
    @RequestMapping(path="/my-reply/{userId}",method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId, Model model, Page page){
        //找出所有回复的帖子
        long commentCount = commentService.findCountByUserIdAndEntity(userId, ENTITY_TYPE_COMMENT);
        model.addAttribute("commentCount", commentCount);
        //查询回复分页信息
        page.setLimit(5);
        page.setPath("/discuss/my-reply/" + userId);
        page.setRows((int)commentCount);

        List<Comment> list = commentService.selectEntityId(userId, ENTITY_TYPE_COMMENT, page.getOffset(),page.getLimit());
        List<Map<String, Object>> replys = new ArrayList<>();
        if(list != null){
            for(Comment comment: list){
                Map<String, Object> map = new HashMap<>();
                map.put("reply", comment);
                Comment co = commentService.findCommentByEntity(comment.getEntityId());
                map.put("content",co.getContent());
                replys.add(map);
            }
            model.addAttribute("replys", replys);
        }
        User user = hostHolder.getUser();
        model.addAttribute("owner", user);
        model.addAttribute("userId", userId);


        return "/site/my-reply";
    }

    // 置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        discussPostService.updateType(id, 1);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    // 加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        discussPostService.updateStatus(id, 1);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0);
    }

    // 删除
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);

        // 触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
