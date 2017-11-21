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

import unicorn.bigtable._
import unicorn.json._

/** Scan operations.
  *
  * @author Haifeng Li
  */
trait Scan {
  val table: BigTable with RowScan
  val rowkey: RowKey

  /** Scan the whole table. */
  def scan: Scanner = {
    scanner(table.scan(DocumentColumnFamily))
  }

  /** Scan the the rows whose key starts with the given prefix. */
  def scan(prefix: Key): Scanner = {
    if (prefix.isInstanceOf[CompoundKey]) {
      if (!rowkey.isInstanceOf[CompoundRowKey])
        throw new IllegalArgumentException("Invalid compound key. The table doesn't have compound row key")
      else {
        val key = prefix.asInstanceOf[CompoundKey]
        val compound = rowkey.asInstanceOf[CompoundRowKey]
        if (key.keys.size > compound.keys.size)
          throw new IllegalArgumentException("Too many compound key elements.")
      }
    }

    scanner(table.scanPrefix(rowkey(prefix), DocumentColumnFamily))
  }

  /** Scan the the rows in the given range.
    * @param start row to start scanner at or after (inclusive)
    * @param end row to stop scanner before (exclusive)
    */
  def scan(start: Key, end: Key): Scanner = {
    if (start.isInstanceOf[CompoundKey]) {
      if (!rowkey.isInstanceOf[CompoundRowKey])
        throw new IllegalArgumentException("Invalid compound key. The table doesn't have compound row key")
      else {
        val key = start.asInstanceOf[CompoundKey]
        val compound = rowkey.asInstanceOf[CompoundRowKey]
        if (key.keys.size > compound.keys.size)
          throw new IllegalArgumentException("Too many compound key elements.")
      }
    }

    if (end.isInstanceOf[CompoundKey]) {
      if (!rowkey.isInstanceOf[CompoundRowKey])
        throw new IllegalArgumentException("Invalid compound key. The table doesn't have compound row key")
      else {
        val key = end.asInstanceOf[CompoundKey]
        val compound = rowkey.asInstanceOf[CompoundRowKey]
        if (key.keys.size > compound.keys.size)
          throw new IllegalArgumentException("Too many compound key elements.")
      }
    }

    scanner(table.scan(rowkey(start), rowkey(end), DocumentColumnFamily))
  }

  private def scanner(rows: RowScanner): Scanner = {
    if (this.isInstanceOf[Drawer])
      new DrawerScanner(rows)
    else if (this.isInstanceOf[Table])
      new TableScanner(rows)
    else
      throw new IllegalStateException("Unsupported Scan table type: " + getClass)
  }
}

/** Row scan iterator */
trait Scanner extends Traversable[JsValue] {
  val rows: RowScanner

  def foreach[U](f: JsValue => U): Unit = {
    while (rows.hasNext) {
      val columns = rows.next.families(0).columns
      f(deserialize(columns))
    }
  }

  def close: Unit = rows.close

  def deserialize(columns: Seq[Column]): JsObject
}

private class DrawerScanner(val rows: RowScanner) extends Scanner {
  val serializer = new JsonSerializer()

  override def deserialize(columns: Seq[Column]): JsValue = {
    serializer.deserialize(columns(0).value)
  }
}


private class TableScanner(val rows: RowScanner) extends Scanner {
  val serializer = new JsonSerializer()

  override def deserialize(columns: Seq[Column]): JsValue = {
    val doc = JsObject()
    columns.foreach { case Column(qualifier, value, _) =>
      doc(qualifier) = serializer.deserialize(value)
    }
    doc
  }
}