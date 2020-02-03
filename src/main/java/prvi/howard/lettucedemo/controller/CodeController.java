package prvi.howard.lettucedemo.controller;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import prvi.howard.lettucedemo.entity.CodeResponse;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log
@Controller
public class CodeController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Resource(name = "redisTemplate")
    ValueOperations<String, String> stringOps;

    @ResponseBody
    @RequestMapping("getCode")
    public int getCode(@RequestParam("phoneNum") final String phoneNum) {
//        生成验证信息内容
        String identifyCode = (int) (Math.random() * 9000 + 1000) + "";
        String content = "您的验证码是：" + identifyCode + "。请不要把验证码泄露给其他人。";
        log.info("为手机号:" + phoneNum + ",生成验证码为:" + identifyCode);

//        放入调用接口的参数列表
        Map<String, String> map = new HashMap<>();
        map.put("mobile", phoneNum);
        map.put("content", content);

//        返回前端结果，0为验证码API异常、1为已成功发送短信、2为60秒内重复发送
        int result = 0;
        if (redisTemplate.hasKey(phoneNum)) {
            result = 2;
        } else {
//            请求验证码API
            CodeResponse codeResponse = restTemplate.getForObject("http://106.ihuyi.com/webservice/sms.php?method=Submit&account=*****&password=*****8&mobile={mobile}&content={content}&format=json", CodeResponse.class, map);
            if (codeResponse.getCode() == 2) {
                log.info("成功发送短信验证码，API返回内容为" + codeResponse);
//                key为手机号，value为验证码，有效时间为60s放入Redis中
                stringOps.set(phoneNum, identifyCode);
                redisTemplate.expire(phoneNum, 60, TimeUnit.SECONDS);
                log.info("将key为" + phoneNum + "，value为" + identifyCode + "放入Redis中");
                result = 1;
            } else {
                log.info("验证码API调用异常，返回错误信息为" + codeResponse);
                result = 0;
            }
        }
        return result;
    }

    @RequestMapping("checkCode")
    public String checkCode(@RequestParam("phoneNum") final String phoneNum, @RequestParam("code") final String code) {
        log.info("用户手机号为:" + phoneNum + "输入的验证码为:" + code);
        String value = stringOps.get(phoneNum);
        log.info("从Redis获取的验证码为:" + value);
        if (code.equals(value)) {
            return "success";
        } else {
            return "error";
        }
    }
}
