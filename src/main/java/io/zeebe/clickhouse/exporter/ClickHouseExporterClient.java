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
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          clickHouseConfigTable);
      // 检查是否有初始化配置信息
      final long i =
          ClickHouseConfig.queryClickHouseConfig(
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
              clickHouseConfigTable);

      if (i <= 0L) {
        // 执行初始化
        ClickHouseConfig.InitClickHouseConfigTable(
            configuration.chUrl,
            configuration.chUser,
            configuration.chPassword,
            clickHouseConfigTable);
      } else {
        cfgPosition = i;
      }
      // 创建流程定义信息表
      ClickHouseConfig.CreateProcessTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.PROCESS.name());
      // 创建流程实例表
      ClickHouseConfig.CreateProcessInstanceTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.PROCESS_INSTANCE.name());
      // 创建任务实例表
      ClickHouseConfig.CreateElementInstanceTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          elementInstanceTable);
      // 创建调度表
      ClickHouseConfig.CreateJobTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.JOB.name());
      // 创建流程变量表
      ClickHouseConfig.CreateVariableTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.VARIABLE.name());
      // 创建事件表
      ClickHouseConfig.CreateIncidentTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.INCIDENT.name());
      // 创建定时器表
      ClickHouseConfig.CreateTimerTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.TIMER.name());
      // 创建异常表
      ClickHouseConfig.CreateErrorTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.ERROR.name());
      // 创建消息表
      ClickHouseConfig.CreateMessageTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.MESSAGE.name());
      // 创建消息订阅表
      ClickHouseConfig.CreateMessageSubscriptionTable(
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          ValueType.MESSAGE_SUBSCRIPTION.name());

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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
              ValueType.PROCESS_INSTANCE.name(),
              record);

          // 任务实例信息
          ProcessInstanceImporter.batchElementInstanceInsert(
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
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
              configuration.chUrl,
              configuration.chUser,
              configuration.chPassword,
              ValueType.MESSAGE_SUBSCRIPTION.name(),
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
          configuration.chUrl,
          configuration.chUser,
          configuration.chPassword,
          clickHouseConfigTable,
          lastPosition);
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }
}
