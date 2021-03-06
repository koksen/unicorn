/*******************************************************************************
  * (C) Copyright 2017 Haifeng Li
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *******************************************************************************/

package unicorn.unibase

import unicorn.bigtable.{Column, OrderedBigTable, RowIterator}
import unicorn.json._
import unicorn.kv._

/** Keyspace scan operations.
  *
  * @author Haifeng Li
  */
trait KeyspaceScanOps {
  val table: OrderedKeyspace
  val rowkey: RowKey

  /** Scan the whole table. */
  def scan: Iterator[KeyValue] = {
    table.scan
  }

  /** Scan the the rows whose key starts with the given prefix. */
  def scan(prefix: Key): Iterator[KeyValue] = {
    table.scan(rowkey.serialize(prefix))
  }

  /** Scan the the rows in the given range.
    * @param start row to start scanner at or after (inclusive)
    * @param end row to stop scanner before (exclusive)
    */
  def scan(start: Key, end: Key): Iterator[KeyValue] = {
    val startKey = rowkey.serialize(start)
    val endKey = rowkey.serialize(end)

    val c = compareByteArray(startKey, endKey)
    if (c == 0)
      throw new IllegalArgumentException("Start and end keys are the same")

    if (c < 0)
      table.scan(startKey, endKey)
    else
      table.scan(endKey, startKey)
  }
}

/** BigTable scan operations.
  *
  * @author Haifeng Li
  */
trait BigTableScanOps {
  val table: OrderedBigTable
  val rowkey: RowKey

  /** Scan the whole table. */
  def scan(fields: Seq[String]): RowIterator = {
    table.scan(DocumentColumnFamily, fields)
  }

  /** Scan the the rows whose key starts with the given prefix. */
  def scan(prefix: Key, fields: String*): RowIterator = {
    table.scanPrefix(rowkey.serialize(prefix), DocumentColumnFamily, fields)
  }

  /** Scan the the rows in the given range.
    * @param start row to start scanner at or after (inclusive)
    * @param end row to stop scanner before (exclusive)
    */
  def scan(start: Key, end: Key, fields: String*): RowIterator = {
    val startKey = rowkey.serialize(start)
    val endKey = rowkey.serialize(end)

    val c = compareByteArray(startKey, endKey)
    if (c == 0)
      throw new IllegalArgumentException("Start and end keys are the same")

    if (c < 0)
      table.scan(startKey, endKey, DocumentColumnFamily, fields)
    else
      table.scan(endKey, startKey, DocumentColumnFamily, fields)
  }
}

/** Row scan iterator */
trait DocumentIterator extends Iterator[JsObject] with AutoCloseable {
  val rows: RowIterator

  override def hasNext: Boolean = rows.hasNext

  override def next: JsObject = {
    val columns = rows.next.families(0).columns
    deserialize(columns)
  }

  override def close: Unit = rows.close

  private[unibase] def deserialize(columns: Seq[Column]): JsObject
}

private class SimpleDocumentIterator(val rows: RowIterator) extends DocumentIterator {
  val serializer = new JsonSerializer()

  override def deserialize(columns: Seq[Column]): JsObject = {
    serializer.deserialize(columns(0).value).asInstanceOf[JsObject]
  }
}

private class TableIterator(val rows: RowIterator) extends DocumentIterator {
  val serializer = new JsonSerializer()

  override def deserialize(columns: Seq[Column]): JsObject = {
    val doc = JsObject()
    columns.foreach { case Column(qualifier, value, _) =>
      doc(qualifier) = serializer.deserialize(value)
    }
    doc
  }
}
