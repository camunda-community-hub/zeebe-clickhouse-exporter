/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.sql.*;

public class IncidentImporter {
  public static void batchIncidentInsertOrUpdate(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    final Intent intent = record.getIntent();
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      String sql = "";

      if (intent == IncidentIntent.CREATED) {
        sql =
            String.format(
                "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select KEY_, BPMN_PROCESS_ID_, ERROR_MSG_, "
                    + " ERROR_TYPE_, CREATED_, RESOLVED_, PROCESS_INSTANCE_KEY_, "
                    + " ELEMENT_INSTANCE_KEY_, JOB_KEY_, PROCESS_DEFINITION_KEY_ "
                    + " from input('KEY_ UInt64,BPMN_PROCESS_ID_ String, ERROR_MSG_ String,"
                    + " ERROR_TYPE_ String,"
                    + " CREATED_ DateTime64(3), RESOLVED_ Nullable(DateTime64(3)), PROCESS_INSTANCE_KEY_ UInt64, "
                    + " ELEMENT_INSTANCE_KEY_ UInt64, JOB_KEY_ UInt64, PROCESS_DEFINITION_KEY_ UInt64')",
                table);
      } else if (intent == IncidentIntent.RESOLVED) {
        sql =
            String.format(
                "alter table %1$s update RESOLVED_ = toDateTime64(?/1000,3) where KEY_ = ?", table);
      }
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final IncidentRecordValue incident = (IncidentRecordValue) record.getValue();
        if (intent == IncidentIntent.CREATED) {
          // 记录Id
          ps.setLong(1, record.getKey());
          // 流程定义标识
          ps.setString(2, incident.getBpmnProcessId());
          // 节点定义Id
          ps.setString(3, incident.getErrorMessage());
          // 任务处理器
          ps.setString(4, incident.getErrorType().name());
          // 开始时间
          ps.setLong(5, record.getTimestamp());
          // 最后更新时间
          ps.setString(6, null);
          // 版本号
          ps.setLong(7, incident.getProcessInstanceKey());
          // 节点实例Id
          ps.setLong(8, incident.getElementInstanceKey());
          // 节点实例Id
          ps.setLong(9, incident.getJobKey());
          // 流程定义Id
          ps.setLong(10, incident.getProcessDefinitionKey());

          ps.addBatch();
          ps.executeBatch();
        } else if (intent == IncidentIntent.RESOLVED) {
          // 更新时间
          ps.setLong(1, record.getTimestamp());
          // 记录Id
          ps.setLong(2, record.getKey());

          System.out.println("incident----->record.getTimestamp()->" + record.getTimestamp());
          ps.execute();
        }
      }
    }
  }
}
