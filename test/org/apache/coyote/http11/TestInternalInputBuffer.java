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

package org.apache.coyote.http11;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.apache.catalina.Context;
import org.apache.catalina.startup.SimpleHttpClient;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.TomcatBaseTest;

public class TestInternalInputBuffer extends TomcatBaseTest {
    
    /**
     * Test case for https://issues.apache.org/bugzilla/show_bug.cgi?id=48839
     */
    @Test
    public void testBug48839() {
        
        Bug48839Client client = new Bug48839Client();
        client.setPort(getPort());
        
        client.doRequest();
        assertTrue(client.isResponse200());
        assertTrue(client.isResponseBodyOK());
    }

    
    /**
     * Bug 48839 test client.
     */
    private class Bug48839Client extends SimpleHttpClient {
                
        private Exception doRequest() {
        
            Tomcat tomcat = getTomcatInstance();
            
            Context root = tomcat.addContext("", TEMP_DIR);
            Tomcat.addServlet(root, "Bug48839", new Bug48839Servlet());
            root.addServletMapping("/test", "Bug48839");

            try {
                tomcat.start();

                // Open connection
                connect();
                
                String[] request = new String[1];
                request[0] =
                    "GET http://localhost:8080/test HTTP/1.1" + CRLF +
                    "X-Bug48839: abcd" + CRLF +
                    "\tefgh" + CRLF +
                    "Connection: close" + CRLF +
                    CRLF;
                
                setRequest(request);
                processRequest(); // blocks until response has been read
                
                // Close the connection
                disconnect();
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        public boolean isResponseBodyOK() {
            if (getResponseBody() == null) {
                return false;
            }
            if (!getResponseBody().contains("abcd\tefgh")) {
                return false;
            }
            return true;
        }
        
    }

    private static class Bug48839Servlet extends HttpServlet {
        
        private static final long serialVersionUID = 1L;

        /**
         * Only interested in the request headers from a GET request
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Just echo the header value back as plain text
            resp.setContentType("text/plain");
            
            PrintWriter out = resp.getWriter();
            
            Enumeration<String> values = req.getHeaders("X-Bug48839");
            while (values.hasMoreElements()) {
                out.println(values.nextElement());
            }
        }
    }


    @Test
    public void testBug51557NoColon() {
        
        Bug51557Client client = new Bug51557Client("X-Bug51557NoColon");
        client.setPort(getPort());
        
        client.doRequest();
        assertTrue(client.isResponse200());
        assertEquals("abcd", client.getResponseBody());
        assertTrue(client.isResponseBodyOK());
    }

    
    @Test
    public void testBug51557Separators() throws Exception {
        char httpSeparators[] = new char[] {
                '\t', ' ', '\"', '(', ')', ',', '/', ':', ';', '<',
                '=', '>', '?', '@', '[', '\\', ']', '{', '}' };
        
        for (char s : httpSeparators) {
            doTestBug51557Char(s);
            tearDown();
            setUp();
        }
    }


    @Test
    public void testBug51557Ctl() throws Exception {
        for (int i = 0; i < 31; i++) {
            doTestBug51557Char((char) i);
            tearDown();
            setUp();
        }
        doTestBug51557Char((char) 127);
    }


    @Test
    public void testBug51557Continuation() {
        
        Bug51557Client client = new Bug51557Client("X-Bug=51557NoColon",
                "foo" + SimpleHttpClient.CRLF + " bar");
        client.setPort(getPort());
        
        client.doRequest();
        assertTrue(client.isResponse200());
        assertEquals("abcd", client.getResponseBody());
        assertTrue(client.isResponseBodyOK());
    }

    
    @Test
    public void testBug51557BoundaryStart() {
        
        Bug51557Client client = new Bug51557Client("=X-Bug51557",
                "invalid");
        client.setPort(getPort());
        
        client.doRequest();
        assertTrue(client.isResponse200());
        assertEquals("abcd", client.getResponseBody());
        assertTrue(client.isResponseBodyOK());
    }

    
    @Test
    public void testBug51557BoundaryEnd() {
        
        Bug51557Client client = new Bug51557Client("X-Bug51557=",
                "invalid");
        client.setPort(getPort());
        
        client.doRequest();
        assertTrue(client.isResponse200());
        assertEquals("abcd", client.getResponseBody());
        assertTrue(client.isResponseBodyOK());
    }

    
    private void doTestBug51557Char(char s) {
        Bug51557Client client =
            new Bug51557Client("X-Bug" + s + "51557", "invalid");

        client.setPort(getPort());
        client.doRequest();
        assertTrue(client.isResponse200());
        assertEquals("abcd", client.getResponseBody());
        assertTrue(client.isResponseBodyOK());
    }
    
    /**
     * Bug 51557 test client.
     */
    private class Bug51557Client extends SimpleHttpClient {

        private String headerName;
        private String headerLine;

        public Bug51557Client(String headerName) {
            this.headerName = headerName;
            this.headerLine = headerName;
        }

        public Bug51557Client(String headerName, String headerValue) {
            this.headerName = headerName;
            this.headerLine = headerName + ": " + headerValue;
        }

        private Exception doRequest() {
        
            Tomcat tomcat = getTomcatInstance();
            
            Context root = tomcat.addContext("", TEMP_DIR);
            Tomcat.addServlet(root, "Bug51557",
                    new Bug51557Servlet(headerName));
            root.addServletMapping("/test", "Bug51557");

            try {
                tomcat.start();

                // Open connection
                connect();
                
                String[] request = new String[1];
                request[0] =
                    "GET http://localhost:8080/test HTTP/1.1" + CRLF +
                    headerLine + CRLF +
                    "X-Bug51557: abcd" + CRLF +
                    "Connection: close" + CRLF +
                    CRLF;
                
                setRequest(request);
                processRequest(); // blocks until response has been read
                
                // Close the connection
                disconnect();
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        public boolean isResponseBodyOK() {
            if (getResponseBody() == null) {
                return false;
            }
            if (!getResponseBody().contains("abcd")) {
                return false;
            }
            return true;
        }
        
    }

    private static class Bug51557Servlet extends HttpServlet {
        
        private static final long serialVersionUID = 1L;

        private String invalidHeaderName;

        /**
         * @param invalidHeaderName The header name should be invalid and
         *                          therefore ignored by the header parsing code
         */
        public Bug51557Servlet(String invalidHeaderName) {
            this.invalidHeaderName = invalidHeaderName;
        }

        /**
         * Only interested in the request headers from a GET request
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Just echo the header value back as plain text
            resp.setContentType("text/plain");
            
            PrintWriter out = resp.getWriter();
            
            processHeaders(invalidHeaderName, req, out);
            processHeaders("X-Bug51557", req, out);
        }
        
        private void processHeaders(String header, HttpServletRequest req,
                PrintWriter out) {
            Enumeration<String> values = req.getHeaders(header);
            while (values.hasMoreElements()) {
                out.println(values.nextElement());
            }
        }
    }
}
