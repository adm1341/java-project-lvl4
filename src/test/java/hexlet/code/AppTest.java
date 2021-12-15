package hexlet.code;


import hexlet.code.domain.Url;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.Transaction;
import io.ebean.DB;
import hexlet.code.domain.query.QUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static Url existingUrl;
    private static MockWebServer mockWebServer;

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        existingUrl = new Url("https://github.com");
        existingUrl.save();

        mockWebServer = new MockWebServer();

        String expected = Files.readString(Paths.get("src", "test", "resources", "expected", "mock"));

        mockWebServer.enqueue(new MockResponse().setBody(expected));

        mockWebServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockWebServer.shutdown();
    }


    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testCreate() {
        String inputName = "https://github.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputName)
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);

        Url actualurl = new QUrl()
                .name.equalTo(inputName)
                .findOne();

        assertThat(actualurl).isNotNull();
        assertThat(actualurl.getName()).isEqualTo(inputName);
    }

    @Test
    void testIncorrectShowId() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/100")
                .asString();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void testIndexUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();
        assertThat(body).contains(existingUrl.getName());
        assertThat(response.getStatus()).isEqualTo(200);
    }
    @Test
    void testCreateExistingUrl() {
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", existingUrl.getName())
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl)
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);

    }

    @Test
    void testCreateInvalidUrl() {
        String inputUrl = "Qwerty";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputUrl)
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).doesNotContain(inputUrl);

        Url actualUrl = new QUrl()
                .name.equalTo(inputUrl)
                .findOne();

        assertThat(actualUrl).isNull();
    }
    @Test
    void testCheckUrl() {
        String mockDescription = "GitHub is where over 73 million";
        String mockTitle = "GitHub: Where the";
        String mockH1 = "Where the world";

        String mockUrl = mockWebServer.url("/").toString();

        Unirest.post(baseUrl + "/urls")
                .field("url", mockUrl)
                .asEmpty();

        Url actualUrl = new QUrl()
                .name.equalTo(mockUrl.substring(0, mockUrl.length() - 1))
                .findOne();

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls/" + actualUrl.getId() + "/checks")
                .asString();

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/urls/" + actualUrl.getId());

        String body = Unirest
                .get(baseUrl + "/urls/" + actualUrl.getId())
                .asString()
                .getBody();

        assertThat(body).contains("200");
        assertThat(body).contains(mockDescription);
        assertThat(body).contains(mockH1);
        assertThat(body).contains(mockTitle);
    }
}
