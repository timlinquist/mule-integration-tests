/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class FlowUseCaseProcessingStrategyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-usecase-processing-strategy-config.xml";
    }

    @Test
    public void testExceptionSyncStrategy() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        MuleMessage exception = client.send("http://localhost:" + dynamicPort.getNumber(), null, null);

        assertEquals("500", exception.getInboundProperty("http.status", "0"));        
    }

    @Test
    @Ignore
    public void testFileAutoDeleteSyncStrategy() throws Exception
    {
        Prober prober = new PollingProber(10000, 5000);
        File directory = new File("./.mule");
        final File file = File.createTempFile("mule-file-test-", ".txt", directory);       
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.write("The quick brown fox jumps over the lazy dog", fos);
        IOUtils.closeQuietly(fos);
        
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should still exist";
            }
        });                
    }
         
}


