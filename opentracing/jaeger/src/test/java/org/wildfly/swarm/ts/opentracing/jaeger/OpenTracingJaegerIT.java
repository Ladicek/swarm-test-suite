package org.wildfly.swarm.ts.opentracing.jaeger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.fluent.Request;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;
import org.wildfly.swarm.ts.common.DockerRunner;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(Arquillian.class)
@DefaultDeployment
public class OpenTracingJaegerIT {
    
    private static DockerRunner dockerRunner;

    @BeforeClass
    public static void setupJaeger() throws Exception {
        dockerRunner = new DockerRunner("jaegertracing/all-in-one:latest","jaeger").
                logWait("\"Health Check state change\",\"status\":\"ready\"").
                port("26831:6831/udp").  //default Jaeger agent
                port("16686:16686");  //query service and UI

        dockerRunner.start();
    }

    @AfterClass
    public static void tearDownJaeger() throws IOException, InterruptedException {
        dockerRunner.stop();
    }
    @Test
    @InSequence(1)
    @RunAsClient
    public void applicationRequest() throws IOException {
        String response = Request.Get("http://localhost:8080/").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from traced servlet");
    }

    @Test
    @InSequence(2)
    @RunAsClient
    public void trace() {
        // the tracer inside the application doesn't send traces to the Jaeger server immediately,
        // they are batched, so we need to wait a bit
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = Request.Get("http://localhost:16686/api/traces?service=test-traced-service").execute().returnContent().asString();
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            assertThat(json.has("data")).isTrue();
            JsonArray data = json.getAsJsonArray("data");
            assertThat(data.size()).isEqualTo(1);
            JsonObject trace = data.get(0).getAsJsonObject();
            assertThat(trace.has("spans")).isTrue();
            JsonObject span = trace.getAsJsonArray("spans").get(0).getAsJsonObject();
            assertThat(span.has("tags")).isTrue();
            JsonArray tags = span.getAsJsonArray("tags");
            for (JsonElement tagElement : tags) {
                JsonObject tag = tagElement.getAsJsonObject();
                switch (tag.get("key").getAsString()) {
                    case "http.method":
                        assertThat(tag.get("value").getAsString()).isEqualTo("GET");
                        break;
                    case "http.url":
                        assertThat(tag.get("value").getAsString()).isEqualTo("http://localhost:8080/");
                        break;
                    case "http.status.code":
                        assertThat(tag.get("value").getAsInt()).isEqualTo(200);
                        break;
                }
            }
        });
    }
}
