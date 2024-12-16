import java.sql.*;

public class CompositeKeyTest {
    public static void main(String[] args) throws SQLException {
        // MariaDB 연결 정보
        String url = "jdbc:mariadb://homepage.ct8omig282in.ap-northeast-2.rds.amazonaws.com/back";
        String user = "admin";
        String password = "ussum123!";

        try {
            // DB 연결
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();

            // 1. DEFAULT 값이 없는 테이블 생성
            System.out.println("\n1. DEFAULT 값이 없는 테이블 생성");
            String createTableSql1 =
                    "CREATE TABLE notification_history (" +
                            "   HISTORY_SEQ INTEGER NOT NULL AUTO_INCREMENT," +
                            "   CREATED_AT datetime NOT NULL," +  // DEFAULT 값 없음
                            "   content VARCHAR(255)," +
                            "   PRIMARY KEY (HISTORY_SEQ, CREATED_AT)" +
                            ")";
            stmt.execute(createTableSql1);
            System.out.println("테이블 생성 완료");

            // 2. INSERT 실패 케이스
            System.out.println("\n2. CREATED_AT NULL로 INSERT 시도");
            String insertSql1 =
                    "INSERT INTO notification_history" +
                            "(CREATED_AT, content) VALUES" +
                            "(NULL, 'test content')";
            try {
                stmt.execute(insertSql1);
            } catch (Exception e) {
                System.out.println("INSERT 실패: " + e.getMessage());
                // 복합키의 일부가 NULL이므로 실패
            }

            // 3. DEFAULT 값이 있는 테이블로 재생성
            System.out.println("\n3. DEFAULT 값이 있는 테이블로 재생성");
            stmt.execute("DROP TABLE notification_history");
            String createTableSql2 =
                    "CREATE TABLE notification_history (" +
                            "   HISTORY_SEQ INTEGER NOT NULL AUTO_INCREMENT," +
                            "   CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," + // DEFAULT 값 추가
                            "   content VARCHAR(255)," +
                            "   PRIMARY KEY (HISTORY_SEQ, CREATED_AT)" +
                            ")";
            stmt.execute(createTableSql2);
            System.out.println("테이블 재생성 완료");

            // 4. INSERT 성공 케이스
            System.out.println("\n4. DEFAULT 값이 있는 테이블에 INSERT 시도");
            String insertSql2 =
                    "INSERT INTO notification_history" +
                            "(content) VALUES" +
                            "('test content')";
            stmt.execute(insertSql2);
            System.out.println("INSERT 성공");

            // 5. 결과 확인
            System.out.println("\n5. INSERT된 데이터 확인");
            ResultSet rs = stmt.executeQuery("SELECT * FROM notification_history");
            while(rs.next()) {
                System.out.println("HISTORY_SEQ: " + rs.getInt("HISTORY_SEQ"));
                System.out.println("CREATED_AT: " + rs.getTimestamp("CREATED_AT"));
                System.out.println("content: " + rs.getString("content"));
            }

            // 6. 여러 건 INSERT 테스트
            System.out.println("\n6. 여러 건 INSERT 테스트");
            for(int i = 0; i < 3; i++) {
                stmt.execute(
                        "INSERT INTO notification_history(content) " +
                                "VALUES('test content " + (i+1) + "')"
                );
            }

            // 7. 최종 결과 확인
            System.out.println("\n7. 최종 데이터 확인");
            rs = stmt.executeQuery("SELECT * FROM notification_history");
            while(rs.next()) {
                System.out.println("=============================");
                System.out.println("HISTORY_SEQ: " + rs.getInt("HISTORY_SEQ"));
                System.out.println("CREATED_AT: " + rs.getTimestamp("CREATED_AT"));
                System.out.println("content: " + rs.getString("content"));
            }

            // 리소스 정리
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("데이터베이스 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}