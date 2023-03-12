package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageSubscriptionRecordValue;
import java.sql.*;
import java.util.UUID;

public class MessageSubscriptionImporter {
  public static void batchMessageSubscriptionInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select ID_, MESSAGE_NAME_, "
                  + " TIMESTAMP_, STATE_, PROCESS_INSTANCE_KEY_,ELEMENT_INSTANCE_KEY_,"
                  + "  PROCESS_DEFINITION_KEY_,CORRELATION_KEY_,TARGET_FLOW_NODE_ID_"
                  + " from input('ID_ String,MESSAGE_NAME_ String,"
                  + " TIMESTAMP_ DateTime64(3), STATE_ String,"
                  + " PROCESS_INSTANCE_KEY_ Int64,ELEMENT_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ Int64,"
                  + " CORRELATION_KEY_ Nullable(String),TARGET_FLOW_NODE_ID_ Nullable(String)')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final MessageSubscriptionRecordValue msg =
            (MessageSubscriptionRecordValue) record.getValue();
        // 记录Id
        ps.setString(1, generateId());
        // 名称
        ps.setString(2, msg.getMessageName());
        // 记录时间
        ps.setLong(3, record.getTimestamp());
        // 状态
        ps.setString(4, record.getIntent().name().toLowerCase());
        ps.setLong(5, msg.getProcessInstanceKey());
        ps.setLong(6, msg.getElementInstanceKey());

        ps.setLong(7, -1);
        // 关联key
        ps.setString(8, msg.getCorrelationKey());

        ps.setString(9, null);
        ps.addBatch();
        ps.executeBatch();
      }
    }
  }

  public static void batchMessageStartEventSubscriptionInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s select ID_, MESSAGE_NAME_,MESSAGE_KEY_,"
                  + " TIMESTAMP_, STATE_, PROCESS_INSTANCE_KEY_,ELEMENT_INSTANCE_KEY_,"
                  + "  PROCESS_DEFINITION_KEY_,CORRELATION_KEY_,TARGET_FLOW_NODE_ID_"
                  + " from input('ID_ String,MESSAGE_NAME_ String,MESSAGE_KEY_ Int64,"
                  + " TIMESTAMP_ DateTime64(3), STATE_ String,"
                  + " PROCESS_INSTANCE_KEY_ Int64,ELEMENT_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ Int64,"
                  + " CORRELATION_KEY_ Nullable(String),TARGET_FLOW_NODE_ID_ Nullable(String)')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final MessageStartEventSubscriptionRecordValue msg =
            (MessageStartEventSubscriptionRecordValue) record.getValue();
        // 记录Id
        ps.setString(1, generateId());
        // 消息名称
        ps.setString(2, msg.getMessageName());
        // 消息实例Key
        ps.setLong(3, msg.getMessageKey());
        // 记录时间
        ps.setLong(4, record.getTimestamp());
        // 状态
        ps.setString(5, record.getIntent().name().toLowerCase());
        ps.setLong(6, msg.getProcessInstanceKey());
        ps.setLong(7, -1);

        ps.setLong(8, msg.getProcessDefinitionKey());
        // 关联key
        ps.setString(9, msg.getCorrelationKey());

        ps.setString(10, msg.getStartEventId());
        ps.addBatch();
        ps.executeBatch();
      }
    }
  }

  private static String generateId() {
    return UUID.randomUUID().toString();
  }
}
