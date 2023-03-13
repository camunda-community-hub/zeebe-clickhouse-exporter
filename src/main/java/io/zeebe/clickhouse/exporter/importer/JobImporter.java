package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.sql.*;

public class JobImporter {
  public static void batchJobInsertOrUpdate(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    final Intent intent = record.getIntent();
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      String sql = "";
      if (intent == JobIntent.CREATED) {
        sql =
            String.format(
                "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select KEY_, BPMN_PROCESS_ID_, "
                    + " ELEMENT_ID_, WORKER_, JOB_TYPE_, STATE_, RETRIES_, START_, END_, PROCESS_INSTANCE_KEY_, "
                    + " ELEMENT_INSTANCE_KEY_,  PROCESS_DEFINITION_KEY_ "
                    + " from input('KEY_ UInt64,BPMN_PROCESS_ID_ String, ELEMENT_ID_ String,"
                    + " WORKER_ String,JOB_TYPE_ String,STATE_ String, RETRIES_ UInt8,"
                    + " START_ DateTime64(3), END_ Nullable(DateTime64(3)), PROCESS_INSTANCE_KEY_ UInt64, "
                    + " ELEMENT_INSTANCE_KEY_ UInt64, PROCESS_DEFINITION_KEY_ UInt64')",
                table);
      } else {
        sql =
            String.format(
                "alter table %1$s update END_ = toDateTime64(?/1000,3), "
                    + " WORKER_ =?, RETRIES_=?, STATE_=? "
                    + " where KEY_ = ?",
                table);
      }
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final JobRecordValue job = (JobRecordValue) record.getValue();
        if (intent == JobIntent.CREATED) {
          // 记录Id
          ps.setLong(1, record.getKey());
          // 流程定义标识
          ps.setString(2, job.getBpmnProcessId());
          // 节点定义Id
          ps.setString(3, job.getElementId());
          // 任务处理器
          ps.setString(4, job.getWorker());
          // 任务类型
          ps.setString(5, job.getType());
          // 状态
          ps.setString(6, record.getIntent().name().toLowerCase());
          // 重试次数
          ps.setInt(7, job.getRetries());
          // 开始时间
          ps.setLong(8, record.getTimestamp());
          // 最后更新时间
          ps.setString(9, null);
          // 版本号
          ps.setLong(10, job.getProcessInstanceKey());
          // 节点实例Id
          ps.setLong(11, job.getElementInstanceKey());
          // 流程定义Id
          ps.setLong(12, job.getProcessDefinitionKey());

          ps.addBatch();
          ps.executeBatch();
        } else {
          // 更新时间
          ps.setLong(1, record.getTimestamp());
          // 任务处理器
          ps.setString(2, job.getWorker());
          // 重试次数
          ps.setInt(3, job.getRetries());
          // 状态
          ps.setString(4, record.getIntent().name().toLowerCase());
          // 记录Id
          ps.setLong(5, record.getKey());

          ps.execute();
        }
      }
    }
  }
}
