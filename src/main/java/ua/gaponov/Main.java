package ua.gaponov;

import ua.gaponov.conf.LoggingConfiguration;
import ua.gaponov.dahua.Dahua;

public class Main {

    public static void main(String[] args) {
        new LoggingConfiguration();

        Dahua dahua = new Dahua();
        dahua.init();
        dahua.login();
        dahua.findRecords();
        dahua.logout();
    }
}