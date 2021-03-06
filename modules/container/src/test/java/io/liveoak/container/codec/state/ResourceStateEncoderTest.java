/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.codec.state;

import io.liveoak.common.codec.state.ResourceStateEncoder;
import io.liveoak.container.InMemoryObjectResource;
import io.liveoak.common.codec.DefaultResourceState;
import io.liveoak.common.codec.NonEncodableValueException;
import io.liveoak.common.codec.driver.EncodingDriver;
import io.liveoak.common.codec.driver.RootEncodingDriver;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.state.ResourceRef;
import io.liveoak.spi.state.ResourceState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.fest.assertions.Fail;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Bob McWhirter
 */
public class ResourceStateEncoderTest {

    protected EncodingDriver createDriver(Resource resource, CompletableFuture<ResourceState> future) throws Exception {
        ResourceStateEncoder encoder = new ResourceStateEncoder();
        ByteBuf buffer = Unpooled.buffer();
        encoder.initialize(buffer);
        RootEncodingDriver driver = new RootEncodingDriver(new RequestContext.Builder().build(), encoder, resource, () -> {
            future.complete(encoder.root());
        }, null);
        return driver;
    }

    @Test
    public void testEmptyObject() throws Exception {
        DefaultResourceState state = new DefaultResourceState();
        InMemoryObjectResource resource = new InMemoryObjectResource(null, "bob", state);

        CompletableFuture<ResourceState> future = new CompletableFuture<>();
        EncodingDriver driver = createDriver(resource, future);
        driver.encode();

        ResourceState encoded = future.get();

        assertThat(encoded.id()).isEqualTo("bob");
        assertThat(encoded.getPropertyNames()).isEmpty();
        assertThat(encoded.members()).isEmpty();
    }

    @Test
    public void testInvalidObjectType() throws Exception {
        DefaultResourceState state = new DefaultResourceState();
        Object invalid = new Object();
        state.putProperty("invalid", invalid );

        InMemoryObjectResource resource = new InMemoryObjectResource(null, "bob", state);

        CompletableFuture<ResourceState> future = new CompletableFuture<>();
        EncodingDriver driver = createDriver(resource, future);

        try {
            driver.encode();
            Fail.fail();
        } catch (NonEncodableValueException e) {
            //expected
            assertThat( e.getValue() ).isSameAs( invalid );
        }

    }

    @Test
    public void testObjectWithProperties() throws Exception {
        DefaultResourceState state = new DefaultResourceState();
        state.putProperty("name", "Bob McWhirter");
        InMemoryObjectResource resource = new InMemoryObjectResource(null, "bob", state);

        CompletableFuture<ResourceState> future = new CompletableFuture<>();
        EncodingDriver driver = createDriver(resource, future);
        driver.encode();
        ResourceState encoded = future.get();

        assertThat(encoded.id()).isEqualTo("bob");
        assertThat(encoded.getPropertyNames()).hasSize(1);
        assertThat(encoded.getPropertyNames()).contains("name");
        assertThat(encoded.getProperty("name")).isEqualTo("Bob McWhirter");
        assertThat(encoded.members()).hasSize(0);
    }

    @Test
    public void testObjectWithResourceProperty() throws Exception {
        DefaultResourceState mosesState = new DefaultResourceState();
        mosesState.putProperty("name", "Moses");
        mosesState.putProperty("breed", "German Shepherd");
        InMemoryObjectResource mosesResourse = new InMemoryObjectResource(null, "moses", mosesState);


        DefaultResourceState bobState = new DefaultResourceState();
        bobState.putProperty("name", "Bob McWhirter");
        bobState.putProperty("dog", mosesResourse);
        InMemoryObjectResource bobResource = new InMemoryObjectResource(null, "bob", bobState);

        CompletableFuture<ResourceState> future = new CompletableFuture<>();
        EncodingDriver driver = createDriver(bobResource, future);
        driver.encode();
        ResourceState encoded = future.get();

        System.err.println(encoded);

        assertThat(encoded.id()).isEqualTo("bob");
        assertThat(encoded.getPropertyNames()).hasSize(2);

        assertThat(encoded.getPropertyNames()).contains("name");
        assertThat(encoded.getProperty("name")).isEqualTo("Bob McWhirter");

        assertThat(encoded.getPropertyNames()).contains("dog");
        assertThat(encoded.getProperty("dog")).isNotNull();
        assertThat(encoded.getProperty("dog")).isInstanceOf(ResourceRef.class);

        assertThat(encoded.members()).hasSize(0);

        ResourceRef encodedMoses = (ResourceRef) encoded.getProperty("dog");
        assertThat(encodedMoses.uri()).isEqualTo(mosesResourse.uri());
    }

    @Test
    public void testObjectWithResourceArrayProperty() throws Exception {
        DefaultResourceState mosesState = new DefaultResourceState();
        mosesState.putProperty("name", "Moses");
        mosesState.putProperty("breed", "German Shepherd");
        InMemoryObjectResource mosesResourse = new InMemoryObjectResource(null, "moses", mosesState);

        DefaultResourceState onlyState = new DefaultResourceState();
        onlyState.putProperty("name", "Only");
        onlyState.putProperty("breed", "Lab/Huskie Mix");
        InMemoryObjectResource onlyResource = new InMemoryObjectResource(null, "only", onlyState);

        DefaultResourceState bobState = new DefaultResourceState();
        bobState.putProperty("name", "Bob McWhirter");
        ArrayList<Resource> dogs = new ArrayList<>();
        dogs.add(mosesResourse);
        dogs.add(onlyResource);
        bobState.putProperty("dogs", dogs);
        InMemoryObjectResource bobResource = new InMemoryObjectResource(null, "bob", bobState);

        CompletableFuture<ResourceState> future = new CompletableFuture<>();
        EncodingDriver driver = createDriver(bobResource, future);
        driver.encode();

        ResourceState encoded = future.get();
        System.err.println(encoded);

        assertThat(encoded.id()).isEqualTo("bob");
        assertThat(encoded.getPropertyNames()).hasSize(2);

        assertThat(encoded.getPropertyNames()).contains("name");
        assertThat(encoded.getProperty("name")).isEqualTo("Bob McWhirter");

        assertThat(encoded.getPropertyNames()).contains("dogs");
        assertThat(encoded.getProperty("dogs")).isNotNull();
        assertThat(encoded.getProperty("dogs")).isInstanceOf(List.class);

        assertThat(encoded.members()).hasSize(0);

        List<ResourceRef> encodedDogs = (List<ResourceRef>) encoded.getProperty("dogs");

        assertThat(encodedDogs).hasSize(2);

        assertThat(encodedDogs.get(0).uri()).isEqualTo(mosesResourse.uri());
        assertThat(encodedDogs.get(1).uri()).isEqualTo(onlyResource.uri());
    }
}
