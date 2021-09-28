package org.excavator.boot.jfrevent;

import org.junit.jupiter.api.Test;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.moditect.jfrunit.ExpectedEvent.event;
import static org.moditect.jfrunit.JfrEventsAssert.assertThat;

@JfrEventTest
public class GarbageCollectionTest {

    public JfrEvents jfrEvents = new JfrEvents();

    @Test
    @EnableEvent("jdk.GarbageCollection")
    public void testGarbageCollectionEvent()throws Exception{
        System.gc();
        jfrEvents.awaitEvents();
        assertThat(jfrEvents).contains(event("jdk.GarbageCollection"));
    }

}
