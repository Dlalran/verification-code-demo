package prvi.howard.lettucedemo.service.impl;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import prvi.howard.lettucedemo.entity.User;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Log
public class UserServiceImpl {
    /**
     * 测试基于Lettuce的RedisTemplate操作Redis
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 统一注入Operations并指定泛型类型
     * TODO 属性编辑器PropertyEditor，@Resource注解
     */
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> stringOps;

    @Resource(name = "redisTemplate")
    private HashOperations<String, String, User> hashOps;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

//    RedisTemplate操作String类型
    public String testString(String key) {
        String result = null;
//        查询是否存在key，等同于exists
        if (redisTemplate.hasKey(key)) {
//            opsForValue封装对于String类型的操作
//            result = (String) redisTemplate.opsForValue().get(key);
            result = stringOps.get("111");
            log.info("从Redis中获取key:" + key + "，value:" + result);
        } else {
            log.info("Redis中不存在该Key");
            result = "ValueContent";
            stringOps.set(key, result);
            log.info("将数据库查询的数据放入Redis中，value:" + result);
        }
        return result;
    }

//    设置String的有效时间
    public void testExpireString(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
//        作为不限制数据类型的命令expire，不被封装到任何数据类型的operation中
//        参数依次指定key、有效时间值、有效时间单位
        redisTemplate.expire(key,10, TimeUnit.SECONDS);
        log.info("设置有效时间为10秒的数据key:" + key + "，value:" + value);
    }

//    操作Hash类型
    public User selectById(String id) {
        User result = null;
//        相当于Redis命令 hget user id
        if (hashOps.hasKey("user", id)) {
            log.info("Redis中存在Hash类型数据，field:" + id);
            result = hashOps.get("user", id);
        } else {
            result = new User("1", "howard", 22);
            log.info("Redis中不存在该数据，数据库查询后放入");
            hashOps.put("user", id, result);
        }
        return result;
    }

//    TODO 操作List类型
    public void listTest1(ArrayList<String> messages) {
        String key = "msg";

//        将字符串一一放入list中
//        messages.forEach(message -> listOps.leftPush(key, message));
        listOps.leftPushAll(key, messages);

//        删除指定key的第count个元素，count>0时，删除从表头开始指定数量等于value的元素，count<0时从表尾开始删除，count=0时删除所有等于value的元素
        listOps.remove(key, 0, messages.get(1));

//        获取list中所有元素
        List<String> results = listOps.range(key, 0, -1);
        results.forEach(System.out::println);
    }
}
