import { EmblaCarouselType } from 'embla-carousel';
import useEmblaCarousel from 'embla-carousel-react';
import {
  Dispatch,
  SetStateAction,
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

type IPicketItemProps = {
  slides: string[];
  select: Dispatch<SetStateAction<number>>;
};

export default function PickerItems({ slides, select }: IPicketItemProps) {
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
    [slideCount, rotationOffset, totalRadius],
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

    api.on('pointerUp', emblaApi => {
      const { scrollTo, target, location } = emblaApi.internalEngine();
      const diffToTarget = target.get() - location.get();
      const factor = Math.abs(diffToTarget) < WHEEL_ITEM_SIZE / 2.5 ? 10 : 0.1;
      const distance = diffToTarget * factor;
      scrollTo.distance(distance, true);
    });

    api.on('select', selectPlaylist);

    api.on('scroll', rotateWheel);

    api.on('reInit', emblaApi => {
      inactivateEmblaTransform(emblaApi);
      rotateWheel(emblaApi);
    });

    inactivateEmblaTransform(api);
    rotateWheel(api);
  }, [api, inactivateEmblaTransform, rotateWheel, selectPlaylist]);

  return (
    <div className="flex size-full items-center justify-center text-3xl leading-none">
      <div
        className="flex size-full touch-pan-x items-center overflow-hidden"
        ref={rootNodeRef}
      >
        <div
          className="h-8 w-full touch-none select-none"
          style={{ perspective: 1000 }} // only available in tailwind 4 beta
          ref={ref}
        >
          <div
            className="size-full will-change-transform"
            style={{ transformStyle: 'preserve-3d' }} // only available in tailwind 4 beta
          >
            {slides.map((name, index) => (
              <div
                className="flex size-full items-center justify-center text-center text-lg opacity-0"
                style={{ backfaceVisibility: 'hidden' }} // only available in tailwind 4 beta
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
