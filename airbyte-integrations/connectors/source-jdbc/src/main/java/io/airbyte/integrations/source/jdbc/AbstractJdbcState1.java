package io.airbyte.integrations.source.jdbc;

import io.airbyte.integrations.source.jdbc.models.JdbcState;
import io.airbyte.integrations.source.jdbc.models.JdbcStreamState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AbstractJdbcState1 {
  private final Map<String, CursorInfo> map;

  public AbstractJdbcState1(JdbcState serialized) {
    final JdbcStreamState jdbcStreamState = new JdbcStreamState();
    map = serialized.getStreams()
        .stream()
        .collect(Collectors.toMap(s -> s.getStreamName(),  s -> new CursorInfo(s.getCursorField(), s.getCursor())));
  }

  public Optional<CursorInfo> getCursorInfo(String streamName) {
    return Optional.ofNullable(map.get(streamName));
  }


  public static class CursorInfo {
    private final List<String> originalCusorField;
    private final String originalCursor;

    private List<String> cursorField;
    private String cursor;

    public CursorInfo(List<String> cursorField, String cursor) {
      this.originalCusorField = cursorField;
      this.originalCursor = cursor;
      this.cursorField = cursorField;
      this.cursor = cursor;
    }

    public List<String> getOriginalCursorField() {
      return originalCusorField;
    }

    public String getOriginalCursorField2() {
      return String.join(".", originalCusorField);
    }

    public String getOriginalCursor() {
      return originalCursor;
    }

    public List<String> getCursorField() {
      return cursorField;
    }

    public String getCursorField2() {
      return String.join(".", cursorField);
    }

    public String getCursor() {
      return cursor;
    }

    public void setCursorField(List<String> cursorField) {
      this.cursorField = cursorField;
    }

    public void setCursor(String cursor) {
      this.cursor = cursor;
    }
  }
}
