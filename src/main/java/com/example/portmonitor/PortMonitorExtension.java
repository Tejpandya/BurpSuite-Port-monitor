package com.example.portmonitor;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;

public class PortMonitorExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        Logging logging = api.logging();
        logging.logToOutput("Port Monitor Extension loaded.");

        api.userInterface().registerSuiteTab("Port Monitor", new PortMonitorTab(api));
    }
}
