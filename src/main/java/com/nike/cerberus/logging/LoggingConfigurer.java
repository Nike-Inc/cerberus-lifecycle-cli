/*
 * Copyright (c) 2016 Nike Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Configures logging for the Cerberus Admin CLI.
 */
public final class LoggingConfigurer {

    private LoggingConfigurer() {}

    /**
     * Initializes the logger and the requested log level.
     *
     * @param level Logging level threshold
     */
    @SuppressWarnings("unchecked")
    public static void configure(final Level level) {
        final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(root.getLoggerContext());
        encoder.setPattern("%msg%n");
        encoder.start();

        Iterator<Appender<ILoggingEvent>> iterator = root.iteratorForAppenders();

        while (iterator.hasNext()) {
            Appender<ILoggingEvent> appender = iterator.next();
            if (appender instanceof OutputStreamAppender) {
                ((OutputStreamAppender) appender).setEncoder(encoder);
            }
            appender.stop();
            appender.start();
        }
    }
}
