package org.excavator.boot.jfrevent;

import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import java.time.Duration;

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

    @Test
    @EnableEvent("jdk.ThreadSleep")
    public void testThreadSleepEvent() throws Exception{
        Thread.sleep(42);

        jfrEvents.awaitEvents();

        assertThat(jfrEvents).contains(event("jdk.ThreadSleep")
                .with("time", Duration.ofMillis(42)));
    }

    @Test
    @EnableEvent("jdk.ObjectAllocationInNewTLAB")
    @EnableEvent("jdk.ObjectAllocationOutsideTLAB")
    public void testAllocationEvent()throws Exception{
        var threadName = Thread.currentThread().getName();

        // Application Logic which creates objects
        jfrEvents.awaitEvents();
        var sum = jfrEvents.filter(this::isObjectAllocationEvent)
                .filter(event -> event.getThread().getJavaName().equals(threadName))
                .mapToLong(this::getAllocationSize)
                .sum();
        org.assertj.core.api.Assertions.assertThat(sum).isLessThan(43_000_000);
        org.assertj.core.api.Assertions.assertThat(sum).isGreaterThan(1_000_000);

        var ITERATIONS = 1_000_000;
        for (int i = 0; i < ITERATIONS; i++) {
            jfrEvents.awaitEvents();
            jfrEvents.reset();
        }
    }

    private boolean isObjectAllocationEvent(RecordedEvent re){
        var name = re.getEventType().getName();
        return name.equals("jdk.ObjectAllocationInNewTLAB") ||
                name.equals("jdk.ObjectAllocationOutsideTLAB");
    }

    private long getAllocationSize(RecordedEvent recordedEvent){
        return recordedEvent.getEventType().getName()
                .equals("jdk.ObjectAllocationInNewTLAB") ?
                recordedEvent.getLong("tlabSize") :
                recordedEvent.getLong("allocationSize");
    }

}
