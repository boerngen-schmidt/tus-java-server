package me.desair.tus.server.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TusServletResponseTest {

    private static final DateTimeFormatter inputDateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US).withZone(ZoneId.of("Etc/GMT"));
    // the spring MockHttpServletResponse does not use unix time in date headers
    private static final DateTimeFormatter springHeaderDateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(ZoneId.of("Etc/GMT"));;

    private TusServletResponse tusServletResponse;

    private MockHttpServletResponse servletResponse;

    @BeforeEach
    public void setUp() {
        servletResponse = new MockHttpServletResponse();
        tusServletResponse = new TusServletResponse(servletResponse);
    }

    @Test
    public void setDateHeader() throws Exception {
        ZonedDateTime zd1 = ZonedDateTime.from(inputDateFormater.parse("2018-01-03 22:34:14"));
        ZonedDateTime zd2 = ZonedDateTime.from(inputDateFormater.parse("2018-01-03 22:38:14"));
        tusServletResponse.setDateHeader("TEST", zd1.toInstant().toEpochMilli());
        tusServletResponse.setDateHeader("TEST", zd2.toInstant().toEpochMilli());
        assertThat(tusServletResponse.getHeader("TEST"), is("" + zd2.toInstant().toEpochMilli()));
        assertThat(servletResponse.getHeaders("TEST"), contains(springHeaderDateFormat.format(zd2)));
    }

    @Test
    public void addDateHeader() throws Exception {
        ZonedDateTime zd1 = ZonedDateTime.from(inputDateFormater.parse("2018-01-03 22:34:12"));
        ZonedDateTime zd2 = ZonedDateTime.from(inputDateFormater.parse("2018-01-03 22:38:12"));
        tusServletResponse.addDateHeader("TEST", zd1.toInstant().toEpochMilli());
        tusServletResponse.addDateHeader("TEST", zd2.toInstant().toEpochMilli());
        assertThat(tusServletResponse.getHeader("TEST"), is(String.valueOf(zd1.toInstant().toEpochMilli())));
        assertThat(servletResponse.getHeaders("TEST"), containsInAnyOrder(springHeaderDateFormat.format(zd1), springHeaderDateFormat.format(zd2)));
    }

    @Test
    public void setHeader() throws Exception {
        tusServletResponse.setHeader("TEST", "foo");
        tusServletResponse.setHeader("TEST", "bar");
        assertThat(tusServletResponse.getHeader("TEST"), is("bar"));
        assertThat(servletResponse.getHeaders("TEST"), contains("bar"));
    }

    @Test
    public void addHeader() throws Exception {
        tusServletResponse.addHeader("TEST", "foo");
        tusServletResponse.addHeader("TEST", "bar");
        assertThat(tusServletResponse.getHeader("TEST"), is("foo"));
        assertThat(servletResponse.getHeaders("TEST"), containsInAnyOrder("foo", "bar"));
    }

    @Test
    public void setIntHeader() throws Exception {
        tusServletResponse.setIntHeader("TEST", 1);
        tusServletResponse.setIntHeader("TEST", 2);
        assertThat(tusServletResponse.getHeader("TEST"), is("2"));
        assertThat(servletResponse.getHeaders("TEST"), contains("2"));
    }

    @Test
    public void addIntHeader() throws Exception {
        tusServletResponse.addIntHeader("TEST", 1);
        tusServletResponse.addIntHeader("TEST", 2);
        assertThat(tusServletResponse.getHeader("TEST"), is("1"));
        assertThat(servletResponse.getHeaders("TEST"), contains("1", "2"));
    }

    @Test
    public void getHeaderNull() throws Exception {
        assertThat(tusServletResponse.getHeader("TEST"), is(nullValue()));
    }
}
