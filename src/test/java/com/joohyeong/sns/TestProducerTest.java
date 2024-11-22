package com.joohyeong.sns;

import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.dto.response.*;
import com.joohyeong.sns.post.repository.PostRepository;
import com.joohyeong.sns.post.service.FeedService;
import com.joohyeong.sns.post.service.PostService;


import com.joohyeong.sns.user.dto.request.UserRegisterRequest;
import com.joohyeong.sns.user.repository.FollowRepository;
import com.joohyeong.sns.user.repository.UserRepository;
import com.joohyeong.sns.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest

class TestProducerTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;
    @Autowired
    private FeedService feedService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private FollowRepository followRepository;
    @Qualifier("postRedisTemplate")
    @Autowired
    private RedisTemplate postRedisTemplate;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Qualifier("feedRedisTemplate")
    @Autowired
    private RedisTemplate<String,String> feedRedisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    void testFollowe() {
        for (int i = 1; i < 10000; i++) {
            userService.follow((long) i, 1000200L);
        }

    }

    @Test
    @Transactional
    void testmasterslave() {
        Post post = postRepository.findById(1L).orElse(null);
        System.out.println(post.getContent());
        Post slavePost = feedService.testFind(2L);
        System.out.println(slavePost.getContent());
    }

    @Test
    @Transactional
    void testAsynkFeed1() {
        Post post = postRepository.findById(4725045L).orElse(null);
        feedService.addFeedWithRetry(post);
    }

    @Test
    @Transactional
    void testGetFeed1() {
        Page<FeedDetailResponse> feed = null;
        try {
            long start = System.currentTimeMillis();
            feed = feedService.getFeed(447L, 1, 5);
            long end = System.currentTimeMillis();
            System.out.println((end - start) + "ms");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(feed.getContent());
    }


    @Test
    @Transactional
    void testAddFeed() {
        // 10000명의 팔로워를 가진 유저
        List<Long> followerIds = followRepository.findFollowerUserIds(1000220L);

        long start = System.currentTimeMillis();
        for (Long followerId : followerIds) {
            stringRedisTemplate.opsForZSet().add("feed:userId" + followerId, "1000:2024111111:0", 1000);
        }
        System.out.println("파이프라이닝 X 총 피드 생성 소요 시간 :" + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();

        List<Object> result = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        for (long followerId : followerIds) {
                            stringRedisConn.zAdd("feed:userId" + followerId, 1000, "1000:2024111111:0");
                        }
                        return null;
                    }
                });

        System.out.println("파이프라이닝 O 총 피드 생성 소요 시간 :" + (System.currentTimeMillis() - start) + "ms");

    }



    @Test
    @Transactional
    void testGetFeed3() {

        long start = System.currentTimeMillis();
        feedService.getPostIdWithCacheData2(447L, 1, 5);
        long end = System.currentTimeMillis();
        System.out.println((end - start) + "ms");

    }

    @Test
    void date() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);  // 현재 시각 기준 일주일 전
        System.out.println(oneWeekAgo.format(formatter));
    }


    @Test
    void testGetPostList() {
        List<PostThumbnailResponse> dtoList = postService.getPostThumbnail(12345L);
        System.out.println(dtoList);
    }


    @Test
    void testGetPostDetail() {
        FeedDetailResponse dto = postService.getPostDetail(12345L);
        System.out.println(dto);
    }


    @Test
    void testGetFeed() throws Exception {
        Page<FeedDetailResponse> feed = feedService.getFeed(125215L, 1, 5);
        System.out.println(feed);
    }

    @Test
    void deleteFeed() {
        feedService.deleteExpiredFeedPosts("feed:userId:203");
    }


    @Test
    void testRegisterUser() {
        UserRegisterRequest dto = new UserRegisterRequest("asd", "joohyeongzz@naver.com", "asd");
        for (int i = 0; i < 20; i++) {
            userService.registerUser(dto);
        }

    }

    @Test
    void testRedisConnection() {
        String key = "test:key";
        String value = "Hello, Redis!";

        redisTemplate.opsForValue().set(key, value);
        String retrievedValue = redisTemplate.opsForValue().get(key);

        assertEquals(value, retrievedValue);
    }


    @Test
    public void testGetKeyFromCluster() {
        String key = "asd";
        String value = "testValue";

        // 키-값 쌍을 설정합니다.
        redisTemplate.opsForValue().set(key, value);

        // 키의 슬롯을 확인합니다.
        Long slot = redisTemplate.execute(RedisScript.of("return redis.call('CLUSTER', 'KEYSLOT', KEYS[1])", Long.class),
                Collections.singletonList(key));

        // 키를 읽습니다.
        String result = redisTemplate.opsForValue().get(key);

        assertNotNull(result);
        System.out.println("Key '" + key + "' is in slot: " + slot);
        System.out.println("Value read: " + result);

        // 클러스터 노드 정보를 가져옵니다.
        String clusterInfo = redisTemplate.execute(RedisScript.of("return redis.call('CLUSTER', 'NODES')", String.class),
                Collections.emptyList());

        System.out.println(clusterInfo);
    }


}
