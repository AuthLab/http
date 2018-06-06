/*
 * MIT License
 *
 * Copyright (c) 2018 Johan Fylling
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.authlab.logging;

import com.google.gson.Gson;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Layout that formats {@link LogEvent}s as json.
 * The parameters of a {@link MapMessage} are put under the key 'data'.
 * <p>
 * Example log4j2.xml configuration:
 * <pre>
 * {@code <Appenders>
 *     <Http name="HAR" url="http://localhost:8180/api/sources/server/events">
 *       <Property name="X-Java-Runtime" value="$${java:runtime}" />
 *       <JsonMapLayout properties="true" />
 *     </Http>
 *   </Appenders>
 * }
 * </pre>
 */
@Plugin(name = "JsonMapLayout", category = "Core", elementType = "layout", printObject = true)
public class JsonMapLayout extends AbstractStringLayout
{
    private static final String CONTENT_TYPE = "application/json";
    private static final String DEFAULT_HEADER = "[";
    private static final String DEFAULT_FOOTER = "]";

    private static final Gson _gson = new Gson();

    private final boolean _wrapData;
    private final boolean _includeMeta;
    private final boolean _includeMessage;
    private final boolean _complete;

    private JsonMapLayout(Configuration config, Charset charset, String headerPattern, String footerPattern,
                          boolean wrapData, boolean includeMeta, boolean includeMessage, boolean complete) {
        super(config, charset,
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build());

        _wrapData = wrapData;
        _includeMeta = includeMeta;
        _includeMessage = includeMessage;
        _complete = complete;
    }

    @Override
    public String toSerializable(LogEvent event)
    {
        Map<String, Object> jsonData = new HashMap<>();

        Message message = event.getMessage();

        if (message instanceof MapMessage) {
            if (_wrapData) {
                jsonData.put("data", ((MapMessage) message).getData());
            } else {
                ((MapMessage) message).forEach(
                        (key, value) -> jsonData.put(key.toString(), value));
            }
        }

        if (_includeMessage) {
            jsonData.put("message", message.getFormattedMessage());
        }

        if (_includeMeta) {
            jsonData.put("context", event.getContextData().toMap());
            jsonData.put("logger", event.getLoggerName());
            jsonData.put("level", event.getLevel().toString());
            jsonData.put("timeMillis", event.getTimeMillis());
            jsonData.put("thread", event.getThreadName());
            jsonData.put("marker", event.getMarker());
            jsonData.put("source", event.getSource());
            jsonData.put("exception", event.getThrown());

            Optional.ofNullable(event.getMarker())
                    .ifPresent(marker -> jsonData.put("marker", marker.toString()));
        }

        StringBuilder sb = new StringBuilder();

        if (_complete && eventCount > 0) {
            sb.append(",");
        }

        sb.append(_gson.toJson(jsonData));

        markEvent();

        return sb.toString();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    public static class Builder<B extends Builder<B>> extends AbstractStringLayout.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<JsonMapLayout> {

        @PluginAttribute("wrapData")
        private boolean _wrapData = false;

        @PluginAttribute("includeMeta")
        private boolean _includeMeta = false;

        @PluginAttribute("includeMessage")
        private boolean _includeMessage = false;

        @PluginAttribute("complete")
        private boolean _complete = false;

        @Override
        public JsonMapLayout build() {
            final String headerPattern = Optional.ofNullable(getHeader()).map((header) -> new String(header, Charset.defaultCharset())).orElse(null);
            final String footerPattern = Optional.ofNullable(getFooter()).map((footer) -> new String(footer, Charset.defaultCharset())).orElse(null);
            return new JsonMapLayout(getConfiguration(), getCharset(), headerPattern, footerPattern,
                    _wrapData, _includeMeta, _includeMessage, _complete);
        }
    }

    @PluginBuilderFactory
    public static  <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }
}
