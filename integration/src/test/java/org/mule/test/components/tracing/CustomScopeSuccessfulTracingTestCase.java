/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.components.tracing.TracingTestUtils.assertSpanAttributes;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class CustomScopeSuccessfulTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_CUSTOM_SCOPE_SPAN_NAME = "heisenberg:execute-anything";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_CUSTOM_SCOPE_ROUTE_SPAN_NAME = "heisenberg:execute-anything:route";
  public static final String CUSTOM_SCOPE_FLOW = "custom-scope-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String TEST_ARTIFACT_ID = "CustomScopeSuccessfulTracingTestCase#testCustomScopeFlow";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/custom-scope-success.xml";
  }

  @Test
  public void testCustomScopeFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(CUSTOM_SCOPE_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan customScopeSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_CUSTOM_SCOPE_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan customScopeRoute =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_CUSTOM_SCOPE_ROUTE_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst()
              .orElse(null);

      assertThat(exportedSpans, hasSize(4));

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_CUSTOM_SCOPE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_CUSTOM_SCOPE_ROUTE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .endChildren()
          .endChildren()
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

      assertSpanAttributes(muleFlowSpan, "custom-scope-flow", TEST_ARTIFACT_ID);
      assertSpanAttributes(customScopeSpan, "custom-scope-flow/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(customScopeRoute, "custom-scope-flow/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(loggerSpan, "custom-scope-flow/processors/0/processors/0", TEST_ARTIFACT_ID);
    } finally {
      spanCapturer.dispose();
    }
  }
}

