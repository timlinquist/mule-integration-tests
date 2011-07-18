/*
 * $Id: XmlFilterNamespaceHandlerTestCase.java 22438 2011-07-18 14:33:27Z claude.mamo $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.construct.Flow;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class XmlFilterNamespaceHandlerFlowTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.FLOW,
            "org/mule/module/xml/xml-filter-functional-test-flow.xml"}});
    }

    public XmlFilterNamespaceHandlerFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * IsXmlFilter doesn't have any properties to test, so just check it is created
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    @Test
    public void testIsXmlFilter()
        throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException
    {
        Object flow = muleContext.getRegistry().lookupObject("test for xml");
        Object notXmlSubFlowWrapper;
        Field f;
        MessageProcessorChain notXmlSubFlow;
        List<MessageProcessor> outEndpoints = new ArrayList<MessageProcessor>(2);

        outEndpoints.add(((Flow) flow).getMessageProcessors().get(0));
        notXmlSubFlowWrapper = muleContext.getRegistry().lookupObject("notXml");

        f = notXmlSubFlowWrapper.getClass().getDeclaredField("delegate");
        f.setAccessible(true);
        notXmlSubFlow = (MessageProcessorChain) f.get(notXmlSubFlowWrapper);
        outEndpoints.add((notXmlSubFlow.getMessageProcessors().get(0)));

        assertEquals(2, outEndpoints.size());
        assertTrue(outEndpoints.get(0).getClass().getName(), outEndpoints.get(0) instanceof MessageFilter);
        assertTrue(((MessageFilter) outEndpoints.get(0)).getFilter() instanceof IsXmlFilter);
        assertTrue(outEndpoints.get(1).getClass().getName(), outEndpoints.get(1) instanceof MessageFilter);
        assertTrue(((MessageFilter) outEndpoints.get(1)).getFilter() instanceof NotFilter);
        assertTrue(((NotFilter) ((MessageFilter) outEndpoints.get(1)).getFilter()).getFilter() instanceof IsXmlFilter);

    }

    @Test
    public void testJXPathFilter()
    {
        Object flow = muleContext.getRegistry().lookupObject("filter xml for content");

        List<MessageProcessor> outEndpoints = new ArrayList<MessageProcessor>(1);
        outEndpoints.add(((Flow) flow).getMessageProcessors().get(0));

        assertEquals(1, outEndpoints.size());
        assertTrue(outEndpoints.get(0).getClass().getName(), outEndpoints.get(0) instanceof MessageFilter);

        assertTrue(((MessageFilter) outEndpoints.get(0)).getFilter() instanceof JXPathFilter);
        JXPathFilter filter = (JXPathFilter) ((MessageFilter) outEndpoints.get(0)).getFilter();
        assertEquals("filter xml for content", filter.getExpectedValue());
        assertEquals("/mule:mule/mule:flow[2]/@name", filter.getPattern());
        assertNotNull(filter.getNamespaces());
        Map<?, ?> namespaces = filter.getNamespaces();
        assertEquals(2, namespaces.size());
        assertEquals("http://www.springframework.org/schema/beans", namespaces.get("spring"));
        assertTrue(namespaces.get("mule").toString().startsWith("http://www.mulesoft.org/schema/mule/core"));

    }
}
