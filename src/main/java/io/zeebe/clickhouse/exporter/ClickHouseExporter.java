/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter;

import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.protocol.record.Record;
import org.slf4j.Logger;

public class ClickHouseExporter implements Exporter {

  private Logger logger;
  private Controller controller;

  private ExporterConfiguration configuration;
  private ClickHouseExporterClient client;

  @Override
  public void configure(final Context context) {
    logger = context.getLogger();
    configuration = context.getConfiguration().instantiate(ExporterConfiguration.class);
    logger.info("Exporter configured with {}", configuration);
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
    client = createClient();
    logger.info("Exporter opened");
  }

  @Override
  public void close() {}

  @Override
  public void export(final Record<?> record) {
    final long lastPosition = record.getPosition();
    client.insert(record, lastPosition);
    controller.updateLastExportedRecordPosition(lastPosition);
  }

  protected ClickHouseExporterClient createClient() {
    return new ClickHouseExporterClient(configuration, logger);
  }
}
