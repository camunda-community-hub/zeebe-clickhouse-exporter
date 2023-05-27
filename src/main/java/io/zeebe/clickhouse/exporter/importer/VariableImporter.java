/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import java.sql.*;

public class VariableImporter {

  public static void batchVariableInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {

      final String sql =
          String.format(
              "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select ID, NAME_, VALUE_, "
                  + " TIMESTAMP_, PARTITION_ID_, POSITION_,  "
                  + " PROCESS_INSTANCE_KEY_, PROCESS_DEFINITION_KEY_,SCOPE_KEY_, STATE_"
                  + " from input('ID String,NAME_ String, VALUE_ String,"
                  + " TIMESTAMP_ DateTime64(3), PARTITION_ID_ UInt16, POSITION_ UInt64,"
                  + " PROCESS_INSTANCE_KEY_ UInt64,PROCESS_DEFINITION_KEY_ UInt64,"
                  + " SCOPE_KEY_ Int64, STATE_ String')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final VariableRecordValue variable = (VariableRecordValue) record.getValue();
        // 记录Id
        ps.setString(1, String.format("%s-%s", record.getPartitionId(), record.getPosition()));
        // ps.setString(1,generateId());
        // 变量名
        ps.setString(2, variable.getName());
        // 变量值
        ps.setString(3, variable.getValue());
        // 记录时间
        ps.setLong(4, record.getTimestamp());
        // 所属分区
        ps.setInt(5, record.getPartitionId());
        // 位置
        ps.setLong(6, record.getPosition());
        // 流程实例Id
        ps.setLong(7, variable.getProcessInstanceKey());
        // 流程定义id
        ps.setLong(8, variable.getProcessDefinitionKey());
        // 所属范围
        ps.setLong(9, variable.getScopeKey());
        // 状态
        ps.setString(10, record.getIntent().name().toLowerCase());

        ps.addBatch();
        ps.executeBatch();
      }
    }
  }
}
