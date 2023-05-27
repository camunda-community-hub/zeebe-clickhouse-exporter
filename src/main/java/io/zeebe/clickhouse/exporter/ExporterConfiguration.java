/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.clickhouse.exporter;

import java.util.Optional;

public class ExporterConfiguration {
  private static final String ENV_PREFIX = "ZEEBE_CLICKHOUSE_";
  private final String chUrl = "jdbc:ch://127.0.0.1:8123/default";
  private final String chUser = "default";
  private final String chPassword = "";

  public String getChUrl() {
    return getEnv("URL").orElse(chUrl);
  }

  public String getChUser() {
    return getEnv("USER").orElse(chUser);
  }

  public String getChPassword() {
    return getEnv("PASSWORD").orElse(chPassword);
  }

  private Optional<String> getEnv(final String name) {
    return Optional.ofNullable(System.getenv(ENV_PREFIX + name));
  }

  @Override
  public String toString() {
    return "ExporterConfiguration{"
        + "chUrl='"
        + chUrl
        + '\''
        + ", chUser='"
        + chUser
        + '\''
        + ", chPassword='"
        + chPassword
        + '\''
        + '}';
  }
}
