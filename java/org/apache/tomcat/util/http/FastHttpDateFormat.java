/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.http;

import java.text.DateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to generate HTTP dates.
 *
 * @author Remy Maucherat
 */
public final class FastHttpDateFormat {


    // -------------------------------------------------------------- Variables


    private static final int CACHE_SIZE =
        Integer.parseInt(System.getProperty("org.apache.tomcat.util.http.FastHttpDateFormat.CACHE_SIZE", "1000"));


    /**
     * The only date format permitted when generating HTTP headers.
     *
     * @deprecated Unused. This will be removed in Tomcat 10.
     */
    @Deprecated
    public static final String RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";

    // HTTP date formats
    private static final String DATE_OBSOLETE_RFC850 = "EEEE, dd-MMM-[yyyy][yy] HH:mm:ss zzz";
    private static final String DATE_OBSOLETE_ASCTIME = "[EEEE][EE] [MMMM][MMM] d HH:mm:ss yyyy";

    private static final DateTimeFormatter FORMAT_OBSOLETE_RFC850;
    private static final DateTimeFormatter FORMAT_OBSOLETE_ASCTIME;

    private static final DateTimeFormatter[] httpParseFormats;

    static {
        
        FORMAT_OBSOLETE_RFC850 = DateTimeFormatter.ofPattern(DATE_OBSOLETE_RFC850, Locale.US).withZone(ZoneOffset.UTC);
        FORMAT_OBSOLETE_ASCTIME = DateTimeFormatter.ofPattern(DATE_OBSOLETE_ASCTIME, Locale.US).withZone(ZoneOffset.UTC);
        
        httpParseFormats = new DateTimeFormatter[] {
            DateTimeFormatter.RFC_1123_DATE_TIME, FORMAT_OBSOLETE_RFC850, FORMAT_OBSOLETE_ASCTIME };
    }

    /**
     * Instant on which the currentDate object was generated.
     */
    private static volatile long currentDateGenerated = 0L;


    /**
     * Current formatted date.
     */
    private static String currentDate = null;


    /**
     * Formatter cache.
     */
    private static final Map<Long, String> formatCache = new ConcurrentHashMap<>(CACHE_SIZE);


    /**
     * Parser cache.
     */
    private static final Map<String, Long> parseCache = new ConcurrentHashMap<>(CACHE_SIZE);


    // --------------------------------------------------------- Public Methods


    /**
     * Get the current date in HTTP format.
     * @return the HTTP date
     */
    public static final String getCurrentDate() {
        long now = System.currentTimeMillis();
        if ((now - currentDateGenerated) > 1000) {
            ZonedDateTime zonedDateTime = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC);
            currentDate = zonedDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
            currentDateGenerated = now;
        }
        return currentDate;
    }


    /**
     * Get the HTTP format of the specified date.
     * @param value The date
     * @param threadLocalformat Ignored. The local ConcurrentDateFormat will
     *                          always be used.
     * @return the HTTP date
     *
     * @deprecated Unused. This will be removed in Tomcat 10
     */
    @Deprecated
    public static final String formatDate(long value, DateFormat threadLocalformat) {
        return formatDate(value);
    }


    /**
     * Get the HTTP format of the specified date.
     * @param value The date
     * @return the HTTP date
     */
    public static final String formatDate(long value) {
        Long longValue = Long.valueOf(value);
        String cachedDate = formatCache.get(longValue);
        if (cachedDate != null) {
            return cachedDate;
        }

        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
        String newDate = zonedDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);

        updateFormatCache(longValue, newDate);
        return newDate;
    }


    /**
     * Try to parse the given date as an HTTP date.
     * @param value The HTTP date
     * @param threadLocalformats Ignored. The local array of
     *                           ConcurrentDateFormat will always be used.
     * @return the date as a long
     *
     * @deprecated Unused. This will be removed in Tomcat 10
     *             Use {@link #parseDate(String)}
     */
    @Deprecated
    public static final long parseDate(String value, DateFormat[] threadLocalformats) {
        return parseDate(value);
    }


    /**
     * Try to parse the given date as an HTTP date.
     * @param value The HTTP date
     * @return the date as a long or <code>-1</code> if the value cannot be
     *         parsed
     */
    public static final long parseDate(String value) {

        Long cachedDate = parseCache.get(value);
        if (cachedDate != null) {
            return cachedDate.longValue();
        }

        long date = -1;
        for (int i = 0; (date == -1) && (i < httpParseFormats.length); i++) {
            try {
                date = ZonedDateTime.parse(value, httpParseFormats[i]).toInstant().toEpochMilli();
                updateParseCache(value, Long.valueOf(date));
            } catch (DateTimeParseException e) {
                // Ignore
            }
        }

        return date;
    }


    /**
     * Update cache.
     */
    private static void updateFormatCache(Long key, String value) {
        if (value == null) {
            return;
        }
        if (formatCache.size() > CACHE_SIZE) {
            formatCache.clear();
        }
        formatCache.put(key, value);
    }


    /**
     * Update cache.
     */
    private static void updateParseCache(String key, Long value) {
        if (value == null) {
            return;
        }
        if (parseCache.size() > CACHE_SIZE) {
            parseCache.clear();
        }
        parseCache.put(key, value);
    }


}
