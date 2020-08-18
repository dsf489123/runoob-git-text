package com.jk.controller;

import com.jk.entity.UserEntity;
import com.jk.service.UserServiceFeign;
import com.jk.utils.Constant;
import com.jk.utils.RedisUtil;
import com.jk.utils.StringUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/user")
@Controller
public class UserController {

    @Resource
    private UserServiceFeign userService;

    @Resource
    private RedisUtil redisUtil;


    @RequestMapping("/saveOrder")
    @HystrixCommand(fallbackMethod ="saveOrderFail" )
    @ResponseBody
    public Object saveOrder(Integer userId, Integer productId , HttpServletRequest request){
       return userService.saveOrder(userId,productId);

    }
    private Object saveOrderFail(Integer userId,Integer productId , HttpServletRequest request){

        System.out.println("controller,保存订单降级方法");

        String sendValue = redisUtil.get(Constant.SAVE_ORDER_WARNING_KEY).toString();
        String ipAddr =request.getRemoteAddr();
        new Thread(()->{
            if (StringUtil.isEmpty(sendValue)){
                System.out.println("紧急短信，用户下单失败，请离开查找原因，ip地址是="+ipAddr);

            }else {
                System.out.println("已经发送过短信，1分钟内不重复发送");
            }
        }).start();
        Map<String, Object> map = new HashMap<>();
        map.put("code",-1);
        map.put("message","抢购人数太多,");
        return map;
     }
    @RequestMapping("/hello")
    @ResponseBody
    public String hello(String name){
        return userService.hello(name);
    }

    @RequestMapping("/selectUserList")
    @ResponseBody
    public List<UserEntity> selectUserList() {

        List<UserEntity> userList = (List<UserEntity>) redisUtil.get(Constant.SELECT_USER_LIST);

        // 1. 有值   2. 没有值
        if(userList == null || userList.size() <= 0 || userList.isEmpty()) {
            // 从数据库查询，存redis
            userList = userService.findUserList();
            redisUtil.set(Constant.SELECT_USER_LIST, userList, 30);
        }

        return userList;

    }

}
