package com.mrc.community.util;

import com.mrc.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替Session对象
 */
@Component
public class HostHolder {
    private  ThreadLocal<User> users = new ThreadLocal<User>();

    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    public void clear(){
        users.remove();
    }
}
