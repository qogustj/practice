import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 전통적인 스레드 방식과 가상 스레드 방식의 이미지 업로드 성능을 비교하는 클래스
 */
public class ImageUploadComparison {
    /**
     * 이미지 업로드를 시뮬레이션하는 메소드
     * 실제 업로드 대신 랜덤한 지연 시간을 발생시켜 I/O 작업을 모방
     */
    private static void simulateImageUpload(String imageName) {
        try {
            // 100-300ms 사이의 랜덤한 업로드 시간 시뮬레이션
            Thread.sleep((long) (Math.random() * 200 + 100));
            System.out.println("업로드된 이미지: " + imageName + ", 사용된 스레드: " + Thread.currentThread());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 전통적인 CompletableFuture를 사용한 비동기 처리 방식
     * 고정 크기의 스레드 풀을 사용하여 이미지 업로드를 처리
     */
    public static void traditionalThreadApproach(int imageCount) {
        System.out.println("\n=== 전통적인 스레드 방식 ===");
        Instant start = Instant.now();

        // CPU 코어 수만큼의 고정 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 각 이미지에 대한 비동기 업로드 작업 생성
        for (int i = 0; i < imageCount; i++) {
            final String imageName = "이미지-" + i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> simulateImageUpload(imageName),
                    executorService
            );
            futures.add(future);
        }

        // 모든 업로드 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 스레드 풀 정상 종료
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 실행 시간 측정 및 출력
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("전통적 방식 처리 완료 시간: %d 밀리초%n", duration.toMillis());
    }

    /**
     * Java 21의 가상 스레드를 사용한 처리 방식
     * 경량화된 가상 스레드를 사용하여 이미지 업로드를 처리
     */
    public static void virtualThreadApproach(int imageCount) {
        System.out.println("\n=== 가상 스레드 방식 ===");
        Instant start = Instant.now();

        List<Thread> threads = new ArrayList<>();
        // 완료된 업로드 수를 추적하기 위한 원자성 카운터
        AtomicInteger completedCount = new AtomicInteger(0);

        // 각 이미지에 대한 가상 스레드 생성 및 실행
        for (int i = 0; i < imageCount; i++) {
            final String imageName = "이미지-" + i;
            Thread virtualThread = Thread.startVirtualThread(() -> {
                simulateImageUpload(imageName);
                completedCount.incrementAndGet();
            });
            threads.add(virtualThread);
        }

        // 모든 가상 스레드의 작업 완료 대기
        while (completedCount.get() < imageCount) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 실행 시간 측정 및 출력
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("가상 스레드 방식 처리 완료 시간: %d 밀리초%n", duration.toMillis());
    }

    /**
     * 메인 메소드: 두 가지 방식의 성능을 다양한 이미지 수로 테스트
     */
    public static void main(String[] args) {
        // 서로 다른 동시 업로드 수로 테스트 실행
        int[] testSizes = {10, 50, 100};

        for (int size : testSizes) {
            System.out.println("\n========================================");
            System.out.println(size + "개의 동시 업로드 테스트");
            System.out.println("========================================");

            // 전통적 방식 테스트
            traditionalThreadApproach(size);

            // 테스트 간 간격을 두어 리소스 정리 시간 확보
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