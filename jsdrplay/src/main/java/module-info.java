module io.github.dsheirer.sdrplay {
    exports com.github.dsheirer.sdrplay;
    exports com.github.dsheirer.sdrplay.device;
    exports com.github.dsheirer.sdrplay.callback;
    exports com.github.dsheirer.sdrplay.parameter.composite;
    exports com.github.dsheirer.sdrplay.parameter.device;
    exports com.github.dsheirer.sdrplay.parameter.tuner;
    exports com.github.dsheirer.sdrplay.parameter.control;
    exports com.github.dsheirer.sdrplay.error;
    exports com.github.dsheirer.sdrplay.parameter.event;
    exports com.github.dsheirer.sdrplay.util;
    exports com.github.dsheirer.sdrplay.async;

    requires ch.qos.logback.classic;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires sdrplay.api;
}