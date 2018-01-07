package org.authlab.logging;

import org.apache.logging.log4j.message.MapMessage;

import java.util.Map;

public class SimpleMapMessage<T> extends MapMessage<SimpleMapMessage<T>, T> {
    public SimpleMapMessage() {
        super();
    }

    public SimpleMapMessage(final int initialCapacity) {
        super(initialCapacity);
    }

    public SimpleMapMessage(final Map<String, T> map) {
        super(map);
    }

    @Override
    public SimpleMapMessage<T> newInstance(Map<String, T> map)
    {
        return super.newInstance(map);
    }
}
