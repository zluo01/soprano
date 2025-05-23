import { Button } from '@/components/ui/button.tsx';
import {
  Carousel,
  type CarouselApi,
  CarouselContent,
  CarouselItem,
} from '@/components/ui/carousel.tsx';
import { cn } from '@/lib/utils.ts';
import { ComponentProps, ReactNode, useState } from 'react';

export type SwipeActionProps = {
  action: VoidFunction;
  children: ReactNode;
  className?: string;
};

type SwipeActionsProps = {
  main: ReactNode;
  actions?: SwipeActionProps[];
  className?: string;
};

export function SwipeAction({ main, actions, className }: SwipeActionsProps) {
  const [api, setApi] = useState<CarouselApi>();

  return (
    <Carousel
      opts={{
        align: 'start',
      }}
      setApi={setApi}
    >
      <CarouselContent className={cn('gap-0', className)}>
        <CarouselItem className="w-full">{main}</CarouselItem>
        {actions && (
          <CarouselItem className="flex basis-1/3 flex-row flex-nowrap items-center justify-end overflow-hidden pl-0">
            {actions.map(({ action, children, className }, index) => (
              <Action
                key={index}
                className={className}
                onClick={action}
                close={() => api?.scrollTo(0)}
              >
                {children}
              </Action>
            ))}
          </CarouselItem>
        )}
      </CarouselContent>
    </Carousel>
  );
}

interface SwipeActionActionsProps extends ComponentProps<'button'> {
  close: VoidFunction;
}

const Action = ({
  className,
  children,
  onClick,
  close,
  ...props
}: SwipeActionActionsProps) => {
  return (
    <Button
      className={cn(
        'rounded-none rounded-r-md border-0 text-white',
        'z-1 flex aspect-square h-full flex-col items-center justify-center',
        'last:z-0 last:-ml-1.5',
        className,
      )}
      onClick={e => {
        onClick?.(e);
        if (!e.defaultPrevented) {
          close();
          (document.activeElement as HTMLElement | null)?.blur();
        }
      }}
      {...props}
    >
      {children}
    </Button>
  );
};
