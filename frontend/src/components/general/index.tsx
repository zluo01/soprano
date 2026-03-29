import { useSuspenseQuery } from '@tanstack/react-query';
import { LoadingAlbums } from '@/components/loading';
import { AlbumGridView, GeneralTagItems } from '@/components/shares';
import { Separator } from '@/components/ui/separator.tsx';
import {
	generalTagAlbumsQueryOptions,
	generalTagQueryOptions,
} from '@/lib/queries';
import type { GeneralTag } from '@/type';

interface IGeneralTagViewProps {
	tag: GeneralTag;
}

export function GeneralTagView({ tag }: IGeneralTagViewProps) {
	const { data } = useSuspenseQuery(generalTagQueryOptions(tag));
	return (
		<div className="w-full pt-2">
			<GeneralTagItems tag={tag} data={data} />
		</div>
	);
}

interface IGeneralTagAlbumViewProps extends IGeneralTagViewProps {
	id: string;
}

export function GeneralTagAlbumsView({ tag, id }: IGeneralTagAlbumViewProps) {
	const { data, isLoading } = useSuspenseQuery(
		generalTagAlbumsQueryOptions(tag, id),
	);

	return (
		<>
			<div className="px-6 py-3">
				<Separator />
			</div>
			{isLoading ? (
				<LoadingAlbums />
			) : (
				<div className="w-full pt-2 duration-200 animate-in slide-in-from-right-1/2 sm:animate-none">
					<AlbumGridView albums={data!} />
				</div>
			)}
		</>
	);
}
