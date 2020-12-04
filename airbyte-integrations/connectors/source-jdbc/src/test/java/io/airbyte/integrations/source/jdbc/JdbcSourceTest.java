/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
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

package io.airbyte.integrations.source.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.airbyte.commons.json.Jsons;
import io.airbyte.commons.resources.MoreResources;
import io.airbyte.db.Database;
import io.airbyte.db.Databases;
import io.airbyte.integrations.source.jdbc.models.JdbcState;
import io.airbyte.integrations.source.jdbc.models.JdbcStreamState;
import io.airbyte.protocol.models.AirbyteCatalog;
import io.airbyte.protocol.models.AirbyteConnectionStatus;
import io.airbyte.protocol.models.AirbyteConnectionStatus.Status;
import io.airbyte.protocol.models.AirbyteMessage;
import io.airbyte.protocol.models.AirbyteMessage.Type;
import io.airbyte.protocol.models.AirbyteRecordMessage;
import io.airbyte.protocol.models.AirbyteStateMessage;
import io.airbyte.protocol.models.CatalogHelpers;
import io.airbyte.protocol.models.ConfiguredAirbyteCatalog;
import io.airbyte.protocol.models.ConfiguredAirbyteStream;
import io.airbyte.protocol.models.ConnectorSpecification;
import io.airbyte.protocol.models.Field;
import io.airbyte.protocol.models.Field.JsonSchemaPrimitive;
import io.airbyte.protocol.models.SyncMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class JdbcSourceTest {

  private static final String STREAM_NAME = "public.id_and_name";
  private static final AirbyteCatalog CATALOG = CatalogHelpers.createAirbyteCatalog(
      STREAM_NAME,
      Field.of("id", JsonSchemaPrimitive.NUMBER),
      Field.of("name", JsonSchemaPrimitive.STRING),
      Field.of("updated_at", JsonSchemaPrimitive.STRING));
  private static final ConfiguredAirbyteCatalog CONFIGURED_CATALOG = CatalogHelpers.toDefaultConfiguredCatalog(CATALOG);
  private static final List<AirbyteMessage> MESSAGES = Lists.newArrayList(
      new AirbyteMessage().withType(Type.RECORD)
          .withRecord(new AirbyteRecordMessage().withStream(STREAM_NAME).withData(Jsons.jsonNode(ImmutableMap.of("id", 1, "name", "picard", "updated_at", "2004-10-19T10:23:54-07:00")))),
      new AirbyteMessage().withType(Type.RECORD)
          .withRecord(new AirbyteRecordMessage().withStream(STREAM_NAME).withData(Jsons.jsonNode(ImmutableMap.of("id", 2, "name", "crusher", "updated_at", "2005-10-19T10:23:54-07:00")))),
      new AirbyteMessage().withType(Type.RECORD)
          .withRecord(new AirbyteRecordMessage().withStream(STREAM_NAME).withData(Jsons.jsonNode(ImmutableMap.of("id", 3, "name", "vash", "updated_at", "2006-10-19T10:23:54-07:00")))));

  private JsonNode config;

  private PostgreSQLContainer<?> container;
  private Database database;

  @BeforeEach
  void setup() throws Exception {
    container = new PostgreSQLContainer<>("postgres:13-alpine");
    container.start();

    config = Jsons.jsonNode(ImmutableMap.builder()
        .put("username", container.getUsername())
        .put("password", container.getPassword())
        .put("jdbc_url", String.format("jdbc:postgresql://%s:%s/%s",
            container.getHost(),
            container.getFirstMappedPort(),
            container.getDatabaseName()))
        .build());

    database = Databases.createPostgresDatabase(
        config.get("username").asText(),
        config.get("password").asText(),
        config.get("jdbc_url").asText());

    database.query(ctx -> {
      ctx.fetch("CREATE TABLE id_and_name(id INTEGER, name VARCHAR(200), updated_at TIMESTAMP WITH TIME ZONE);");
      ctx.fetch("INSERT INTO id_and_name (id, name, updated_at) VALUES (1,'picard', '2004-10-19T10:23:54-07:00'),  (2, 'crusher', '2005-10-19T10:23:54-07:00'), (3, 'vash', '2006-10-19T10:23:54-07:00');");
      return null;
    });
  }

  @AfterEach
  void tearDown() throws Exception {
    database.close();
    container.close();
  }

  @Test
  void testSpec() throws IOException {
    final ConnectorSpecification actual = new JdbcSource().spec();
    final String resourceString = MoreResources.readResource("spec.json");
    final ConnectorSpecification expected = Jsons.deserialize(resourceString, ConnectorSpecification.class);

    assertEquals(expected, actual);
  }

  @Test
  void testCheckSuccess() {
    final AirbyteConnectionStatus actual = new JdbcSource().check(config);
    final AirbyteConnectionStatus expected = new AirbyteConnectionStatus().withStatus(Status.SUCCEEDED);
    assertEquals(expected, actual);
  }

  @Test
  void testCheckFailure() {
    ((ObjectNode) config).put("password", "fake");
    final AirbyteConnectionStatus actual = new JdbcSource().check(config);
    final AirbyteConnectionStatus expected = new AirbyteConnectionStatus().withStatus(Status.FAILED)
        .withMessage("Can't connect with provided configuration.");
    assertEquals(expected, actual);
  }

  @Test
  void testDiscover() throws Exception {
    final AirbyteCatalog actual = new JdbcSource().discover(config);
    assertEquals(CATALOG, actual);
  }

  @Test
  void testReadSuccess() throws Exception {
    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, CONFIGURED_CATALOG, null).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    assertEquals(MESSAGES, actualMessages);
  }

  @Test
  void testReadOneColumn() throws Exception {
    final ConfiguredAirbyteCatalog catalog = CatalogHelpers.createConfiguredAirbyteCatalog(STREAM_NAME, Field.of("id", JsonSchemaPrimitive.NUMBER));

    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, catalog, null).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    final Set<AirbyteMessage> expectedMessages = MESSAGES.stream()
        .map(Jsons::clone)
        .peek(m -> ((ObjectNode) m.getRecord().getData()).remove("name"))
        .collect(Collectors.toSet());
    assertEquals(expectedMessages, actualMessages);
  }

  @Test
  void testReadMultipleTables() throws Exception {
    final String streamName2 = STREAM_NAME + 2;
    database.query(ctx -> {
      ctx.fetch("CREATE TABLE id_and_name2(id INTEGER, name VARCHAR(200));");
      ctx.fetch("INSERT INTO id_and_name2 (id, name) VALUES (1,'picard'),  (2, 'crusher'), (3, 'vash');");

      return null;
    });

    final ConfiguredAirbyteCatalog catalog = new ConfiguredAirbyteCatalog().withStreams(Lists.newArrayList(
        CONFIGURED_CATALOG.getStreams().get(0),
        CatalogHelpers.createConfiguredAirbyteStream(
            streamName2,
            Field.of("id", JsonSchemaPrimitive.NUMBER),
            Field.of("name", JsonSchemaPrimitive.STRING))));
    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, catalog, null).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    final Set<AirbyteMessage> expectedMessages = MESSAGES
        .stream()
        .map(Jsons::clone)
        .peek(m -> m.getRecord().setStream(streamName2))
        .collect(Collectors.toSet());
    expectedMessages.addAll(MESSAGES);

    assertEquals(expectedMessages, actualMessages);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void testReadFailure() {
    final ConfiguredAirbyteStream spiedAbStream = spy(CONFIGURED_CATALOG.getStreams().get(0));
    final ConfiguredAirbyteCatalog catalog = new ConfiguredAirbyteCatalog().withStreams(Lists.newArrayList(spiedAbStream));
    doCallRealMethod().doThrow(new RuntimeException()).when(spiedAbStream).getStream();

    final JdbcSource source = new JdbcSource();

    assertThrows(RuntimeException.class, () -> source.read(config, catalog, null));
  }

  @Test
  void testIncremental() throws Exception {
    CONFIGURED_CATALOG.getStreams().forEach(airbyteStream -> {
      airbyteStream.setSyncMode(SyncMode.INCREMENTAL);
      airbyteStream.setCursorField(Lists.newArrayList("id"));
    });
    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, CONFIGURED_CATALOG, null).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    final Set<AirbyteMessage> expectedMessages = new HashSet<>(MESSAGES);
    expectedMessages.add(new AirbyteMessage()
        .withType(Type.STATE)
    .withState(new AirbyteStateMessage()
    .withData(Jsons.jsonNode(new JdbcState()
      .withStreams(Lists.newArrayList(new JdbcStreamState()
          .withStreamName("public.id_and_name")
          .withCursorField(ImmutableList.of("id"))
          .withCursor("3")
      ))))));

    assertEquals(expectedMessages, actualMessages);
  }

  @Test
  void testIncrementalIntCheckCursor() throws Exception {
    CONFIGURED_CATALOG.getStreams().forEach(airbyteStream -> {
      airbyteStream.setSyncMode(SyncMode.INCREMENTAL);
      airbyteStream.setCursorField(Lists.newArrayList("id"));
    });

    final JdbcState state = new JdbcState()
            .withStreams(Lists.newArrayList(new JdbcStreamState()
                .withStreamName("public.id_and_name")
                .withCursorField(ImmutableList.of("id"))
                .withCursor("2")
            ));

    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, CONFIGURED_CATALOG, Jsons.jsonNode(state)).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    final Set<AirbyteMessage> expectedMessages = new HashSet<>();
    expectedMessages.add(MESSAGES.get(2));
    expectedMessages.add(new AirbyteMessage()
        .withType(Type.STATE)
        .withState(new AirbyteStateMessage()
            .withData(Jsons.jsonNode(new JdbcState()
                .withStreams(Lists.newArrayList(new JdbcStreamState()
                    .withStreamName("public.id_and_name")
                    .withCursorField(ImmutableList.of("id"))
                    .withCursor("3")
                ))))));

    assertEquals(expectedMessages, actualMessages);
  }

  @Test
  void testIncrementalStringCheckCursor() throws Exception {
    CONFIGURED_CATALOG.getStreams().forEach(airbyteStream -> {
      airbyteStream.setSyncMode(SyncMode.INCREMENTAL);
      airbyteStream.setCursorField(Lists.newArrayList("name"));
    });

    final JdbcState state = new JdbcState()
        .withStreams(Lists.newArrayList(new JdbcStreamState()
            .withStreamName("public.id_and_name")
            .withCursorField(ImmutableList.of("name"))
            .withCursor("patent")
        ));

    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, CONFIGURED_CATALOG, Jsons.jsonNode(state)).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    final Set<AirbyteMessage> expectedMessages = new HashSet<>();
    expectedMessages.add(MESSAGES.get(0));
    expectedMessages.add(MESSAGES.get(2));
    expectedMessages.add(new AirbyteMessage()
        .withType(Type.STATE)
        .withState(new AirbyteStateMessage()
            .withData(Jsons.jsonNode(new JdbcState()
                .withStreams(Lists.newArrayList(new JdbcStreamState()
                    .withStreamName("public.id_and_name")
                    .withCursorField(ImmutableList.of("name"))
                    .withCursor("vash")
                ))))));

    assertEquals(expectedMessages, actualMessages);
  }

  @Test
  void testIncrementalTimestampzCheckCursor() throws Exception {
    CONFIGURED_CATALOG.getStreams().forEach(airbyteStream -> {
      airbyteStream.setSyncMode(SyncMode.INCREMENTAL);
      airbyteStream.setCursorField(Lists.newArrayList("updated_at"));
    });

    final JdbcState state = new JdbcState()
        .withStreams(Lists.newArrayList(new JdbcStreamState()
            .withStreamName("public.id_and_name")
            .withCursorField(ImmutableList.of("updated_at"))
            .withCursor("2005-10-19T9:23:54-07:00")
        ));

    final Set<AirbyteMessage> actualMessages = new JdbcSource().read(config, CONFIGURED_CATALOG, Jsons.jsonNode(state)).collect(Collectors.toSet());

    actualMessages.forEach(r -> {
      if (r.getRecord() != null) {
        r.getRecord().setEmittedAt(null);
      }
    });

    final Set<AirbyteMessage> expectedMessages = new HashSet<>();
    expectedMessages.add(MESSAGES.get(1));
    expectedMessages.add(MESSAGES.get(2));
    expectedMessages.add(new AirbyteMessage()
        .withType(Type.STATE)
        .withState(new AirbyteStateMessage()
            .withData(Jsons.jsonNode(new JdbcState()
                .withStreams(Lists.newArrayList(new JdbcStreamState()
                    .withStreamName("public.id_and_name")
                    .withCursorField(ImmutableList.of("updated_at"))
                    .withCursor("2006-10-19T10:23:54-07:00")
                ))))));

    assertEquals(expectedMessages, actualMessages);
  }
}
