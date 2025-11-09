package cyou.mayloves.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * 数据库连接工具类
 */
public class DBUtil {
    private static String DRIVER;
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;

    static {
        try {
            // 加载 YAML 配置文件
            loadConfig();
            // 加载数据库驱动
            Class.forName(DRIVER);
            System.out.println("MySQL驱动加载成功");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据库配置加载失败", e);
        }
    }

    /**
     * 从环境变量或 YAML 配置文件加载数据库配置
     * 优先使用环境变量，如果环境变量不存在则使用配置文件
     */
    @SuppressWarnings("unchecked")
    private static void loadConfig() {
        try {
            // 优先从环境变量读取配置
            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String database = System.getenv("DB_DATABASE");
            String username = System.getenv("DB_USERNAME");
            String password = System.getenv("DB_PASSWORD");
            
            // 如果环境变量存在，使用环境变量配置
            if (host != null && port != null && database != null && username != null && password != null) {
                DRIVER = "com.mysql.cj.jdbc.Driver";
                USERNAME = username;
                PASSWORD = password;
                
                // 构建连接参数（使用默认值）
                String urlParams = "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=5000";
                URL = "jdbc:mysql://" + host + ":" + port + "/" + database + urlParams;
                
                System.out.println("从环境变量加载数据库配置: " + host + ":" + port + "/" + database);
                return;
            }
            
            // 如果环境变量不存在，从配置文件读取
            InputStream inputStream = DBUtil.class.getClassLoader()
                    .getResourceAsStream("application.yml");
            if (inputStream == null) {
                throw new RuntimeException("找不到配置文件 application.yml，且未设置环境变量");
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            Map<String, Object> databaseConfig = (Map<String, Object>) config.get("database");

            if (databaseConfig == null) {
                throw new RuntimeException("配置文件中缺少 database 配置项");
            }

            // 读取基本配置
            DRIVER = (String) databaseConfig.get("driver");
            host = (String) databaseConfig.get("host");
            port = String.valueOf(databaseConfig.get("port"));
            database = (String) databaseConfig.get("database");
            USERNAME = (String) databaseConfig.get("username");
            PASSWORD = (String) databaseConfig.get("password");

            // 读取连接参数
            Map<String, Object> parameters = (Map<String, Object>) databaseConfig.get("parameters");
            StringBuilder urlParams = new StringBuilder();
            if (parameters != null) {
                boolean first = true;
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    if (first) {
                        urlParams.append("?");
                        first = false;
                    } else {
                        urlParams.append("&");
                    }
                    urlParams.append(entry.getKey()).append("=").append(entry.getValue());
                }
            }

            // 构建完整的数据库连接 URL
            URL = "jdbc:mysql://" + host + ":" + port + "/" + database + urlParams.toString();

            inputStream.close();
            System.out.println("从配置文件加载数据库配置: " + host + ":" + port + "/" + database);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("加载数据库配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 关闭数据库连接
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

