# Zeebe ClickHouse Exporter

[Exporters](https://docs.camunda.io/docs/next/components/zeebe/technical-concepts/architecture/#exporters) allow you to tap into the Zeebe event stream on a partition and export selected events to other systems. You can filter events, perform transformations, and even trigger side-effects from an exporter.

Read a two-part series about building Zeebe exporters on the Zeebe blog: [Part One](https://camunda.com/blog/2019/05/exporter-part-1/) | [Part Two](https://camunda.com/blog/2019/06/exporter-part-2/).

This [Zeebe ClickHouse Exporter](https://github.com/skayliu/zeebe-clickhouse-exporter) is build on [Zeebe Exporter Demo](https://github.com/jwulf/zeebe-exporter-demo.git) and [ClickHouse Java Libraries](https://github.com/ClickHouse/clickhouse-java.git).

You can export records from Zeebe to ClickHouse and query data from ClickHouse use you own program language.

![How it works](how-it-works.jpg)


## About ClickHouse

[ClickHouse](https://clickhouse.com/clickhouse) is a column-oriented database that enables its users to generate powerful analytics, using SQL queries, in real-time.

ClickHouse is an Open Source OLAP database management system. 

ClickHouse runs on ClickHouse Cloud or any Linux, FreeBSD, or macOS system with x86_64, AArch64, or PowerPC64LE CPU architecture. 

[ClickHouse Quick Start:](https://clickhouse.com/docs/en/getting-started/quick-start/) follow these steps to get up and running with ClickHouse.

[ClickHouse Java Libraries](https://github.com/ClickHouse/clickhouse-java) for connecting to ClickHouse and processing data in various formats. Java client is async, lightweight, and low-overhead library for ClickHouse

## Deploy the  Exporter

1. Build the exporter, using `mvn package`.
2. Copy the resulting `zeebe-clickhouse-exporter-1.0-SNAPSHOT.jar` file to the `exporters` directory of your Zeebe broker.
3. Edit the `application.xml` file, and add an entry for the exporter:

```toml
    exporters:
      clickhouse:
        className: io.zeebe.clickhouse.exporter.ClickHouseExporter
        jarPath: exporters/zeebe-clickhouse-exporter-1.0-SNAPSHOT.jar
        args:
          chUrl: jdbc:ch://127.0.0.1:8123/default
          chUser: default
          chPassword: clickhouse123
```