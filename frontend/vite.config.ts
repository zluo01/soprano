import babel from '@rolldown/plugin-babel';
import tailwindcss from '@tailwindcss/vite';
import { tanstackRouter } from '@tanstack/router-plugin/vite';
import react, { reactCompilerPreset } from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { VitePWA, type VitePWAOptions } from 'vite-plugin-pwa';

const manifestForPlugIn: Partial<VitePWAOptions> = {
	registerType: 'autoUpdate',
	includeAssets: [
		'favicon.ico',
		'apple-touch-icon-180x180.png',
		'maskable-icon-512x512.png',
	],
	workbox: {
		runtimeCaching: [
			{
				urlPattern: /\/covers\/.*\.webp$/,
				handler: 'CacheFirst',
				options: {
					cacheName: 'cover-images',
					expiration: {
						maxEntries: 500,
						maxAgeSeconds: 60 * 60 * 24 * 30,
					},
				},
			},
		],
	},
	manifest: {
		id: '/',
		name: 'Soprano',
		short_name: 'Soprano',
		icons: [
			{
				src: 'pwa-64x64.png',
				sizes: '64x64',
				type: 'image/png',
			},
			{
				src: 'pwa-192x192.png',
				sizes: '192x192',
				type: 'image/png',
			},
			{
				src: 'pwa-512x512.png',
				sizes: '512x512',
				type: 'image/png',
			},
			{
				src: 'apple-touch-icon-180x180.png',
				sizes: '180x180',
				type: 'image/png',
				purpose: 'any',
			},
			{
				src: 'maskable-icon-512x512.png',
				sizes: '512x512',
				type: 'image/png',
				purpose: 'maskable',
			},
		],
		theme_color: '#171717',
		background_color: '#f0e7db',
		display: 'standalone',
		scope: '/',
		start_url: '/',
		orientation: 'portrait',
	},
};

// https://vite.dev/config/
export default defineConfig({
	plugins: [
		tanstackRouter({ target: 'react', autoCodeSplitting: true }),
		react(),
		babel({ presets: [reactCompilerPreset()] }),
		tailwindcss(),
		VitePWA(manifestForPlugIn),
	],
	build: {
		target: 'esnext',
	},
	resolve: {
		tsconfigPaths: true,
	},
	server: {
		proxy: {
			'/graphql': {
				target: 'http://localhost:6868',
				changeOrigin: true,
				ws: true,
			},
			'/covers': 'http://localhost:6868',
		},
	},
});
