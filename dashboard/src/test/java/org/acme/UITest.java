package org.acme;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;

@QuarkusTest
@WithPlaywright(debug = true)
public class UITest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL index;

    @Test
    public void testIndex() {
        final Page page = context.newPage();
        Response response = page.navigate(index.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("Dashboard", title);

        // Make sure the web app is loaded and hits the backend
        final ElementHandle el = page.waitForSelector(".container h1");
        String greeting = el.innerText();
        Assertions.assertEquals("One-Two-Three Quarkus", greeting);
    }
}