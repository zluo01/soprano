import type { ReactElement } from 'react';
import { constructImg } from '@/lib/queries';
import { cn } from '@/lib/utils.ts';
import type { IImageLoaderProps } from '@/type';

function Cover({
	albumId,
	style,
	width,
	height,
	alt,
}: IImageLoaderProps): ReactElement {
	return (
		<img
			className={cn('aspect-square h-auto max-w-full object-cover', style)}
			src={constructImg(width, height, albumId)}
			width={width}
			height={height}
			alt={alt}
			loading="lazy"
			crossOrigin="anonymous"
			onError={(e) => {
				e.currentTarget.onerror = null; // Prevent infinite loop
				e.currentTarget.src = '/images/default.avif';
			}}
		/>
	);
}

export default Cover;
