/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.util.http;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


import org.junit.Assert;
import org.junit.Test;

import org.apache.tomcat.unittest.TesterResponse;

public class TestFastHttpDateFormat {

    @Test
    public void testFormatReturnsGMT() {
		Date date = new Date();
		String formattedDate = FastHttpDateFormat.formatDate(date.getTime());
		Assert.assertTrue(formattedDate.endsWith("GMT"));
    }

    @Test
    public void testFormatDateReturnsGMTAfterParseCET() {
		
		FastHttpDateFormat.parseDate("Thu, 12 Mar 2020 14:40:22 CET");
		
		Date date = new Date();
		String formattedDate = FastHttpDateFormat.formatDate(date.getTime());
		Assert.assertTrue(formattedDate.endsWith("GMT"));
    }

    @Test
    public void testFormatDateReturnsRFC5322Date() {
		String formattedDate = FastHttpDateFormat.formatDate(1584370526000L);
		Assert.assertEquals("Mon, 16 Mar 2020 14:55:26 GMT", formattedDate);
    }

    @Test
    public void testParseDateParsesRFC5322Date() {
		long epoc = FastHttpDateFormat.parseDate("Mon, 16 Mar 2020 16:28:23 GMT");
		Assert.assertEquals(1584376103000L, epoc);
    }

    @Test
    public void testParseDateParsesRFC850Date4DigitYear() {
		long epoc = FastHttpDateFormat.parseDate("Monday, 16-Mar-2020 16:28:23 GMT");
		Assert.assertEquals(1584376103000L, epoc);
    }

    @Test
    public void testParseDateParsesRFC850Date2DigitYear() {
		long epoc = FastHttpDateFormat.parseDate("Monday, 16-Mar-20 16:28:23 GMT");
		Assert.assertEquals(1584376103000L, epoc);
    }

    @Test
    public void testParseDateParsesAscTimeShortMonth() {
		long epoc = FastHttpDateFormat.parseDate("Mon Mar 16 16:28:23 2020");
		Assert.assertEquals(1584376103000L, epoc);
    }

    @Test
    public void testParseDateParsesAscTimeLongMonth() {
		long epoc = FastHttpDateFormat.parseDate("Mon March 16 16:28:23 2020");
		Assert.assertEquals(1584376103000L, epoc);
    }

}
