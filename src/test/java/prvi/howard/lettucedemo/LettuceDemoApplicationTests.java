package prvi.howard.lettucedemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import prvi.howard.lettucedemo.entity.User;
import prvi.howard.lettucedemo.service.impl.UserServiceImpl;

import java.util.ArrayList;

@SpringBootTest
class LettuceDemoApplicationTests {

    @Autowired
    private UserServiceImpl userService;

    @Test
    void testStr() {
        String result = userService.testString("test");
        System.out.println(result);
    }

    @Test
    void testExpire() {
        userService.testExpireString("test2", "TestContent");
    }

    @Test
    void testHash() {
        User result = userService.selectById("1");
        System.out.println(result);
    }

    @Test
    void testList() {
        ArrayList<String> messages = new ArrayList<>();
        messages.add("aaa");
        messages.add("bbb");
        messages.add("ccc");
        userService.listTest1(messages);
    }
}
