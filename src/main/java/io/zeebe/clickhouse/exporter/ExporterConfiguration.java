package io.zeebe.clickhouse.exporter;

public class ExporterConfiguration {
    public String chUrl = "jdbc:ch://127.0.0.1:8123/default";
    public String chUser = "default";
    public String chPassword = "clickhouse123";

    @Override
    public String toString() {
        return "ExporterConfiguration{" +
                "chUrl='" + chUrl + '\'' +
                ", chUser='" + chUser + '\'' +
                ", chPassword='" + chPassword + '\'' +
                '}';
    }
}
