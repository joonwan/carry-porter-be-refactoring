package com.e101.carry_porter.domain.robot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.robot.repository.RobotRepository;
import com.e101.carry_porter.domain.robot.service.dto.request.AssignRobotServiceRequest;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.e101.carry_porter.support.IntegrationTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.transaction.TestTransaction;

@Slf4j
class RobotServiceConcurrencyTest extends IntegrationTestSupport {

    private static final int USER_COUNT = 10;
    private static final int ROBOT_COUNT = 5;

    @Autowired
    private RobotService robotService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    private TestFixture fixture;

    @BeforeEach
    void setUp() {
        log.info("setup 로직 시작");
        log.info("BeforeEach active = {}", TestTransaction.isActive());
        List<Long> missionIds = new ArrayList<>();
        List<Long> robotIds = new ArrayList<>();

        log.info("테스트용 사용자와 미션 저장");
        for (int i = 1; i <= USER_COUNT; i++) {
            User user = userRepository.saveAndFlush(User.createUser("concurrent-user-" + i, "password"));
            Mission mission = missionRepository.saveAndFlush(Mission.createMission(user));
            missionIds.add(mission.getId());
        }

        log.info("테스트용 로봇 저장");
        for (int i = 1; i <= ROBOT_COUNT; i++) {
            Robot robot = robotRepository.saveAndFlush(
                    Robot.createRobot("AA:BB:CC:DD:EE:" + String.format("%02d", i))
            );
            robotIds.add(robot.getId());
        }

        fixture = new TestFixture(missionIds, robotIds);
        log.info("setup 로직 종료");
    }

    @AfterEach
    void tearDown() {
        missionRepository.deleteAllInBatch();
        robotRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("사용자 10명과 로봇 5대가 있을 때 동시에 배정 요청이 들어오면 5건만 성공한다")
    void assignRobotConcurrently() throws InterruptedException {

        log.info("동시성 테스트 시작");

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(USER_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        log.info("test 용 데이터 저장되어 있는지 조회");
        log.info("mission size = {}", missionRepository.findAll().size());
        log.info("robot size = {}", robotRepository.findAll().size());

        log.info("executor 실행");
        for (int i = 0; i < USER_COUNT; i++) {

            Long missionId = fixture.missionIds.get(i);
            executorService.execute(() -> {
                try {
                    robotService.assignRobot(new AssignRobotServiceRequest(missionId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(successCount.get()).isEqualTo(ROBOT_COUNT);
        assertThat(failureCount.get()).isEqualTo(USER_COUNT - ROBOT_COUNT);
    }

    private record TestFixture(
            List<Long> missionIds,
            List<Long> robotIds
    ) {
    }
}
