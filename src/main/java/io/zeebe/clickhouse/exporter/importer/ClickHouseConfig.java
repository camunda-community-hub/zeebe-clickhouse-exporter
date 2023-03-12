package io.zeebe.clickhouse.exporter.importer;

import java.sql.*;

public class ClickHouseConfig {

  public static void CreateClickHouseConfigTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ String,POSITION_ Int64) "
                  + " engine = MergeTree() ORDER BY KEY_",
              table));
    }
  }

  public static void InitClickHouseConfigTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql = String.format("insert into %1$s values(?, ?)", table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, "cfg");
        ps.setLong(2, -1);
        ps.execute();
      }
    }
  }

  public static void updateClickHouseConfigTable(
      final String url,
      final String user,
      final String password,
      final String table,
      final long lastPosition)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password)) {
      final String sql =
          String.format("alter table %1$s update POSITION_ = ? where KEY_ = ?", table);
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, lastPosition);

        ps.setString(2, "cfg");
        ps.execute();
      }
    }
  }

  public static long queryClickHouseConfig(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery("select * from " + table)) {
      long count = 0;
      long cfgposition = -1L;
      while (rs.next()) {
        count++;
        cfgposition = rs.getLong("POSITION_");
      }
      if (count > 0) {
        count = cfgposition;
      }
      return count;
    }
  }

  public static void CreateProcessTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ UInt64,BPMN_PROCESS_ID_ String, RESOURCE_NAME_ String,"
                  + " TIMESTAMP_ DateTime64(3), PARTITION_ID_ UInt16, POSITION_ UInt64,VERSION_ UInt16,RESOURCE_ String)"
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY KEY_",
              table));
    }
  }

  public static void CreateProcessInstanceTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ UInt64,BPMN_PROCESS_ID_ String,"
                  + " PROCESS_DEFINITION_KEY_ UInt64, START_ DateTime64(3), "
                  + " END_ Nullable(DateTime64(3)),PARTITION_ID_ UInt16,"
                  + " VERSION_ UInt16,STATE_ String, PARENT_PROCESS_INSTANCE_KEY_ Int64,"
                  + " PARENT_ELEMENT_INSTANCE_KEY_ Int64)"
                  + " engine = ReplacingMergeTree(START_) PARTITION BY toYYYYMM(START_) ORDER BY KEY_",
              table));
    }
  }

  public static void CreateErrorTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (POSITION_ UInt64,ERROR_EVENT_POSITION_ UInt64,"
                  + " TIMESTAMP_ DateTime64(3),"
                  + " PROCESS_INSTANCE_KEY_ UInt64,EXCEPTION_MESSAGE_ String, STACKTRACE_ String) "
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY POSITION_",
              table));
    }
  }

  public static void CreateTimerTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ UInt64,STATE_ String, REPETITIONS Int16,"
                  + " TIMESTAMP_ DateTime64(3),DUE_DATE_ DateTime64(3),"
                  + " PROCESS_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ UInt64,"
                  + " ELEMENT_INSTANCE_KEY_ Int64, TARGET_ELEMENT_ID_ String) "
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY KEY_",
              table));
    }
  }

  public static void CreateMessageTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ UInt64,NAME_ String,"
                  + " TIMESTAMP_ DateTime64(3),STATE_ String,"
                  + " CORRELATION_KEY_ String,MESSAGE_ID_ String, PAYLOAD_ String) "
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY KEY_",
              table));
    }
  }

  public static void CreateMessageSubscriptionTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (ID_ String,MESSAGE_NAME_ String,MESSAGE_KEY_ Int64,"
                  + " TIMESTAMP_ DateTime64(3),STATE_ String,PROCESS_INSTANCE_KEY_ Int64,"
                  + " ELEMENT_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ Int64,"
                  + " CORRELATION_KEY_ Nullable(String),TARGET_FLOW_NODE_ID_ Nullable(String)) "
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY ID_",
              table));
    }
  }

  public static void CreateElementInstanceTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (ID String,KEY_ UInt64,BPMN_PROCESS_ID_ String,"
                  + " PROCESS_DEFINITION_KEY_ UInt64, TIMESTAMP_ DateTime64(3), "
                  + " INTENT_ String,PARTITION_ID_ UInt16,"
                  + " POSITION_ UInt64, PROCESS_INSTANCE_KEY_ UInt64,"
                  + " FLOW_SCOPE_KEY_ Int64,ELEMENT_ID_ String,BPMN_ELEMENT_TYPE_ String)"
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY (ID,INTENT_)",
              table));
    }
  }

  public static void CreateJobTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ UInt64,BPMN_PROCESS_ID_ String, ELEMENT_ID_ String,"
                  + " WORKER_ String, JOB_TYPE_ String, STATE_ String, RETRIES_ UInt8, "
                  + " START_ DateTime64(3),END_ Nullable(DateTime64(3)), PROCESS_INSTANCE_KEY_ UInt64,"
                  + "  ELEMENT_INSTANCE_KEY_ UInt64, PROCESS_DEFINITION_KEY_ UInt64)"
                  + " engine = ReplacingMergeTree(START_) PARTITION BY toYYYYMM(START_)  ORDER BY KEY_",
              table));
    }
  }

  public static void CreateVariableTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (ID String,NAME_ String, VALUE_ String,"
                  + " TIMESTAMP_ DateTime64(3), PARTITION_ID_ UInt16, POSITION_ UInt64,"
                  + " PROCESS_INSTANCE_KEY_ UInt64,PROCESS_DEFINITION_KEY_ UInt64,"
                  + " SCOPE_KEY_ Int64, STATE_ String) "
                  + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY (PARTITION_ID_,POSITION_)",
              table));
    }
  }

  public static void CreateIncidentTable(
      final String url, final String user, final String password, final String table)
      throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement stmt = conn.createStatement()) {
      stmt.execute(
          String.format(
              "create table if not exists %1$s (KEY_ UInt64,BPMN_PROCESS_ID_ String, ERROR_MSG_ String,"
                  + " ERROR_TYPE_ String, "
                  + " CREATED_ DateTime64(3),RESOLVED_ Nullable(DateTime64(3)), "
                  + " PROCESS_INSTANCE_KEY_ UInt64, ELEMENT_INSTANCE_KEY_ UInt64,JOB_KEY_ UInt64,"
                  + " PROCESS_DEFINITION_KEY_ UInt64)"
                  + " engine = ReplacingMergeTree(CREATED_) PARTITION BY toYYYYMM(CREATED_)  ORDER BY KEY_",
              table));
    }
  }


  public static void CreateSignalSubscriptionTable(
          final String url, final String user, final String password, final String table)
          throws SQLException {
    try (final Connection conn = DriverManager.getConnection(url, user, password);
         final Statement stmt = conn.createStatement()) {
      stmt.execute(
              String.format(
                      "create table if not exists %1$s (ID_ String,SIGNAL_NAME_ String,"
                              + " TIMESTAMP_ DateTime64(3),STATE_ String,"
                              + " CATCH_ELEMENT_INSTANCE_KEY_ Int64,PROCESS_DEFINITION_KEY_ Int64,"
                              + " BPMN_PROCESS_ID_ String,CATCH_EVENT_ID_ String) "
                              + " engine = ReplacingMergeTree(TIMESTAMP_) PARTITION BY toYYYYMM(TIMESTAMP_)  ORDER BY ID_",
                      table));
    }
  }

}
