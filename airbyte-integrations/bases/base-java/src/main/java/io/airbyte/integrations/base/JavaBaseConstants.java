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

package io.airbyte.integrations.base;

public class JavaBaseConstants {

  public static String ARGS_CONFIG_KEY = "config";
  public static String ARGS_CATALOG_KEY = "catalog";
  public static String ARGS_STATE_KEY = "state";

  public static String ARGS_CONFIG_DESC = "path to the json configuration file";
  public static String ARGS_CATALOG_DESC = "input path for the catalog";
  public static String ARGS_PATH_DESC = "path to the json-encoded state file";

  public static String COLUMN_NAME_AB_ID = "ab_id";
  public static String COLUMN_NAME_EMITTED_AT = "emitted_at";
  public static String COLUMN_NAME_DATA = "data";

}
