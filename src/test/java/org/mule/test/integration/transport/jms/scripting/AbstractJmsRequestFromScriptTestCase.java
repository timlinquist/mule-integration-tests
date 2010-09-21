/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.jms.scripting;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * Defines a scenario when we request a jms message from inside a groovy script
 * which is executed as part of a service whose endpoints are jms ones.
 * Subclasses must provide the service configuration through the implementation
 * of {@link org.mule.tck.FunctionalTestCase#getConfigResources()}.
 */
public abstract class AbstractJmsRequestFromScriptTestCase extends FunctionalTestCase
{

    /**
     * Requests jms message from inside a groovy script which run as part of a
     * service defined using jms endpoints. The first part of the test loads
     * a couple of jms message in a queue so the script will have data to
     * process.
     */
    public void testRequestingMessageFromScript() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        // Sends data to process
        muleClient.send("vm://in", TEST_MESSAGE, null);
        muleClient.send("vm://in", TEST_MESSAGE, null);

        // Sends the signal to start the batch process
        muleClient.send("vm://startBatch", TEST_MESSAGE, null);

        // Checks that the batch has processed the two messages without error
        MuleMessage message = muleClient.request("jms://status.queue?connector=jmsConnector", 5000);
        assertNotNull(message);
        assertEquals("messagemessage", message.getPayloadAsString());
    }
}
