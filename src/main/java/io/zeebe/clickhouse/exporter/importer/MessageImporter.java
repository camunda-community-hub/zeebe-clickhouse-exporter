package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import java.sql.*;

public class MessageImporter {
  public static void batchMessageInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select KEY_, NAME_, "
                  + " TIMESTAMP_, STATE_, CORRELATION_KEY_,MESSAGE_ID_,PAYLOAD_"
                  + " from input('KEY_ UInt64,NAME_ String,"
                  + " TIMESTAMP_ DateTime64(3), STATE_ String,"
                  + " CORRELATION_KEY_ String,MESSAGE_ID_ String,"
                  + " PAYLOAD_ String')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final MessageRecordValue msg = (MessageRecordValue) record.getValue();
        // 记录Id
        ps.setLong(1, record.getKey());
        // 名称
        ps.setString(2, msg.getName());
        // 记录时间
        ps.setLong(3, record.getTimestamp());
        // 状态
        ps.setString(4, record.getIntent().name().toLowerCase());
        // 关联key
        ps.setString(5, msg.getCorrelationKey());
        // 消息id
        ps.setString(6, msg.getMessageId());
        // 报文
        ps.setString(7, msg.getVariables().toString());
        ps.addBatch();
        ps.executeBatch();
      }
    }
  }
}
