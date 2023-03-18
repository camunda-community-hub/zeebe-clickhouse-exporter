package io.zeebe.clickhouse.exporter;

import java.util.Optional;

public class ExporterConfiguration {
  private static final String ENV_PREFIX = "ZEEBE_CLICKHOUSE_";
  private String chUrl = "jdbc:ch://127.0.0.1:8123/default";
  private String chUser = "default";
  private String chPassword = "";

  public String getChUrl() {
    return getEnv("URL").orElse(chUrl);
  }

  public String getChUser() {
    return getEnv("USER").orElse(chUser);
  }

  public String getChPassword() {
    return getEnv("PASSWORD").orElse(chPassword);
  }

  private Optional<String> getEnv(String name) {
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
