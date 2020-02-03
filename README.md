### Lettuce

​		Lettuce是基于Netty的Redis客户端实现，其可以在多个线程并发访问，并线程安全，同时采用可伸缩设计，连接实例不够时可以按需增加连接实例。

​		这里使用的是Spring Data Redis，其中对Redis底层开发包(Jedis、Lettuce)进行了封装，并通过RedisTemplate提供对于Redis的操作、异常处理和序列化，对同一类型的数据封装为Operation并自动管理连接池。

##### 添加依赖

​		Spring Boot 2.0+中添加Spring Data Redis启动器，其中默认集成Lettuce，同时还要添加其依赖的Commons-pool.

```xml
<!--        Redis(Lettuce)-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
<!--        Commmons-pool(Lettuce依赖需要)-->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
```

##### 配置参数

​		基本和Jedis相同，区别是设置后不用手动装配，`RedisProperties`会自动装配。

```yml
spring:
  redis:
    #    Redis服务端主机地址
    host: 192.168.2.128
    #    服务端端口号
    port: 6379
    #    服务端连接密码
    password: 12345
#    连接超时时间(ms)
    timeout: 2000
    lettuce:
      pool:
#        最大连接数(负值为无限制)
        max-active: 8
#        最大空闲连接
        max-idle: 8
#        最小空闲连接
        min-idle: 0
#        最大阻塞等待时间(负值为无限制)
        max-wait: 1000
```

##### 配置类

​		注入自定义RedisTemplate，指定连接实现、序列化方式等配置。

```java
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
```

##### 操作Redis数据

​		`RestTemplate`对不同类型的数据操作封装为不同的Operations，调用这些Operation的方法来边界的操作Redis。

- 操作String类型	

```java
//    RedisTemplate操作String类型
    public String testString(String key) {
        String result = null;
//        查询是否存在key，等同于exists
        if (redisTemplate.hasKey(key)) {
//            opsForValue封装对于String类型的操作
            result = (String) redisTemplate.opsForValue().get(key);
        } else {
            result = "ValueContent";
            redisTemplate.opsForValue().set(key, result);
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
```

- 操作Hash类型

  ​		`RestTemplate`会对对象进行配置中指定方式的序列化，存储方式是指定key和field(这里是hashkey，用于区分不同对象)，将对象的属性作为value放入Hash类型中。**注意与Jedis的区别**

```java
   public User selectById(String id) {
        User result = null;
//        相当于Redis命令 hget user id
        if (redisTemplate.opsForHash().hasKey("user", id)) {
            redisTemplate.opsForHash().get("user", id);
        } else {
            result = new User("1", "howard", 22);
            redisTemplate.opsForHash().put("user", id, result);
        }
        return result;
    }
```

- 操作List类型

```java
    public void listTest1(ArrayList<String> messages) {
        String key = "msg";

//        将字符串一一放入list中
//        messages.forEach(message -> listOps.leftPush(key, message));
        listOps.leftPushAll(key, messages);

//        删除指定key的第count个值为value的元素，count>0时，删除从表头开始count个等于value的元素，count<0时从表尾开始删除，count=0时删除所有等于value的元素
        listOps.remove(key, 0, messages.get(1));

//        获取list中所有元素
        List<String> results = listOps.range(key, 0, -1);
        results.forEach(System.out::println);
    }
```

- 优化

  ​		可以将数据类型的封装操作Operations进行统一注入代替手动获取；同时对泛型进行指定，代替手动类型转换。

  ​		具体实现是通过`@Resource`注解，再通过Spring的属性编辑器(PropertyEditor)机制，在不同Operations的Editor(如`HashOperationsEditor`)中的方法`setValue`中，通过`RedisTemplate`获得对应的Operations并注入。

```java
@Resource(name = "redisTemplate")
private HashOperations<String, String, User> hashOps;

 public User selectById(String id) {
        User result = null;
        if (hashOps.hasKey("user", id)) {
            result = hashOps.get("user", id);
        } else {
            result = new User("1", "howard", 22);
            hashOps.put("user", id, result);
        }
        return result;
    }
```
