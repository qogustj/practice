import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CPU 바운드 작업에서 전통적인 스레드와 가상 스레드의 성능을 비교하는 클래스
 */
public class CpuBoundComparison {

    /**
     * CPU 집약적인 작업을 시뮬레이션하는 메소드
     * 피보나치 수열 계산과 소수 판별을 복합적으로 수행
     */
    private static long performCpuIntensiveTask(int number) {
        // 피보나치 수열 계산
        long result = calculateFibonacci(number);

        // 소수 판별 작업 추가
        for (int i = 2; i < 100000; i++) {
            isPrime(i);
        }

        System.out.println("작업 완료: " + number + " on thread: " + Thread.currentThread());
        return result;
    }

    /**
     * 피보나치 수열의 n번째 값을 계산
     */
    private static long calculateFibonacci(int n) {
        if (n <= 1) return n;
        return calculateFibonacci(n - 1) + calculateFibonacci(n - 2);
    }

    /**
     * 소수 판별 메소드
     */
    private static boolean isPrime(int number) {
        if (number < 2) return false;
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) return false;
        }
        return true;
    }

    /**
     * 전통적인 스레드 풀 방식으로 CPU 바운드 작업 처리
     */
    public static void traditionalThreadApproach(int taskCount) {
        System.out.println("\n=== 전통적인 스레드 방식 ===");
        Instant start = Instant.now();

        // CPU 코어 수만큼의 고정 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        // 각 작업에 대한 비동기 처리 작업 생성
        for (int i = 0; i < taskCount; i++) {
            final int taskNumber = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(
                    () -> performCpuIntensiveTask(taskNumber),
                    executorService
            );
            futures.add(future);
        }

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("전통적 방식 처리 완료 시간: %d 밀리초%n", duration.toMillis());
    }

    /**
     * 가상 스레드 방식으로 CPU 바운드 작업 처리
     */
    public static void virtualThreadApproach(int taskCount) {
        System.out.println("\n=== 가상 스레드 방식 ===");
        Instant start = Instant.now();

        List<Thread> threads = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);

        // 각 작업에 대한 가상 스레드 생성
        for (int i = 0; i < taskCount; i++) {
            final int taskNumber = i;
            Thread virtualThread = Thread.startVirtualThread(() -> {
                performCpuIntensiveTask(taskNumber);
                completedCount.incrementAndGet();
            });
            threads.add(virtualThread);
        }

        // 모든 가상 스레드 작업 완료 대기
        while (completedCount.get() < taskCount) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("가상 스레드 방식 처리 완료 시간: %d 밀리초%n", duration.toMillis());
    }

    public static void main(String[] args) {
        // 다양한 작업 수로 테스트
        int[] testSizes = {4, 8, 16};

        for (int size : testSizes) {
            System.out.println("\n========================================");
            System.out.println(size + "개의 CPU 집약적 작업 테스트");
            System.out.println("========================================");

            // 전통적 방식 테스트
            traditionalThreadApproach(size);

            // 테스트 간 간격
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 가상 스레드 방식 테스트
            virtualThreadApproach(size);
        }
    }
}