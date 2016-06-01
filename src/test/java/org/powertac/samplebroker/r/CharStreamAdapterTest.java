/*
 * Copyright (c) 2016 by the original author
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
package org.powertac.samplebroker.r;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author John Collins
 */
public class CharStreamAdapterTest
{
  MessageDispatcher dispatcher;
  CharStreamAdapter uut;
  ArrayList<Object[]> methodArgs;

  String out;
  String in;

  @Before
  public void setUp () throws Exception
  {
    uut = new CharStreamAdapter();
    methodArgs = new ArrayList<>();
    dispatcher = mock(MessageDispatcher.class);
    ReflectionTestUtils.setField(uut, "dispatcher", dispatcher);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        methodArgs.add(invocation.getArguments());
        return null;
      }
    }).when(dispatcher).sendRawMessage(anyObject());
    uut.initialize(null);
  }

  /**
   * Test method for {@link org.powertac.samplebroker.r.CharStreamAdapter#exportMessage(java.lang.String)}.
   */
  @Test
  public void testExportMessage ()
  {
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    PrintStream capture;
    try {
      capture = new PrintStream(bao, true, "utf-8");
      uut.setOutputStream(capture);
      uut.exportMessage("<test-message />");
      String result = bao.toString();
      assertEquals("correct output", "<test-message />\n", result);
    }
    catch (UnsupportedEncodingException e) {
      fail("exception creating capture stream");
    }
  }

  /**
   * Test method for {@link org.powertac.samplebroker.r.CharStreamAdapter#startMessageImport()}.
   */
  @Test
  public void testStartMessageImport ()
  {
    assertFalse("not finished", uut.isFinished());
    String msg1 = "<order id=\"200000026\" timeslot=\"362\" mWh=\"37.5198\"/>";
    String msg2 = "<order id=\"200000026\" timeslot=\"362\" mWh=\"37.5198\"> <broker>Sample</broker> </order>";
    String msg3 = "<order id=\"200000026\" timeslot=\"362\" mWh=\"37.5198\">\n <broker>Sample</broker>\n </order>";
    String data = msg1 + "\n" + msg2 + "\n" + msg3 + "\nquit\n";
    InputStream stream =
        new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    uut.setInputStream(stream);
    // send the stream
    uut.startMessageImport();
    // should be finished
    assertTrue("finished", uut.isFinished());
    // should be one call
    assertEquals("three calls", 3, methodArgs.size());
    // msg comes through intact
    assertEquals("correct msg1", msg1, methodArgs.get(0)[0]);
    assertEquals("correct msg2", msg2, methodArgs.get(1)[0]);
    assertEquals("correct msg3", msg3, methodArgs.get(2)[0]);
  }
}
