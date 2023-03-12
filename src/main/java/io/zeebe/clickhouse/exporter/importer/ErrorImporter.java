package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.ErrorRecordValue;
import java.sql.*;

public class ErrorImporter {
  public static void batchErrorInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select POSITION_, ERROR_EVENT_POSITION_, "
                  + " TIMESTAMP_, PROCESS_INSTANCE_KEY_, EXCEPTION_MESSAGE_,STACKTRACE_"
                  + " from input('POSITION_ UInt64,ERROR_EVENT_POSITION_ UInt64,"
                  + " TIMESTAMP_ DateTime64(3), "
                  + " PROCESS_INSTANCE_KEY_ UInt64,EXCEPTION_MESSAGE_ String,"
                  + " STACKTRACE_ String')",
              table);

      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final ErrorRecordValue error = (ErrorRecordValue) record.getValue();
        // 记录Id
        ps.setLong(1, record.getPosition());
        // 发生位置
        ps.setLong(2, error.getErrorEventPosition());
        // 记录时间
        ps.setLong(3, record.getTimestamp());
        // 流程实例Id
        ps.setLong(4, error.getProcessInstanceKey());
        // 异常信息
        ps.setString(5, error.getExceptionMessage());
        // 堆栈
        ps.setString(6, error.getStacktrace());
        ps.addBatch();
        ps.executeBatch();
      }
    }
  }
}
