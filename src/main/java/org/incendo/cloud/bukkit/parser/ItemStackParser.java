//
// MIT License
//
// Copyright (c) 2024 Incendo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package org.incendo.cloud.bukkit.parser;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.brigadier.parser.WrappedBrigadierParser;
import org.incendo.cloud.bukkit.data.ProtoItemStack;
import org.incendo.cloud.bukkit.internal.CommandBuildContextSupplier;
import org.incendo.cloud.bukkit.internal.CraftBukkitReflection;
import org.incendo.cloud.bukkit.internal.MinecraftArgumentTypes;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.SuggestionProvider;

/**
 * Custom override of ItemStackParser to support Minecraft / Paper 26.3 without
 * throwing ExceptionInInitializerError during PaperCommandManager initialization.
 *
 * @param <C> Command sender type
 */
public class ItemStackParser<C> implements ArgumentParser.FutureArgumentParser<C, ProtoItemStack> {

    public static <C> @NonNull ParserDescriptor<C, ProtoItemStack> itemStackParser() {
        return ParserDescriptor.of(new ItemStackParser<>(), ProtoItemStack.class);
    }

    public static <C> CommandComponent.@NonNull Builder<C, ProtoItemStack> itemStackComponent() {
        return CommandComponent.<C, ProtoItemStack>builder().parser(itemStackParser());
    }

    private final ArgumentParser<C, ProtoItemStack> parser;

    private static @Nullable Class<?> findItemInputClass() {
        try {
            final Class<?>[] classes = new Class<?>[]{
                    CraftBukkitReflection.findNMSClass("ArgumentPredicateItemStack"),
                    CraftBukkitReflection.findMCClass("commands.arguments.item.ArgumentPredicateItemStack"),
                    CraftBukkitReflection.findMCClass("commands.arguments.item.ItemInput")
            };
            for (final Class<?> clazz : classes) {
                if (clazz != null) {
                    return clazz;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Create a new {@link ItemStackParser} with resilient fallback.
     */
    public ItemStackParser() {
        ArgumentParser<C, ProtoItemStack> chosenParser;
        try {
            if (findItemInputClass() != null) {
                chosenParser = new ModernParser<>();
            } else {
                chosenParser = new LegacyParser<>();
            }
        } catch (Throwable t) {
            chosenParser = new LegacyParser<>();
        }
        this.parser = chosenParser;
    }

    @Override
    public final @NonNull CompletableFuture<@NonNull ArgumentParseResult<ProtoItemStack>> parseFuture(
            final @NonNull CommandContext<C> commandContext,
            final @NonNull CommandInput commandInput
    ) {
        return this.parser.parseFuture(commandContext, commandInput);
    }

    @Override
    public final @NonNull SuggestionProvider<C> suggestionProvider() {
        return this.parser.suggestionProvider();
    }

    private static final class ModernParser<C> implements ArgumentParser.FutureArgumentParser<C, ProtoItemStack> {

        private static final Class<?> NMS_ITEM_STACK_CLASS = findClassSafely(
                "net.minecraft.world.item.ItemStack", "ItemStack"
        );
        private static final Class<?> CRAFT_ITEM_STACK_CLASS = findOBCClassSafely("inventory.CraftItemStack");
        private static final Supplier<Class<?>> ARGUMENT_ITEM_STACK_CLASS =
            Suppliers.memoize(() -> {
                try {
                    return MinecraftArgumentTypes.getClassByKey(NamespacedKey.minecraft("item_stack"));
                } catch (Throwable t) {
                    return null;
                }
            });
        private static final Class<?> ITEM_INPUT_CLASS = findItemInputClass();
        private static final Class<?> NMS_ITEM_CLASS = findClassSafely(
                "net.minecraft.world.item.Item", "Item"
        );
        private static final Supplier<Method> GET_MATERIAL_METHOD = Suppliers.memoize(() -> {
            try {
                return CraftBukkitReflection.needMethod(CraftBukkitReflection.needOBCClass("util.CraftMagicNumbers"), "getMaterial", NMS_ITEM_CLASS);
            } catch (Throwable t) {
                return null;
            }
        });
        private static final Method CREATE_ITEM_STACK_METHOD = findMethodSafely(
                ITEM_INPUT_CLASS,
                new String[]{"a", "createItemStack", "createItemStack"},
                new Class<?>[][]{new Class<?>[]{int.class, boolean.class}, new Class<?>[]{int.class, boolean.class}, new Class<?>[]{int.class}}
        );
        private static final Method AS_BUKKIT_STACK_METHOD = findAsBukkitStackMethod();
        private static final Field ITEM_FIELD = findFieldSafely(ITEM_INPUT_CLASS, "b", "item");
        private static final Field EXTRA_DATA_FIELD = findFieldSafely(ITEM_INPUT_CLASS, "c", "tag", "components");
        private static final Class<?> HOLDER_CLASS = findClassSafely("net.minecraft.core.Holder", "core.Holder");
        private static final @Nullable Method VALUE_METHOD = HOLDER_CLASS == null
                ? null
                : findMethodSafely(HOLDER_CLASS, new String[]{"value", "a"}, new Class<?>[][]{new Class<?>[]{}, new Class<?>[]{}});
        private static final Class<?> NBT_TAG_CLASS = findClassSafely(
                "net.minecraft.nbt.Tag", "net.minecraft.nbt.NBTBase", "NBTBase"
        );

        private static Class<?> findClassSafely(String... names) {
            for (String name : names) {
                try {
                    Class<?> clazz = CraftBukkitReflection.findMCClass(name);
                    if (clazz != null) return clazz;
                    clazz = CraftBukkitReflection.findNMSClass(name);
                    if (clazz != null) return clazz;
                    clazz = CraftBukkitReflection.findClass(name);
                    if (clazz != null) return clazz;
                } catch (Throwable ignored) {}
            }
            return null;
        }

        private static Class<?> findOBCClassSafely(String name) {
            try {
                return CraftBukkitReflection.findOBCClass(name);
            } catch (Throwable t) {
                return null;
            }
        }

        private static Method findMethodSafely(Class<?> clazz, String[] names, Class<?>[][] paramTypes) {
            if (clazz == null) return null;
            for (int i = 0; i < names.length; i++) {
                try {
                    Method m = CraftBukkitReflection.findMethod(clazz, names[i], paramTypes[i]);
                    if (m != null) return m;
                } catch (Throwable ignored) {}
            }
            return null;
        }

        private static Method findAsBukkitStackMethod() {
            if (CRAFT_ITEM_STACK_CLASS == null) return null;
            if (NMS_ITEM_STACK_CLASS != null) {
                try {
                    Method m = CraftBukkitReflection.findMethod(CRAFT_ITEM_STACK_CLASS, "asBukkitCopy", NMS_ITEM_STACK_CLASS);
                    if (m != null) return m;
                } catch (Throwable ignored) {}
                try {
                    Method m = CraftBukkitReflection.findMethod(CRAFT_ITEM_STACK_CLASS, "asCraftMirror", NMS_ITEM_STACK_CLASS);
                    if (m != null) return m;
                } catch (Throwable ignored) {}
            }
            try {
                for (Method m : CRAFT_ITEM_STACK_CLASS.getMethods()) {
                    if (("asBukkitCopy".equals(m.getName()) || "asCraftMirror".equals(m.getName())) && m.getParameterCount() == 1) {
                        return m;
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        }

        private static Field findFieldSafely(Class<?> clazz, String... names) {
            if (clazz == null) return null;
            for (String name : names) {
                try {
                    Field f = CraftBukkitReflection.findField(clazz, name);
                    if (f != null) return f;
                } catch (Throwable ignored) {}
            }
            return null;
        }

        private final ArgumentParser<C, ProtoItemStack> parser;

        ModernParser() {
            if (ITEM_INPUT_CLASS == null || ARGUMENT_ITEM_STACK_CLASS.get() == null) {
                throw new IllegalStateException("ModernParser reflection failed to find ItemInput or ArgumentItemStack");
            }
            this.parser = this.createParser();
        }

        @SuppressWarnings("unchecked")
        private ArgumentParser<C, ProtoItemStack> createParser() {
            final Supplier<ArgumentType<Object>> inst = () -> {
                final Constructor<?> ctr = ARGUMENT_ITEM_STACK_CLASS.get().getDeclaredConstructors()[0];
                try {
                    if (ctr.getParameterCount() == 0) {
                        return (ArgumentType<Object>) ctr.newInstance();
                    } else {
                        // 1.19+
                        return (ArgumentType<Object>) ctr.newInstance(CommandBuildContextSupplier.commandBuildContext());
                    }
                } catch (final ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to initialize modern ItemStack parser.", e);
                }
            };
            return new WrappedBrigadierParser<C, Object>(inst)
                    .flatMapSuccess((ctx, itemInput) -> ArgumentParseResult.successFuture(
                            new ModernProtoItemStack(itemInput)));
        }

        @Override
        public @NonNull CompletableFuture<@NonNull ArgumentParseResult<@NonNull ProtoItemStack>> parseFuture(
                final @NonNull CommandContext<@NonNull C> commandContext,
                final @NonNull CommandInput commandInput
        ) {
            return this.parser.parseFuture(commandContext, commandInput);
        }

        @Override
        public @NonNull SuggestionProvider<C> suggestionProvider() {
            return this.parser.suggestionProvider();
        }

        private static final class ModernProtoItemStack implements ProtoItemStack {

            private final Object itemInput;
            private final Material material;
            private final boolean hasExtraData;

            ModernProtoItemStack(final @NonNull Object itemInput) {
                this.itemInput = itemInput;
                try {
                    Object item = ITEM_FIELD != null ? ITEM_FIELD.get(itemInput) : null;
                    if (HOLDER_CLASS != null && HOLDER_CLASS.isInstance(item) && VALUE_METHOD != null) {
                        item = VALUE_METHOD.invoke(item);
                    }
                    Method getMat = GET_MATERIAL_METHOD.get();
                    this.material = getMat != null && item != null ? (Material) getMat.invoke(null, item) : Material.AIR;
                    final Object extraData = EXTRA_DATA_FIELD != null ? EXTRA_DATA_FIELD.get(itemInput) : null;
                    if (extraData == null || (NBT_TAG_CLASS != null && NBT_TAG_CLASS.isInstance(extraData))) {
                        this.hasExtraData = extraData != null;
                    } else {
                        final List<Method> isEmptyMethod = Arrays.stream(extraData.getClass().getMethods())
                            .filter(it -> it.getParameterCount() == 0 && it.getReturnType().equals(boolean.class))
                            .collect(Collectors.toList());
                        if (isEmptyMethod.size() == 1) {
                            this.hasExtraData = !(boolean) isEmptyMethod.get(0).invoke(extraData);
                        } else {
                            this.hasExtraData = true;
                        }
                    }
                } catch (final ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public @NonNull Material material() {
                return this.material;
            }

            @Override
            public boolean hasExtraData() {
                return this.hasExtraData;
            }

            @Override
            public @NonNull ItemStack createItemStack(final int stackSize) {
                try {
                    if (CREATE_ITEM_STACK_METHOD != null && AS_BUKKIT_STACK_METHOD != null) {
                        final Object nmsItemStack = CREATE_ITEM_STACK_METHOD.getParameterCount() == 1
                                ? CREATE_ITEM_STACK_METHOD.invoke(this.itemInput, stackSize)
                                : CREATE_ITEM_STACK_METHOD.invoke(this.itemInput, stackSize, true);
                        return (ItemStack) AS_BUKKIT_STACK_METHOD.invoke(null, nmsItemStack);
                    }
                } catch (final InvocationTargetException ex) {
                    final Throwable cause = ex.getCause();
                    if (cause instanceof CommandSyntaxException) {
                        throw new IllegalArgumentException(cause.getMessage(), cause);
                    }
                } catch (Throwable ignored) {
                }
                return new ItemStack(this.material, stackSize);
            }
        }
    }

    private static final class LegacyParser<C> implements ArgumentParser.FutureArgumentParser<C, ProtoItemStack>,
            BlockingSuggestionProvider.Strings<C> {

        private final ArgumentParser<C, ProtoItemStack> parser = new MaterialParser<C>()
                .mapSuccess((ctx, material) -> CompletableFuture.completedFuture(new LegacyProtoItemStack(material)));

        @Override
        public @NonNull CompletableFuture<@NonNull ArgumentParseResult<@NonNull ProtoItemStack>> parseFuture(
                final @NonNull CommandContext<@NonNull C> commandContext,
                final @NonNull CommandInput commandInput
        ) {
            return this.parser.parseFuture(commandContext, commandInput);
        }

        @Override
        public @NonNull Iterable<@NonNull String> stringSuggestions(final @NonNull CommandContext<C> commandContext,
                                                                    final @NonNull CommandInput input) {
            return Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(value -> value.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toList());
        }

        private static final class LegacyProtoItemStack implements ProtoItemStack {

            private final Material material;

            private LegacyProtoItemStack(final @NonNull Material material) {
                this.material = material;
            }

            @Override
            public @NonNull Material material() {
                return this.material;
            }

            @Override
            public boolean hasExtraData() {
                return false;
            }

            @Override
            public @NonNull ItemStack createItemStack(final int stackSize) throws IllegalArgumentException {
                if (stackSize > this.material.getMaxStackSize()) {
                    throw new IllegalArgumentException(String.format(
                            "The maximum stack size for %s is %d",
                            this.material,
                            this.material.getMaxStackSize()
                    ));
                }
                return new ItemStack(this.material, stackSize);
            }
        }
    }
}
