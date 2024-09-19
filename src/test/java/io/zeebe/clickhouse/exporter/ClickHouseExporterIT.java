/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.clickhouse.data.ClickHouseDataType;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import org.agrona.CloseHelper;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
@Timeout(value = 2, unit = TimeUnit.MINUTES)
@Execution(ExecutionMode.CONCURRENT)
public class ClickHouseExporterIT {

  private static final String JOB_TYPE = "work";
  private static final String MESSAGE_NAME = "catch";
  private static final String PROCESS_NAME = "testProcess";
  private static final String PROCESS_FILE_NAME = "sample_workflow.bpmn";
  private static final String TASK_NAME = "task";
  private static final BpmnModelInstance SAMPLE_PROCESS =
      Bpmn.createExecutableProcess(PROCESS_NAME)
          .startEvent()
          .intermediateCatchEvent(
              "message",
              e -> e.message(m -> m.name(MESSAGE_NAME).zeebeCorrelationKeyExpression("orderId")))
          .serviceTask(TASK_NAME, t -> t.zeebeJobType(JOB_TYPE).zeebeTaskHeader("foo", "bar"))
          .endEvent()
          .done();
  private final MountableFile exporterJar =
      MountableFile.forClasspathResource("zeebe-clickhouse-exporter.jar", 775);
  private final MountableFile exporterConfig =
      MountableFile.forClasspathResource("application.yaml", 775);
  private final MountableFile loggingConfig =
      MountableFile.forClasspathResource("logback-test.xml", 775);
  private final Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(newContainerLogger(), true);
  private final Network network = Network.newNetwork();

  @Container
  public ClickHouseContainer clickHouse =
      new ClickHouseContainer(
              DockerImageName.parse("clickhouse/clickhouse-server:22.8.15.23-alpine"))
          .withNetwork(network)
          .withNetworkAliases("clickhouse")
          .withExposedPorts(8123);

  @Container
  public ZeebeContainer zeebeContainer =
      new ZeebeContainer(DockerImageName.parse("camunda/zeebe:8.2.0"))
          .withNetwork(network)
          .withNetworkAliases("zeebe")
          .withEnv("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST", "zeebe")
          .withEnv("ZEEBE_CLICKHOUSE_URL", "jdbc:ch://clickhouse/default")
          .withEnv("ZEEBE_CLICKHOUSE_USER", "default")
          .withEnv("ZEEBE_CLICKHOUSE_PASSWORD", "")
          .withEnv("ZEEBE_LOG_LEVEL", "debug")
          .withCopyFileToContainer(
              exporterJar, "/usr/local/zeebe/exporters/zeebe-clickhouse-exporter.jar")
          .withCopyFileToContainer(exporterConfig, "/usr/local/zeebe/config/application.yaml")
          .withCopyFileToContainer(loggingConfig, "/usr/local/zeebe/config/logback-test.xml")
          .withEnv(
              "SPRING_CONFIG_ADDITIONAL_LOCATION", "file:/usr/local/zeebe/config/application.yaml")
          .withLogConsumer(logConsumer);

  private ZeebeClient zeebeClient;

  private static Logger newContainerLogger() {
    return LoggerFactory.getLogger(ClickHouseExporterIT.class.getName() + "." + "zeebeContainer");
  }

  @AfterEach
  void tearDown() {
    CloseHelper.quietCloseAll(zeebeClient, zeebeContainer, clickHouse, network);
  }

  @Test
  public void testClickHouseContainer() throws SQLException {
    clickHouse.start();

    final String sql = "select cast(1 as UInt64) a, cast([1, 2] as Array(Int8)) b";
    try (final Connection conn =
            DriverManager.getConnection(
                clickHouse.getJdbcUrl(), clickHouse.getUsername(), clickHouse.getPassword());
        final Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery(sql)) {

      assertThat(rs.next()).isTrue();
      assertThat(rs.getMetaData().getColumnTypeName(1)).isEqualTo(ClickHouseDataType.UInt64.name());
      assertThat(rs.getMetaData().getColumnTypeName(2)).isEqualTo("Array(Int8)");
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void shouldExportProcessToClickHouse() throws SQLException, InterruptedException {
    // given
    clickHouse.start();
    zeebeContainer.start();
    zeebeClient = getLazyZeebeClient();

    // when
    final DeploymentEvent deploymentEvent =
        zeebeClient
            .newDeployResourceCommand()
            .addProcessModel(SAMPLE_PROCESS, PROCESS_FILE_NAME)
            .send()
            .join();

    Thread.sleep(1000);
    // then
    final String sql =
        "select KEY_,BPMN_PROCESS_ID_,RESOURCE_NAME_,TIMESTAMP_,PARTITION_ID_,POSITION_,VERSION_,RESOURCE_ from PROCESS";

    try (final Connection conn =
            DriverManager.getConnection(
                clickHouse.getJdbcUrl(), clickHouse.getUsername(), clickHouse.getPassword());
        final Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery(sql)) {

      assertThat(rs.next()).isTrue();
      assertThat(rs.getMetaData().getColumnTypeName(1)).isEqualTo(ClickHouseDataType.UInt64.name());
      assertThat(rs.getMetaData().getColumnTypeName(2)).isEqualTo(ClickHouseDataType.String.name());
      assertThat(rs.getMetaData().getColumnTypeName(3)).isEqualTo(ClickHouseDataType.String.name());
      assertThat(rs.getMetaData().getColumnTypeName(4)).isEqualTo("DateTime64(3)");
      assertThat(rs.getMetaData().getColumnTypeName(5)).isEqualTo(ClickHouseDataType.UInt16.name());
      assertThat(rs.getMetaData().getColumnTypeName(6)).isEqualTo(ClickHouseDataType.UInt64.name());
      assertThat(rs.getMetaData().getColumnTypeName(7)).isEqualTo(ClickHouseDataType.UInt16.name());
      assertThat(rs.getMetaData().getColumnTypeName(8)).isEqualTo(ClickHouseDataType.String.name());

      assertThat(rs.getLong(1))
          .isEqualTo(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey());
      assertThat(rs.getString(2)).isEqualTo(PROCESS_NAME);
      assertThat(rs.getString(3)).isEqualTo(PROCESS_FILE_NAME);
      assertThat(rs.getInt(7)).isEqualTo(deploymentEvent.getProcesses().get(0).getVersion());
      assertThat(rs.getString(8)).isEqualTo(Bpmn.convertToString(SAMPLE_PROCESS));

      assertThat(rs.next()).isFalse();
    }
  }

  private ZeebeClient getLazyZeebeClient() {
    if (zeebeClient == null) {
      zeebeClient =
          ZeebeClient.newClientBuilder()
              .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
              .usePlaintext()
              .build();
    }
    return zeebeClient;
  }
}
