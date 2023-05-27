/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter.importer;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.SignalSubscriptionRecordValue;
import java.sql.*;
import java.util.UUID;

public class SignalSubscriptionImporter {

  public static void batchSignalSubscriptionInsert(
      final String url,
      final String user,
      final String password,
      final String table,
      final Record<?> record)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format(
              "insert into %1$s select ID_, SIGNAL_NAME_,"
                  + " TIMESTAMP_, STATE_, CATCH_ELEMENT_INSTANCE_KEY_,"
                  + " PROCESS_DEFINITION_KEY_,BPMN_PROCESS_ID_,CATCH_EVENT_ID_"
                  + " from input('ID_ String,SIGNAL_NAME_ String,"
                  + " TIMESTAMP_ DateTime64(3), STATE_ String,"
                  + " CATCH_ELEMENT_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ Int64,"
                  + " BPMN_PROCESS_ID_ String,CATCH_EVENT_ID_ String')",
              table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        final SignalSubscriptionRecordValue signal =
            (SignalSubscriptionRecordValue) record.getValue();
        // 记录Id
        ps.setString(1, generateId());
        // 信号名称
        ps.setString(2, signal.getSignalName());
        // 记录时间
        ps.setLong(3, record.getTimestamp());
        // 状态
        ps.setString(4, record.getIntent().name().toLowerCase());
        ps.setLong(5, signal.getCatchEventInstanceKey());

        ps.setLong(6, signal.getProcessDefinitionKey());
        // 关联key
        ps.setString(7, signal.getBpmnProcessId());

        ps.setString(8, signal.getCatchEventId());
        ps.addBatch();
        ps.executeBatch();
      }
    }
  }

  private static String generateId() {
    return UUID.randomUUID().toString();
  }
}
