import { afterEach, describe, expect, it, vi } from 'vitest';
import {
	playAlbum,
	playSongInAtPosition,
	removeSongFromQueue,
} from './playback';

// Mock the request function to track calls
const mockRequest = vi.fn().mockResolvedValue({});
vi.mock('./utils.ts', () => ({
	request: (...args: unknown[]) => mockRequest(...args),
}));

afterEach(() => {
	mockRequest.mockClear();
});

describe('playSongInAtPosition', () => {
	it('should send request when position is 0', async () => {
		await playSongInAtPosition(0);
		expect(mockRequest).toHaveBeenCalledOnce();
	});

	it('should send request when position is a positive number', async () => {
		await playSongInAtPosition(5);
		expect(mockRequest).toHaveBeenCalledOnce();
	});

	it('should not send request when position is undefined', async () => {
		await playSongInAtPosition(undefined);
		expect(mockRequest).not.toHaveBeenCalled();
	});
});

describe('removeSongFromQueue', () => {
	it('should send request when position is 0', async () => {
		await removeSongFromQueue(0);
		expect(mockRequest).toHaveBeenCalledOnce();
	});

	it('should send request when position is a positive number', async () => {
		await removeSongFromQueue(3);
		expect(mockRequest).toHaveBeenCalledOnce();
	});

	it('should not send request when position is undefined', async () => {
		await removeSongFromQueue(undefined);
		expect(mockRequest).not.toHaveBeenCalled();
	});
});

describe('playAlbum', () => {
	it('should send request when id is 0', async () => {
		await playAlbum(0);
		expect(mockRequest).toHaveBeenCalledOnce();
	});

	it('should send request when id is a positive number', async () => {
		await playAlbum(42);
		expect(mockRequest).toHaveBeenCalledOnce();
	});

	it('should not send request when id is undefined', async () => {
		await playAlbum(undefined);
		expect(mockRequest).not.toHaveBeenCalled();
	});
});
