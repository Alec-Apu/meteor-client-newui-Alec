/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.renderer;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;

/** Dynamic uniform uploads using the 26.3 backend-independent storage API. */
public final class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> implements AutoCloseable {
    private final DynamicGpuDataStorage<T> storage;

    public DynamicUniformStorage(String name, int size, int capacity) {
        storage = new DynamicGpuDataStorageMapped<>(name, size, GpuBuffer.USAGE_UNIFORM, capacity);
    }

    public GpuBufferSlice writeUniform(T data) { return storage.writeData(data); }
    public void endFrame() { storage.endFrame(); }
    @Override public void close() { storage.close(); }

    public interface DynamicUniform extends DynamicGpuDataStorage.DynamicGpuData {}
}
