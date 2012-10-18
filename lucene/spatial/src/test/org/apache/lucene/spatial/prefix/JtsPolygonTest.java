package org.apache.lucene.spatial.prefix;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.spatial4j.core.context.SpatialContextFactory;
import com.spatial4j.core.shape.Shape;
import org.apache.lucene.spatial.StrategyTestCase;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class JtsPolygonTest extends StrategyTestCase {

  private static final double LUCENE_4464_distErrPct = SpatialArgs.DEFAULT_DISTERRPCT;//DEFAULT 2.5%

  public JtsPolygonTest() {
    try {
      HashMap<String, String> args = new HashMap<String, String>();
      args.put("spatialContextFactory",
          "com.spatial4j.core.context.jts.JtsSpatialContextFactory");
      ctx = SpatialContextFactory.makeSpatialContext(args, getClass().getClassLoader());
    } catch (NoClassDefFoundError e) {
      assumeTrue("This test requires JTS jar: "+e, false);
    }

    GeohashPrefixTree grid = new GeohashPrefixTree(ctx, 11);//< 1 meter == 11 maxLevels
    this.strategy = new RecursivePrefixTreeStrategy(grid, getClass().getSimpleName());
    ((RecursivePrefixTreeStrategy)this.strategy).setDistErrPct(LUCENE_4464_distErrPct);//1% radius (small!)
  }

  @Test
  /** LUCENE-4464 */
  public void testCloseButNoMatch() throws IOException {
    getAddAndVerifyIndexedDocuments("LUCENE-4464.txt");
    SpatialArgs args = q(
        "POLYGON((-93.18100824442227 45.25676372469945," +
            "-93.23182001200654 45.21421290799412," +
            "-93.16315546122038 45.23742639412364," +
            "-93.18100824442227 45.25676372469945))",
        LUCENE_4464_distErrPct);
    SearchResults got = executeQuery(strategy.makeQuery(args), 100);
    assertEquals(1, got.numFound);
    assertEquals("poly2", got.results.get(0).document.get("id"));
    //did not find poly 1 !
  }

  private SpatialArgs q(String shapeStr, double distErrPct) {
    Shape shape = ctx.readShape(shapeStr);
    SpatialArgs args = new SpatialArgs(SpatialOperation.Intersects, shape);
    args.setDistErrPct(distErrPct);
    return args;
  }

}
