/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ProcessImporter {
  public static void batchProcessInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s SETTINGS async_insert=1, wait_for_async_insert=0 select KEY_,"
                  + " BPMN_PROCESS_ID_, RESOURCE_NAME_, "
                  + " TIMESTAMP_, PARTITION_ID_, POSITION_,  VERSION_, RESOURCE_"
                  + " from input('KEY_ UInt64, BPMN_PROCESS_ID_ String, RESOURCE_NAME_ String,"
                  + " TIMESTAMP_ DateTime64(3), PARTITION_ID_ UInt16, POSITION_ UInt64,VERSION_ UInt16,RESOURCE_ String')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final Process process = (Process) record.getValue();
        // 流程定义Id
        ps.setLong(1, record.getKey());
        // 流程定义键值
        ps.setString(2, process.getBpmnProcessId());
        // 流程定义文件名
        ps.setString(3, process.getResourceName());
        // 部署时间
        ps.setLong(4, record.getTimestamp());
        // 所属分区
        ps.setInt(5, record.getPartitionId());
        // 位置
        ps.setLong(6, record.getPosition());
        // 版本号
        ps.setInt(7, process.getVersion());
        // 模型定义信息
        final String resource = new String(process.getResource(), StandardCharsets.UTF_8);
        ps.setString(8, resource);

        ps.addBatch();
        ps.executeBatch();
      }
    }
  }
}
