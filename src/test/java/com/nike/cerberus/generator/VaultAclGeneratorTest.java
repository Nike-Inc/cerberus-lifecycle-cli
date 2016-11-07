/*
 * Copyright (c) 2016 Nike Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.generator;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.nike.cerberus.domain.configuration.VaultAclEntry;
import com.nike.cerberus.domain.template.VaultAclInput;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VaultAclGeneratorTest {

    @Test
    public void testGenerate() throws IOException {
        UuidSupplier uuidSupplier = mock(UuidSupplier.class);
        MustacheFactory mustacheFactory = mock(MustacheFactory.class);
        Mustache mustache = mock(Mustache.class);

        String aclToken = "acl-token";
        when(uuidSupplier.get()).thenReturn(aclToken);
        when(mustacheFactory.compile("templates/vault-acl.json.mustache")).thenReturn(mustache);

        ArgumentCaptor<Writer> writer = ArgumentCaptor.forClass(Writer.class);
        ArgumentCaptor<VaultAclInput> input = ArgumentCaptor.forClass(VaultAclInput.class);

        when(mustache.execute(writer.capture(), input.capture())).thenReturn(mock(Writer.class));

        VaultAclGenerator generator = new VaultAclGenerator(uuidSupplier, mustacheFactory);

        // invoke method under test
        VaultAclEntry result = generator.generate();

        assertEquals(aclToken, input.getValue().getAclToken());
        assertEquals(aclToken, result.getAclToken());
        assertEquals(writer.getValue().toString(), result.getEntry()); // test could be improved, compares empty Strings here
    }

}