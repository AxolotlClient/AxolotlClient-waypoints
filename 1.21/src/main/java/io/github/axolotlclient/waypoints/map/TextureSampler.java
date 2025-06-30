/*
 * Copyright © 2025 moehreag <moehreag@gmail.com> & Contributors
 *
 * This file is part of AxolotlClient.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, see the LICENSE file.
 */

package io.github.axolotlclient.waypoints.map;

public class TextureSampler {
	/*private static final Minecraft minecraft = Minecraft.getInstance();

	private static final Object2IntMap<BlockState> sampleCache = new Object2IntOpenHashMap<>();
	private static final Object2ObjectMap<ResourceLocation, NativeImage> atlasCache = new Object2ObjectOpenHashMap<>();

	static {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return AxolotlClientWaypoints.rl("reload_listener");
			}

			@Override
			public void onResourceManagerReload(ResourceManager resourceManager) {
				atlasCache.values().forEach(NativeImage::close);
				atlasCache.clear();
				sampleCache.clear();
				sampleQueue.clear();
				sampling = false;
			}
		});
	}

	private static boolean sampling;
	private static final List<Supplier<CompletableFuture<Integer>>> sampleQueue = new ArrayList<>();

	public static CompletableFuture<Integer> getSample(BlockState state, Level level, BlockPos pos, MapColor.Brightness brightness) {
		if (state.isAir()) {
			return CompletableFuture.completedFuture(0);
		}
		if (sampleCache.containsKey(state)) {
			return CompletableFuture.completedFuture(applyTransforms(sampleCache.getInt(state), state, level, pos, brightness));
		}

		Supplier<CompletableFuture<Integer>> sup = () -> {
			if (sampleCache.containsKey(state)) {
				int cached = sampleCache.getInt(state);
				if (!sampleQueue.isEmpty()) {
					var list = sampleQueue.toArray(Supplier[]::new);
					sampleQueue.clear();
					for (var e : list) {
						e.get();
					}
				}
				sampling = false;
				return CompletableFuture.completedFuture(applyTransforms(cached, state, level, pos, brightness));
			}
			BakedModel model = minecraft.getModelManager().getBlockModelShaper().getBlockModel(state);
			var tex = model.getParticleIcon();
			return downloadAtlas(tex.atlasLocation()).thenApply(i -> sampleSprite(i, tex))
				.thenApply(x -> {
					sampleCache.put(state, (int) x);
					if (!sampleQueue.isEmpty()) {
						var list = sampleQueue.toArray(Supplier[]::new);
						sampleQueue.clear();
						for (var e : list) {
							e.get();
						}
					}
					sampling = false;

					return applyTransforms(x, state, level, pos, brightness);
				});
		};
		if (!sampling) {
			sampling = true;
			return sup.get();
		} else {
			CompletableFuture<Integer> cf = new CompletableFuture<>();
			sampleQueue.add(() -> sup.get().thenApply(i -> {
				cf.complete(i);
				return i;
			}));
			return cf;
		}
	}

	private static int applyTransforms(int color, BlockState state, Level level, BlockPos pos, MapColor.Brightness brightness) {
		int col = minecraft.getBlockColors().getColor(state, level, pos, 0);
		var fluid = state.getFluidState();
		if (!fluid.isEmpty() && fluid.is(FluidTags.WATER)) {
			int b = minecraft.level.getBiome(pos).value().getWaterColor();
			float shade = minecraft.level.getShade(Direction.UP, true);
			int f = ARGB.colorFromFloat(1f, ARGB.redFloat(b) * shade, ARGB.greenFloat(b) * shade, ARGB.blueFloat(b) * shade);
			color = ARGB.color(ARGB.alpha(color), f);
		}
		if (col != -1 && isGrayscaleColor(color)) {
			color = ARGB.color(ARGB.alpha(color), col);
		}
		return ARGB.scaleRGB(color, brightness.modifier);
	}

	private static boolean isGrayscaleColor(int color) {
		return ARGB.red(color) == ARGB.green(color) && ARGB.red(color) == ARGB.blue(color);
	}

	private static CompletableFuture<NativeImage> downloadAtlas(ResourceLocation loc) {
		if (atlasCache.containsKey(loc)) {
			return CompletableFuture.completedFuture(atlasCache.get(loc));
		}
		RenderSystem.assertOnRenderThread();
		var cf = new CompletableFuture<NativeImage>();

		minecraft.getTextureManager().getTexture(loc).
		var gpuTexture = minecraft.getTextureManager().getTexture(loc).getTexture();
		int bufferSize = gpuTexture.getFormat().pixelSize() * gpuTexture.getWidth(0) * gpuTexture.getHeight(0);

		GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture Atlas buffer", 9, bufferSize);
		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

		commandEncoder.copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
			try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {

				int textureWidth = gpuTexture.getWidth(0);
				int textureHeight = gpuTexture.getHeight(0);

				var img = new NativeImage(textureWidth, textureHeight, false);

				for (int n = 0; n < textureHeight; n++) {
					for (int o = 0; o < textureWidth; o++) {
						img.setPixelABGR(o, n, mappedView.data().getInt((o + n * textureWidth) * gpuTexture.getFormat().pixelSize()));
					}
				}

				atlasCache.put(loc, img);
				cf.complete(img);
			}
			gpuBuffer.close();
		}, 0);
		return cf;
	}

	private static int sampleSprite(NativeImage image, TextureAtlasSprite sprite) {
		int sampled = 0;

		for (int n = sprite.getY(); n < sprite.getY() + sprite.contents().height(); n++) {
			for (int o = sprite.getX(); o < sprite.getX() + sprite.contents().width(); o++) {
				int c = image.getPixel(o, n);
				sampled = getColorAvg(sampled, c);
			}
		}
		return sampled;
	}

	private static int getColorAvg(int a, int b) {
		if (a == 0) {
			return b;
		} else if (ARGB.alpha(b) == 0) {
			return a;
		} else {
			return ARGB.average(a, b);
		}
	}*/
}
