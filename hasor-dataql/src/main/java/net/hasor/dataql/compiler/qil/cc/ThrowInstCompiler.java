/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.compiler.qil.cc;
import net.hasor.dataql.compiler.ast.Variable;
import net.hasor.dataql.compiler.ast.inst.ThrowInst;
import net.hasor.dataql.compiler.qil.CompilerStack;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;

/**
 * throw指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ThrowInstCompiler extends InstCompiler<ThrowInst> {
    @Override
    public void doCompiler(ThrowInst inst, InstQueue queue, CompilerStack stackTree) {
        queue.inst(LDC_D, inst.getErrorCode());
        Variable dataValue = inst.getThrowData();
        findInstCompilerByInst(dataValue).doCompiler(dataValue, queue, stackTree);
        queue.inst(THROW);
    }
}