/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class ForEachErrorTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:foreach:route";
  public static final String EXPECTED_FOREACH_SPAN_NAME = "mule:foreach";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String FOR_EACH_TELEMETRY_FLOW = "for-each-telemetryFlow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "http:request";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/foreach-error.xml";
  }

  @Test
  public void testFlowWithError() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(FOR_EACH_TELEMETRY_FLOW).withPayload(TEST_PAYLOAD).dispatch();

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan forEachSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FOREACH_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> loggerSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> httpRequestSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .collect(Collectors.toList());

      CapturedExportedSpan onErrorPropagateSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(ON_ERROR_PROPAGATE_SPAN_NAME)).findFirst().orElse(null);

      assertThat(exportedSpans, hasSize(7));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(setPayloadSpan, notNullValue());
      assertThat(setPayloadSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(forEachSpan, notNullValue());
      assertThat(forEachSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(1));
      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(forEachSpan.getSpanId())));
      assertThat(loggerSpanList, hasSize(1));
      assertThat(loggerSpanList.get(0).getParentSpanId(), equalTo(muleRouteSpanList.get(0).getSpanId()));
      assertThat(httpRequestSpanList, hasSize(1));
      assertThat(httpRequestSpanList.get(0).getParentSpanId(), equalTo(muleRouteSpanList.get(0).getSpanId()));
      assertThat(onErrorPropagateSpan, notNullValue());
      assertThat(onErrorPropagateSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
    } finally {
      spanCapturer.dispose();
    }
  }
}
