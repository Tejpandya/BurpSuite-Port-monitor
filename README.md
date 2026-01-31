# Real-time Port Monitor for IoT/OT Security

**Author:** Tejas Pandya
**Version:** 1.0.0

## Objective

This Burp Suite extension is designed to assist IoT (Internet of Things) and OT (Operational Technology) security analysts in monitoring the stability of device ports during web application security testing.

When performing intensive scanning with Burp Suite, the TCP/IP stack of embedded devices can sometimes fail to handle the traffic load. This can lead to device reboots or the failure of critical services running on other ports (e.g., IEC 61850, Modbus, DNP3).

This extension provides a simple, real-time visualization of the status of multiple TCP ports, allowing analysts to continuously monitor critical device services while conducting their web-focused testing in Burp.

## Features

- **Real-time Port Monitoring:** Continuously sends TCP handshake requests to user-defined ports at a 1-second interval.
- **Visual Graph:** Displays the status of each port on a time-series chart. Each port is given its own line on the graph for clear visibility.
- **Hostname and IP Support:** Target can be specified as either a hostname or an IP address.
- **Session Summary:** Logs all "UP" and "DOWN" events for each port. This log can be viewed in a separate window at any time.
- **Export Functionality:**
  - Export the session summary to a `.txt` file for reporting.
  - Export the chart view as a `.png` image for documentation.

## How to Use

1.  Navigate to the **Port Monitor** tab in Burp Suite.
2.  Enter the target **Host/IP** address (e.g., `192.168.1.1` or `plc.local`).
3.  Enter a comma-separated list of TCP **Ports** to monitor (e.g., `80,443,502,20000`).
4.  Click the **Start** button. The graph will begin plotting the status of each port.
5.  Click **Stop** to end the monitoring session.
6.  Use the **Show Summary**, **Export Summary**, and **Export Chart** buttons to review and save the session data.

## Building from Source

This project is built with Gradle.

1.  Clone the repository or download the source code.
2.  Navigate to the project's root directory (`Burp`).
3.  Run the following command to build the extension JAR file:
    ```bash
    ./gradlew shadowJar
    ```
4.  The compiled JAR file will be located in `build/libs/burp-port-monitor.jar`.
5.  Load this JAR file into Burp Suite via the `Extensions` -> `Add` menu.

