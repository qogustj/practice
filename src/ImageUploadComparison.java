import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageUploadComparison {
    private static final int UPLOAD_MIN_DELAY_MS = 100;
    private static final int UPLOAD_MAX_DELAY_MS = 300;
    private static final int POLL_INTERVAL_MS = 10;
    private static final Duration EXECUTOR_SHUTDOWN_TIMEOUT = Duration.ofMinutes(1);

    /**
     * I/O 작업을 시뮬레이션하는 이미지 업로드 메소드
     * 실제 네트워크 지연을 모방하기 위해 랜덤한 지연 시간 추가
     */
    private static void simulateImageUpload(String imageName) {
        try {
            // 100-300ms 사이의 랜덤한 지연으로 네트워크 I/O 시뮬레이션
            long delay = (long) (Math.random() * (UPLOAD_MAX_DELAY_MS - UPLOAD_MIN_DELAY_MS) + UPLOAD_MIN_DELAY_MS);
            Thread.sleep(delay);
            System.out.printf("이미지 업로드 완료: %s (스레드: %s, 소요시간: %dms)%n",
                    imageName, Thread.currentThread(), delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("이미지 업로드 중단: %s%n", imageName);
        }
    }

    /**
     * Platform Thread를 사용한 전통적인 비동기 처리 방식# Java Virtual Thread 도입 가이드: 실무 적용과 API 선택
     * - ExecutorService를 사용하여 스레드 풀 관리
     * - Back pressure를 위한 제한된 스레드 풀 사이즈
     */
    public static void platformThreadApproach(int imageCount) {
        System.out.println("\n=== Platform Thread 방식 ===");
        Instant start = Instant.now();

        // CPU 코어 수 기반의 제한된 스레드 풀 생성
        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            // 각 이미지에 대한 비동기 업로드 작업 생성
            for (int i = 0; i < imageCount; i++) {
                final String imageName = String.format("이미지-%d", i);
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> simulateImageUpload(imageName),
                        executorService
                );
                futures.add(future);
            }

            // 모든 업로드 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            // 스레드 풀 정상 종료 처리
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    System.err.println("스레드 풀이 정상적으로 종료되지 않았습니다.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("스레드 풀 종료 대기 중 인터럽트 발생");
            }
        }

        printExecutionTime("Platform Thread", start);
    }

    /**
     * Virtual Thread를 사용한 처리 방식
     * - 경량 스레드를 활용한 높은 동시성 처리
     * - Structured Concurrency 패턴 적용
     */
    public static void virtualThreadApproach(int imageCount) {
        System.out.println("\n=== Virtual Thread 방식 ===");
        Instant start = Instant.now();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // 각 이미지에 대한 가상 스레드 생성 및 실행
            for (int i = 0; i < imageCount; i++) {
                final String imageName = String.format("이미지-%d", i);
                scope.fork(() -> {
                    simulateImageUpload(imageName);
                    return null;
                });
            }

            // 모든 작업 완료 대기 및 예외 처리
            try {
                scope.join().throwIfFailed();
            } catch (ExecutionException e) {
                System.err.println("이미지 업로드 중 오류 발생: " + e.getCause().getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Virtual Thread 작업 중 인터럽트 발생");
        }

        printExecutionTime("Virtual Thread", start);
    }

    private static void printExecutionTime(String approach, Instant start) {
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("%s 방식 처리 완료 시간: %d 밀리초%n", approach, duration.toMillis());
    }

    /**
     * 성능 테스트 실행
     */
    public static void main(String[] args) {
        int[] testSizes = {10, 50, 100, 500}; // 다양한 크기로 테스트

        for (int size : testSizes) {
            System.out.println("\n============================================");
            System.out.printf("동시 업로드 테스트 (이미지 개수: %d)%n", size);
            System.out.println("============================================");

            // Platform Thread 테스트
            platformThreadApproach(size);

            // 테스트 간 간격
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Virtual Thread 테스트
            virtualThreadApproach(size);
        }
    }
}