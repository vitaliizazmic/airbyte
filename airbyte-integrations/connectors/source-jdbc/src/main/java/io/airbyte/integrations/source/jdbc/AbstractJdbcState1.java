package io.airbyte.integrations.source.jdbc;

import io.airbyte.integrations.source.jdbc.models.JdbcState;
import java.util.List;
import java.util.Map;

public class AbstractJdbcState1 {
  private final Map<String, CursorInfo> originalStreamNameToCursorInfo;
  private final Map<String, CursorInfo> currentstreamNameToCursorInfo;

  public AbstractJdbcState1(JdbcState serialized) {
    serialized
  }

  public static class CursorInfo {
    final List<String> cursorField;
    final String value;

    public CursorInfo(List<String> cursorField, String value) {
      this.cursorField = cursorField;
      this.value = value;
    }

    public List<String> getCursorField() {
      return cursorField;
    }

    public String getCursorField2() {
      return Arrays.;
    }

    public String getValue() {
      return value;
    }
  }

  public static class CursorInfoMutable extends CursorInfo {

    public CursorInfoMutable(List<String> cursorField, String value) {
      super(cursorField, value);
    }


  }
}
