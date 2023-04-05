package io.zeebe.clickhouse.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.clickhouse.data.ClickHouseDataType;
import java.sql.*;
import org.junit.Test;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class ClickHouseExporterIT {

  @Container
  public ClickHouseContainer clickHouse =
      new ClickHouseContainer(DockerImageName.parse("clickhouse/clickhouse-server:22.8.15.23-alpine"))
          .withExposedPorts(8123);

  @Test
  public void testSimple() throws SQLException {
    clickHouse.start();

    String sql = "select cast(1 as UInt64) a, cast([1, 2] as Array(Int8)) b";
    try (final Connection conn =
            DriverManager.getConnection(
                clickHouse.getJdbcUrl(), clickHouse.getUsername(), clickHouse.getPassword());
        final Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery(sql)) {

      assertThat(rs.next()).isTrue();
      assertThat(rs.getMetaData().getColumnTypeName(1)).isEqualTo(ClickHouseDataType.UInt64.name());
      assertThat(rs.getMetaData().getColumnTypeName(2)).isEqualTo("Array(Int8)");
      assertThat(rs.next()).isFalse();
    }
  }
}
