package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import java.sql.*;

public class TimerImporter {
  public static void batchTimerInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select KEY_, STATE_, REPETITIONS, "
                  + " TIMESTAMP_, DUE_DATE_, PROCESS_INSTANCE_KEY_, PROCESS_DEFINITION_KEY_,ELEMENT_INSTANCE_KEY_, TARGET_ELEMENT_ID_"
                  + " from input('KEY_ UInt64,STATE_ String, REPETITIONS Int16,"
                  + " TIMESTAMP_ DateTime64(3), DUE_DATE_ DateTime64(3), "
                  + " PROCESS_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ UInt64,"
                  + " ELEMENT_INSTANCE_KEY_ Int64, TARGET_ELEMENT_ID_ String')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final TimerRecordValue timer = (TimerRecordValue) record.getValue();
        // 记录Id
        ps.setLong(1, record.getKey());
        // 状态
        ps.setString(2, record.getIntent().name().toLowerCase());
        // 重复次数
        ps.setInt(3, timer.getRepetitions());
        // 记录时间
        ps.setLong(4, record.getTimestamp());
        // 截止时间
        ps.setLong(5, timer.getDueDate());
        // 流程实例Id
        ps.setLong(6, timer.getProcessInstanceKey());
        // 流程定义id
        ps.setLong(7, timer.getProcessDefinitionKey());
        // 节点实例Id
        ps.setLong(8, timer.getElementInstanceKey());
        // 节点Id
        ps.setString(9, timer.getTargetElementId());

        ps.addBatch();
        ps.executeBatch();

      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}
