package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.sql.*;

public class ProcessInstanceImporter {

  public static void batchProcessInstanceInsertOrUpdate(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    final Intent intent = record.getIntent();
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      String sql = "";
      if (intent == ProcessInstanceIntent.ELEMENT_ACTIVATED) {
        sql =
            String.format(
                "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select KEY_, BPMN_PROCESS_ID_, "
                    + " PROCESS_DEFINITION_KEY_, START_,END_, PARTITION_ID_,  VERSION_, STATE_,"
                    + " PARENT_PROCESS_INSTANCE_KEY_, PARENT_ELEMENT_INSTANCE_KEY_"
                    + " from input('KEY_ UInt64,BPMN_PROCESS_ID_ String,"
                    + " PROCESS_DEFINITION_KEY_ UInt64, START_ DateTime64(3), END_ Nullable(DateTime64(3)),"
                    + " PARTITION_ID_ UInt16, VERSION_ UInt16,STATE_ String, "
                    + " PARENT_PROCESS_INSTANCE_KEY_ Int64, PARENT_ELEMENT_INSTANCE_KEY_ Int64')",
                table);
      } else {
        sql =
            String.format(
                "alter table %1$s update END_ = toDateTime64(?/1000,3),STATE_ =? where KEY_ = ?",
                table);
      }

      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final ProcessInstanceRecordValue processInstance =
            (ProcessInstanceRecordValue) record.getValue();
        if (BpmnElementType.PROCESS.name().equals(processInstance.getBpmnElementType().name())) {
          if (intent == ProcessInstanceIntent.ELEMENT_ACTIVATED) {
            // 流程实例Id
            ps.setLong(1, record.getKey());
            // 流程定义键值
            ps.setString(2, processInstance.getBpmnProcessId());
            // 流程定义Id
            ps.setLong(3, processInstance.getProcessDefinitionKey());
            // 创建时间
            ps.setLong(4, record.getTimestamp());
            ps.setString(5, null);
            // 所属分区
            ps.setInt(6, record.getPartitionId());
            // 版本号
            ps.setInt(7, processInstance.getVersion());
            // 状态
            ps.setString(8, "Active");
            // 父流程实例Id
            ps.setLong(9, processInstance.getParentProcessInstanceKey());
            // 父节点实例Id
            ps.setLong(10, processInstance.getParentElementInstanceKey());
            ps.addBatch();
            ps.executeBatch();
          } else if (intent == ProcessInstanceIntent.ELEMENT_COMPLETED) {
            // 结束时间
            ps.setLong(1, record.getTimestamp());
            // 状态
            ps.setString(2, "Completed");
            // 流程实例Id
            ps.setLong(3, record.getKey());
            // System.out.println("record.getTimestamp()->"+record.getTimestamp());
            ps.execute();
          } else if (intent == ProcessInstanceIntent.ELEMENT_TERMINATED) {
            // 结束时间
            ps.setLong(1, record.getTimestamp());
            // 状态
            ps.setString(2, "Terminated");
            // 流程实例Id
            ps.setLong(3, record.getKey());

            ps.execute();
          }
        }
      }
    }
  }

  public static void batchElementInstanceInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s select ID,KEY_, BPMN_PROCESS_ID_, PROCESS_DEFINITION_KEY_, "
                  + " TIMESTAMP_,INTENT_ ,PARTITION_ID_,"
                  + " POSITION_, PROCESS_INSTANCE_KEY_,"
                  + " FLOW_SCOPE_KEY_,ELEMENT_ID_,BPMN_ELEMENT_TYPE_"
                  + " from input('ID String,KEY_ UInt64,BPMN_PROCESS_ID_ String,"
                  + " PROCESS_DEFINITION_KEY_ UInt64, TIMESTAMP_ DateTime64(3), "
                  + " INTENT_ String,PARTITION_ID_ UInt16,"
                  + " POSITION_ UInt64, PROCESS_INSTANCE_KEY_ UInt64,"
                  + " FLOW_SCOPE_KEY_ Int64,ELEMENT_ID_ String,BPMN_ELEMENT_TYPE_ String')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {

        final ProcessInstanceRecordValue processInstance =
            (ProcessInstanceRecordValue) record.getValue();
        // 记录Id
        ps.setString(1, record.getPartitionId() + "-" + record.getPosition());
        // 元素记录Id
        ps.setLong(2, record.getKey());
        // 流程定义键值
        ps.setString(3, processInstance.getBpmnProcessId());
        // 流程定义Id
        ps.setLong(4, processInstance.getProcessDefinitionKey());
        // 创建时间
        ps.setLong(5, record.getTimestamp());
        // 活动状态
        ps.setString(6, record.getIntent().name());
        // 所属分区
        ps.setInt(7, record.getPartitionId());
        // 版本号
        ps.setLong(8, record.getPosition());
        // 流程实例Id
        ps.setLong(9, processInstance.getProcessInstanceKey());
        // 范围Id
        ps.setLong(10, processInstance.getFlowScopeKey());
        // 元素定义Id
        ps.setString(11, processInstance.getElementId());
        // 元素定义类型
        ps.setString(12, processInstance.getBpmnElementType().name());
        ps.addBatch();
        ps.executeBatch();
      } catch (final SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
