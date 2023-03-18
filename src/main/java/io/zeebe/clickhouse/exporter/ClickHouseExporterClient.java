package io.zeebe.clickhouse.exporter;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.zeebe.clickhouse.exporter.importer.*;
import java.sql.SQLException;
import org.slf4j.Logger;

public class ClickHouseExporterClient {

  private final String elementInstanceTable = "ELEMENT_INSTANCE";
  private final String clickHouseConfigTable = "CLICKHOUSE_CONFIG";

  private final ExporterConfiguration configuration;

  private final Logger logger;
  private long cfgPosition = -1;

  ClickHouseExporterClient(final ExporterConfiguration configuration, final Logger logger) {
    this.configuration = configuration;
    this.logger = logger;
    try {
      // 初始化配置表
      ClickHouseConfig.CreateClickHouseConfigTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          clickHouseConfigTable);
      // 检查是否有初始化配置信息
      final long i =
          ClickHouseConfig.queryClickHouseConfig(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              clickHouseConfigTable);

      if (i <= 0L) {
        // 执行初始化
        ClickHouseConfig.InitClickHouseConfigTable(
            configuration.getChUrl(),
            configuration.getChUser(),
            configuration.getChPassword(),
            clickHouseConfigTable);
      } else {
        cfgPosition = i;
      }
      // 创建流程定义信息表
      ClickHouseConfig.CreateProcessTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.PROCESS.name());
      // 创建流程实例表
      ClickHouseConfig.CreateProcessInstanceTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.PROCESS_INSTANCE.name());
      // 创建任务实例表
      ClickHouseConfig.CreateElementInstanceTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          elementInstanceTable);
      // 创建调度表
      ClickHouseConfig.CreateJobTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.JOB.name());
      // 创建流程变量表
      ClickHouseConfig.CreateVariableTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.VARIABLE.name());
      // 创建事件表
      ClickHouseConfig.CreateIncidentTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.INCIDENT.name());
      // 创建定时器表
      ClickHouseConfig.CreateTimerTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.TIMER.name());
      // 创建异常表
      ClickHouseConfig.CreateErrorTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.ERROR.name());
      // 创建消息表
      ClickHouseConfig.CreateMessageTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.MESSAGE.name());
      // 创建消息订阅表
      ClickHouseConfig.CreateMessageSubscriptionTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.MESSAGE_SUBSCRIPTION.name());
      // 创建信号订阅表
      ClickHouseConfig.CreateSignalSubscriptionTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          ValueType.SIGNAL_SUBSCRIPTION.name());

    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }
  /** 执行数据导出 * */
  public void insert(final Record<?> record, final long lastPostion) {
    if ((lastPostion > cfgPosition || cfgPosition == -1L)
        && RecordType.EVENT.name().equals(record.getRecordType().name())) {

      logger.info(
          String.format("------%s---------->%s", record.getValueType().name(), record.toJson()));
      // 流程定义信息
      if (ValueType.PROCESS.name().equals(record.getValueType().name())) {
        try {

          ProcessImporter.batchProcessInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.PROCESS.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 流程实例信息 & 任务实例信息
      if (ValueType.PROCESS_INSTANCE.name().equals(record.getValueType().name())) {
        try {

          // 流程实例信息
          ProcessInstanceImporter.batchProcessInstanceInsertOrUpdate(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.PROCESS_INSTANCE.name(),
              record);

          // 任务实例信息
          ProcessInstanceImporter.batchElementInstanceInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              elementInstanceTable,
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }

      // 调度信息
      if (ValueType.JOB.name().equals(record.getValueType().name())) {
        try {

          JobImporter.batchJobInsertOrUpdate(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.JOB.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 流程变量信息
      if (ValueType.VARIABLE.name().equals(record.getValueType().name())) {
        try {

          VariableImporter.batchVariableInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.VARIABLE.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 事件信息
      if (ValueType.INCIDENT.name().equals(record.getValueType().name())) {
        try {

          IncidentImporter.batchIncidentInsertOrUpdate(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.INCIDENT.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 定时器信息
      if (ValueType.TIMER.name().equals(record.getValueType().name())) {
        try {

          TimerImporter.batchTimerInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.TIMER.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 异常记录信息
      if (ValueType.ERROR.name().equals(record.getValueType().name())) {
        try {

          ErrorImporter.batchErrorInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.ERROR.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 消息记录信息
      if (ValueType.MESSAGE.name().equals(record.getValueType().name())) {
        try {

          MessageImporter.batchMessageInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.MESSAGE.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 消息订阅记录信息
      if (ValueType.MESSAGE_SUBSCRIPTION.name().equals(record.getValueType().name())) {
        try {

          MessageSubscriptionImporter.batchMessageSubscriptionInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.MESSAGE_SUBSCRIPTION.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 消息启动记录信息
      if (ValueType.MESSAGE_START_EVENT_SUBSCRIPTION.name().equals(record.getValueType().name())) {
        try {

          MessageSubscriptionImporter.batchMessageStartEventSubscriptionInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.MESSAGE_SUBSCRIPTION.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
      // 信号启动记录信息
      if (ValueType.SIGNAL_SUBSCRIPTION.name().equals(record.getValueType().name())) {
        try {

          SignalSubscriptionImporter.batchSignalSubscriptionInsert(
              configuration.getChUrl(),
              configuration.getChUser(),
              configuration.getChPassword(),
              ValueType.SIGNAL_SUBSCRIPTION.name(),
              record);
          // 更新记录位置
          update(lastPostion);
        } catch (final SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  // 更新记录位置
  public void update(final long lastPosition) {
    try {
      ClickHouseConfig.updateClickHouseConfigTable(
          configuration.getChUrl(),
          configuration.getChUser(),
          configuration.getChPassword(),
          clickHouseConfigTable,
          lastPosition);
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }
}
