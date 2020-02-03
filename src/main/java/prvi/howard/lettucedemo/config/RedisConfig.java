package prvi.howard.lettucedemo.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    /**
     * @Description Redis配置类
     */
//    注入RedisTemplate，指定使用Lettuce客户端，并在其中做其他配置
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        使用Lettuce连接Redis
        template.setConnectionFactory(factory);

//        指定Key的序列化方式
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

//        使用Jackson序列化方式代替默认的JDK序列化
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
//        配置自定义ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
//        序列化的范围(all包括field、getter、setter、creator)，和可见性修饰符(any包括public、private等)
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        序列化的类型必须是非final的
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
//        指定Value的序列化方式
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
//        注意根据需要来对String类型的value进行序列化，如果在String类型中存储对象JSON字符串则使用Jackson序列化方式
        template.setValueSerializer(stringRedisSerializer);

//        初始化RedisTemplate
        template.afterPropertiesSet();
        return template;
    }
}
