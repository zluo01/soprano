import type { EmblaCarouselType } from 'embla-carousel';
import useEmblaCarousel from 'embla-carousel-react';
import {
	type Dispatch,
	type SetStateAction,
	useCallback,
	useEffect,
	useRef,
} from 'react';

const CIRCLE_DEGREES = 360;
const WHEEL_ITEM_SIZE = 32;
const WHEEL_ITEM_COUNT = 18;
const WHEEL_ITEMS_IN_VIEW = 4;

const WHEEL_ITEM_RADIUS = CIRCLE_DEGREES / WHEEL_ITEM_COUNT;
const IN_VIEW_DEGREES = WHEEL_ITEM_RADIUS * WHEEL_ITEMS_IN_VIEW;
const WHEEL_RADIUS = Math.round(
	WHEEL_ITEM_SIZE / 2 / Math.tan(Math.PI / WHEEL_ITEM_COUNT),
);

const isInView = (wheelLocation: number, slidePosition: number): boolean =>
	Math.abs(wheelLocation - slidePosition) < IN_VIEW_DEGREES;

const setSlideStyles = (
	api: EmblaCarouselType,
	index: number,
	loop: boolean,
	slideCount: number,
	totalRadius: number,
): void => {
	const slideNode = api.slideNodes()[index];
	const wheelLocation = api.scrollProgress() * totalRadius;
	const positionDefault = api.scrollSnapList()[index] * totalRadius;
	const positionLoopStart = positionDefault + totalRadius;
	const positionLoopEnd = positionDefault - totalRadius;

	let inView = false;
	let angle = index * -WHEEL_ITEM_RADIUS;

	if (isInView(wheelLocation, positionDefault)) {
		inView = true;
	}

	if (loop && isInView(wheelLocation, positionLoopEnd)) {
		inView = true;
		angle = -CIRCLE_DEGREES + (slideCount - index) * WHEEL_ITEM_RADIUS;
	}

	if (loop && isInView(wheelLocation, positionLoopStart)) {
		inView = true;
		angle = -(totalRadius % CIRCLE_DEGREES) - index * WHEEL_ITEM_RADIUS;
	}

	if (inView) {
		slideNode.style.opacity = '1';
		slideNode.style.transform = `translateY(-${
			index * 100
		}%) rotateX(${angle}deg) translateZ(${WHEEL_RADIUS}px)`;
	} else {
		slideNode.style.opacity = '0';
		slideNode.style.transform = 'none';
	}
};

const setContainerStyles = (
	api: EmblaCarouselType,
	wheelRotation: number,
): void => {
	api.containerNode().style.transform = `translateZ(${WHEEL_RADIUS}px) rotateX(${wheelRotation}deg)`;
};

type IPickerItemProps = {
	slides: string[];
	select: Dispatch<SetStateAction<number>>;
};

export default function PickerItems({ slides, select }: IPickerItemProps) {
	const [ref, api] = useEmblaCarousel({
		loop: false,
		axis: 'y',
		dragFree: true,
		containScroll: false,
		watchSlides: false,
	});
	const slideCount = slides.length;
	const rootNodeRef = useRef<HTMLDivElement>(null);
	const totalRadius = slideCount * WHEEL_ITEM_RADIUS;
	const rotationOffset = WHEEL_ITEM_RADIUS;

	const inactivateEmblaTransform = useCallback((api: EmblaCarouselType) => {
		if (!api) return;
		const { translate, slideLooper } = api.internalEngine();
		translate.clear();
		translate.toggleActive(false);
		slideLooper.loopPoints.forEach(({ translate }) => {
			translate.clear();
			translate.toggleActive(false);
		});
	}, []);

	const rotateWheel = useCallback(
		(api: EmblaCarouselType) => {
			const rotation = slideCount * WHEEL_ITEM_RADIUS - rotationOffset;
			const wheelRotation = rotation * api.scrollProgress();
			setContainerStyles(api, wheelRotation);
			api.slideNodes().forEach((_, index) => {
				setSlideStyles(api, index, false, slideCount, totalRadius);
			});
		},
		[slideCount, totalRadius],
	);

	const selectPlaylist = useCallback(
		(api: EmblaCarouselType) => {
			if (!api) return;
			const currentSlide = api.selectedScrollSnap();
			select(currentSlide);
		},
		[select],
	);

	useEffect(() => {
		if (!api) return;

		const onPointerUp = (emblaApi: EmblaCarouselType) => {
			const { scrollTo, target, location } = emblaApi.internalEngine();
			const diffToTarget = target.get() - location.get();
			const factor = Math.abs(diffToTarget) < WHEEL_ITEM_SIZE / 2.5 ? 10 : 0.1;
			const distance = diffToTarget * factor;
			scrollTo.distance(distance, true);
		};

		const onReInit = (emblaApi: EmblaCarouselType) => {
			inactivateEmblaTransform(emblaApi);
			rotateWheel(emblaApi);
		};

		api.on('pointerUp', onPointerUp);
		api.on('select', selectPlaylist);
		api.on('scroll', rotateWheel);
		api.on('reInit', onReInit);

		inactivateEmblaTransform(api);
		rotateWheel(api);

		return () => {
			api.off('pointerUp', onPointerUp);
			api.off('select', selectPlaylist);
			api.off('scroll', rotateWheel);
			api.off('reInit', onReInit);
		};
	}, [api, inactivateEmblaTransform, rotateWheel, selectPlaylist]);

	return (
		<div className="flex size-full items-center justify-center text-3xl leading-none">
			<div
				className="flex size-full touch-pan-x items-center overflow-hidden"
				ref={rootNodeRef}
			>
				<div
					className="h-8 w-full touch-none select-none perspective-distant"
					ref={ref}
				>
					<div className="will-change-transform size-full transform-3d">
						{slides.map((name, index) => (
							<div
								className="flex size-full items-center justify-center text-center text-lg opacity-0 backface-hidden"
								key={index}
							>
								{name}
							</div>
						))}
					</div>
				</div>
			</div>
		</div>
	);
}
