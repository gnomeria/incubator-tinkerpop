/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.process.traversal;

import org.apache.tinkerpop.gremlin.process.computer.Computer;
import org.apache.tinkerpop.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.SubgraphStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.ReadOnlyStrategy;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BytecodeTest {

    @Test
    public void shouldHaveProperHashAndEquality() {
        final GraphTraversalSource g = EmptyGraph.instance().traversal();
        final Traversal.Admin traversal1 = g.V().out().repeat(__.out().in()).times(2).groupCount().by(__.outE().count()).select(Column.keys).order().by(Order.decr).asAdmin();
        final Traversal.Admin traversal2 = g.V().out().repeat(__.out().in()).times(2).groupCount().by(__.outE().count()).select(Column.keys).order().by(Order.decr).asAdmin();
        final Traversal.Admin traversal3 = g.V().out().repeat(__.out().in()).times(2).groupCount().by(__.outE().count()).select(Column.values).order().by(Order.decr).asAdmin();

        assertEquals(traversal1, traversal2);
        assertNotEquals(traversal1, traversal3);
        assertNotEquals(traversal2, traversal3);
        //
        assertEquals(traversal1.hashCode(), traversal2.hashCode());
        assertNotEquals(traversal1.hashCode(), traversal3.hashCode());
        assertNotEquals(traversal2.hashCode(), traversal3.hashCode());
        //
        assertEquals(traversal1.getBytecode(), traversal2.getBytecode());
        assertNotEquals(traversal1.getBytecode(), traversal3.getBytecode());
        assertNotEquals(traversal2.getBytecode(), traversal3.getBytecode());
        //
        assertEquals(traversal1.getBytecode().hashCode(), traversal2.getBytecode().hashCode());
        assertNotEquals(traversal1.getBytecode().hashCode(), traversal3.getBytecode().hashCode());
        assertNotEquals(traversal2.getBytecode().hashCode(), traversal3.getBytecode().hashCode());

    }

    @Test
    public void shouldCloneCorrectly() {
        final GraphTraversalSource g = EmptyGraph.instance().traversal();
        final Bytecode bytecode = g.V().out().asAdmin().getBytecode();
        final Bytecode bytecodeClone = bytecode.clone();
        assertEquals(bytecode, bytecodeClone);
        assertEquals(bytecode.hashCode(), bytecodeClone.hashCode());
        bytecodeClone.addStep("in", "created"); // mutate the clone and the original should stay the same
        assertNotEquals(bytecode, bytecodeClone);
        assertNotEquals(bytecode.hashCode(), bytecodeClone.hashCode());
        assertEquals(2, IteratorUtils.count(bytecode.getInstructions()));
        assertEquals(3, IteratorUtils.count(bytecodeClone.getInstructions()));
    }

    @Test
    public void shouldIncludeBindingsInEquality() {
        final Bindings b = new Bindings();
        final GraphTraversalSource g = EmptyGraph.instance().traversal().withBindings(b);

        final Bytecode bytecode1 = g.V().out(b.of("a", "created")).asAdmin().getBytecode();
        final Bytecode bytecode2 = g.V().out(b.of("a", "knows")).asAdmin().getBytecode();
        final Bytecode bytecode3 = g.V().out(b.of("b", "knows")).asAdmin().getBytecode();
        final Bytecode bytecode4 = g.V().out(b.of("b", "knows")).asAdmin().getBytecode();

        assertNotEquals(bytecode1, bytecode2);
        assertNotEquals(bytecode1, bytecode3);
        assertNotEquals(bytecode2, bytecode3);
        assertNotEquals(bytecode2, bytecode4);
        assertNotEquals(bytecode1, bytecode4);
        assertEquals(bytecode3, bytecode4);

        assertEquals(1, bytecode1.getBindings().size());
        assertEquals("created", bytecode1.getBindings().get("a"));
    }

    @Test
    public void shouldConvertStrategies() {
        final GraphTraversalSource g = EmptyGraph.instance().traversal();
        Bytecode bytecode = g.withStrategies(ReadOnlyStrategy.instance()).getBytecode();
        assertEquals(ReadOnlyStrategy.instance(), bytecode.getSourceInstructions().iterator().next().getArguments()[0]);
        bytecode = g.withStrategies(SubgraphStrategy.build().edges(__.hasLabel("knows")).create()).getBytecode();
        assertEquals(SubgraphStrategy.build().edges(__.hasLabel("knows")).create().getEdgeCriterion().asAdmin().getBytecode(),
                ((SubgraphStrategy) bytecode.getSourceInstructions().iterator().next().getArguments()[0]).getEdgeCriterion().asAdmin().getBytecode());
    }

    @Test
    public void shouldConvertComputer() {
        final GraphTraversalSource g = EmptyGraph.instance().traversal();
        Bytecode bytecode = g.withComputer(Computer.compute().workers(10)).getBytecode();
        assertEquals(VertexProgramStrategy.build().create(), bytecode.getSourceInstructions().iterator().next().getArguments()[0]);
        assertEquals(VertexProgramStrategy.build().workers(10).create().getConfiguration().getInt(VertexProgramStrategy.WORKERS),
                ((VertexProgramStrategy) bytecode.getSourceInstructions().iterator().next().getArguments()[0]).getConfiguration().getInt(VertexProgramStrategy.WORKERS));
    }
}
