package io.sentry.servlet

import io.sentry.SentryEvent
import io.sentry.SentryOptions
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class SentryRequestHttpServletRequestProcessorTest {

    @Test
    fun `attaches basic information from HTTP request to SentryEvent`() {
        val request = MockMvcRequestBuilders
            .get(URI.create("http://example.com?param1=xyz"))
            .header("some-header", "some-header value")
            .accept("application/json")
            .buildRequest(MockServletContext())
        val eventProcessor = SentryRequestHttpServletRequestProcessor(request)
        val event = SentryEvent()

        eventProcessor.process(event, null)

        assertEquals("GET", event.request.method)
        assertEquals(mapOf(
            "some-header" to "some-header value",
            "Accept" to "application/json"
        ), event.request.headers)
        assertEquals("http://example.com", event.request.url)
        assertEquals("param1=xyz", event.request.queryString)
    }

    @Test
    fun `attaches header with multiple values`() {
        val request = MockMvcRequestBuilders
            .get(URI.create("http://example.com?param1=xyz"))
            .header("another-header", "another value")
            .header("another-header", "another value2")
            .buildRequest(MockServletContext())
        val eventProcessor = SentryRequestHttpServletRequestProcessor(request)
        val event = SentryEvent()

        eventProcessor.process(event, null)

        assertEquals(mapOf(
            "another-header" to "another value,another value2"
        ), event.request.headers)
    }

    @Test
    fun `does not attach cookies`() {
        val request = MockMvcRequestBuilders
            .get(URI.create("http://example.com?param1=xyz"))
            .header("Cookie", "name=value")
            .buildRequest(MockServletContext())
        val sentryOptions = SentryOptions()
        sentryOptions.isSendDefaultPii = false
        val eventProcessor = SentryRequestHttpServletRequestProcessor(request)
        val event = SentryEvent()

        eventProcessor.process(event, null)

        assertNull(event.request.cookies)
    }

    @Test
    fun `does not attach sensitive headers`() {
        val request = MockMvcRequestBuilders
            .get(URI.create("http://example.com?param1=xyz"))
            .header("some-header", "some-header value")
            .header("X-FORWARDED-FOR", "192.168.0.1")
            .header("authorization", "Token")
            .header("Authorization", "Token")
            .header("Cookie", "some cookies")
            .buildRequest(MockServletContext())
        val sentryOptions = SentryOptions()
        sentryOptions.isSendDefaultPii = false
        val eventProcessor = SentryRequestHttpServletRequestProcessor(request)
        val event = SentryEvent()

        eventProcessor.process(event, null)

        assertFalse(event.request.headers.containsKey("X-FORWARDED-FOR"))
        assertFalse(event.request.headers.containsKey("Authorization"))
        assertFalse(event.request.headers.containsKey("authorization"))
        assertFalse(event.request.headers.containsKey("Cookies"))
        assertTrue(event.request.headers.containsKey("some-header"))
    }
}
